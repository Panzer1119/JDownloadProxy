package de.codemakers.jdownloadproxy;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.client.HttpClient;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Assertions;

import javax.inject.Inject;
import java.net.MalformedURLException;
import java.net.URL;

@MicronautTest
public class JdownloadproxyTest {

    private static EmbeddedServer server;
    private static HttpClient client;
    
    @Inject
    EmbeddedApplication application;
    
    @BeforeAll
    public static void setupServer() {
        server = ApplicationContext.run(EmbeddedServer.class);
        client = server.getApplicationContext().createBean(HttpClient.class, server.getURL());
        //client = HttpClient.create(new URL("http://localhost:8080"));
    }
    
    @AfterAll
    public static void stopServer() {
        if (server != null) {
            server.stop();
        }
        if (client != null) {
            client.stop();
        }
    }

    @Test
    public void testItWorks() {
        Assertions.assertTrue(application.isRunning());
    }
    
    @Test
    public void retrieveString() throws InterruptedException {
        Thread.sleep(5000);
        final HttpRequest request = HttpRequest.GET("/hello");
        final String string = client.toBlocking().retrieve(request, Argument.of(String.class));
        Assertions.assertEquals("hi", string);
    }

}
