# SPR-W8-D5 公開確認記録

## SPR-W8-D5-01：Secret／履歴最終確認

最終確認の目的は、公開時に現在ファイルと過去のGit履歴から秘密情報・個人情報・実データを公開しないこと。

- 2026-11-13、公開対象はmainのみ、タグなし。開始時のHEADは `b6e4f329d2bef44643a74d79e08b643406eb4783`。
- 現在の追跡ファイル316件、全6コミット・324種類のblobを点検。
- 秘密鍵、GitHub・AWS・APIトークン、認証情報入りURL、個人端末パス、不要ファイルの混入は検出なし。
- 資格情報に関係する25候補は、環境変数参照、空の演習用H2設定、UUIDによる実行時生成、CSRF取得、標準Wrapperの処理と確認。
- メールアドレスは架空データ用example.test。Gitの著者・コミッターはGitHubユーザー名とnoreplyメールのみ。
- 公開用PNGとSVGを確認。個人名・端末パス・秘密値は写っていない。
- 検査範囲で問題を見つけなかったという結果であり、未知の秘密値まで自動検査だけで保証するものではない。

## SPR-W8-D5-02：Go／No-Go

**判定：Go。** READMEの目的・構成・起動手順・代表API・Security説明、設定例、画像、全テスト、公開対象の履歴を確認した。

2026-11-13 10:01:58 JST、Java 17、Maven Wrapper `clean package` がBUILD SUCCESS。42スイート、全173件、失敗0・エラー0・スキップ0。[実行結果抜粋](validation/w8d5-test-results.txt)は今回の実ログとSurefire XMLの集計。

target、IDE設定、logs、H2データ、.env、ローカル設定、秘密鍵の除外を10例で検証。開始時のJUnit起動設定12ファイルのローカル変更は保持し、今回のcommitに含めない。

## SPR-W8-D5-03：Public切替

公開日時：**2026-11-13 10:17:16 JST**（API変更完了時刻 `2026-11-13T10:17:16.921256+09:00`）。

公開URL：<https://github.com/YuzuTomato4423/spring-demo>

公開時commit：`2a8496050d9f599290e59fa659443071b8770fff`。mainのSHAとREADME・公開確認記録・テスト抜粋をGitHub APIで照合してからPublicへ変更した。

認証ヘッダーもCookieも使わない第三者視点で、公開ページ、README、代表コード、設定例、テスト抜粋、PNG・SVGの8ファイルを取得。すべてHTTP 200で、ローカル内容とSHA-256が一致した。未ログインの画面でもPublic・ファイル一覧・READMEプレビューを確認した。

## SPR-W8-D5-04：公開後最終整理

公開表示の確認後、READMEに公開リポジトリのclone／移動コマンドと公開日時・確認結果を追記した。この記録の未確定だった公開後欄も更新した。ソースコード・依存関係・認証設定の変更はない。

今回の変更は通常commit/pushし、GitHubのmain・公開内容を再照合する。既存JUnit起動設定12ファイルのローカル変更は保持する。画面証跡はWindows標準Alt+Print Screenで取得し、Computer Useの観測画像は使用しない。
