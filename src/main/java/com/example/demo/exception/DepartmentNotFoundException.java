package com.example.demo.exception;
/** SPR-W6-D3-02/04: Day3の部署未存在。過去課題の400応答は変更しない。 */
public class DepartmentNotFoundException extends RuntimeException {
    public DepartmentNotFoundException(int id) { super("部署が見つかりません。 id="+id); }
}
