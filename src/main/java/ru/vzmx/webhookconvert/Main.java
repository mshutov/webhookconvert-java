package ru.vzmx.webhookconvert;

import io.undertow.Handlers;
import io.undertow.Undertow;
import io.undertow.server.RoutingHandler;
import io.undertow.server.handlers.BlockingHandler;

public final class Main {
    private static final RoutingHandler ROUTES = Handlers.routing()
            .post("/j2s/{p1}/{p2}/{p3}/{subDomain}/{project}/{version}", new BlockingHandler(JiraToSlackRoutes::jiraToSlack));

    private Main() {
    }

    public static void main(String[] args) {
        validateArgs(args);
        Undertow server = Undertow.builder()
                .addHttpListener(port(args), host(args))
                .setHandler(ROUTES).build();
        server.start();
    }

    private static void validateArgs(String[] args) {
        if (args.length != 2) {
            throw new IllegalArgumentException("Should provide 2 arguments: host and port to listen on. For example: localhost 8080");
        }
    }

    private static String host(String[] args) {
        return args[0];
    }

    private static int port(String[] args) {
        return Integer.parseInt(args[1]);
    }
}
