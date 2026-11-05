package com.example.demo.entity;

import jakarta.persistence.*;

/** SPR-W5-D1-02/03: 社員をEMPLOYEEへ永続化し、DBによるLongの主キー採番を使う。 */
@Entity
@Table(name = "EMPLOYEE", uniqueConstraints = @UniqueConstraint(name = "UK_EMPLOYEE_EMAIL", columnNames = "EMAIL"))
public class EmployeeEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(name = "NAME", nullable = false) private String name;

    // SPR-W6-D3-01: 正規化済みメールをDBでも一意にし、同時登録の競合を防ぐ。
    // 過去課題のメールなし社員を維持するためNULL可。Day3のServiceでは必須を検証する。
    @Column(name = "EMAIL", length = 254) private String email;
    public EmployeeEntity(String name, String email, DepartmentEntity department) {
        this(name, department); this.email=email;
    }
    public void update(String name, String email, DepartmentEntity department) {
        update(name, department); this.email=email;
    }
    public String getEmail() { return email; }

    // SPR-W5-D3-02 / SPR-W6-D2-01: 社員側が双方向関連の所有側。departmentがFKを管理する。
    // JoinColumnは社員表の外部キーを指定する。共有部署へ削除を伝播させない。
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "DEPARTMENT_ID", nullable = false,
            foreignKey = @ForeignKey(name = "FK_EMPLOYEE_DEPARTMENT"))
    private DepartmentEntity department;

    protected EmployeeEntity() { }
    public EmployeeEntity(String name, DepartmentEntity department) {
        this.name=name; this.department=department;
    }
    /** SPR-W5-D2: 更新でも採番済みIDを維持する。 */
    public void update(String name, DepartmentEntity department) {
        this.name=name; this.department=department;
    }
    public Long getId() { return id; }
    public String getName() { return name; }
    public DepartmentEntity getDepartment() { return department; }
    public Integer getDepartmentId() { return department.getId(); }
}
