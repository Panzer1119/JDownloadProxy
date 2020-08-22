/*
 *    Copyright 2020 Paul Hagedorn (Panzer1119)
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *
 */

package de.codemakers.jdownloadproxy;

import io.micronaut.context.ApplicationContext;
import io.micronaut.core.type.Argument;
import io.micronaut.http.HttpRequest;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.client.HttpClient;
import io.micronaut.http.client.netty.DefaultHttpClient;
import io.micronaut.http.netty.NettyHttpParameters;
import io.micronaut.runtime.EmbeddedApplication;
import io.micronaut.runtime.server.EmbeddedServer;
import io.micronaut.test.annotation.MicronautTest;
import io.netty.buffer.CompositeByteBuf;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.charset.Charset;

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
        System.out.println("server=" + server);
        System.out.println("client=" + client);
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
        //Thread.sleep(5000);
        final HttpRequest request = HttpRequest.GET("/hello");
        final String string = client.toBlocking().retrieve(request, Argument.of(String.class));
        Assertions.assertEquals("hi", string);
    }
    
    @Test
    public void testDownload() throws IOException {
        final HttpRequest request = HttpRequest.GET("/download");
        //request.setAttribute("url", "https://google.de");
        System.out.println("request=" + request.getParameters());
        ((NettyHttpParameters) request.getParameters()).add("url", "test");
        System.out.println("request=" + request.getParameters());
        //final StreamedFile streamedFile = client.toBlocking().retrieve(request, StreamedFile.class);
        //Assertions.assertNotNull(streamedFile);
        //System.out.println("streamedFile=" + streamedFile);
        //final byte[] data = streamedFile.getInputStream().readAllBytes();
        //System.out.println("data=\n" + new String(data));
        //final String data = client.toBlocking().retrieve(request, String.class);
        //Files.write(new File("test.mp4").toPath(),data.getBytes());
        //client.toBlocking().retrieve(HttpRequest.create(HttpMethod.CUSTOM,""));
        final HttpResponse response = ((DefaultHttpClient) client).toBlocking().exchange(request, Object.class);
        System.out.println("response=" + response);
        System.out.println("response=" + response.getAttributes());
        System.out.println("response=" + response.getContentLength());
        System.out.println("response=" + response.getContentType());
        System.out.println("response=" + response.getStatus());
        System.out.println("response=" + response.getHeaders());
        System.out.println("response=" + response.getHeaders().getOrigin());
        System.out.println("response=" + response.reason());
        System.out.println("response=" + response.code());
        System.out.println("response=" + response.code());
        final Object body = response.body();
        System.out.println("body==null=" + (body == null));
        Assertions.assertNotNull(body);
        /*
        final String bodyString = "" + body;
        if (bodyString != null && bodyString.length() >= 2048) {
            System.out.println(String.format("body=%n%s%n%n[...]%n%n%s", bodyString.substring(0, 1024), bodyString.substring(bodyString.length() - 1024)));
        } else {
            System.out.println("body=" + bodyString);
        }
        */
        final CompositeByteBuf compositeByteBuf = (CompositeByteBuf) body;
        if (body != null) {
            //System.out.println("compositeByteBuf=" + compositeByteBuf.capacity());
            //System.out.println("compositeByteBuf=" + compositeByteBuf);
            //System.out.println("compositeByteBuf=" + compositeByteBuf.skipBytes(compositeByteBuf.capacity()));
            //System.out.println("compositeByteBuf=" + compositeByteBuf.capacity());
            System.out.println("compositeByteBuf=" + compositeByteBuf.retainedDuplicate().toString(0, 1000, Charset.defaultCharset()));
            System.out.println("body.getClass()=" + body.getClass());
        }
        
    }
    
}
