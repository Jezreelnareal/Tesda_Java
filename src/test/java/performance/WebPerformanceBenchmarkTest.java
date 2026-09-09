package performance;

import com.google.gson.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class WebPerformanceBenchmarkTest {
    private static JsonObject fixture() {
        JsonObject root=new JsonObject(),user=new JsonObject();
        user.addProperty("mobileNumber","09900000001");
        user.addProperty("fullName","Performance Customer 1");
        user.addProperty("balance","10000.00");
        root.add("user",user);
        JsonArray transactions=new JsonArray();
        for (int id=100;id>=1;id--) {
            JsonObject transaction=new JsonObject();
            transaction.addProperty("id",id); transaction.addProperty("type","CASH_IN");
            transaction.addProperty("amount","100.00"); transaction.addProperty("receiver","09900000001");
            transaction.addProperty("direction","in"); transactions.add(transaction);
        }
        root.add("transactions",transactions);
        return root;
    }

    @Test void acceptsCompleteExpectedDashboard() {
        assertDoesNotThrow(()->WebPerformanceBenchmark.validate(fixture().toString(),1));
    }

    @Test void rejectsFastButWrongAccountOrBalance() {
        JsonObject response=fixture();
        response.getAsJsonObject("user").addProperty("mobileNumber","09900000002");
        assertThrows(IllegalArgumentException.class,()->WebPerformanceBenchmark.validate(response.toString(),1));
        response.getAsJsonObject("user").addProperty("mobileNumber","09900000001");
        response.getAsJsonObject("user").addProperty("balance","0.00");
        assertThrows(IllegalArgumentException.class,()->WebPerformanceBenchmark.validate(response.toString(),1));
    }

    @Test void rejectsIncompleteOrDuplicatedHistory() {
        JsonObject response=fixture();
        response.getAsJsonArray("transactions").get(0).getAsJsonObject().addProperty("id",99);
        assertThrows(IllegalArgumentException.class,()->WebPerformanceBenchmark.validate(response.toString(),1));
        response.getAsJsonArray("transactions").remove(0);
        assertThrows(IllegalArgumentException.class,()->WebPerformanceBenchmark.validate(response.toString(),1));
    }

    @Test void rejectsCredentialFieldsAndErrorPayloads() {
        JsonObject response=fixture();
        response.getAsJsonObject("user").addProperty("pinHash","must-not-be-returned");
        assertThrows(IllegalArgumentException.class,()->WebPerformanceBenchmark.validate(response.toString(),1));
        assertThrows(RuntimeException.class,()->WebPerformanceBenchmark.validate("{\"error\":\"Database unavailable\"}",1));
    }
}
