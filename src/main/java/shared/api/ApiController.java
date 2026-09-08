package shared.api;

import admin.api.AdminApi;
import admin.model.Admin;
import com.google.gson.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import shared.service.Auth;
import user.api.UserApi;
import user.model.User;

/** Spring MVC endpoints retaining the JSON and session contract used by Next.js. */
@RestController
public class ApiController {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private static final int MAX_BODY_BYTES = 8192;
    private final SessionStore sessions;
    private final Auth auth;
    private final UserApi users;
    private final AdminApi admins;
    private final DatabaseCheck databaseCheck;
    private volatile boolean databaseReady;

    public ApiController(SessionStore sessions, Auth auth, UserApi users, AdminApi admins, DatabaseCheck databaseCheck) {
        this.sessions = sessions;
        this.auth = auth;
        this.users = users;
        this.admins = admins;
        this.databaseCheck = databaseCheck;
    }

    @GetMapping("/api/health")
    ResponseEntity<String> health(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> {
            databaseCheck.run(); databaseReady = true;
            return Map.of("status", "connected");
        });
    }

    @GetMapping("/api/session")
    ResponseEntity<String> session(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> sessionView(session(request)));
    }

    @PostMapping("/api/logout")
    ResponseEntity<String> logout(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> {
            var session = session(request);
            session.logout(); sessions.rotate(session);
            return sessionView(session);
        });
    }

    @PostMapping("/api/login")
    ResponseEntity<String> login(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> {
            var session = session(request);
            JsonObject body = body(request);
            String role = field(body, "role");
            if (!role.equals("user") && !role.equals("admin")) throw new Problem(400, "Choose user or admin.");
            if (session.remaining(role) == 0) throw new Problem(429, "Three failed attempts. This browser session is locked for this role.");
            if (session.role() != null) throw new Problem(409, "Sign out before signing in to another account.");
            ensureDatabase();
            String identity = field(body, "identity");
            String pin = field(body, "pin");
            String authenticatedIdentity;
            if (role.equals("user")) {
                User user = auth.authenticate(identity, pin);
                authenticatedIdentity = user == null ? null : user.getMobileNumber();
            } else {
                Admin admin = auth.authenticateAdmin(identity, pin);
                authenticatedIdentity = admin == null ? null : admin.username();
            }
            if (authenticatedIdentity == null) {
                session.fail(role);
                int remaining = session.remaining(role);
                throw new Problem(remaining == 0 ? 429 : 401, remaining == 0
                        ? "Three failed attempts. This browser session is locked for this role."
                        : "Incorrect credentials. " + remaining + " attempt(s) remaining.");
            }
            session.login(role, authenticatedIdentity); sessions.rotate(session);
            return sessionView(session);
        });
    }

    @PostMapping({"/api/register", "/api/admin/accounts"})
    ResponseEntity<String> register(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> {
            JsonObject body = body(request);
            ensureDatabase();
            return admins.create(field(body, "fullName"), field(body, "mobileNumber"), confirmedPin(body));
        });
    }

    @GetMapping("/api/user/dashboard")
    ResponseEntity<String> userDashboard(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> {
            ensureDatabase(); return users.dashboard(session(request).identity());
        });
    }

    @PostMapping({"/api/user/cash-in", "/api/user/withdraw", "/api/user/transfer"})
    ResponseEntity<String> operate(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> {
            ensureDatabase();
            JsonObject body = body(request);
            String operation = request.getRequestURI().substring("/api/user/".length());
            return users.operate(session(request).identity(), operation, amount(body),
                    operation.equals("transfer") ? field(body, "receiver") : "");
        });
    }

    @GetMapping("/api/admin/dashboard")
    ResponseEntity<String> adminDashboard(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> { ensureDatabase(); return admins.dashboard(); });
    }

    @PostMapping({"/api/admin/credit", "/api/admin/debit"})
    ResponseEntity<String> adjust(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> {
            ensureDatabase();
            JsonObject body = body(request);
            return admins.adjust(session(request).identity(), field(body, "mobileNumber"), amount(body),
                    request.getRequestURI().endsWith("/credit"));
        });
    }

    @PostMapping("/api/admin/transactions/delete")
    ResponseEntity<String> deleteTransactionLog(HttpServletRequest request, HttpServletResponse response) {
        return execute(request, response, () -> {
            JsonObject body = body(request);
            String id = field(body, "transactionId");
            if (!id.matches("[1-9][0-9]{0,18}")) {
                throw new Problem(400, "Enter a valid positive transaction ID.");
            }
            long transactionId;
            try { transactionId = Long.parseLong(id); }
            catch (NumberFormatException exception) {
                throw new Problem(400, "Enter a valid positive transaction ID.");
            }
            String confirmation = field(body, "confirmation");
            if (!("DELETE " + transactionId).equals(confirmation)) {
                throw new Problem(400, "Type DELETE " + transactionId + " to confirm deletion.");
            }
            ensureDatabase();
            if (!admins.deleteTransactionLog(transactionId, confirmation)) {
                throw new Problem(404, "Transaction log not found. It may already have been deleted.");
            }
            org.slf4j.LoggerFactory.getLogger(ApiController.class).info(
                    "Administrator {} deleted transaction log {}", session(request).identity(), transactionId);
            return Map.of("deletedId", transactionId,
                    "message", "Transaction log #" + transactionId + " deleted. Account balances were not changed.");
        });
    }

    @RequestMapping("/**")
    ResponseEntity<String> unknown() { return json(404, Map.of("error", "Endpoint not found.")); }

    private synchronized void ensureDatabase() throws SQLException {
        if (!databaseReady) { databaseCheck.run(); databaseReady = true; }
    }

    private ResponseEntity<String> execute(HttpServletRequest request, HttpServletResponse response, Operation operation) {
        try {
            return json(200, operation.run());
        } catch (Problem e) {
            return json(e.status, Map.of("error", e.getMessage()));
        } catch (JsonParseException | IllegalArgumentException e) {
            return json(400, Map.of("error", e instanceof JsonParseException ? "Invalid JSON request." : e.getMessage()));
        } catch (SQLException e) {
            databaseReady = false;
            boolean conflict = e.getSQLState() != null && e.getSQLState().startsWith("23");
            return json(conflict ? 409 : 503, Map.of("error", conflict
                    ? "That record already exists or conflicts with existing data."
                    : "Database unavailable. Check MySQL and try again."));
        } catch (Exception e) {
            System.err.println("API request failed: " + e.getClass().getSimpleName());
            return json(500, Map.of("error", "Request could not be completed. Please try again."));
        } finally {
            var session = session(request);
            if (session != null) SessionAuthenticationFilter.setCookie(response, session);
        }
    }

    private static SessionStore.Session session(HttpServletRequest request) {
        return (SessionStore.Session) request.getAttribute(SessionAuthenticationFilter.SESSION_ATTRIBUTE);
    }

    private static ResponseEntity<String> json(int status, Object value) {
        return ResponseEntity.status(status).header("Content-Type", "application/json; charset=utf-8")
                .header("Cache-Control", "no-store").body(JSON.toJson(value));
    }

    private static Object sessionView(SessionStore.Session session) {
        return Map.of("role", session.role() == null ? "guest" : session.role(),
                "identity", session.identity() == null ? "" : session.identity(),
                "userAttempts", session.remaining("user"), "adminAttempts", session.remaining("admin"));
    }

    private static String confirmedPin(JsonObject body) {
        String pin = field(body, "pin");
        if (!pin.equals(field(body, "confirmPin"))) throw new Problem(400, "PINs do not match.");
        return pin;
    }

    private static BigDecimal amount(JsonObject body) {
        String amount = field(body, "amount");
        if (!amount.matches("\\d{1,13}(\\.\\d{1,2})?")) throw new Problem(400, "Enter an amount with up to two decimal places.");
        BigDecimal value = new BigDecimal(amount);
        if (value.signum() <= 0) throw new Problem(400, "Amount must be greater than zero.");
        return value;
    }

    private static String field(JsonObject body, String key) {
        if (!body.has(key) || !body.get(key).isJsonPrimitive() || !body.getAsJsonPrimitive(key).isString()) {
            throw new Problem(400, "Missing or invalid " + key + ".");
        }
        return body.get(key).getAsString().trim();
    }

    private static JsonObject body(HttpServletRequest request) throws IOException {
        String type = request.getContentType();
        if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            throw new Problem(415, "Use application/json.");
        }
        byte[] bytes = request.getInputStream().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) throw new Problem(413, "Request is too large.");
        var element = JSON.fromJson(new String(bytes, StandardCharsets.UTF_8), JsonElement.class);
        if (element == null || !element.isJsonObject()) throw new Problem(400, "Expected a JSON object.");
        return element.getAsJsonObject();
    }

    @FunctionalInterface private interface Operation { Object run() throws Exception; }
    private static final class Problem extends RuntimeException {
        final int status;
        Problem(int status, String message) { super(message); this.status = status; }
    }
}
