package com.example.demo.training.relations;

import java.util.Locale;
import org.hibernate.resource.jdbc.spi.StatementInspector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** SPR-W6-D2-03: 学習APIの同一スレッド・処理区間だけを計数し、起動SQLや別リクエストを除外する。 */
public class RelationSqlTrace implements StatementInspector {
    private static final Logger log = LoggerFactory.getLogger(RelationSqlTrace.class);
    private static final ThreadLocal<Scope> active = new ThreadLocal<>();
    public static Scope begin() {
        if (active.get() != null) throw new IllegalStateException("Nested SQL trace");
        Scope scope = new Scope(); active.set(scope); return scope;
    }
    @Override public String inspect(String sql) {
        Scope scope = active.get();
        if (scope != null && sql.stripLeading().toLowerCase(Locale.ROOT).startsWith("select")) {
            scope.selects++;
            log.info("[SPR-W6-D2] SQL #{} {}", scope.selects, sql.replaceAll("\\s+", " "));
        }
        return sql;
    }
    public static final class Scope implements AutoCloseable {
        private int selects;
        public int count() { return selects; }
        @Override public void close() { active.remove(); }
    }
}
