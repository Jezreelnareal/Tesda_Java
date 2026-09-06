package shared.api;

import admin.model.Admin;
import admin.repository.AdminRepository;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import shared.service.Auth;
import shared.util.PinHasher;
import user.model.User;
import user.repository.UserRepository;
import static org.junit.jupiter.api.Assertions.*;

class ApiServerTest {
    private ApiServer server;
    private String token;
    private String base;
    private final HttpClient client = HttpClient.newHttpClient();

    @BeforeEach void start() throws Exception {
        User user = new User("Test Customer", "09171234567", "1234");
        Admin admin = new Admin("admin", PinHasher.hash("1234"));
        Auth auth = new Auth(new UserRepository() {
            @Override public User findByMobileNumber(String mobile) { return mobile.equals(user.getMobileNumber()) ? user : null; }
        }, new AdminRepository() {
            @Override public Admin findByUsername(String name) { return name.equals("admin") ? admin : null; }
        });
        server = new ApiServer(0, auth, () -> { });
        server.start(); base = "http://127.0.0.1:" + server.port();
    }

    @AfterEach void stop() { server.close(); }

    private HttpResponse<String> request(String path, String body, boolean verified) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create(base + "/api/" + path));
        if (token != null) builder.header("Cookie", token);
        if (body != null) {
            builder.header("Content-Type", "application/json").POST(HttpRequest.BodyPublishers.ofString(body));
            if (verified) builder.header("X-JCash-Request", "1");
        }
        var response = client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
        response.headers().firstValue("set-cookie").ifPresent(cookie -> token = cookie.split(";", 2)[0]);
        return response;
    }

    private HttpResponse<String> login(String role, String identity, String pin) throws Exception {
        return request("login", "{\"role\":\"" + role + "\",\"identity\":\"" + identity + "\",\"pin\":\"" + pin + "\"}", true);
    }

    @Test void anonymousCannotAccessEitherDashboardOrMoneyOperations() throws Exception {
        assertEquals(401, request("user/dashboard", null, true).statusCode());
        assertEquals(401, request("admin/dashboard", null, true).statusCode());
        assertEquals(401, request("user/cash-in", "{\"amount\":\"100\"}", true).statusCode());
    }

    @Test void rolesAreEnforcedAndLogoutInvalidatesOldToken() throws Exception {
        request("session", null, true); String anonymous = token;
        var signedIn = login("user", "09171234567", "1234");
        assertEquals(200, signedIn.statusCode());
        assertNotEquals(anonymous, token);
        assertFalse(signedIn.body().contains("pin"));
        assertTrue(signedIn.headers().firstValue("set-cookie").orElseThrow().contains("HttpOnly; SameSite=Strict"));
        assertEquals(403, request("admin/dashboard", null, true).statusCode());
        String signedInToken = token;
        assertEquals(200, request("logout", "{}", true).statusCode());
        token = signedInToken;
        assertEquals(401, request("user/dashboard", null, true).statusCode());
    }

    @Test void threeAttemptsPersistAcrossReloadAndLogoutButAreSeparatePerRole() throws Exception {
        assertEquals(401, login("user", "09999999999", "1234").statusCode());
        assertEquals(401, login("user", "09171234567", "0000").statusCode());
        assertTrue(request("session", null, true).body().contains("\"userAttempts\":1"));
        assertEquals(429, login("user", "09171234567", "0000").statusCode());
        request("logout", "{}", true);
        assertEquals(429, login("user", "09171234567", "1234").statusCode());
        assertEquals(200, login("admin", "admin", "1234").statusCode());
        assertEquals(403, request("user/dashboard", null, true).statusCode());
    }

    @Test void mutationRequiresVerificationAndBodiesAreValidated() throws Exception {
        assertEquals(403, request("login", "{}", false).statusCode());
        assertEquals(400, request("login", "{bad", true).statusCode());
        assertEquals(400, request("login", "[]", true).statusCode());
        assertEquals(413, request("login", " ".repeat(8193), true).statusCode());
        login("user", "09171234567", "1234");
        for (String amount : new String[]{"0", "-1", "1.001", "NaN", "1e3"}) {
            assertEquals(400, request("user/transfer", "{\"amount\":\"" + amount + "\",\"receiver\":\"09181234567\"}", true).statusCode());
        }
    }
}
