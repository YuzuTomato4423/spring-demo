package com.example.demo.repository;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.context.annotation.Profile;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import com.example.demo.entity.EmployeeEntity;

/**
 * SPR-W6-D5-03: ServiceからCRUD・関連検索を受け、Spring Data/HibernateでDBへ委譲する。Testが保存と検索結果を検証する。
 * SPR-W5-D2-01: Spring DataがJpaRepositoryの実装を生成する。主キー型はLong。
 * SPR-W5-D5-01/03: CRUD・検索・ページングをDBへ委譲する永続化層。SQL実装はSpring Data/Hibernateが担う。
 */
@Profile("spr-w5-d2")
public interface EmployeeJpaRepository extends JpaRepository<EmployeeEntity, Long> {
    // SPR-W6-D3-01: Serviceが保存前に一意性を判断するためのDB照会。更新では本人を除外。
    boolean existsByEmail(String email);
    boolean existsByEmailAndIdNot(String email, Long id);
    // SPR-W5-D4-03/04: 既存検索のPageable/Sort版。部署も取得し、OSIVなしでDTOへ変換する。
    @Override @EntityGraph(attributePaths="department") List<EmployeeEntity> findAll(Sort sort);
    @Override @EntityGraph(attributePaths="department") Page<EmployeeEntity> findAll(Pageable pageable);
    @EntityGraph(attributePaths="department") List<EmployeeEntity> findByNameContaining(String name,Sort sort);
    @EntityGraph(attributePaths="department") List<EmployeeEntity> findByDepartment_Name(String department,Sort sort);
    @EntityGraph(attributePaths="department") List<EmployeeEntity> findByNameContainingAndDepartment_Name(String name,String department,Sort sort);
    @EntityGraph(attributePaths="department") Page<EmployeeEntity> findByNameContaining(String name,Pageable pageable);
    @EntityGraph(attributePaths="department") Page<EmployeeEntity> findByDepartment_Name(String department,Pageable pageable);
    @EntityGraph(attributePaths="department") Page<EmployeeEntity> findByNameContainingAndDepartment_Name(String name,String department,Pageable pageable);
    @Override @EntityGraph(attributePaths = "department") List<EmployeeEntity> findAll();
    @Override @EntityGraph(attributePaths = "department") Optional<EmployeeEntity> findById(Long id);
    // SPR-W5-D3-04: Name/Containingから氏名の部分一致SQLをSpring Dataが生成する。
    @EntityGraph(attributePaths = "department")
    List<EmployeeEntity> findByNameContaining(String name);
    @EntityGraph(attributePaths = "department")
    List<EmployeeEntity> findByDepartment_Name(String department);
    // SPR-W5-D3-05: 同名社員を部署で区別するため、氏名部分一致と部署名完全一致をANDで結ぶ。
    // Department_Nameは関連先のnameをたどる。EntityGraphでDTO用の部署もまとめて取得する。
    @EntityGraph(attributePaths = "department")
    List<EmployeeEntity> findByNameContainingAndDepartment_Name(String name, String department);
}
