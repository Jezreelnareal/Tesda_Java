package shared.api;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;
import org.junit.jupiter.api.Test;
import user.model.TransferTransaction;
import user.model.User;
import static org.junit.jupiter.api.Assertions.*;

class SessionAndViewsTest {
    @Test void sessionsExpireAfterThirtyMinutesWithoutActivity() {
        class TestClock extends Clock {
            Instant now = Instant.parse("2026-01-01T00:00:00Z");
            public ZoneId getZone() { return ZoneOffset.UTC; }
            public Clock withZone(ZoneId zone) { return this; }
            public Instant instant() { return now; }
        }
        TestClock clock = new TestClock();
        SessionStore store = new SessionStore(clock);
        var session = store.open(null); session.login("user", "09171234567");
        String token = session.token();
        clock.now = clock.now.plusSeconds(1801);
        var expired = store.open(token);
        assertNull(expired.role()); assertNotEquals(token, expired.token());
    }

    @Test void viewsExcludeCredentialsAndKeepMoneyAsExactDecimalStrings() {
        User user = new User("Test Customer", "09171234567", "1234");
        user.deposit(new BigDecimal("9999999999999.99"));
        var view = Views.user(user);
        assertEquals(3, view.size()); assertFalse(view.containsKey("pinHash"));
        assertEquals("9999999999999.99", view.get("balance"));
        var transfer = new TransferTransaction("09171234567", "09181234567", new BigDecimal("0.01"), "Transfer");
        assertEquals("out", Views.transaction(transfer, "09171234567").get("direction"));
        assertEquals("in", Views.transaction(transfer, "09181234567").get("direction"));
        assertEquals("0.01", Views.transaction(transfer, null).get("amount"));
    }
}
