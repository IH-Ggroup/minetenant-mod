# minetenant-mod
このリポジトリは、Minecraft java edition の開発をするためのリポジトリです。
## このリポジトリにあるmod
- callAPI : APIの疎通確認を行うmod
- MinetenantClient : Minetenant用のmod
- MinetenantServer : Minetenant用のServer mod
## Server設定

| キー | 内容 |
| --- | --- |
| `hono.base-url` | Honoのbase URL(endpointは含めない。例: `http://localhost:8787`) |
| `hono.connect-timeout-seconds` | 接続タイムアウト(1秒以上) |
| `hono.read-timeout-seconds` | 読み取りタイムアウト(1秒以上) |
| `hono.poll-interval-seconds` | poll間隔(2秒以上、推奨2〜3秒) |

不足・不正な値があると、キー名付きのエラーを出してサーバーの起動を止めます。

### Fabric Server token
Minecraft専用API(`/api/v1/minecraft/**`)の認証に使うtokenは、設定ファイルではなく環境変数で渡します。
Dedicated Serverだけに設定し、Client JAR・Web・Git・ログには入れないでください。

| 環境変数 | 内容 |
| --- | --- |
| `MINETENANT_FABRIC_SERVER_TOKEN` | backendの同名の環境変数と同じ値。64〜256文字の空白なしASCII |

- 生成例: `openssl rand -hex 32`
- 未設定でも起動できますが、Minecraft APIは呼びません。形式が不正なら起動を止めます。
- 以前の`hono.server-token`は使いません。残っていても無視されます。
## 今回使用する環境
- Minecraft Version: 1.21.11
- ModLoader: [Fabric](https://fabricmc.net/use/)
## 必須依存mod
- [MaLiLib](https://modrinth.com/mod/malilib) by masa

## 開発環境セットアップ
- 使用するIDE: IntelliJ IDEA
    - [公式サイト](https://www.jetbrains.com/ja-jp/idea/download/#section=windows)
- 使用するJDK: マイクラのバージョンに応じて、以下のJDKを使用してください。
    - [Adoptium](https://adoptium.net/ja/)
    - [Oracle](https://www.oracle.com/jp/java/technologies/downloads/)
- Minecraft Version / JDK Version
    - ~ 1.16.5 / JDK 8
    - 1.17 ~ 1.17.1 / JDK 16
    - 1.18 ~ 1.20.4 / JDK 17
    - 1.20.5 ~ 1.21.1 / JDK 21
    - 26.1 ~ / JDK 25
