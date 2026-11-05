package com.example.demo.training.relations;

import java.util.List;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

/** SPR-W6-D2-02～05: 課題用API。通常取得とfetch joinで同じ公開DTOを返す。 */
@RestController
@Profile("spr-w6-d2")
public class DepartmentRelationController {
    private final DepartmentRelationService service;
    public DepartmentRelationController(DepartmentRelationService service) { this.service=service; }
    @GetMapping("/training/departments")
    public ResponseEntity<List<DepartmentResponse>> departments(@RequestParam(defaultValue="plain") String fetch) {
        if (!fetch.equals("plain") && !fetch.equals("join"))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"fetchはplainまたはjoinを指定してください。");
        var result=service.findAll(fetch.equals("join"));
        return ResponseEntity.ok().header("X-Training-After-Load-Selects",String.valueOf(result.afterLoad()))
            .header("X-Training-Total-Selects",String.valueOf(result.total()))
            .header("X-Training-Initially-Loaded",result.loaded().toString()).body(result.body());
    }
}
