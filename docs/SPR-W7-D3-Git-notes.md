# SPR-W7-D3-01～05 Git / GitHub学習メモ

## SPR-W7-D3-01：学習用commit
現在のSpringプロジェクトを初めてGit管理するための学習用commit。
`git status`で未追跡・変更を確認し、`git add`で内容を選び、`git commit`で保存し、`git log`で履歴を確認する。
ブランチはmain。署名はこのリポジトリだけGitHubのnoreplyを使用する。

## SPR-W7-D3-02：除外理由
targetとIDE設定は再生成可能、ログとH2実DBは実データや端末情報を含み得るため除外する。
環境変数ファイル、ローカル専用・秘密設定、秘密鍵とキーストアも除外する。
共有launchファイルは内容を確認し、秘密情報を含まないものを学習再現用に保持する。
公開可能なapplication設定とMaven Wrapperは追跡する。

## SPR-W7-D3-03：アップロード前の機密確認
Privateでの保存でも、ソース・設定・文書・launchと送信対象履歴を確認する。
実DB、APIキー、実パスワード、個人メールは追跡しない。過去文書の個人パスは伏せ字にした。
H2の空パスワードはローカル学習専用、FAKEと明示したテスト文字列は架空データ。
社員名とexample.testのメールは学習用のダミーデータである。
確認の実行結果と証跡は、リポジトリ外のローカル実績記録に保存する。

## SPR-W7-D3-04：remote先の用途
保存先は https://github.com/YuzuTomato4423/spring-demo 、remote名はorigin、接続方式はHTTPS。
用途はSpring学習成果のPrivate保管と履歴確認。ユーザー指定によりPrivateを維持する。
リポジトリ作成・接続・到達確認の実績は実行後にローカル記録とExcelへ記載する。

## SPR-W7-D3-05：初回push単位
今回の初回push単位は、既存のSpring実装・テスト・課題資料と、除外設定・機密確認メモ。
実装スナップショットと確認メモを意味のある2つのcommitに分け、messageへ課題IDを付ける。
push成功はGitHubのPrivate表示、ファイル一覧、mainのcommit一致を確認してから記録する。

## 今回の対象と翌日対応
対象はSPR-W7-D3-01～05。READMEの概要・技術スタック・起動方法・API説明等の整備は翌日対応。
GitHub公開チェックの該当項目だけを確認済みにし、未実施項目を完了扱いにしない。
