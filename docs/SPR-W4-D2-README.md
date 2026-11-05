# SPR-W4-D2-01〜04: Serviceを使う社員GET API

通常起動はD2の実データAPI。D1の仮APIは `w4d1` プロファイルで再実行できる。

## 実行

- Eclipseの `SPR-W4-D2.launch`: ポート8080、`carry-demo`で既存6名を登録。
- `SPR-W4-D2-HTTP.launch`: Java製RESTクライアントから実HTTPを送信。
- 課題ごとのHTTP起動設定は一覧／詳細／検索／DTOの結果をConsoleへ表示する。
- サンプル登録は `carry-demo` のみ。通常起動の保存先は空で、一覧は200と `[]`。
- D1は `SPR-W4-D1.launch` （`w4d1`）。D1とD2のControllerは同時に有効にならない。
- 保存先は既存のInMemoryEmployeeRepository。再起動すると保存内容は消える。

## APIと責務

| 課題 | API／実装 | 意図 |
|---|---|---|
| 01 | GET /employees | ControllerはHTTP受付、取得ルールはService |
| 02 | GET /employees/{id} | Service.findByIdへ委譲。存在IDは200、未存在は404 |
| 03 | GET /employees?name=...&department=... | Service.searchで検索し、該当なしは200と空配列 |
| 04 | EmployeeMapper.toResponse | Employeeを直接返さずEmployeeResponseに変換 |

nameは任意の部分一致（英字の大小文字を区別）、departmentは任意の部署名完全一致。
両方指定したときはAND。前後の空白は除去し、null／空文字は条件なしとして全件を返す。
数値でないIDはSpring MVCの引数変換により400。
JSON公開項目は `id`、`name`、`departmentName` の3つ。内部のDepartmentは公開しない。

## サンプルと検証例

1=田中／開発部、2=佐藤／営業部、3=鈴木／開発部、4=高橋／営業部、5=伊藤／開発部、6=山田／開発部。
名前「田」は田中・山田の2名。営業部は佐藤・高橋の2名。
名前「田」＋開発部は2名、名前「田」＋営業部は0名。

DTOで公開形式を分離するため、内部モデルの変更をAPIへ直接波及させず、不要な項目の公開を防ぐ。
検索条件はHTTP以外の受付からも同じルールを再利用できるようServiceへ置く。

## 証跡

Windows標準Alt+Print Screenで前面のEclipseウィンドウを撮影し、クリップボード画像をPNG保存する。
Computer Useの観測画像は証跡へ転用しない。撮影前にカーソルを対象外へ移動し、保存後に全PNGを確認する。
作業領域のreportsにテスト結果、実HTTP応答、証跡一覧、Excel記入用メモ、完了報告を保存する。
