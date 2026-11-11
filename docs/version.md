# modバージョン表記ルール

- バージョンは `MAJOR.MINOR.PATCH` の形式で表記する。
- MAJOR: 後方互換性のない変更があったときに増やす。
- MINOR: 後方互換性のある機能追加があったときに増やす。
- PATCH: 後方互換性のあるバグ修正があったときに増やす。
- `1.0.0` より前は、MINOR を互換性の境界とする。

## ClientとServer

- MAJOR と MINOR は、ClientとServerで必ず同じにする。
- PATCH は、それぞれ独立して増やしてよい。
- ClientとServer間のpacket形式を変える場合は、MINOR 以上を増やし、両方を同時に上げる。
