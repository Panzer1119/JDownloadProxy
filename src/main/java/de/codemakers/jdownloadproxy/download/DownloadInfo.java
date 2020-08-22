package de.codemakers.jdownloadproxy.download;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.net.URL;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

public class DownloadInfo {
    
    public static final String DEFAULT_FILENAME = "unknown";
    
    private final UUID uuid;
    private final URL url;
    private String filename;
    private volatile DownloadStatus downloadStatus;
    private String hash;
    private ZonedDateTime timestamp = null;
    
    public DownloadInfo(UUID uuid, URL url, DownloadStatus downloadStatus, String filename, String hash) {
        this.uuid = uuid;
        this.url = url;
        this.downloadStatus = downloadStatus;
        this.filename = filename;
        this.hash = hash;
    }
    
    public UUID getUuid() {
        return uuid;
    }
    
    public URL getUrl() {
        return url;
    }
    
    public String getFilename() {
        return filename;
    }
    
    public String resolveFilename() {
        if (filename == null || filename.isBlank()) {
            return DEFAULT_FILENAME;
        }
        return filename;
    }
    
    public DownloadInfo setFilename(String filename) {
        this.filename = filename;
        return this;
    }
    
    public DownloadStatus getDownloadStatus() {
        if (downloadStatus == null) {
            return DownloadStatus.UNKNOWN;
        }
        return downloadStatus;
    }
    
    public boolean isDone() {
        final DownloadStatus downloadStatus = getDownloadStatus();
        return downloadStatus.isDone() && downloadStatus != DownloadStatus.UNKNOWN;
    }
    
    public DownloadInfo setDownloadStatus(DownloadStatus downloadStatus) {
        this.downloadStatus = downloadStatus;
        return this;
    }
    
    public String getHash() {
        return hash;
    }
    
    public DownloadInfo setHash(String hash) {
        this.hash = hash;
        return this;
    }
    
    public ZonedDateTime getTimestamp() {
        return timestamp;
    }
    
    public DownloadInfo setTimestamp(ZonedDateTime timestamp) {
        this.timestamp = timestamp;
        return this;
    }
    
    public DownloadInfo setTimestampNow() {
        return setTimestamp(ZonedDateTime.now());
    }
    
    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (other == null || getClass() != other.getClass()) {
            return false;
        }
        final DownloadInfo that = (DownloadInfo) other;
        return Objects.equals(uuid, that.uuid);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(uuid);
    }
    
    @Override
    public String toString() {
        return "DownloadInfo{" + "uuid=" + uuid + ", url=" + url + ", filename='" + filename + '\'' + ", downloadStatus=" + downloadStatus + ", hash='" + hash + '\'' + ", timestamp=" + timestamp + '}';
    }
    
    public static class DownloadInfoSerializer extends StdSerializer<DownloadInfo> {
        
        protected DownloadInfoSerializer(Class<DownloadInfo> clazz) {
            super(clazz);
        }
        
        @Override
        public void serialize(DownloadInfo downloadInfo, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            jsonGenerator.writeStringField("uuid", downloadInfo.getUuid().toString());
            jsonGenerator.writeStringField("url", downloadInfo.getUrl().toString());
            jsonGenerator.writeStringField("filename", downloadInfo.getFilename());
            jsonGenerator.writeStringField("status", downloadInfo.getDownloadStatus().name());
            jsonGenerator.writeBooleanField("done", downloadInfo.getDownloadStatus().isDone());
            jsonGenerator.writeStringField("hash", downloadInfo.getHash());
            jsonGenerator.writeStringField("hash_sha256_base64", downloadInfo.getHash());
            jsonGenerator.writeStringField("timestamp", downloadInfo.getTimestamp() == null ? null : downloadInfo.getTimestamp().format(DateTimeFormatter.ISO_DATE_TIME));
            jsonGenerator.writeEndObject();
        }
        
        public static ObjectMapper createObjectMapper() {
            final ObjectMapper objectMapper = new ObjectMapper();
            final DownloadInfoSerializer downloadInfoSerializer = new DownloadInfoSerializer(DownloadInfo.class);
            final SimpleModule simpleModule = new SimpleModule("DownloadInfoSerializer", new Version(1, 0, 0, null, null, null));
            simpleModule.addSerializer(DownloadInfo.class, downloadInfoSerializer);
            objectMapper.registerModule(simpleModule);
            return objectMapper;
        }
        
    }
    
}
