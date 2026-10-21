# MineTenant mod
This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.
# 基本ルール
- 変更をcommit,pushする前にユーザーに確認を行うこと。
- コミットメッセージとソースコード内のコメント・メッセージ・説明は、日本語で簡潔に書く。

# ブランチ
- 通常は`develop`から作業ブランチを作り、作業が完了したら`develop`にマージする。
- `main`ブランチはリリース用であり、また、`develop`ブランチにも直接変更を加えないこと。
- ブランチ名には、変更内容がわかる`type`(`feature`、`fix`、`refactor`、`docs`、`chore`、`release`など)を含めること。

例: 
- `feature/add-login`
- `fix/typo-in-readme`
- `refactor/user-service`
- `docs/api-documentation`
- `chore/update-dependencies`
- `release/v1.0.0`

# PR
- PRの内容は基本的にテンプレートに沿った形式で記述すること。
- なるべく長くならないように、人が読みやすい形で記述すること。