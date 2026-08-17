package com.ax.template.authblueprint.common;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;

/**
 * Validate-then-extract helper for RestAssured responses. Shared by every backend test that
 * reads a value out of an HTTP response.
 *
 * <p><b>BACKLOG P2-117 / P3-144 — why this exists.</b> The dominant shape in this tree is
 * {@code given()...when().get(...).then().extract().path("field")} with <b>no status assertion</b>
 * (a 2026-08-16 census found 86 such files). That shape converts every infrastructural failure
 * into a parser error that names nothing. Measured against the exact test-runtime classpath
 * (rest-assured 6.0.1), the four response shapes behave as follows:
 *
 * <table>
 *   <caption>Measured behaviour of a blind {@code extract().path(...)}</caption>
 *   <tr><th>response shape</th><th>what a blind extract does</th></tr>
 *   <tr><td>no {@code Content-Type} header at all</td>
 *       <td>{@code IllegalStateException: Cannot invoke the path method because no content-type
 *           was present in the response and no default parser has been set} — <b>this is the
 *           single preimage of the P3-144 exception</b></td></tr>
 *   <tr><td>200 + {@code application/json} + empty body</td>
 *       <td>{@code JsonPathException} (parse failure), not the above</td></tr>
 *   <tr><td>401 + {@code application/problem+json}</td>
 *       <td><b>no exception at all</b> — parses fine and yields {@code null}, so the caller
 *           carries an authentication failure forward as a null token</td></tr>
 *   <tr><td>{@code RestAssured.port} = 0</td>
 *       <td>{@code IllegalArgumentException} raised <i>before</i> any response exists — out of
 *           this helper's reach by construction; guard it where the port is published</td></tr>
 * </table>
 *
 * <p>Three of those four are diagnosable only if the response is described. So the rule here is:
 * <b>never extract from an unvalidated response.</b> Status is checked first, then the
 * content-type, then the extraction, then the value's presence — and any failure is reported as
 * an {@link AssertionError} naming the caller-supplied context, the status, the content-type,
 * <i>every</i> response header, a bounded body excerpt, and the process-global RestAssured
 * target. The success path is unchanged and side-effect free: nothing is read off the response
 * unless the call is already failing.
 *
 * <p>Request URI: a RestAssured {@link Response} does not carry the request it answered, so the
 * closest obtainable target is the process-global {@code baseURI + port + basePath} triple —
 * which is reported verbatim, since a wrong global there is exactly how P3-144 presented.
 */
public final class HttpExtract {

    private HttpExtract() {}

    /**
     * Bodies are truncated to this many characters in failure messages so one dump cannot drown
     * the report. The full length is printed alongside the excerpt.
     */
    public static final int BODY_EXCERPT_LIMIT = 400;

    /**
     * Extracts {@code jsonPath} from a response that MUST have a 2xx status, a JSON-family
     * content type, and a non-null value at that path. Use this for the normal case.
     *
     * @param context caller-supplied identification (endpoint + intent) reproduced in failures
     * @throws AssertionError if the status is not 2xx, the content type is absent or not JSON,
     *                        the body cannot be parsed, or the path resolves to {@code null}
     */
    public static <T> T path(Response response, String jsonPath, String context) {
        return extract(response, ANY_2XX, jsonPath, context);
    }

    /**
     * Same as {@link #path}, but pins an exact status. Use this where the extraction is
     * deliberately performed on a non-2xx response (error bodies, ProblemDetail fields).
     *
     * @param expectedStatus the exact HTTP status the response must carry
     */
    public static <T> T pathAt(Response response, int expectedStatus, String jsonPath, String context) {
        if (expectedStatus < 100 || expectedStatus > 599) {
            throw new IllegalArgumentException(
                "pathAt(" + context + "): expectedStatus must be a real HTTP status, was " + expectedStatus);
        }
        return extract(response, expectedStatus, jsonPath, context);
    }

    /** Sentinel meaning "any 2xx" rather than one pinned status. */
    private static final int ANY_2XX = -1;

