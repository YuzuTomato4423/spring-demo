# SPR-1009-01〜03 Bean統合レビュー

## 説明（本文400字）

EmployeeService.registerは保存後に通知する。EmployeeRegistrationNotifierのinitialize（@PostConstruct）はDI後に環境名を読み、release（@PreDestroy）は終了後の利用を拒否する。Qualifierは複数の通知Beanから社員登録用を選び、EmployeeNotificationConfigのProfileはdefault・devのコンソール模擬通知とprodのメール模擬通知を切り替える。ScopeはService・Repository・通知Beanをsingletonとして共有し、社員やDTOは登録ごとに生成する。外部設定は共通とProfile別のpropertiesからAppPropertiesへ注入し、表示と通知で同じ環境名を使う。通知方式と設定値を分離するため、環境変更で登録処理を変えずに済む。

## 実行方法

Java 17 / Spring Boot 4.1.1。社員登録は入力検証と保存に成功した後だけ模擬通知する。実メール・Slack APIは呼び出さない。

EclipseでSPR-1009-default.launch、SPR-1009-dev.launch、SPR-1009-prod.launchをJava Applicationとして実行する。default/devはDEV_CONSOLE、prodはPROD_MAILの模擬通知。

EmployeeIntegrationReviewLauncherは127.0.0.1の空きポートで実Bootアプリを起動し、登録・同一保存先・部署検索・Qualifier・singleton・不正登録の通知抑止・実HTTP GET /helloを照合する。Context終了後の利用拒否を検証してからPASSを表示する。専用入口は通常アプリのComponent Scan範囲外。

## コメント品質と既存課題

社員管理の重要クラス・登録・DTO生成・一覧返却・通知の選択/準備/終了に設計意図を記載した。DI構文の読み上げ、重複、古い課題番号中心の説明を整理した。既存のLifecycle/通知/Profile/BPP学習例は専用Contextのまま保持する。

## 証跡

Windows標準Alt+Print ScreenでEclipseウィンドウを取得し、PNGとして保存する。Computer Useの観測画像は使わない。

保存先: <学習作業フォルダ>\screenshots\G-1009-SPR

- SPR-1009-01_startup.png
- SPR-1009-02_code.png
- SPR-1009-02_execution.png
- SPR-1009-03_final-dev.png
- SPR-1009-03_final-prod.png

