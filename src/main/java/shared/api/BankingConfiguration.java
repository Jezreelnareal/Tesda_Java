package shared.api;

import admin.api.AdminApi;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import shared.service.Auth;
import shared.util.DatabaseConnection;
import user.api.UserApi;

@Configuration(proxyBeanMethods = false)
public class BankingConfiguration {
    @Bean SessionStore sessionStore() { return new SessionStore(); }
    @Bean Auth auth() { return new Auth(); }
    @Bean UserApi userApi() { return new UserApi(); }
    @Bean AdminApi adminApi() { return new AdminApi(); }
    @Bean DatabaseCheck databaseCheck(Environment environment) {
        DatabaseConnection.configureSettings(environment::getProperty);
        return DatabaseConnection::verifyConnection;
    }
    @Bean DisposableBean databaseShutdown() { return DatabaseConnection::shutdown; }
}
