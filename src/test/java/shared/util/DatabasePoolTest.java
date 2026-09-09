package shared.util;

import static org.junit.jupiter.api.Assertions.*;
import java.math.BigDecimal;
import java.sql.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import shared.repository.TransactionRepository;
import shared.model.Transaction;
import user.repository.UserRepository;
import user.service.Transfer;
import user.service.Withdrawal;

/** Real-MySQL integration checks; opt in only with the isolated database runner. */
@EnabledIfEnvironmentVariable(named="JCASH_POOL_TEST_DB_URL",matches="jdbc:mysql://localhost:3307/jcash_pool_test_[a-f0-9]{32}")
class DatabasePoolTest {
    private Map<String,String> settings;
    @BeforeEach void configure() throws Exception {
        DatabaseConnection.shutdown();
        settings=new HashMap<>();
        settings.put("JCASH_DB_URL",System.getenv("JCASH_POOL_TEST_DB_URL"));
        settings.put("JCASH_DB_USER","jcash");
        settings.put("JCASH_DB_PASSWORD",System.getenv("JCASH_DB_PASSWORD"));
        settings.put("JCASH_DB_POOL_SIZE","2");
        settings.put("JCASH_DB_POOL_TIMEOUT_MS","500");
        DatabaseConnection.configureSettings(settings::get);
        try (Connection c=DatabaseConnection.getConnection(); Statement s=c.createStatement()) {
            s.executeUpdate("DELETE FROM transactions");
            s.executeUpdate("UPDATE users SET balance=CASE WHEN mobile_number='09171234567' THEN 1000.00 ELSE 500.00 END");
        }
    }
    @AfterEach void close() { DatabaseConnection.shutdown(); DatabaseConnection.configureSettings(System::getenv); }

