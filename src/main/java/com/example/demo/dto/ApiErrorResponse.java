package com.example.demo.dto;

import java.util.Map;

/** SPR-W4-D4-03: 400/404とも同じ公開形式。項目別エラーがなければerrorsは空。 */
public record ApiErrorResponse(int status, String message, Map<String, String> errors) {
}
