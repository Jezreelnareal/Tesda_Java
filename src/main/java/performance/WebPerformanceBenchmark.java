package performance;

import com.google.gson.*;
import java.io.*;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.sql.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.zip.GZIPOutputStream;
import shared.util.PinHasher;

/** Reproducible closed-loop HTTP benchmark. Only seed writes; measured traffic is read-only. */
public final class WebPerformanceBenchmark {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();
    private static final int CUSTOMERS = 100;
    private static final int TRANSACTIONS = 100;
    private static final String PIN = "2468"; // Synthetic performance accounts only.

    public static void main(String[] args) throws Exception {
        requireTestDatabase();
        switch (args[0]) {
            case "seed" -> seed();
            case "verify-data" -> verifyData();
            case "run" -> run(URI.create(args[1]), Integer.parseInt(args[2]),
                    Integer.parseInt(args[3]), Integer.parseInt(args[4]), Path.of(args[5]));
            default -> throw new IllegalArgumentException("Use seed, verify-data, or run URL clients warmupSeconds measuredSeconds outputDir");
        }
    }

    private static void requireTestDatabase() {
        String url = System.getenv("JCASH_DB_URL");
        if (url == null || !url.matches("jdbc:mysql://(?:localhost|127\\.0\\.0\\.1):[0-9]+/jcash_perf_[a-f0-9]{32}")) {
            throw new IllegalArgumentException("This tool requires an isolated jcash_perf_<32 hex digits> database.");
        }
    }

    private static Connection connect() throws SQLException {
        return DriverManager.getConnection(System.getenv("JCASH_DB_URL"),
                System.getenv("JCASH_DB_USER"), System.getenv("JCASH_DB_PASSWORD"));
    }

    private static String mobile(int customer) { return String.format(Locale.ROOT, "099%08d", customer); }

