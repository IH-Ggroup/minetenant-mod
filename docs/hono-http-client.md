# Hono HTTPクライアント(Server側)

`MinetenantServer/.../http/` にある、ServerからHonoへ通信する共通処理の説明。
個別の連携・購入APIは、ここに載せて作る。ClientからHonoへ直接通信はしない。

## 構成

| クラス | 役割 |
| --- | --- |
| `HonoApiClient` | 非同期のGET/POST。同時実行数の上限とServer停止時の終了処理を持つ |
| `ApiResult` | GETの結果。`Success` / `Failure` |
| `PostOutcome` | POSTの結果。`Success` / `BusinessError` / `Unknown` / `NotSent` |
| `ApiError` | 失敗の種類。messageは固定文言で、URL・本文・tokenを含めない |
| `ServerAuthHeaderProvider` | Server認証headerとrequest ID headerの名前をまとめる |
| `JsonCodec` | `{ "data": T }` の変換、リクエスト本文のJSON化、エラー本文の読み取り |

## 使い方

```java
// GET: 結果は ApiResult
client.get("/api/v1/...", Item.class).thenAcceptAsync(result -> { ... }, server);

// POST: requestIdは呼び出し側が持つ
String requestId = ...; // 再送する場合は同じ値を使う
client.post("/api/v1/...", requestId, body, Item.class).thenAcceptAsync(outcome -> {
    switch (outcome) { ... }
}, server);
```

- Futureは例外で完了しない。必ず結果型で完了する。
- worldを触る後処理は、`thenAcceptAsync(handler, server)` でServer threadへ戻す。

## POSTの結果の扱い

POSTは副作用があるため、「失敗」と「結果不明」を分ける。

| 結果 | 意味 | 呼び出し側の対応 |
| --- | --- | --- |
| `Success(data)` | 200/201で、`data` を変換できた | 結果を使う |
| `BusinessError(status, code, message)` | Honoが応答した確定済みのエラー(401・409・422・500など) | `status`ではなく`code`で分岐する。`code`が無い場合はnull |
| `Unknown(reason)` | timeout、送信後の切断、成功statusなのに本文が不正 | 失敗と決めつけない。同じrequestIdで確認・再送する |
| `NotSent(reason)` | 送られていない(token未設定、同時実行数超過、close後、接続できない) | 二重実行にならないので再送してよい |

### 設計上の判断

- **request IDは呼び出し側が持つ。** 自動採番だと、結果不明からの再送で別のIDになり、backendの冪等性(同じIDなら再実行しない)が効かないため。
- **成功statusで本文が不正な場合は`Unknown`。** 処理は済んでいる可能性が高く、失敗にすると二重実行を招くため。
- **500は`BusinessError`にする。** 区別は`status`でできる。再送するかどうかは呼び出し側が、同じrequestIdで判断する。
- **GETは`ApiResult`のまま。** GETは副作用がなく、timeoutしても再取得すればよいので、「結果不明」は不要。型を分けているのは意図的。
- **無限retryはしない。** 再送が必要なら、呼び出し側が回数と間隔を決める。

## ログとtokenの扱い

- tokenはServer設定(`hono.server-token`)だけに置く。Client・packet・ログへ出さない。
- ログに出すのは、結果の種類・status・code・path(クエリ除く)だけ。URL全体、リクエスト本文、エラーの`message`は出さない。
- `BusinessError.toString()` は`message`を含めない。`HonoConfig.toString()` はtokenを伏せる。
- ログと例外のメッセージは英語で書く。

## 未確定事項(仮置き)

backendの `docs/api.md` では、Fabric Server向けの認証・最終pathは「B-CONTRACT-05以降」で未確定。
次の値は仮置きで、`ServerAuthHeaderProvider` だけを直せば差し替えられる。

- 認証header: `Authorization: Bearer <token>`
- request ID header: `X-Request-Id`

契約が確定したら、この2つを確定値に直し、この節を消すこと。
backendのAPIは、本文に `requestId` を持つものがある。その場合、本文の`requestId`とheaderの値は呼び出し側で同じにする。

## テスト

`HonoApiClientTest`(GET)と`HonoApiClientPostTest`(POST)は、JDK標準の`HttpServer`を使ったfake Serverで、status別の結果を確認している。
