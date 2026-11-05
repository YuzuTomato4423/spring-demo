package com.example.demo.lifecycle;

import java.io.IOException;
import java.io.StringReader;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;

/** SPR-1002-02/03: 注入される依存Bean。メモリ内Readerの確保と解放を示す。 */
@Component
@Profile("lifecycle-demo")
public class LifecycleResource {
    private StringReader reader;
    private boolean initialized;
    private boolean closed;

    public LifecycleResource() {
        // 図の依存Bean生成。constructorでは生成を記録し、利用準備はcallbackに任せる。
        System.out.println("[LIFE 01] RESOURCE_CONSTRUCTOR bean=" + identity());
    }

    @PostConstruct
    public void initialize() {
        // 初期化callback: SpringのDI完了後に、利用に必要なリソースを確保する。
        reader = new StringReader("Dependency is ready");
        initialized = true;
        System.out.println("[LIFE 02] RESOURCE_POST_CONSTRUCT initialized=" + initialized);
    }

    public String readMessage() {
        if (!initialized || closed) {
            throw new IllegalStateException("依存Beanのリソースが利用可能ではありません。");
        }
        char[] buffer = new char[64];
        try {
            int count = reader.read(buffer);
            return count < 0 ? "" : new String(buffer, 0, count);
        } catch (IOException exception) {
            throw new IllegalStateException("リソースの読み取りに失敗しました。", exception);
        }
    }

    @PreDestroy
    public void release() {
        // 破棄callback: Context終了時にReaderをcloseし、利用済みの参照を解放する。
        // 実際のファイル/接続でもcloseが必要。GCだけでは外部リソースの解放時期を保証できない。
        // この課題では外部へ接続せず、メモリ内Readerで同じ管理責任を学ぶ。
        if (reader != null) {
            reader.close();
            reader = null;
        }
        initialized = false;
        closed = true;
        System.out.println("[LIFE 08] RESOURCE_PRE_DESTROY resourceClosed=" + closed);
    }

    public boolean isInitialized() { return initialized; }
    public boolean isClosed() { return closed; }

    public String identity() {
        return getClass().getSimpleName() + "@" + Integer.toHexString(System.identityHashCode(this));
    }
}
