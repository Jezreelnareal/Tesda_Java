package shared.api;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Locale;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/** Adapts JCash's existing opaque sessions to Spring Security. */
final class SessionAuthenticationFilter extends OncePerRequestFilter {
    static final String SESSION_ATTRIBUTE = "jcash.session";
    private final SessionStore sessions;

    SessionAuthenticationFilter(SessionStore sessions) { this.sessions = sessions; }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String method = request.getMethod();
        if (!method.equals("GET") && !method.equals("POST")) {
            error(response, 405, "Method not allowed."); return;
        }
        if (method.equals("POST") && !"1".equals(request.getHeader("X-JCash-Request"))) {
            error(response, 403, "Request verification failed."); return;
        }
        if (method.equals("GET") && request.getRequestURI().equals("/api/health")) {
            chain.doFilter(request, response); return;
        }
        SessionStore.Session session;
        try { session = sessions.open(cookie(request)); }
        catch (IllegalStateException e) { error(response, 503, "Server is busy. Try again later."); return; }
        // Preserve the original per-session serialization and login-attempt accounting.
        synchronized (session) {
            request.setAttribute(SESSION_ATTRIBUTE, session);
            setCookie(response, session);
            if (session.role() != null) {
                var context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(session.identity(), null,
                        List.of(new SimpleGrantedAuthority("ROLE_" + session.role().toUpperCase(Locale.ROOT)))));
                SecurityContextHolder.setContext(context);
            }
            chain.doFilter(request, response);
        }
    }

    private static String cookie(HttpServletRequest request) {
        String cookies = request.getHeader("Cookie");
        if (cookies != null) for (String entry : cookies.split(";")) {
            String[] pair = entry.trim().split("=", 2);
            if (pair.length == 2 && pair[0].equals("jcash_session")) return pair[1];
        }
        return null;
    }

    static void setCookie(HttpServletResponse response, SessionStore.Session session) {
        response.setHeader("Set-Cookie", "jcash_session=" + session.token() + "; Path=/; HttpOnly; SameSite=Strict");
    }

    static void error(HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json; charset=utf-8");
        response.setHeader("Cache-Control", "no-store");
        response.getWriter().write("{\"error\":\"" + message + "\"}");
    }
}
