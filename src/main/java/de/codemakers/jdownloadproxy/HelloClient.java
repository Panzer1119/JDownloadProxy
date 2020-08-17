package de.codemakers.jdownloadproxy;

import io.micronaut.http.annotation.Get;
import io.micronaut.http.client.annotation.Client;

@Client("/hello")
public interface HelloClient {
    
    @Get
    String index();
    
}
