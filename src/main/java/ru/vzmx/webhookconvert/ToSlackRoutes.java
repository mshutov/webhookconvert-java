package ru.vzmx.webhookconvert;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.undertow.server.HttpServerExchange;
import org.apache.commons.io.IOUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.util.function.Function;

final class ToSlackRoutes {
    private final static ObjectMapper mapper = new ObjectMapper();

    private ToSlackRoutes() {
    }

    static void fromJira(HttpServerExchange exchange) {
        from(exchange, ToSlackRoutes::buildSlackHookFromJira);
    }

    static void fromMS(HttpServerExchange exchange) {
        from(exchange, ToSlackRoutes::buildSlackHookFromMS);
    }

    private static void from(HttpServerExchange exchange, Function<HttpServerExchange, SlackHook> f){
        CloseableHttpClient httpClient = HttpClients.createDefault();
        HttpPost post = new HttpPost(buildSlackUrl(exchange));
        post.setEntity(buildResponseEntity(exchange, f));
        try (CloseableHttpResponse response = httpClient.execute(post)) {
            response.getEntity().writeTo(exchange.getOutputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static StringEntity buildResponseEntity(HttpServerExchange exchange, Function<HttpServerExchange, SlackHook> f) {
        StringEntity entity;
        try {
            entity = new StringEntity(mapper.writeValueAsString(f.apply(exchange)), "UTF-8");
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return entity;
    }

    private static SlackHook buildSlackHookFromMS(HttpServerExchange exchange) {
        String webhook;
        try {
            webhook = IOUtils.toString(exchange.getInputStream(), "UTF-8");
        } catch (IOException e) {
            webhook = e.getMessage();
        }
        return new SlackHook("```" + webhook + "```");
    }

    private static SlackHook buildSlackHookFromJira(HttpServerExchange exchange) {
        JiraHook hook;
        try {
            hook = mapper.readValue(exchange.getInputStream(), JiraHook.class);
        } catch (IOException e) {
            hook = new JiraHook();
        }

        String text;
        if (hook.version != null && hook.version.description != null) {
            text = hook.version.description + " " + buildJiraUrl(exchange);
        } else {
            text = buildJiraUrl(exchange);
        }
        return new SlackHook(text);
    }

    private static String buildJiraUrl(HttpServerExchange exchange) {
        String subDomain = getParam(exchange, "subDomain");
        String project = getParam(exchange, "project");
        String version = getParam(exchange, "version");
        String jiraUrlPattern = "https://%s.atlassian.net/projects/%s/versions/%s/";
        return String.format(jiraUrlPattern, subDomain, project, version);
    }

    private static String buildSlackUrl(HttpServerExchange exchange) {
        String p1 = getParam(exchange, "p1");
        String p2 = getParam(exchange, "p2");
        String p3 = getParam(exchange, "p3");
        String slackUrlPattern = "https://hooks.slack.com/services/%s/%s/%s";
        return String.format(slackUrlPattern, p1, p2, p3);
    }

    private static String getParam(HttpServerExchange exchange, String key) {
        return exchange.getQueryParameters().get(key).getFirst();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class JiraHook {
        @JsonProperty
        private Version version;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class Version {
        @JsonProperty
        private String description;
    }

}
