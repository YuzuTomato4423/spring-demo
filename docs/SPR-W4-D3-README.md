# SPR-W4-D3-01～04: 社員登録・更新・削除API

Java 17と現行Spring Bootを使用。通常起動ではD2 GET APIとD3更新系APIが有効。
D1の仮APIは従来のw4d1プロファイルで再実行できる。

## Spring Toolsでの再実行

1. `SPR-W4-D3.launch`をJava Applicationとして起動する。localhost:8080、carry-demoの既存6名を使用。
2. `SPR-W4-D3-HTTP.launch`で全シナリオを実行する。
3. 課題別は`SPR-W4-D3-01-post.launch`、`SPR-W4-D3-02-put.launch`、`SPR-W4-D3-03-delete.launch`、`SPR-W4-D3-04-summary.launch`。

HTTPクライアントは実際にAPIへ送信し、受信したStatus・DTO・更新前後を検証する。
専用社員を作成して検証後に削除するため、既存6名を更新・削除しない。
クライアント再実行では採番値が進む。再起動時はインメモリ保存先が初期化される。
通常起動はサンプルなし、carry-demo起動だけが6名を登録する。

## APIと責務

| メソッド | URL | Request body | 成功応答 |
|---|---|---|---|
| POST | /employees | name, departmentId | 201、Location、EmployeeResponse |
| PUT | /employees/{id} | name, departmentId | 200、更新後EmployeeResponse |
| DELETE | /employees/{id} | なし | 204、空本文 |

EmployeeResponseの公開項目はid、name、departmentNameのみ。
POSTのIDはRepositoryで自動採番し、既存IDと衝突せず削除後も再利用しない。
PUTは指定idの名前と部署を置換し、件数・idを変えない。未存在は新規作成しない。
部署は1=開発部、2=営業部。入力受付とHTTP応答はController、業務ルールはService、保存はRepository。
登録成功後だけ既存通知を呼び、更新・削除を登録通知として扱わない。

未存在のPUT/DELETEは404、既存Serviceの名前・部署不正は400。
非数値ID、JSON不正、body省略はSpring MVCが400を返す。
Bean Validationと共通エラー形式はD4課題で追加する。

## コメントと検証

Request DTOとServiceの責務、更新対象idとbodyの役割、201/200/204と削除のVoid本文の理由をコメントした。
実HTTPのCRUD連携、公開DTO、Location、削除後404、入力不正時の保存・通知抑止をテストする。
Repositoryでは既存ID・並行登録の重複防止、更新による件数維持、削除後の非再利用をテストする。
既存MavenテストとD1プロファイルの回帰検証も行う。

## 証跡

対象アプリを前面にし、カーソルを対象外へ移動後、Windows標準Alt+Print Screenでウィンドウを取得。
Windowsクリップボード画像を未加工PNGとして保存する。Computer Use観測画像は証跡に使用しない。
保存後に各PNGを開いて、通常の色味、文字の可読性、カーソルや操作表示がないことを確認する。

バックアップ・変更一覧・復元手順・検証ログ・証跡一覧は指定Java作業フォルダのbackups/reportsに保存する。
