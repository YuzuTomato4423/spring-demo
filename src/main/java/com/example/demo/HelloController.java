package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.example.demo.config.AppProperties;

/**
 * SPR-W4-D1-02: 外部設定の表示文をHTTPで返す。
 * @RestControllerは戻り値をView名ではなくレスポンス本文にするために使う。
 * Stringは本文として、オブジェクトはHttpMessageConverterによりJSONなどへ変換される。
 */
@RestController
public class HelloController {
    private final AppProperties properties;

    public HelloController(AppProperties properties) {
        this.properties = properties;
    }

    // HTTP GET /hello をDispatcherServletがこのメソッドへ振り分ける。
    @GetMapping("/hello")
    public String hello() {
        return properties.getMessage();
    }
}
