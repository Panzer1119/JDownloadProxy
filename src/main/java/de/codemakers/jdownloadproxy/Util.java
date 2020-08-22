package de.codemakers.jdownloadproxy;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

public class Util {
    
    public static final String HASH_ALGORITHM = "SHA-256";
    public static final String REGEX_FORBIDDEN_FILENAMES = "[\\\\/:\"*?<>|]+";
    public static final String DEFAULT_REPLACEMENT = "_";
    
    public static String base64encodeToString(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.getEncoder().encodeToString(data);
    }
    
    public static byte[] base64decode(String base64) {
        if (base64 == null) {
            return null;
        }
        return Base64.getDecoder().decode(base64);
    }
    
    public static String base64URLencodeToString(byte[] data) {
        if (data == null) {
            return null;
        }
        return Base64.getUrlEncoder().encodeToString(data);
    }
    
    public static byte[] base64URLdecode(String base64) {
        if (base64 == null) {
            return null;
        }
        return Base64.getUrlDecoder().decode(base64);
    }
    
    public static MessageDigest getMessageDigestInstance() {
        return getMessageDigestInstance(HASH_ALGORITHM);
    }
    
    public static MessageDigest getMessageDigestInstance(String algorithm) {
        try {
            return MessageDigest.getInstance(algorithm);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static byte[] hashStream(InputStream inputStream) {
        if (inputStream == null) {
            return null;
        }
        final MessageDigest messageDigest = getMessageDigestInstance();
        if (messageDigest == null) {
            return null;
        }
        try (final DigestInputStream digestInputStream = new DigestInputStream(inputStream, messageDigest)) {
            final byte[] buffer = new byte[1024];
            while (digestInputStream.read(buffer) != -1) {
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return messageDigest.digest();
    }
    
    public static byte[] hashFile(File file) {
        if (file == null || !file.exists()) {
            return null;
        }
        try (final FileInputStream fileInputStream = new FileInputStream(file)) {
            return hashStream(fileInputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    
    public static String sanitizeFilename(String filename) {
        return sanitizeFilename(filename, DEFAULT_REPLACEMENT);
    }
    
    public static String sanitizeFilename(String filename, String replacement) {
        return filename.replaceAll(REGEX_FORBIDDEN_FILENAMES, replacement);
    }
    
}
