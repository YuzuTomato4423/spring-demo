# SPR-W4-D4-01～03: 入力検証と共通例外処理

既存のJava 17 / Spring Boot 4.1.1と社員APIを使用する。

## Spring Toolsでの再実行

1. `SPR-W4-D4.launch`をJava Applicationとして起動する。localhost:8080、carry-demoの既存6名を使用。
2. `SPR-W4-D4-HTTP.launch`で全シナリオを実行する。
3. 課題別には`SPR-W4-D4-01-validation.launch`、`SPR-W4-D4-02-not-found.launch`、`SPR-W4-D4-03-common.launch`を使う。

Java HTTPクライアントは起動したAPIへ実際に送信し、受信したHTTP StatusとJSONを検証してConsoleへ表示する。
不正入力と未存在IDだけを使用し、一覧を前後比較して既存データが変わっていないことを確認する。

## 入力検証と責務

- EmployeeRequest.name: `@NotBlank`。未指定、null、空文字、空白のみを拒否する。
- EmployeeRequest.departmentId: `@Positive`。0、負数を拒否する。未指定は現行JSON読取設定で400になる。
- POST/PUT: `@Valid @RequestBody`で共通DTOを検証する。失敗時はServiceを呼ばない。
- 部署1=開発部、2=営業部。部署の存在や社員の存在はServiceで判定する。
- Serviceから直接呼ぶ場合の名前・部署検証は維持する。
- 未存在のGET/PUT/DELETEは既存EmployeeNotFoundExceptionを使用する。

## 共通エラー形式

EmployeeApiExceptionHandlerの`@RestControllerAdvice` / `@ExceptionHandler`でHTTP変換を集約する。
DTO制約、JSON不正、本文欠落、数値でないID、Serviceの業務入力不正は400、社員未存在は404。
エラー本文は常に`status`（数値）、`message`（文字列）、`errors`（項目名からメッセージへのオブジェクト）。
項目エラーがなければerrorsは空。例外クラス名、スタックトレース、入力値をJSONへ公開しない。

```json
{"status":400,"message":"入力内容を確認してください。","errors":{"name":"社員名を入力してください。"}}
```

```json
{"status":404,"message":"社員が見つかりません。 id=999999","errors":{}}
```

対象はWeek4Day2EmployeeControllerとWeek4Day3EmployeeController。D1の仮APIと他の学習用Controllerは対象外。
入力形式をDTO、存在判定と業務ルールをService、HTTP変換をAdviceへ分ける理由を課題ID付きでコメントする。
正常時のGET=200 / POST=201+Location / PUT=200 / DELETE=204（本文なし）は従来どおり。

## 検証・証跡

`mvnw.cmd test`で既存課題とD4の実HTTPテストを実行する。400/404の形式、入力境界、保存・更新・通知の抑止を確認する。
証跡は対象アプリを前面表示し、カーソルを対象外へ移動後、Windows標準Alt+Print Screenで取得する。
Windowsクリップボードから未加工PNGで保存し、文字・色味・カーソル・操作表示を各画像で確認する。
Computer Useの観測画像は証跡に使わない。バックアップ・復元手順・検証ログ・証跡一覧は指定Javaフォルダ内に保存する。

参考: [Spring MVC Validation](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-validation.html)、
[Controller Advice](https://docs.spring.io/spring-framework/reference/web/webmvc/mvc-controller/ann-advice.html)。