    @Test void concurrentLeasesAreIndependentAndPoolExhaustionIsBounded() throws Exception {
        CountDownLatch entered=new CountDownLatch(2),release=new CountDownLatch(1);
        Set<Long> ids=ConcurrentHashMap.newKeySet();
        try (ExecutorService workers=Executors.newFixedThreadPool(2)) {
            List<Future<Long>> tasks=new ArrayList<>();
            try {
                for (int i=0;i<2;i++) tasks.add(workers.submit(()->DatabaseConnection.withReusableConnection(c->{
                    long id=id(c); ids.add(id); entered.countDown();
                    try { if (!release.await(5,TimeUnit.SECONDS)) throw new SQLException("Test barrier expired"); }
                    catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new SQLException(e); }
                    return id;
                })));
                assertTrue(entered.await(4,TimeUnit.SECONDS),"Two operations must enter concurrently");
                assertEquals(2,ids.size(),"Simultaneous callbacks must not share a physical connection");
                long waitStarted=System.nanoTime();
                assertThrows(SQLTransientConnectionException.class,()->DatabaseConnection.withReusableConnection(DatabasePoolTest::id));
                long waitedMs=TimeUnit.NANOSECONDS.toMillis(System.nanoTime()-waitStarted);
                assertTrue(waitedMs>=250 && waitedMs<2000,"The configured 500 ms acquisition timeout must bound the wait");
            } finally { release.countDown(); }
            for (Future<Long> task:tasks) assertNotNull(task.get(5,TimeUnit.SECONDS));
        }
        assertNotNull(DatabaseConnection.withReusableConnection(DatabasePoolTest::id));
    }

    @Test void failedCallbackRollsBackAndResetsConnectionState() throws Exception {
        settings.put("JCASH_DB_POOL_SIZE","1");
        assertThrows(SQLException.class,()->DatabaseConnection.withReusableConnection(c->{
            c.setAutoCommit(false);
            try (Statement s=c.createStatement()) { s.executeUpdate("UPDATE users SET balance=balance+99 WHERE mobile_number='09171234567'"); }
            throw new SQLException("Injected callback failure");
        }));
        DatabaseConnection.withReusableConnection(c->{ assertTrue(c.getAutoCommit()); assertFalse(c.isReadOnly()); return null; });
        assertEquals(new BigDecimal("1000.00"),new UserRepository().findByMobileNumber("09171234567").getBalance());
    }

    @Test void runtimeFailureAlsoReturnsLeaseAndShutdownCanReopen() throws Exception {
        settings.put("JCASH_DB_POOL_SIZE","1");
        long before=DatabaseConnection.withReusableConnection(DatabasePoolTest::id);
        assertThrows(IllegalStateException.class,()->DatabaseConnection.withReusableConnection(c->{throw new IllegalStateException("Injected");}));
        assertEquals(before,DatabaseConnection.withReusableConnection(DatabasePoolTest::id).longValue());
        DatabaseConnection.shutdown();
        assertNotEquals(before,DatabaseConnection.withReusableConnection(DatabasePoolTest::id).longValue());
    }

    @Test void badConfigurationFailsAndDatabaseConnectionFailureIsSqlException() {
        settings.put("JCASH_DB_POOL_SIZE","0");
        assertThrows(IllegalArgumentException.class,()->DatabaseConnection.withReusableConnection(DatabasePoolTest::id));
        settings.put("JCASH_DB_POOL_SIZE","1");
        settings.put("JCASH_DB_URL","jdbc:mysql://127.0.0.1:1/unavailable?connectTimeout=500");
        assertThrows(SQLException.class,()->DatabaseConnection.withReusableConnection(DatabasePoolTest::id));
    }

    @Test void concurrentWithdrawalsCannotOverspendAndCreateExactlyFiveLogs() throws Exception {
        UserRepository users=new UserRepository();
        try (ExecutorService workers=Executors.newFixedThreadPool(8)) {
            List<Future<Boolean>> results=new ArrayList<>();
            for (int i=0;i<8;i++) results.add(workers.submit(()->{
                try { new Withdrawal().withdraw(users.findByMobileNumber("09171234567"),new BigDecimal("200.00")); return true; }
                catch (IllegalArgumentException e) { assertTrue(e.getMessage().contains("Insufficient")); return false; }
            }));
            int successes=0;
            for (Future<Boolean> result:results) if (result.get(15,TimeUnit.SECONDS)) successes++;
            assertEquals(5,successes);
        }
        assertEquals(0,users.findByMobileNumber("09171234567").getBalance().compareTo(BigDecimal.ZERO));
        assertEquals(5,new TransactionRepository().findByUserMobileNumber("09171234567").size());
    }

    @Test void transferCommitsBothBalancesAndFailureRollsBackBoth() throws Exception {
        UserRepository users=new UserRepository();
        new Transfer().transfer(users.findByMobileNumber("09171234567"),"09181234567",new BigDecimal("100.00"));
        assertEquals(new BigDecimal("900.00"),users.findByMobileNumber("09171234567").getBalance());
        assertEquals(new BigDecimal("600.00"),users.findByMobileNumber("09181234567").getBalance());
        TransactionRepository failing=new TransactionRepository() {
            @Override public long save(Connection c,Transaction transaction) throws SQLException { throw new SQLException("Injected insert failure"); }
        };
        assertThrows(SQLException.class,()->new Transfer(users,failing).transfer(users.findByMobileNumber("09171234567"),"09181234567",new BigDecimal("100.00")));
        assertEquals(new BigDecimal("900.00"),users.findByMobileNumber("09171234567").getBalance());
        assertEquals(new BigDecimal("600.00"),users.findByMobileNumber("09181234567").getBalance());
        assertEquals(1,new TransactionRepository().findAll().size());
    }

    private static long id(Connection c) throws SQLException {
        try (Statement s=c.createStatement(); ResultSet r=s.executeQuery("SELECT CONNECTION_ID()")) { r.next(); return r.getLong(1); }
    }
}
