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

import com.fasterxml.jackson.core.JsonProcessingException;
import de.codemakers.jdownloadproxy.download.DownloadContainer;
import de.codemakers.jdownloadproxy.download.DownloadInfo;
import de.codemakers.jdownloadproxy.download.Downloader;
import io.micronaut.http.MediaType;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Get;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.server.types.files.StreamedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.UUID;

@Controller("/download")
public class DownloadController {
    
    @Get(produces = MediaType.APPLICATION_OCTET_STREAM)
    public StreamedFile download(@QueryValue("url") String url) throws FileNotFoundException {
        System.out.println("url=" + url);
        /*
        try (final FileInputStream fileInputStream = new FileInputStream(new File("O:\\ArchiveBox\\MCPRBackup\\Live\\archive\\1597545353\\media\\Friday Facts #351 - Beacon re-redesign & Simplified fluid mixing _ Factorio (2)-fff-351-2.info.json"))) {
            return new StreamedFile(fileInputStream, MediaType.TEXT_JSON_TYPE);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
        */
        try (final FileInputStream fileInputStream = new FileInputStream(new File("O:\\ArchiveBox\\MCPRBackup\\Live\\archive\\1597545353\\media\\Friday Facts #351 - Beacon re-redesign & Simplified fluid mixing _ Factorio (2)-fff-351-2.mp4"))) {
            return new StreamedFile(fileInputStream, MediaType.APPLICATION_OCTET_STREAM_TYPE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    @Get(uri = "/add", produces = MediaType.TEXT_PLAIN)
    public String addDownload(@QueryValue String url, @QueryValue(defaultValue = "0") boolean forceDownload) throws MalformedURLException {
        System.out.printf("[DEBUG][%s#addDownload] forceDownload=%b%n", getClass().getSimpleName(), forceDownload); //DEBUG
        final DownloadContainer downloadContainer = Downloader.createDownloadContainer(new URL(url), forceDownload);
        downloadContainer.startAsync();
        return downloadContainer.getDownloadInfo().getUuid().toString();
    }
    
    @Get(uri = "/status", produces = MediaType.APPLICATION_JSON)
    public String statusDownload(@QueryValue String uuid) throws JsonProcessingException {
        final DownloadContainer downloadContainer = Downloader.getDownloadContainer(UUID.fromString(uuid));
        System.out.printf("[DEBUG][%s#statusDownload] downloadContainer=%s%n", getClass().getSimpleName(), downloadContainer); //DEBUG
        if (downloadContainer == null) {
            return null;
        }
        return DownloadInfo.DownloadInfoSerializer.createObjectMapper().writeValueAsString(downloadContainer.getDownloadInfo());
    }
    
    public static final String FILENAME_NONE = "//\\NONE\\//";
    
    @Get(uri = "/get", produces = MediaType.APPLICATION_OCTET_STREAM)
    public StreamedFile getDownload(@QueryValue String uuid, @QueryValue(defaultValue = FILENAME_NONE) String filename) throws FileNotFoundException {
        final DownloadContainer downloadContainer = Downloader.getDownloadContainer(UUID.fromString(uuid));
        System.out.printf("[DEBUG][%s#getDownload] downloadContainer=%s%n", getClass().getSimpleName(), downloadContainer); //DEBUG
        if (downloadContainer == null || !downloadContainer.getDownloadInfo().isDone()) {
            return null;
        }
        if (FILENAME_NONE.equals(filename)) {
            filename = downloadContainer.getDownloadInfo().getFilename();
        }
        filename = Util.sanitizeFilename(filename);
        return new StreamedFile(new FileInputStream(downloadContainer.getFile()), MediaType.APPLICATION_OCTET_STREAM_TYPE).attach(filename);
    }
    
    @Get(uri = "/remove", produces = MediaType.APPLICATION_JSON)
    public String removeDownload(@QueryValue String uuid) {
        final DownloadContainer downloadContainer = Downloader.getDownloadContainer(UUID.fromString(uuid));
        System.out.printf("[DEBUG][%s#removeDownload] downloadContainer=%s%n", getClass().getSimpleName(), downloadContainer); //DEBUG
        if (downloadContainer == null) {
            return "{\"removed\": false}";
        }
        return String.format("{\"removed\": %b}", Downloader.removeDownloadContainer(downloadContainer.getDownloadInfo().getUuid()));
    }
    
}
