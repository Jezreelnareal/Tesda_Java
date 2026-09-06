package shared.api;

import admin.api.AdminApi;
import admin.model.Admin;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.Strictness;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import shared.service.Auth;
import shared.util.DatabaseConnection;
import user.api.UserApi;
import user.model.User;

/** Local JSON API consumed through Next.js's same-origin /api route. */
public final class ApiServer implements AutoCloseable {
    private static final Gson JSON = new GsonBuilder().setStrictness(Strictness.STRICT).create();
    private static final int MAX_BODY_BYTES = 8192;
    private final HttpServer server;
    private final ExecutorService workers = Executors.newFixedThreadPool(8);
    private final SessionStore sessions = new SessionStore();
    private final Auth auth;
    private final UserApi users = new UserApi();
    private final AdminApi admins = new AdminApi();
    private final DatabaseCheck databaseCheck;
    private volatile boolean databaseReady;

    public ApiServer(int port) throws IOException { this(port, new Auth(), DatabaseConnection::verifyConnection); }

    // Allows HTTP authentication tests to run without a database.
    public ApiServer(int port, Auth auth, DatabaseCheck databaseCheck) throws IOException {
        this.auth = auth;
        this.databaseCheck = databaseCheck;
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", port), 64);
        server.setExecutor(workers);
        server.createContext("/", this::handle);
    }

    public void start() {
        try { ensureDatabase(); }
        catch (SQLException e) { System.err.println("Database unavailable. API will retry on the next request."); }
        server.start();
    }

    public int port() { return server.getAddress().getPort(); }

    private synchronized void ensureDatabase() throws SQLException {
        if (!databaseReady) { databaseCheck.run(); databaseReady = true; }
    }

    private void handle(HttpExchange exchange) throws IOException {
        try {
            String method = exchange.getRequestMethod();
            String path = exchange.getRequestURI().getPath();
            if (!method.equals("GET") && !method.equals("POST")) throw new Problem(405, "Method not allowed.");
            if (method.equals("POST") && !"1".equals(exchange.getRequestHeaders().getFirst("X-JCash-Request"))) {
                throw new Problem(403, "Request verification failed.");
            }
            if (path.equals("/api/health") && method.equals("GET")) {
                databaseCheck.run(); databaseReady = true;
                send(exchange, 200, Map.of("status", "connected")); return;
            }
            SessionStore.Session session = sessions.open(cookie(exchange));
            synchronized (session) {
                try {
                    Object result = route(method, path, exchange, session);
                    setCookie(exchange, session);
                    send(exchange, 200, result);
                } catch (Exception exception) {
                    setCookie(exchange, session);
                    throw exception;
                }
            }
        } catch (Problem e) {
            send(exchange, e.status, Map.of("error", e.getMessage()));
        } catch (JsonParseException | IllegalArgumentException e) {
            send(exchange, 400, Map.of("error", e instanceof JsonParseException ? "Invalid JSON request." : e.getMessage()));
        } catch (SQLException e) {
            databaseReady = false;
            boolean conflict = e.getSQLState() != null && e.getSQLState().startsWith("23");
            send(exchange, conflict ? 409 : 503, Map.of("error", conflict
                    ? "That record already exists or conflicts with existing data."
                    : "Database unavailable. Check MySQL and try again."));
        } catch (Exception e) {
            System.err.println("API request failed: " + e.getClass().getSimpleName());
            send(exchange, 500, Map.of("error", "Request could not be completed. Please try again."));
        } finally {
            exchange.close();
        }
    }

