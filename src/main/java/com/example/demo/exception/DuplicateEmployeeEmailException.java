package com.example.demo.exception;
/** SPR-W6-D3-01/04: 保存状態との競合を表す業務例外。HTTPへの変換はAdviceが担当。 */
public class DuplicateEmployeeEmailException extends RuntimeException {
    public DuplicateEmployeeEmailException() { super("このメールは既に登録されています。"); }
    public DuplicateEmployeeEmailException(Throwable cause) { super("このメールは既に登録されています。",cause); }
}
