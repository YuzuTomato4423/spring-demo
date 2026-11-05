package com.example.demo.entity;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

/**
 * SPR-W5-D3-01: 同じ部署を複数社員が参照するため、部署ID・名称を別Entityへ分離する。
 * 部署名の重複保存や社員ごとの名称不一致を避ける。IDは既存入力の1/2を維持する。
 */
@Entity
@Table(name = "DEPARTMENT")
public class DepartmentEntity {
    @Id private Integer id;
    @Column(name = "NAME", nullable = false) private String name;
    // SPR-W6-D2-01: 逆側。mappedByは所有側EmployeeEntity.departmentのフィールド名。
    // FKは社員表のDEPARTMENT_ID。中間表や削除cascadeは作らず、共有部署を保護する。
    // SPR-W6-D2-02: 通常はLAZYで不要な社員取得を避ける。EAGER比較だけ専用XMLで上書き。
    @OneToMany(mappedBy = "department", fetch = FetchType.LAZY)
    private List<EmployeeEntity> employees = new ArrayList<>();
    public List<EmployeeEntity> getEmployees() { return employees; }
    protected DepartmentEntity() { }
    public DepartmentEntity(Integer id, String name) { this.id=id; this.name=name; }
    public Integer getId() { return id; }
    public String getName() { return name; }
}