    @SuppressWarnings("unchecked")
    private static <T> T extract(Response response, int expectedStatus, String jsonPath, String context) {
        if (response == null) {
            throw new AssertionError(context + ": response was null — nothing was extracted.\n" + target());
        }

        int status = response.getStatusCode();
        boolean statusOk = (expectedStatus == ANY_2XX) ? (status >= 200 && status <= 299) : (status == expectedStatus);
        if (!statusOk) {
            String wanted = (expectedStatus == ANY_2XX) ? "a 2xx status" : "HTTP " + expectedStatus;
            throw new AssertionError(
                context + ": expected " + wanted + " before reading '" + jsonPath + "', but was " + status + ".\n"
                    + evidence(response));
        }

        String contentType = response.getContentType();
        if (contentType == null || contentType.isBlank()) {
            throw new AssertionError(
                context + ": response carries NO Content-Type header, so '" + jsonPath + "' cannot be parsed. "
                    + "This is the exact preimage of the P3-144 IllegalStateException — it means whatever "
                    + "answered this request did not render a body through this application's handlers.\n"
                    + evidence(response));
        }
        if (!isJsonFamily(contentType)) {
            throw new AssertionError(
                context + ": response content type '" + contentType + "' is not a JSON family type, so '"
                    + jsonPath + "' cannot be parsed.\n" + evidence(response));
        }

        Object value;
        try {
            value = response.path(jsonPath);
        } catch (RuntimeException e) {
            throw new AssertionError(
                context + ": body could not be parsed while reading '" + jsonPath + "' (" + e.getClass().getName()
                    + ": " + e.getMessage() + ").\n" + evidence(response), e);
        }
        if (value == null) {
            throw new AssertionError(
                context + ": body parsed, but '" + jsonPath + "' was absent or JSON null. Returning it would "
                    + "have carried the failure forward silently.\n" + evidence(response));
        }
        return (T) value;
    }

    /** {@code application/json}, {@code application/problem+json}, {@code application/hal+json}, … */
    private static boolean isJsonFamily(String contentType) {
        return contentType.toLowerCase(java.util.Locale.ROOT).contains("json");
    }

    /**
     * The full dump. Headers are included on purpose: an absent {@code Content-Type} plus a
     * foreign {@code Server}/{@code WWW-Authenticate} header is what distinguishes "our Tomcat
     * denied it" from "something that is not our Tomcat answered".
     */
    private static String evidence(Response response) {
        return "  " + describe(response) + "\n" + target();
    }

    private static String describe(Response response) {
        String body;
        try {
            body = response.getBody().asString();
        } catch (RuntimeException e) {
            body = "<body unreadable: " + e + ">";
        }
        if (body == null) {
            body = "<null>";
        } else if (body.isEmpty()) {
            body = "<empty>";
        } else if (body.length() > BODY_EXCERPT_LIMIT) {
            body = body.substring(0, BODY_EXCERPT_LIMIT)
                + "…(truncated at " + BODY_EXCERPT_LIMIT + " of " + body.length() + " chars)";
        }

        String contentType = response.getContentType();
        if (contentType == null || contentType.isBlank()) {
            contentType = "<absent>";
        }

        StringBuilder headers = new StringBuilder();
        try {
            for (Header h : response.getHeaders()) {
                if (headers.length() > 0) {
                    headers.append("; ");
                }
                headers.append(h.getName()).append('=').append(h.getValue());
            }
        } catch (RuntimeException e) {
            headers.append("<headers unreadable: ").append(e).append('>');
        }

        return "status=" + response.getStatusCode()
            + " content-type=" + contentType
            + " headers=[" + headers + "]"
            + " body=" + body;
    }

    /**
     * A {@link Response} does not expose the request it answered, so this reports the closest
     * obtainable request target: the process-global RestAssured statics every HTTP test shares.
     */
    private static String target() {
        return "  RestAssured target: baseURI=" + RestAssured.baseURI
            + " RestAssured.port=" + RestAssured.port
            + " basePath=" + RestAssured.basePath;
    }
}
