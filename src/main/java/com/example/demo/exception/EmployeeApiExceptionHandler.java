package com.example.demo.exception;

import java.util.Map;
import java.util.TreeMap;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import com.example.demo.controller.Week5Day2EmployeeController;
import com.example.demo.controller.Week4Day2EmployeeController;
import com.example.demo.controller.Week4Day3EmployeeController;
import com.example.demo.dto.ApiErrorResponse;

/**
 * SPR-W4-D4-03: エラー形式とHTTP Statusの変換を一箇所へまとめ、Controller間の重複をなくす。
 * SPR-W5-D5-02/03: JPA構成でもValidation例外を400、Serviceの社員未存在を404へ変換する。
 * status/message/errorsを揃える責務だけを持ち、DB更新や業務判定は行わない。
 * クライアントは400/404を同じ構造で読める。業務判定はService、HTTP変換はAdviceが担当する。
 * 対象を実データの社員APIへ限定し、D1の仮APIや他の学習用Controllerへ影響させない。
 */
@RestControllerAdvice(assignableTypes = {
        Week4Day2EmployeeController.class, Week4Day3EmployeeController.class,
        Week5Day2EmployeeController.class, com.example.demo.controller.Week6Day3EmployeeController.class})
public class EmployeeApiExceptionHandler {
    // SPR-W6-D3-04: 入力形式/制約の不正は400。現在の保存状態と競合するメール重複は409。
    @ExceptionHandler(DuplicateEmployeeEmailException.class)
    public ResponseEntity<ApiErrorResponse> duplicateEmail(DuplicateEmployeeEmailException exception) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ApiErrorResponse(409,exception.getMessage(),Map.of("email",exception.getMessage())));
    }
    // SPR-W6-D3-04: 正のIDでも参照先部署が存在しなければ404。社員未存在と同じ分類にする。
    @ExceptionHandler(DepartmentNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> departmentNotFound(DepartmentNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404,exception.getMessage(),Map.of("departmentId",exception.getMessage())));
    }
    @ExceptionHandler(jakarta.validation.ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> serviceValidation(jakarta.validation.ConstraintViolationException exception) {
        Map<String,String> errors=new TreeMap<>();
        exception.getConstraintViolations().forEach(error->errors.putIfAbsent(error.getPropertyPath().toString(),error.getMessage()));
        return ResponseEntity.badRequest().body(new ApiErrorResponse(400,"入力内容を確認してください。",errors));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> validation(MethodArgumentNotValidException exception) {
        Map<String, String> errors = new TreeMap<>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(400, "入力内容を確認してください。", errors));
    }

    @ExceptionHandler(EmployeeNotFoundException.class)
    public ResponseEntity<ApiErrorResponse> notFound(EmployeeNotFoundException exception) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ApiErrorResponse(404, exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponse> businessInput(IllegalArgumentException exception) {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(400, exception.getMessage(), Map.of()));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponse> unreadableBody() {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(400, "JSON形式のリクエスト本文を指定してください。", Map.of()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> invalidType() {
        return ResponseEntity.badRequest()
                .body(new ApiErrorResponse(400, "IDは整数で指定してください。", Map.of()));
    }
}
