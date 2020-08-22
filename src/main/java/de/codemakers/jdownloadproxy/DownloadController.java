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
import io.micronaut.http.annotation.PathVariable;
import io.micronaut.http.annotation.QueryValue;
import io.micronaut.http.server.types.files.StreamedFile;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.stream.Collectors;

@Controller("/download")
public class DownloadController {
    
    @Get(uri = "/shutdown", produces = MediaType.APPLICATION_JSON)
    public String shutdown(@QueryValue(defaultValue = "-1") long delay) {
        if (delay > 0) {
            System.out.printf("Shutdown requested in %d ms%n", delay);
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    System.exit(0);
                }
            }, delay);
            return String.format("{\"done\": %b, \"delay\": \"%d\"}", true, delay);
        } else {
            System.out.println("Shutting down");
            System.exit(0);
            return String.format("{\"done\": %b}", true);
        }
    }
    
    @Get(uri = "/add", produces = MediaType.TEXT_PLAIN)
    public String addDownload(@QueryValue String url, @QueryValue(defaultValue = "false") boolean forceDownload) throws MalformedURLException {
        System.out.printf("[DEBUG][%s#addDownload] url=\"%s\", forceDownload=%b%n", getClass().getSimpleName(), url, forceDownload); //DEBUG
        System.out.printf("[DEBUG][%s#addDownload] forceDownload=%b%n", getClass().getSimpleName(), forceDownload); //DEBUG
        final DownloadContainer downloadContainer = Downloader.createDownloadContainer(new URL(url), forceDownload);
        downloadContainer.startAsync(); //FIXME What if there is already someone downloading that url?!
        return downloadContainer.getDownloadInfo().getUuid().toString();
    }
    
    @Get(uri = "/status/{uuid}", produces = MediaType.APPLICATION_JSON)
    public String statusDownload(@PathVariable String uuid) throws JsonProcessingException {
        System.out.printf("[DEBUG][%s#statusDownload] uuid=\"%s\"%n", getClass().getSimpleName(), uuid); //DEBUG
        final DownloadContainer downloadContainer = Downloader.getDownloadContainer(UUID.fromString(uuid));
        System.out.printf("[DEBUG][%s#statusDownload] downloadContainer=%s%n", getClass().getSimpleName(), downloadContainer); //DEBUG
        if (downloadContainer == null) {
            return null;
        }
        return DownloadInfo.DownloadInfoSerializer.createObjectMapper().writeValueAsString(downloadContainer.getDownloadInfo());
    }
    
    @Get(uri = "/status", produces = MediaType.APPLICATION_JSON)
    public String statusDownloadUrl(@QueryValue String url) throws JsonProcessingException, MalformedURLException {
        System.out.printf("[DEBUG][%s#statusDownload] url=\"%s\"%n", getClass().getSimpleName(), url); //DEBUG
        final URL url_ = new URL(url);
        final List<DownloadContainer> downloadContainers = Downloader.getDownloadContainers().stream().filter((downloadContainer) -> url_.equals(downloadContainer.getDownloadInfo().getUrl())).collect(Collectors.toList());
        System.out.printf("[DEBUG][%s#statusDownload] downloadContainers=%s%n", getClass().getSimpleName(), downloadContainers); //DEBUG
        if (downloadContainers.isEmpty()) {
            return "[]";
        }
        return DownloadInfo.DownloadInfoSerializer.createObjectMapper().writeValueAsString(downloadContainers.stream().map(DownloadContainer::getDownloadInfo).collect(Collectors.toList())); //TODO //TEST //IMPORTANT
    }
    
    public static final String FILENAME_NONE = "//\\NONE\\//";
    
    @Get(uri = "/get/{uuid}", produces = MediaType.APPLICATION_OCTET_STREAM)
    //TODO Add parameter if the local file should be deleted after it has been downloaded by the client
    public StreamedFile getDownload(@PathVariable String uuid, @QueryValue(defaultValue = FILENAME_NONE) String filename, @QueryValue(defaultValue = "true") boolean delete) throws FileNotFoundException {
        System.out.printf("[DEBUG][%s#getDownload] uuid=\"%s\", filename=\"%s\", delete=%b%n", getClass().getSimpleName(), uuid, filename, delete); //DEBUG
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
    
    @Get(uri = "/get", produces = MediaType.APPLICATION_OCTET_STREAM)
    //TODO Add parameter if the local file should be deleted after it has been downloaded by the client
    public StreamedFile getDownloadUrl(@QueryValue String url, @QueryValue(defaultValue = FILENAME_NONE) String filename, @QueryValue(defaultValue = "true") boolean delete) throws FileNotFoundException, MalformedURLException {
        System.out.printf("[DEBUG][%s#getDownload] url=\"%s\", filename=\"%s\", delete=%b%n", getClass().getSimpleName(), url, filename, delete); //DEBUG
        final URL url_ = new URL(url);
        final List<DownloadContainer> downloadContainers = Downloader.getDownloadContainers().stream().filter((downloadContainer) -> url_.equals(downloadContainer.getDownloadInfo().getUrl())).collect(Collectors.toList());
        System.out.printf("[DEBUG][%s#getDownload] downloadContainers=%s%n", getClass().getSimpleName(), downloadContainers); //DEBUG
        if (downloadContainers.isEmpty() || downloadContainers.stream().map(DownloadContainer::getDownloadInfo).noneMatch(DownloadInfo::isDone)) {
            return null;
        }
        final DownloadContainer downloadContainer = downloadContainers.stream().filter((downloadContainer_) -> downloadContainer_.getDownloadInfo().isDone()).findAny().orElse(null);
        if (downloadContainer == null) {
            return null;
        }
        if (FILENAME_NONE.equals(filename)) {
            filename = downloadContainer.getDownloadInfo().getFilename();
        }
        filename = Util.sanitizeFilename(filename);
        return new StreamedFile(new FileInputStream(downloadContainer.getFile()), MediaType.APPLICATION_OCTET_STREAM_TYPE).attach(filename);
    }
    
    @Get(uri = "/remove/{uuid}", produces = MediaType.APPLICATION_JSON)
    public String removeDownload(@PathVariable String uuid, @QueryValue(defaultValue = "false") boolean delete) {
        System.out.printf("[DEBUG][%s#removeDownload] uuid=\"%s\", delete=%b%n", getClass().getSimpleName(), uuid, delete); //DEBUG
        final DownloadContainer downloadContainer = Downloader.getDownloadContainer(UUID.fromString(uuid));
        System.out.printf("[DEBUG][%s#removeDownload] downloadContainer=%s%n", getClass().getSimpleName(), downloadContainer); //DEBUG
        if (downloadContainer == null || !downloadContainer.getDownloadInfo().isDone()) {
            return "{\"removed\": false, \"deleted\": false}";
        }
        boolean deleted = false;
        if (delete) {
            deleted = Downloader.removeFile(downloadContainer.getFile(), downloadContainer.getDownloadInfo().getUrl(), true);
        }
        return String.format("{\"removed\": %b, \"deleted\": %b}", Downloader.removeDownloadContainer(downloadContainer.getDownloadInfo().getUuid()), deleted);
    }
    
    @Get(uri = "/remove", produces = MediaType.APPLICATION_JSON)
    public String removeDownloadUrl(@QueryValue String url, @QueryValue(defaultValue = "false") boolean delete) throws MalformedURLException {
        System.out.printf("[DEBUG][%s#removeDownload] url=\"%s\", delete=%b%n", getClass().getSimpleName(), url, delete); //DEBUG
        final URL url_ = new URL(url);
        final List<DownloadContainer> downloadContainers = Downloader.getDownloadContainers().stream().filter((downloadContainer) -> url_.equals(downloadContainer.getDownloadInfo().getUrl())).collect(Collectors.toList());
        System.out.printf("[DEBUG][%s#removeDownload] downloadContainers=%s%n", getClass().getSimpleName(), downloadContainers); //DEBUG
        if (downloadContainers.isEmpty() || !downloadContainers.stream().map(DownloadContainer::getDownloadInfo).allMatch(DownloadInfo::isDone)) {
            boolean deleted = false;
            if (delete) {
                final String hash = Downloader.getHashForURL(url_);
                final File file = Downloader.getFileForHash(hash);
                deleted = Downloader.removeFile(file, url_, true);
            }
            return String.format("{\"removed\": 0, \"deleted\": %b}", deleted);
        }
        int removed = 0;
        for (DownloadContainer downloadContainer : downloadContainers) {
            if (Downloader.removeDownloadContainer(downloadContainer.getDownloadInfo().getUuid())) {
                removed++;
            }
        }
        boolean deleted = false;
        if (delete) {
            deleted = downloadContainers.stream().anyMatch((downloadContainer) -> Downloader.removeFile(downloadContainer.getFile(), downloadContainer.getDownloadInfo().getUrl(), true));
        }
        return String.format("{\"removed\": %d, \"deleted\": %b}", removed, deleted);
    }
    
}
