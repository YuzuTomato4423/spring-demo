package com.example.demo.controller;

import java.net.URI;

import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import com.example.demo.dto.EmployeeRequest;
import com.example.demo.dto.EmployeeResponse;
import com.example.demo.mapper.EmployeeMapper;
import com.example.demo.service.EmployeeService;

import jakarta.validation.Valid;

/** D3更新系API。D2のGET APIと併用し、D1の仮APIでは無効にする。 */
@RestController
@Profile("!w4d1 & !spr-w5-d2")
public class Week4Day3EmployeeController {
    private final EmployeeService service;
    private final EmployeeMapper mapper;

    public Week4Day3EmployeeController(EmployeeService service, EmployeeMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    /**
     * SPR-W4-D3-01: Request DTOはJSON入力を受け、登録ルール・保存・通知はServiceへ委ねる。
     * SPR-W4-D3-04: 新規作成なので201。Locationは取得先、本文は公開用DTOを返す。
     * SPR-W4-D4-01: @ValidでDTOの制約を検証し、手書きifの重複・検証漏れを防ぐ。
     */
    @PostMapping("/employees")
    public ResponseEntity<EmployeeResponse> create(@Valid @RequestBody EmployeeRequest request) {
        EmployeeResponse response = mapper.toResponse(service.create(request.name(), request.departmentId()));
        return ResponseEntity.created(URI.create("/employees/" + response.id())).body(response);
    }

    /**
     * SPR-W4-D3-02: URLのidが更新対象、bodyが新しい名前・部署を指定する。IDは変更しない。
     * SPR-W4-D3-04: 更新後DTOを本文で返すため200。未存在の社員は新規作成しない。
     * SPR-W4-D4-01: POSTと同じDTO制約を適用し、検証失敗時はServiceを呼ばない。
     */
    @PutMapping("/employees/{id}")
    public ResponseEntity<EmployeeResponse> update(
            @PathVariable("id") int id, @Valid @RequestBody EmployeeRequest request) {
        return ResponseEntity.ok(mapper.toResponse(service.update(id, request.name(), request.departmentId())));
    }

    /**
     * SPR-W4-D3-03/04: 削除後は返す社員がないため本文はVoid、成功Statusは204。
     * 削除有無はServiceで判定し、未存在を成功として返さない。
     */
    @DeleteMapping("/employees/{id}")
    public ResponseEntity<Void> delete(@PathVariable("id") int id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }

}
