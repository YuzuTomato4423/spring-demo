# SPR-1008-01～04 外部設定

Java 17 / Spring Boot 4.1.1。通常の /hello は app.message を AppProperties から受け取る。
Profile未指定: Hello Spring!、dev: Hello Spring! [dev]、prod: Hello Spring! [prod]。
表示差分は環境識別のための学習用であり、本番サービスへの接続は行わない。

設定: application.properties（共通）→ application-dev/prod.properties（Profile別上書き）
注入: AppConfig の @EnableConfigurationProperties → AppProperties → HelloController → /hello
message は表示文、environment-name は環境ラベル、sample-password は外部注入のサンプル。
実行時の --app.message=... による上書きでも、コード変更なしでHTTP応答が変化する。

Spring ToolsでSPR-1008-01、02、03-dev、03-prod、04.launchをJava Applicationとして実行。
Launcherは実サーバーを空きポートで起動し、HTTP GET /hello と設定Beanを照合して正常終了する。
通常起動のComponent Scan範囲外にLauncherを置き、通常アプリからは実習を自動実行しない。

## 秘密情報

app.sample-password=${SPR1008_SAMPLE_PASSWORD:} として環境変数から取得する。未設定は空文字。
実習では架空のサンプルのみ使用し、実パスワード、APIキー、個人情報は入力しない。
値の直接記載・共有・ログ出力で本物の情報が漏れることを防ぐため。
本番では管理された環境変数/シークレット管理機構から渡し、Java、設定ファイル、共有launch、
Git、スクリーンショットに実値を保存しない。値は認証や外部接続に使用しない。
04のサンプル注入はローカルの実行環境のみで行う。共有launchにはサンプル値も記載しない。
ログには configured=true/false のみ出力。AppProperties.toString()も値を伏せる。

過去のSPR-1007はBootを使わないため、実習構成の@PropertySourceで共通設定を読み込む。

公式参照: https://docs.spring.io/spring-boot/reference/features/external-config.html
