package com.example.demo.controller;

import java.util.List;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EmployeeResponse;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.service.EmployeeService;

/** D2の実データAPI。D1の仮APIはw4d1プロファイルで切り替える。 */
@RestController
@Profile("!w4d1 & !spr-w5-d2")
public class Week4Day2EmployeeController {
    private final EmployeeService service;
    private final EmployeeMapper mapper;

    public Week4Day2EmployeeController(EmployeeService service, EmployeeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /**
     * SPR-W4-D2-01/03: ControllerはHTTP受付とDTOへの変換を担当する。
     * 業務ロジックを置かずServiceへ委ね、検索ルールを他の受付からも再利用する。
     * name/departmentは任意query。両方省略したGET /employeesは全件を返す。
     */
    @GetMapping("/employees")
    public List<EmployeeResponse> employees(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "department", required = false) String department) {
        return service.search(name, department).stream().map(mapper::toResponse).toList();
    }

    /**
     * SPR-W4-D2-02: ID検索と未存在の判定は既存Serviceへ委譲する。
     * SPR-W4-D2-04: Employeeを直接公開せずMapperでEmployeeResponseへ変換する。
     */
    @GetMapping("/employees/{id}")
    public EmployeeResponse employee(@PathVariable("id") int id) {
        return mapper.toResponse(service.findById(id));
    }

}
