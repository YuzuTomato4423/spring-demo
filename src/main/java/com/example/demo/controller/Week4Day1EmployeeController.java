package com.example.demo.controller;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** D1の仮API。HTTP引数の受け渡しを確認し、実データ取得はD2でServiceへ委ねる。 */
@RestController
@Profile("w4d1 & !spr-w5-d2")
public class Week4Day1EmployeeController {

    /** SPR-W4-D1-03: URLの{id}をint引数へ渡す。1と2で応答のidが変わる。 */
    @GetMapping("/employees/{id}")
    public EmployeeDetail employee(@PathVariable("id") int id) {
        return new EmployeeDetail(id, "D1学習用の仮応答です。実データ検索は行いません。");
    }

    /**
     * SPR-W4-D1-04: departmentは検索対象を指定する必須query（省略時400）。
     * nameは追加条件なので任意とし、省略時はnullで「名前条件なし」を表す。
     * D1では条件をそのまま返す。絞り込みや社員一覧の取得はD2で実装する。
     */
    @GetMapping("/employees")
    public EmployeeQuery employees(
            @RequestParam("department") String department,
            @RequestParam(value = "name", required = false) String name) {
        return new EmployeeQuery(department, name,
                "D1学習用の仮応答です。query条件の受け渡しを確認します。");
    }

    public record EmployeeDetail(int id, String message) { }
    public record EmployeeQuery(String department, String name, String message) { }
}
