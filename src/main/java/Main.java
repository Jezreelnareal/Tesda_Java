import shared.api.ApiServer;

public class Main {

    public static void main(String[] args) throws Exception {
        int port = Integer.parseInt(System.getenv().getOrDefault("JCASH_API_PORT", "8080"));
        ApiServer server = new ApiServer(port);
        Runtime.getRuntime().addShutdownHook(new Thread(server::close));
        server.start();
        System.out.println("JCash API listening on http://127.0.0.1:" + port);
        System.out.println("Open http://localhost:3000 after starting the Next.js frontend.");
    }
}
