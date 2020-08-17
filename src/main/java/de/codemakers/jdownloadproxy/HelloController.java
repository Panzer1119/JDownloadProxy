package de.codemakers.jdownloadproxy;

import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;

@Controller("/hello")
public class HelloController {
    
    @Get()
    public String index() {
        return "hi";
    }
    
}
