# 購入requestのpacketと重複防止の設計(#45)

## 目的

購入確定1回につき `requestId` を1つ作り、`productId` と一緒にServerへ送る。同じpacketが再送されても1つの操作として扱い、二重購入を防ぐ。

## 範囲

- やること: packet DTOとchannel、Clientの `requestId` 生成と保持、Serverの検証と重複防止、fixtureの購入service
- やらないこと: HonoへのPOST(#46)、結果画面の文言、timeout再試行、記録の永続化
- 避けること: Clientからbuyer UUIDや価格を送る、packetにtoken・password・seller情報を入れる

## 前提

- `PurchaseConfirmState`([PR 82](https://github.com/IH-Ggroup/minetenant-mod/pull/82))が必要。未マージの間は `feature/44-purchase-confirm-gui` から作業ブランチを作る。
- backend契約の `requestId` は `[A-Za-z0-9._:-]{1,100}`、UUID v4推奨。

## 方式の比較

| 案 | 内容 | 判断 |
|---|---|---|
| A. 専用クラス | `PurchaseRequestController` が検証・重複判定・記録を持つ | **採用**。Minecraftに依存せずテストしやすい |
| B. serviceを包む | 重複を防ぐserviceで本物のserviceを包む | 拒否の理由がserviceの戻り値に混ざる |
| C. handlerに直接書く | クラスを増やさない | 単体テストが難しい |

## packet

ClientとServerで同じ定義を持つ。

| DTO | 向き | 項目 |
|---|---|---|
| `PurchaseRequest` | Client→Server | `requestId`(文字列、36文字まで)、`productId`(64文字まで) |
| `PurchaseAccepted` | Server→Client | `requestId`、結果コード |

結果コード:

| コード | 意味 |
|---|---|
| `ACCEPTED` | serviceが受け付けた |
| `INVALID_REQUEST` | IDの形式が不正 |
| `REQUEST_ID_CONFLICT` | 同じ `requestId` を別の商品に使った |
| `BUSY` | そのplayerの別の購入が処理中 |
| `UNKNOWN` | 結果が分からない(例外、timeoutなど) |

## Client

- Minecraftに依存しないクラス(`PurchasePacketClient`)が、確定時に `UUID.randomUUID()` で `requestId` を1回だけ作る。
- 結果が返るまで `requestId` と `productId` を保持する。保持中の確定は無視する。
- 待っていない `requestId` の応答は無視する。
- 切断したら保持を破棄する。時間切れでは破棄せず、再送もしない。
- Serverに購入packetの受け口がない場合は、送らずに失敗を返す。
- 呼び出し元は、今の確認画面を開いている `ProductListFixtureLauncher.openPurchaseConfirm`。結果はdebug logに出すだけにする。

## Server

- `PurchasePacketHandler`: buyer UUIDを `context.player()` から取り、controllerへ渡す。結果はserver threadで送信者だけに返す。切断していれば送らない。
- `PurchaseRequestController`: 検証と重複防止。複数threadから呼ばれても壊れないようにする。
- `PurchaseService`: 非同期(`CompletableFuture`)。今は `FixturePurchaseService` だけで、#46で差し替える。

IDの検証: `requestId` はUUID v4の形式、`productId` は商品詳細と同じ判定。

重複の判定は「player UUID + `requestId`」で行う。

| 届いた要求 | 動き |
|---|---|
| 形式が不正 | `INVALID_REQUEST`。記録しない |
| 同じID・同じ商品が処理中 | 同じ処理の完了を待って同じ結果を返す。serviceは呼ばない |
| 同じID・同じ商品が完了済み | 保存した結果を返す。serviceは呼ばない |
| 同じID・別の商品 | `REQUEST_ID_CONFLICT`。元の記録は変えない |
| 別のID・そのplayerが処理中 | `BUSY`。記録しない |
| 新しいID | serviceを呼ぶ |

serviceの例外と時間切れ(10秒)は `UNKNOWN` として完了扱いにする。同じIDの再送には `UNKNOWN` を返し、もう一度は呼ばない。

## 記録の上限

Server再起動まで残し、次の上限を超えたら古いものから消す。値は定数にする。

| 対象 | 上限 |
|---|---|
| 処理中(playerごと) | 1件(消さない) |
| 完了(playerごと) | 20件 |
| 完了(全体) | 1,000件 |

消えた記録が再送されるとserviceはもう一度呼ばれる。最終的な二重購入の防止はHono側(#46)が担う。

## テスト

- 同じpacketを処理中・完了後に再送しても、serviceの呼び出しは1回
- 同じ `requestId` で別の商品は拒否
- 別playerが同じ `requestId` を使っても、結果が混ざらない
- 処理中の別IDは `BUSY`
- 例外・時間切れは `UNKNOWN`
- 上限を超えると古い完了記録から消え、処理中は残る
- Client: 1操作で `requestId` は1つ、保持中の確定は送らない
- packetのcodec(往復と文字数超過)

別playerへの配信は、2クライアントでの手動確認も行う。

## 確認が必要な点

Issueの指示どおり、実装前にこの設計とテストを @kaketo113 と確認する。
