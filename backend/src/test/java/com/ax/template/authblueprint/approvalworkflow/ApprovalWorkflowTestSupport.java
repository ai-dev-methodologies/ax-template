package com.ax.template.authblueprint.approvalworkflow;

import com.ax.template.authblueprint.common.AxPort;
import com.ax.template.authblueprint.common.HttpExtract;

import io.restassured.RestAssured;
import io.restassured.http.Header;
import io.restassured.response.Response;

import java.util.UUID;

import static io.restassured.RestAssured.given;

/**
 * Shared HTTP helpers for the approval-workflow ITs.
 *
 * <p><b>BACKLOG P3-144 — why these helpers validate before extracting.</b> These methods used
 * to end in {@code .then().extract().path("accessToken")} with no status assertion. Measured
 * against the exact test runtime classpath (rest-assured 6.0.1), {@code path()} throws
 * {@code IllegalStateException: Cannot invoke the path method because no content-type was
 * present in the response and no default parser has been set} — <b>and it throws that for
 * exactly one input: a response carrying NO {@code Content-Type} header at all</b> (an empty
 * body with a JSON content type raises {@code JsonPathException} instead, and
 * {@code application/problem+json} parses fine and yields {@code null}).
 *
 * <p>That is why the 2026-08-15 R25 failure was undiagnosable: the whole class died with a bare
 * {@code IllegalStateException} that named no status, no body, and no port — even though the
 * exception was in fact carrying the strongest possible signal, namely "whatever answered this
 * request was not this application's login endpoint" (every application-level outcome of
 * {@code /api/auth/email/login} is rendered by {@code AuthExceptionHandler} as
 * {@code application/problem+json}, and a success is {@code application/json}).
 *
 * <p>The rule these helpers now follow: <b>never extract from an unvalidated response.</b> The
 * status is checked first, so a wrong-endpoint / wrong-server / container-level response fails
 * as itself (status + content-type + body + port + headers) instead of as a parser error.
 * The success path is unchanged.
 */
public final class ApprovalWorkflowTestSupport {

    private ApprovalWorkflowTestSupport() {}

    /** Bodies are truncated in failure messages so one dump cannot drown the report. */
    private static final int BODY_EXCERPT_LIMIT = 400;

    public static String freshEmail(String prefix) {
        return prefix + "-" + UUID.randomUUID() + "@example.com";
    }

    public static String obtainToken(String email, String role) {
        Response signup = given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\",\"role\":\"" + role + "\"}")
        .when().post("/api/auth/email/signup");

        Response login = given()
            .header("Content-Type", "application/json")
            .body("{\"email\":\"" + email + "\",\"password\":\"securepassword12\"}")
        .when().post("/api/auth/email/login");

        return HttpExtract.pathAt(login, 200, "accessToken",
            "obtainToken(" + email + ", " + role + "): POST /api/auth/email/login (accessToken)\n"
                + context(signup, login));
    }

    public static String resolveUserId(String token) {
        Response me = given()
            .header("Authorization", "Bearer " + token)
        .when().get("/api/auth/me");

        return HttpExtract.pathAt(me, 200, "userId",
            "resolveUserId: GET /api/auth/me (userId)\n" + portContext());
    }

    /**
     * Names the global RestAssured target and the verdict of the authority that published it.
     *
     * <p>P2-120 moved the bookkeeping this method used to do itself onto
     * {@link com.ax.template.authblueprint.common.AxPort}, the single writer of
     * {@code RestAssured.port}. The question is unchanged — did the global still hold what was
     * published for this test? — but the answer is now recorded per test by the extension rather
     * than by whichever helper happened to publish last, so it is correct for every domain
     * instead of only this one.
     */
    private static String portContext() {
        return "  RestAssured.port = " + RestAssured.port + "\n" + AxPort.diagnose();
    }

    private static String context(Response signup, Response login) {
        return portContext() + "\n"
            + "  signup " + describe(signup) + "\n"
            + "  login  " + describe(login);
    }

    /**
     * Response headers are part of the dump on purpose: an absent {@code Content-Type} plus a
     * foreign {@code Server}/{@code WWW-Authenticate} header is what distinguishes "our Tomcat
     * denied it" from "something that is not our Tomcat answered".
     */
    private static String describe(Response response) {
        String body;
        try {
            body = response.getBody().asString();
        } catch (RuntimeException e) {
            body = "<body unreadable: " + e + ">";
        }
        if (body == null) {
            body = "<null>";
        }
        if (body.length() > BODY_EXCERPT_LIMIT) {
            body = body.substring(0, BODY_EXCERPT_LIMIT) + "…(" + body.length() + " chars)";
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
            + " content-type=" + response.getContentType()
            + " headers=[" + headers + "]"
            + " body=" + body;
    }
}
