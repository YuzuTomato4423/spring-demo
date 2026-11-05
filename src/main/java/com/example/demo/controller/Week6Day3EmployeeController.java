package com.example.demo.controller;

import java.net.URI;
import jakarta.validation.Valid;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.*;
import com.example.demo.service.*;

/**
 * SPR-W6-D5-01/SPR-W6-D5-03: HTTP入力→Validation→Service→Repository→DB→DTO応答の入口。業務判断はServiceへ渡す。 SPR-W6-D3: Day3専用の入力契約。業務判断とTransactionはServiceへ委譲する。 */
@RestController
@Profile("spr-w6-d3")
public class Week6Day3EmployeeController {
    private final EmployeeBusinessService business;
    private final EmployeeJpaService existing;
    public Week6Day3EmployeeController(EmployeeBusinessService business,EmployeeJpaService existing) {
        this.business=business;this.existing=existing;
    }
    @GetMapping("/employees")
    public Object employees(@RequestParam(value="name",required=false) String name,
            @RequestParam(value="department",required=false) String department,
            @RequestParam MultiValueMap<String,String> params) {
        var options=EmployeeListOptions.from(params);
        if(options.paged()) return EmployeePageResponse.from(existing.findPage(name,department,options.pageable()));
        return existing.findAll(name,department,options.sort());
    }
    @GetMapping("/employees/{id}")
    public BusinessEmployeeResponse employee(@PathVariable("id") long id) { return business.findById(id); }
    @PostMapping("/employees")
    public ResponseEntity<BusinessEmployeeResponse> create(@Valid @RequestBody BusinessEmployeeRequest request) {
        var response=business.create(request);
        return ResponseEntity.created(URI.create("/employees/"+response.id())).body(response);
    }
    @PutMapping("/employees/{id}")
    public BusinessEmployeeResponse update(@PathVariable("id") long id,@Valid @RequestBody BusinessEmployeeRequest request) {
        return business.update(id,request);
    }
    @PostMapping("/employees/transfers")
    public EmployeeTransferResponse transfer(@Valid @RequestBody EmployeeTransferRequest request) {
        return business.transfer(request);
    }
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") long id) {
        existing.delete(id);return ResponseEntity.noContent().build();
    }
}
