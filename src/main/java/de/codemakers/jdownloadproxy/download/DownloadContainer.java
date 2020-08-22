package de.codemakers.jdownloadproxy.download;

import de.codemakers.jdownloadproxy.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class DownloadContainer {
    
    private final DownloadInfo downloadInfo;
    private transient File file;
    private transient boolean forceDownload;
    
    public DownloadContainer(URL url) {
        this(UUID.randomUUID(), url, null);
    }
    
    public DownloadContainer(UUID uuid, URL url) {
        this(uuid, url, null);
    }
    
    public DownloadContainer(UUID uuid, URL url, String filename) {
        this(uuid, url, DownloadStatus.QUEUED, filename, null);
    }
    
    public DownloadContainer(UUID uuid, URL url, DownloadStatus downloadStatus, String filename, String hash_sha256_base64) {
        this(new DownloadInfo(uuid, url, downloadStatus, filename, hash_sha256_base64));
    }
    
    public DownloadContainer(DownloadInfo downloadInfo) {
        this.downloadInfo = downloadInfo;
    }
    
    private boolean checkCache() {
        Downloader.loadHashes();
        final URL url = getDownloadInfo().getUrl();
        if (Downloader.hasHashForURL(url)) {
            final String hash = Downloader.getHashForURL(url);
            final File file = Downloader.getFileForHash(hash);
            final String filename = Downloader.getFilenameForURL(url);
            if (file != null) {
                finish(file, filename, hash);
                return true;
            } else {
                Downloader.removeHashForURL(url);
                Downloader.removeFilenameForURL(url);
                Downloader.saveHashes();
            }
        }
        return false;
    }
    
    public DownloadInfo getDownloadInfo() {
        return downloadInfo;
    }
    
    public File getFile() {
        return file;
    }
    
    public DownloadContainer setFile(File file) {
        this.file = file;
        return this;
    }
    
    public boolean isForceDownload() {
        return forceDownload;
    }
    
    public DownloadContainer setForceDownload(boolean forceDownload) {
        this.forceDownload = forceDownload;
        return this;
    }
    
    public void startAsync() {
        Downloader.startDownloadContainerAsync(this);
    }
    
    public void startVoid() {
        start();
    }
    
    public boolean start() {
        return start(isForceDownload());
    }
    
    public boolean start(boolean forceDownload) {
        final DownloadStatus downloadStatus = getDownloadInfo().getDownloadStatus();
        if (downloadStatus.isDone() || downloadStatus.isLocked()) {
            return false;
        }
        getDownloadInfo().setDownloadStatus(DownloadStatus.CHECKING);
        if (!forceDownload) {
            if (checkCache()) {
                getDownloadInfo().setDownloadStatus(DownloadStatus.FINISHED);
                return true;
            }
        }
        getDownloadInfo().setDownloadStatus(DownloadStatus.DOWNLOADING);
        if (forceDownload) {
            Downloader.removeHashForURL(getDownloadInfo().getUrl());
            Downloader.removeFilenameForURL(getDownloadInfo().getUrl());
        }
        final boolean successful = download();
        System.out.println(String.format("[INFO ][%s#start] Download was %ssuccessful (\"%s\")", getClass().getSimpleName(), successful ? "" : "not ", getDownloadInfo().getUrl())); //DEBUG
        return successful;
    }
    
    private boolean download() {
        final File tempFile = Downloader.createTempFileForUUID(getDownloadInfo().getUuid());
        final URL url = getDownloadInfo().getUrl();
        try (final InputStream inputStream = url.openStream()) {
            String filename = url.getFile();
            final int index = filename.lastIndexOf("/");
            if (index != -1) {
                filename = filename.substring(index + 1);
            }
            filename = Util.sanitizeFilename(filename);
            long transferred;
            try (final FileOutputStream fileOutputStream = new FileOutputStream(tempFile, false)) {
                transferred = inputStream.transferTo(fileOutputStream);
            }
            System.out.printf("[DEBUG][%s#download] Transferred %d Bytes from \"%s\" to \"%s\"%n", getClass().getSimpleName(), transferred, url, tempFile.getAbsolutePath()); //DEBUG
            final Map.Entry<File, String> entry = Downloader.handleFile(url, tempFile, filename, isForceDownload());
            if (entry == null) {
                error(new NullPointerException("entry is null"));
                return false;
            } final File file = entry.getKey();
            if (file == null) {
                error(new NullPointerException("file is null"));
                return false;
            }
            finish(file, filename, entry.getValue());
            return true;
        } catch (Exception ex) {
            error(ex);
            return false;
        }
    }
    
    private void finish(File file, String filename, String hash) {
        getDownloadInfo().setTimestampNow();
        System.out.printf("[DEBUG][%s#finish] Finished downloading: \"%s\"%n", getClass().getSimpleName(), getDownloadInfo().getUrl()); //DEBUG
        setFile(file);
        getDownloadInfo().setFilename(filename);
        getDownloadInfo().setHash(hash);
        getDownloadInfo().setDownloadStatus(DownloadStatus.FINISHED);
    }
    
    private void error(Throwable throwable) {
        getDownloadInfo().setTimestampNow();
        System.err.printf("[ERROR][%s#error] Failed downloading: \"%s\"%n", getClass().getSimpleName(), getDownloadInfo().getUrl()); //DEBUG
        if (throwable != null) {
            throwable.printStackTrace(); //DEBUG
        }
        setFile(null);
        getDownloadInfo().setFilename(null);
        getDownloadInfo().setDownloadStatus(DownloadStatus.ERRORED);
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final DownloadContainer that = (DownloadContainer) other;
        return Objects.equals(downloadInfo, that.downloadInfo);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(downloadInfo);
    }
    
    @Override
    public String toString() {
        return "DownloadContainer{" + "downloadInfo=" + downloadInfo + ", file=" + file + ", forceDownload=" + forceDownload + '}';
    }
    
}
