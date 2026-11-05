package com.example.demo.training.rollback;
import java.util.Map;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.example.demo.dto.ApiErrorResponse;
import com.example.demo.dto.EmployeeRequest;
import com.example.demo.service.EmployeeJpaService;
import com.example.demo.training.rollback.EmployeeRollbackExperiment.VerificationException;

/** SPR-W5-D4-02: 通常APIとは別の学習用操作。専用Profileを指定しない起動では登録しない。 */
@RestController
@Profile("spr-w5-d4-rollback")
public class EmployeeRollbackController {
    private static final Logger log=LoggerFactory.getLogger(EmployeeRollbackController.class);
    private final EmployeeRollbackExperiment experiment;
    private final EmployeeJpaService service;
    public EmployeeRollbackController(EmployeeRollbackExperiment experiment,EmployeeJpaService service) {
        this.experiment=experiment; this.service=service;
    }
    @PostMapping("/training/rollback/employees/{id}")
    public ResponseEntity<ApiErrorResponse> rollback(@PathVariable("id") long id,@Valid @RequestBody EmployeeRequest request) {
        var before=service.findById(id);
        try { experiment.updateThenFail(id,request.name(),request.departmentId()); }
        catch(VerificationException expected) {
            // Proxyがrollbackを終えた後、別トランザクションで再取得して照合する。
            var after=service.findById(id);
            if(!before.equals(after)) throw new IllegalStateException("Rollback verification failed");
            log.error("SPR-W5-D4-02 expected verification exception; rollback completed",expected);
            log.info("SPR-W5-D4-02 PASS: before={} / after={}",before,after);
            return ResponseEntity.internalServerError().body(new ApiErrorResponse(500,
                    expected.getMessage(),Map.of()));
        }
        throw new IllegalStateException("Verification exception was not thrown");
    }
}
