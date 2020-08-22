package de.codemakers.jdownloadproxy;

import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.client.annotation.Client;
import io.micronaut.http.server.types.files.StreamedFile;

@Client("/download")
public interface DownloadClient {
    
    @Get(processes = MediaType.APPLICATION_OCTET_STREAM)
    StreamedFile download(@PathVariable(name = "url", defaultValue = "DEFAULT") String url);
    
}
