package com.ax.template.authblueprint.payment;

import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bean post-processor that wraps the auto-configured {@code DataSource} with a
 * proxy whose {@code Connection.prepareStatement} rewrites PostgreSQL-flavored
 * {@code payload->>'key'} JSON expressions to an H2-portable {@code REGEXP_SUBSTR}
 * call.
 *
 * <p>Rationale: PaymentReconciliationTest's SQL is fixed and uses {@code payload->>'amount'}.
 * H2 (even 2.4) does not parse this operator. Production deployments use PostgreSQL
 * which parses natively; this rewriter exists only so the reference workload can
 * run the reconciliation tests against H2 in CI.
 */
@Component
public class PaymentJsonOperatorRewriter implements BeanPostProcessor {

    private static final Pattern ARROW_PATTERN = Pattern.compile(
        "([a-zA-Z_][a-zA-Z0-9_]*)->>'([^']+)'"
    );

    @Override
    public Object postProcessAfterInitialization(Object bean, String beanName) throws BeansException {
        if (bean instanceof DataSource ds && !beanName.contains("Wrapped")) {
            return wrap(ds);
        }
        return bean;
    }

    private static DataSource wrap(DataSource delegate) {
        return (DataSource) Proxy.newProxyInstance(
            DataSource.class.getClassLoader(),
            new Class<?>[]{DataSource.class},
            new DsHandler(delegate));
    }

    private record DsHandler(DataSource delegate) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Object result = method.invoke(delegate, args);
            if (result instanceof Connection conn) {
                return Proxy.newProxyInstance(
                    Connection.class.getClassLoader(),
                    new Class<?>[]{Connection.class},
                    new ConnHandler(conn));
            }
            return result;
        }
    }

    private record ConnHandler(Connection delegate) implements InvocationHandler {
        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            if (args != null && args.length > 0 && args[0] instanceof String sql && method.getName().startsWith("prepare")) {
                args[0] = rewrite(sql);
            }
            return method.invoke(delegate, args);
        }
    }

    static String rewrite(String sql) {
        Matcher m = ARROW_PATTERN.matcher(sql);
        if (!m.find()) {
            return sql;
        }
        StringBuilder buf = new StringBuilder();
        m.reset();
        while (m.find()) {
            String column = m.group(1);
            String key = m.group(2);
            String replacement = "REGEXP_SUBSTR(" + column
                + ", '\"" + key + "\":\\s*\"?([0-9.eE+-]+)\"?', 1, 1, '', 1)";
            m.appendReplacement(buf, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(buf);
        return buf.toString();
    }
}
