package com.ax.template.authblueprint.reportexport;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Default {@link ReportRowSource} — returns a tiny demo dataset so the catalog
 * tests can exercise the full export pipeline without requiring fork-receivers
 * to wire in real entities first. Production deployments register their own
 * {@code @Primary @Component} bean and let Spring shadow this one.
 *
 * <p>The {@code query} map on the request may carry an optional {@code injectSample}
 * key — when present and truthy, the demo dataset includes a row whose first cell
 * starts with {@code =} so the EXPORT-INJECT-001 / EXPORT-INJECT-002 black-box tests
 * can verify the formula-injection neutralization actually fires end-to-end.
 */
@Component
public class DemoReportRowSource implements ReportRowSource {

    @Override
    public List<String> header(CreateExportRequest request) {
        return List.of("id", "name", "amount");
    }

    @Override
    public List<List<String>> rows(CreateExportRequest request, String ownerUserId) {
        Map<String, Object> query = request.query() == null ? Map.of() : request.query();
        boolean injectSample = Boolean.parseBoolean(String.valueOf(query.getOrDefault("injectSample", "false")));

        if (injectSample) {
            return List.of(
                List.of("1", "alice", "100.00"),
                List.of("2", "=cmd|' /C calc'!A0", "200.00"),
                List.of("3", "bob", "300.00")
            );
        }
        return List.of(
            List.of("1", "alice", "100.00"),
            List.of("2", "bob", "200.00")
        );
    }
}
