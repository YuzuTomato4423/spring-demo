package com.example.demo.training.rollback;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.demo.repository.EmployeeJpaRepository;
import com.example.demo.service.EmployeeJpaService;

/** SPR-W5-D4-02: 明示した学習用Profileだけで有効になる、別Beanの検証処理。 */
@Service
@Profile("spr-w5-d4-rollback")
public class EmployeeRollbackExperiment {
    private static final Logger log=LoggerFactory.getLogger(EmployeeRollbackExperiment.class);
    private final EmployeeJpaService service;
    private final EmployeeJpaRepository repository;
    public EmployeeRollbackExperiment(EmployeeJpaService service,EmployeeJpaRepository repository) {
        this.service=service; this.repository=repository;
    }
    @Transactional
    public void updateThenFail(long id,String name,int departmentId) {
        service.update(id,name,departmentId);
        repository.flush();
        // SPR-W5-D4-02: SQL発行後でもcommit前なら戻ることを学ぶための検証用例外。
        // 同じBean内呼出しを避け、例外を握り潰さずトランザクションProxyの外へ伝播させる。
        log.info("SPR-W5-D4-02 UPDATE flushed: id={}, temporaryName={}, temporaryDepartment={}",id,name,departmentId);
        throw new VerificationException("SPR-W5-D4-02: 検証用例外（UPDATE flush後・commit前）");
    }
    public static class VerificationException extends RuntimeException {
        public VerificationException(String message) { super(message); }
    }
}