    private Object route(String method, String path, HttpExchange exchange, SessionStore.Session session) throws Exception {
        if (path.equals("/api/session") && method.equals("GET")) return sessionView(session);
        if (path.equals("/api/logout") && method.equals("POST")) {
            session.logout(); sessions.rotate(session); return sessionView(session);
        }
        if (path.equals("/api/login") && method.equals("POST")) {
            JsonObject body = body(exchange);
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
        }
        if (path.equals("/api/register") && method.equals("POST")) {
            JsonObject body = body(exchange);
            ensureDatabase();
            return admins.create(field(body, "fullName"), field(body, "mobileNumber"), confirmedPin(body));
        }
        if (path.startsWith("/api/user/")) {
            requireRole(session, "user"); ensureDatabase();
            if (method.equals("GET") && path.equals("/api/user/dashboard")) return users.dashboard(session.identity());
            if (method.equals("POST") && (path.equals("/api/user/cash-in") || path.equals("/api/user/withdraw") || path.equals("/api/user/transfer"))) {
                JsonObject body = body(exchange);
                return users.operate(session.identity(), path.substring("/api/user/".length()), amount(body),
                        path.endsWith("/transfer") ? field(body, "receiver") : "");
            }
        }
        if (path.startsWith("/api/admin/")) {
            requireRole(session, "admin"); ensureDatabase();
            if (method.equals("GET") && path.equals("/api/admin/dashboard")) return admins.dashboard();
            if (method.equals("POST") && path.equals("/api/admin/accounts")) {
                JsonObject body = body(exchange);
                return admins.create(field(body, "fullName"), field(body, "mobileNumber"), confirmedPin(body));
            }
            if (method.equals("POST") && (path.equals("/api/admin/credit") || path.equals("/api/admin/debit"))) {
                JsonObject body = body(exchange);
                return admins.adjust(session.identity(), field(body, "mobileNumber"), amount(body), path.endsWith("/credit"));
            }
        }
        throw new Problem(404, "Endpoint not found.");
    }

    private static Object sessionView(SessionStore.Session session) {
        return Map.of("role", session.role() == null ? "guest" : session.role(),
                "identity", session.identity() == null ? "" : session.identity(),
                "userAttempts", session.remaining("user"), "adminAttempts", session.remaining("admin"));
    }

    private static void requireRole(SessionStore.Session session, String role) {
        if (session.role() == null) throw new Problem(401, "Please sign in to continue.");
        if (!role.equals(session.role())) throw new Problem(403, "This account cannot access that feature.");
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

    private static JsonObject body(HttpExchange exchange) throws IOException {
        String type = exchange.getRequestHeaders().getFirst("Content-Type");
        if (type == null || !type.split(";", 2)[0].trim().equalsIgnoreCase("application/json")) {
            throw new Problem(415, "Use application/json.");
        }
        byte[] bytes = exchange.getRequestBody().readNBytes(MAX_BODY_BYTES + 1);
        if (bytes.length > MAX_BODY_BYTES) throw new Problem(413, "Request is too large.");
        var element = JSON.fromJson(new String(bytes, StandardCharsets.UTF_8), com.google.gson.JsonElement.class);
        if (element == null || !element.isJsonObject()) throw new Problem(400, "Expected a JSON object.");
        return element.getAsJsonObject();
    }

    private static String cookie(HttpExchange exchange) {
        String cookies = exchange.getRequestHeaders().getFirst("Cookie");
        if (cookies != null) for (String entry : cookies.split(";")) {
            String[] pair = entry.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals("jcash_session")) return pair[1];
        }
        return null;
    }

    private static void setCookie(HttpExchange exchange, SessionStore.Session session) {
        exchange.getResponseHeaders().set("Set-Cookie", "jcash_session=" + session.token() + "; Path=/; HttpOnly; SameSite=Strict");
    }

    private static void send(HttpExchange exchange, int status, Object value) throws IOException {
        byte[] bytes = JSON.toJson(value).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json; charset=utf-8");
        exchange.getResponseHeaders().set("Cache-Control", "no-store");
        exchange.getResponseHeaders().set("X-Content-Type-Options", "nosniff");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
    }

    @Override public void close() { server.stop(1); workers.shutdownNow(); DatabaseConnection.shutdown(); }
    @FunctionalInterface public interface DatabaseCheck { void run() throws SQLException; }
    private static final class Problem extends RuntimeException {
        final int status;
        Problem(int status, String message) { super(message); this.status = status; }
    }
}
