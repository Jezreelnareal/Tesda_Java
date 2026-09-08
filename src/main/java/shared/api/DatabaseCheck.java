package shared.api;

import java.sql.SQLException;

@FunctionalInterface
public interface DatabaseCheck {
    void run() throws SQLException;
}
