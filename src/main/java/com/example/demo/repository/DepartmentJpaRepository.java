package com.example.demo.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.DepartmentEntity;
/** SPR-W5-D3-01: 部署の保存と主キーによる存在確認をDBへ委ねる。 */
public interface DepartmentJpaRepository extends JpaRepository<DepartmentEntity, Integer> {
    // SPR-W6-D2-05: N+1を避け、部署と社員を1 SELECTで取得。LEFTで社員0件の部署も保持する。
    // 一覧DTO用。コレクションfetch joinにPageableは適用しない。
    @org.springframework.data.jpa.repository.Query(
        "select distinct d from DepartmentEntity d left join fetch d.employees order by d.id")
    java.util.List<DepartmentEntity> findAllWithEmployees();
}
