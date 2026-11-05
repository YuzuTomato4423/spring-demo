# SPR-W7-D1-01～04：MockMvcによるWeb層テスト

対象は既存の`Week6Day3EmployeeController`と`EmployeeApiExceptionHandler`。
Java 17 / Spring Boot 4.1.1。公開API、DTO、DB設定は変更していません。

## 構成と目的

`com.example.demo.week7.day1`に課題ごとのテストと共通設定を配置しました。
`@WebMvcTest`は実際のController・JSON変換・Bean Validation・Adviceを読み込み、
`@MockitoBean`で`EmployeeBusinessService`と`EmployeeJpaService`を置き換えます。
DBへ接続せず、HTTP契約とServiceへの引数を検証します。
`spr-w6-d3`は既存Controllerの有効化に必要なProfileです。
業務ルール、永続化、実HTTPとDBの連携は既存Week6テストが担います。

|課題|テストクラス|検証内容|件数|
|---|---|---|---:|
|SPR-W7-D1-01|Week7Day1GetTests|一覧・詳細GET 200、JSON、検索条件とIDの委譲|2|
|SPR-W7-D1-02|Week7Day1PostValidationTests|POST 201、Locationと本文、5種類のValidation 400、登録Service未呼出し|6|
|SPR-W7-D1-03|Week7Day1NotFoundTests|未存在IDの404、status/message/errorsの構造と値|1|
|SPR-W7-D1-04|Week7Day1CrudTests|PUT 200、DELETE 204・空本文。全4クラスの一括回帰|2|

POSTの不正入力は氏名空白、メール空白、メール形式、255文字メール、部署ID=0。
255文字メールは各ラベル63文字以下・ローカル部64文字として、@Sizeのメッセージを個別に確認します。
各テストは独立したモックの設定を使い、実行順序に依存しません。

## 再実行

EclipseでプロジェクトをRefreshした後、各`.launch`を右クリックしてRun As → 対応するJUnit起動を実行します。

- `SPR-W7-D1-01-JUnit.launch`：GET 2件
- `SPR-W7-D1-02-JUnit.launch`：POST 6件
- `SPR-W7-D1-03-JUnit.launch`：404 1件
- `SPR-W7-D1-04-JUnit.launch`：PUT/DELETE 2件
- `SPR-W7-D1-04-all-JUnit.launch`：タグ`spr-w7-d1`の全11件

プロジェクト直下でMaven Wrapperを使う場合：

```powershell
.\mvnw.cmd '-Dtest=Week7Day1*Tests' test
.\mvnw.cmd package
```

Java 17を使用してください。Mockitoの計測用Java接続を禁止する実行環境ではテストが停止する場合があります。
通常のEclipse/JUnit環境で実行し、失敗をスキップして成功扱いにしないでください。

## 証跡とバックアップ

証跡は作業フォルダの`screenshots/G-W7-D1-SPR`へ、
Windows標準のAlt+Print ScreenからPNGとして保存します。
Computer Useの観測用画像は証跡へ流用しません。
個別3画面と全11件の一括成功画面を保存し、文字・色味・オーバーレイの有無を確認します。

変更前バックアップ、ハッシュ一覧、復元手順は作業フォルダの
`backups/20261102_095837_G-W7-D1-SPR`、実行ログと集計は同名の`reports`フォルダを参照してください。