    private static void seed() throws Exception {
        try (Connection c = connect(); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT (SELECT COUNT(*) FROM users) + (SELECT COUNT(*) FROM transactions) + (SELECT COUNT(*) FROM admins)")) {
                r.next();
                if (r.getLong(1) != 0) throw new IllegalStateException("Seeding requires empty test tables.");
            }
            c.setAutoCommit(false);
            String hash = PinHasher.hash(PIN);
            try (PreparedStatement users = c.prepareStatement("INSERT INTO users VALUES (?, ?, ?, ?)");
                 PreparedStatement transactions = c.prepareStatement("INSERT INTO transactions (transaction_type, amount, details, transaction_date_time, receiver_mobile_number) VALUES ('CASH_IN',100.00,'Performance fixture cash-in',?,?)")) {
                for (int i = 1; i <= CUSTOMERS; i++) {
                    users.setString(1, mobile(i)); users.setString(2, hash);
                    users.setString(3, "Performance Customer " + i); users.setBigDecimal(4, new BigDecimal("10000.00"));
                    users.addBatch();
                }
                users.executeBatch();
                for (int i = 1; i <= CUSTOMERS; i++) {
                    for (int j = 0; j < TRANSACTIONS; j++) {
                        transactions.setTimestamp(1, Timestamp.valueOf(LocalDateTime.of(2026, 9, 1, 0, 0).plusSeconds(j)));
                        transactions.setString(2, mobile(i)); transactions.addBatch();
                    }
                }
                transactions.executeBatch(); c.commit();
            } catch (Exception e) { c.rollback(); throw e; }
        }
        verifyData();
    }

    private static void verifyData() throws Exception {
        Map<String,Object> snapshot = new LinkedHashMap<>();
        try (Connection c = connect(); Statement s = c.createStatement()) {
            try (ResultSet r = s.executeQuery("SELECT COUNT(*), SUM(balance), BIT_XOR(CRC32(CONCAT_WS('|',mobile_number,full_name,balance))) FROM users")) {
                r.next(); snapshot.put("customers", r.getLong(1)); snapshot.put("combinedBalance", r.getString(2)); snapshot.put("accountChecksum", r.getLong(3));
                if (r.getLong(1) != CUSTOMERS || r.getBigDecimal(2).compareTo(new BigDecimal("1000000.00")) != 0) throw new IllegalStateException("Account fixture changed.");
            }
            try (ResultSet r = s.executeQuery("SELECT COUNT(*), SUM(amount), BIT_XOR(CRC32(CONCAT_WS('|',id,transaction_type,amount,details,transaction_date_time,sender_mobile_number,receiver_mobile_number,admin_username))) FROM transactions")) {
                r.next(); snapshot.put("transactions", r.getLong(1)); snapshot.put("transactionAmount", r.getString(2)); snapshot.put("transactionChecksum", r.getLong(3));
                if (r.getLong(1) != CUSTOMERS * TRANSACTIONS || r.getBigDecimal(2).compareTo(new BigDecimal("1000000.00")) != 0) throw new IllegalStateException("Transaction fixture changed.");
            }
        }
        System.out.println(JSON.toJson(snapshot));
    }

    private static void run(URI base, int clients, int warmup, int seconds, Path output) throws Exception {
        if (!base.getScheme().equals("http") || !base.getHost().equals("127.0.0.1") || base.getPort() < 1024) throw new IllegalArgumentException("Loopback test backend required.");
        if (clients < 1 || clients > CUSTOMERS || warmup < 1 || seconds < 1) throw new IllegalArgumentException("Invalid workload.");
        Files.createDirectories(output);
        try (HttpClient http = HttpClient.newBuilder().version(HttpClient.Version.HTTP_1_1).connectTimeout(Duration.ofSeconds(5)).build()) {
            List<HttpRequest> requests = new ArrayList<>();
            Set<String> cookies = new HashSet<>();
            for (int i = 1; i <= clients; i++) {
                String body = JSON.toJson(Map.of("role","user","identity",mobile(i),"pin",PIN));
                HttpResponse<String> response = http.send(HttpRequest.newBuilder(base.resolve("/api/login"))
                        .timeout(Duration.ofSeconds(10)).header("Content-Type","application/json").header("X-JCash-Request","1")
                        .POST(HttpRequest.BodyPublishers.ofString(body)).build(), HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() != 200 || !JsonParser.parseString(response.body()).getAsJsonObject().get("identity").getAsString().equals(mobile(i))) throw new IllegalStateException("Test login failed.");
                String cookie = response.headers().firstValue("set-cookie").orElseThrow().split(";",2)[0];
                if (!cookies.add(cookie)) throw new IllegalStateException("Customers must have independent sessions.");
                requests.add(HttpRequest.newBuilder(base.resolve("/api/user/dashboard")).timeout(Duration.ofSeconds(10)).header("Cookie",cookie).GET().build());
            }
            System.out.println("Warm-up started: " + warmup + " seconds; " + clients + " independent sessions.");
            Phase warmed = phase(http, requests, warmup, false);
            if (warmed.errors != 0) throw new IllegalStateException("Warm-up had errors: " + warmed.errors);
            Files.writeString(output.resolve("warmup-complete.txt"), Instant.now().toString());
            // JFR is already active during warm-up; the controller releases this
            // barrier when it is ready to sample the measured phase's resources.
            long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60);
            while (!Files.exists(output.resolve("start-measurement"))) {
                if (System.nanoTime() > deadline) throw new IllegalStateException("Measurement controller did not release barrier.");
                Thread.sleep(50);
            }
            Instant start = Instant.now();
            Phase measured = phase(http, requests, seconds, true);
            List<Sample> samples = measured.samples;
            double[] latency = samples.stream().filter(s -> s.error.equals("ok")).mapToDouble(s -> s.durationNs / 1e6).sorted().toArray();
            Map<String,Object> result = new LinkedHashMap<>();
            result.put("startedUtc", start.toString()); result.put("completedUtc", Instant.now().toString());
            result.put("clients", clients); result.put("warmupSeconds",warmup); result.put("requestedSeconds",seconds);
            result.put("elapsedSecondsIncludingDrain", measured.elapsedNs / 1e9);
            result.put("requests", samples.size()); result.put("successes",latency.length); result.put("errors",measured.errors);
            result.put("successfulRequestsPerSecond",latency.length / (measured.elapsedNs / 1e9));
            result.put("meanMs", Arrays.stream(latency).average().orElse(0));
            result.put("p50Ms",percentile(latency,.50)); result.put("p95Ms",percentile(latency,.95)); result.put("p99Ms",percentile(latency,.99));
            result.put("maxMs",latency.length == 0 ? 0 : latency[latency.length-1]);
            result.put("meanValidationMs",samples.stream().mapToDouble(s -> s.validationNs/1e6).average().orElse(0));
            result.put("httpVersion","HTTP/1.1"); result.put("model","Closed loop, one in-flight request per independent session; login excluded; latency ends after response body; throughput includes validation and drain.");
            result.put("payloadValidation","Expected identity, full name, balance, exactly 100 unique ordered fixture transactions, type, receiver, direction, amount; credential keys absent.");
            try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(Files.newOutputStream(output.resolve("requests.csv.gz"))), StandardCharsets.UTF_8))) {
                out.write("customer,startOffsetNs,durationNs,validationNs,status,result\n");
                for (Sample sample : samples) out.write(sample.customer+","+sample.startOffsetNs+","+sample.durationNs+","+sample.validationNs+","+sample.status+","+sample.error+"\n");
            }
            Files.writeString(output.resolve("result.json"),JSON.toJson(result));
            System.out.println(JSON.toJson(result));
            if (measured.errors != 0) throw new IllegalStateException("Measured requests contained errors.");
        }
    }

    private static Phase phase(HttpClient http, List<HttpRequest> requests, int seconds, boolean capture) throws Exception {
        List<Future<WorkerResult>> futures = new ArrayList<>();
        CountDownLatch ready = new CountDownLatch(requests.size());
        CountDownLatch start = new CountDownLatch(1);
        long[] timing = new long[2];
        try (ExecutorService executor = Executors.newFixedThreadPool(requests.size())) {
            for (int i = 0; i < requests.size(); i++) {
                int customer = i + 1;
                HttpRequest request = requests.get(i);
                futures.add(executor.submit(() -> {
                    List<Sample> samples = new ArrayList<>(); long errors = 0;
                    ready.countDown(); start.await();
                    while (System.nanoTime() < timing[1]) {
                        long before = System.nanoTime(), end = before, validated = before;
                        int status = 0; String error = "ok";
                        try {
                            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
                            end = System.nanoTime(); status = response.statusCode();
                            if (status != 200) error = "http_error";
                            else validate(response.body(),customer);
                            validated = System.nanoTime();
                        } catch (HttpTimeoutException e) { end=validated=System.nanoTime(); error="timeout"; }
                        catch (IOException e) { end=validated=System.nanoTime(); error="io_error"; }
                        catch (RuntimeException e) { validated=System.nanoTime(); error="invalid_response"; }
                        if (!error.equals("ok")) errors++;
                        if (capture) samples.add(new Sample(customer,before-timing[0],end-before,Math.max(0,validated-end),status,error));
                    }
                    return new WorkerResult(samples,errors);
                }));
            }
            ready.await(); timing[0]=System.nanoTime(); timing[1]=timing[0]+TimeUnit.SECONDS.toNanos(seconds); start.countDown();
            List<Sample> all = new ArrayList<>(); long errors=0;
            for (Future<WorkerResult> future : futures) { WorkerResult result=future.get(); all.addAll(result.samples); errors+=result.errors; }
            return new Phase(all,errors,System.nanoTime()-timing[0]);
        }
    }

    static void validate(String body, int customer) {
        JsonObject dashboard=JsonParser.parseString(body).getAsJsonObject();
        JsonObject user=dashboard.getAsJsonObject("user");
        if (!user.get("mobileNumber").getAsString().equals(mobile(customer))
                || !user.get("fullName").getAsString().equals("Performance Customer "+customer)
                || user.get("balance").getAsBigDecimal().compareTo(new BigDecimal("10000.00"))!=0
                || user.has("pin") || user.has("pinHash")) throw new IllegalArgumentException("Invalid customer response.");
        JsonArray transactions=dashboard.getAsJsonArray("transactions");
        if (transactions.size()!=TRANSACTIONS) throw new IllegalArgumentException("Wrong transaction count.");
        for (int i=0;i<TRANSACTIONS;i++) {
            JsonObject t=transactions.get(i).getAsJsonObject();
            if (t.get("id").getAsLong()!=(long)(customer-1)*TRANSACTIONS+TRANSACTIONS-i
                    || !t.get("type").getAsString().equals("CASH_IN")
                    || !t.get("receiver").getAsString().equals(mobile(customer))
                    || !t.get("direction").getAsString().equals("in")
                    || t.get("amount").getAsBigDecimal().compareTo(new BigDecimal("100.00"))!=0) throw new IllegalArgumentException("Invalid transaction response.");
        }
    }

    private static double percentile(double[] sorted,double quantile) { return sorted.length==0?0:sorted[(int)Math.ceil(sorted.length*quantile)-1]; }
    private record Sample(int customer,long startOffsetNs,long durationNs,long validationNs,int status,String error) { }
    private record WorkerResult(List<Sample> samples,long errors) { }
    private record Phase(List<Sample> samples,long errors,long elapsedNs) { }
}
