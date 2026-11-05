package com.example.demo.controller;

import java.net.URI;
import org.springframework.util.MultiValueMap;
import com.example.demo.dto.EmployeePageResponse;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.service.EmployeeJpaService;

/**
 * SPR-W5-D2-02～04: 課題用Profileでは同じ社員APIをJPAのServiceへ接続する。
 * SPR-W5-D5-01/03: HTTP入力・@Valid・成功Status/Locationを担当し、保存や検索の判断はServiceへ委譲する。
 * SPR-W5-D5-02: 不正DTOはService呼出し前に拒否し、400/404の共通応答はAdviceへ任せる。
 */
@RestController
@Profile("spr-w5-d2 & !spr-w6-d3")
public class Week5Day2EmployeeController {
    private final EmployeeJpaService service;

    public Week5Day2EmployeeController(EmployeeJpaService service) {
        this.service = service;
    }

    @GetMapping("/employees")
    public Object employees(
            @RequestParam(value = "name", required = false) String name,
            @RequestParam(value = "department", required = false) String department,
            @RequestParam MultiValueMap<String,String> params) {
        var options=EmployeeListOptions.from(params);
        // SPR-W5-D4-03: page/size指定時だけページ情報を返し、従来の配列形式を維持する。
        if(options.paged()) return EmployeePageResponse.from(service.findPage(name,department,options.pageable()));
        return service.findAll(name,department,options.sort());
    }

    @GetMapping("/employees/{id}")
    public EmployeeResponse employee(@PathVariable("id") long id) {
        return service.findById(id);
    }

    /** SPR-W5-D2-03: 入力DTOを受け、保存後Entityを公開用DTOへ変換して201で返す。 */
    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        var response = service.create(request.name(), request.departmentId());
        return ResponseEntity.created(URI.create("/employees/" + response.id())).body(response);
    }

    @PutMapping("/employees/{id}")
    public EmployeeResponse update(@PathVariable("id") long id,
            @Valid @RequestBody EmployeeRequest request) {
        return service.update(id, request.name(), request.departmentId());
    }

    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
