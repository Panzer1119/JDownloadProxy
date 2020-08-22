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

package de.codemakers.jdownloadproxy.download;

import de.codemakers.jdownloadproxy.Application;
import de.codemakers.jdownloadproxy.Util;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

public class Downloader {
    
    public static final File DOWNLOADS_FOLDER = new File(Application.APPLICATION_FOLDER, "Downloads");
    public static final File URL_HASHES_FILE = new File(Application.APPLICATION_FOLDER, "url_hashes.txt");
    public static final File URL_FILENAMES_FILE = new File(Application.APPLICATION_FOLDER, "url_filenames.txt");
    public static final String TEMPLATE_TEMP_FILE = "temp_%s.part";
    public static final String PATTERN_STRING_TEMP_FILE = "temp_.+\\.part";
    public static final Pattern PATTERN_TEMP_FILE = Pattern.compile(PATTERN_STRING_TEMP_FILE);
    
    private static final ExecutorService EXECUTOR_SERVICE = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
    
    private static final Map<URL, String> URL_HASHES = new ConcurrentHashMap<>();
    private static final Map<URL, String> URL_FILENAMES = new ConcurrentHashMap<>();
    private static final Map<String, File> HASH_FILES = new ConcurrentHashMap<>();
    
    private static final List<DownloadContainer> DOWNLOAD_CONTAINERS = new CopyOnWriteArrayList<>();
    
    static {
        DOWNLOADS_FOLDER.mkdirs();
        loadHashes();
        Runtime.getRuntime().addShutdownHook(new Thread(Downloader::saveHashes));
    }
    
    protected static synchronized void loadHashes() {
        URL_HASHES.clear();
        URL_FILENAMES.clear();
        HASH_FILES.clear();
        if (URL_HASHES_FILE.exists()) {
            final Properties properties = new Properties();
            try (final FileInputStream fileInputStream = new FileInputStream(URL_HASHES_FILE)) {
                properties.load(fileInputStream);
                properties.forEach((key, value) -> {
                    try {
                        URL_HASHES.put(new URL((String) key), (String) value);
                    } catch (MalformedURLException | IllegalArgumentException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.printf("[DEBUG][%s#loadHashes] Loaded URL_HASHES: %s%n", Downloader.class.getSimpleName(), URL_HASHES); //DEBUG
        }
        if (URL_FILENAMES_FILE.exists()) {
            final Properties properties = new Properties();
            try (final FileInputStream fileInputStream = new FileInputStream(URL_FILENAMES_FILE)) {
                properties.load(fileInputStream);
                properties.forEach((key, value) -> {
                    try {
                        URL_FILENAMES.put(new URL((String) key), (String) value);
                    } catch (MalformedURLException e) {
                        e.printStackTrace();
                    }
                });
            } catch (IOException e) {
                e.printStackTrace();
            }
            System.out.printf("[DEBUG][%s#loadHashes] Loaded URL_FILENAMES: %s%n", Downloader.class.getSimpleName(), URL_FILENAMES); //DEBUG
        }
        for (File file : DOWNLOADS_FOLDER.listFiles()) {
            if (PATTERN_TEMP_FILE.matcher(file.getName()).matches()) {
                continue;
            }
            try {
                HASH_FILES.put(file.getName(), file);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
            }
        }
        System.out.printf("[DEBUG][%s#loadHashes] Loaded HASH_FILES: %s%n", Downloader.class.getSimpleName(), HASH_FILES); //DEBUG
    }
    
    protected static synchronized void saveHashes() {
        if (URL_HASHES.isEmpty()) {
            URL_HASHES_FILE.delete();
        } else {
            final Properties properties = new Properties();
            URL_HASHES.forEach((key, value) -> properties.put(key.toString(), value));
            try (final FileOutputStream fileOutputStream = new FileOutputStream(URL_HASHES_FILE, false)) {
                properties.store(fileOutputStream, "Changed at");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (URL_FILENAMES.isEmpty()) {
            URL_FILENAMES_FILE.delete();
        } else {
            final Properties properties = new Properties();
            URL_FILENAMES.forEach((key, value) -> properties.put(key.toString(), value));
            try (final FileOutputStream fileOutputStream = new FileOutputStream(URL_FILENAMES_FILE, false)) {
                properties.store(fileOutputStream, "Changed at");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public static File createTempFileForUUID(UUID uuid) {
        return new File(DOWNLOADS_FOLDER, String.format(TEMPLATE_TEMP_FILE, uuid.toString()));
    }
    
    public static File createFileForHash(String hash) {
        return new File(DOWNLOADS_FOLDER, hash);
    }
    
    public static boolean hasHashForURL(URL url) {
        return URL_HASHES.containsKey(url);
    }
    
    public static boolean hasFilenameForHash(String hash) {
        return URL_FILENAMES.containsKey(hash);
    }
    
    public static boolean hasFileForHash(String hash) {
        return HASH_FILES.containsKey(hash);
    }
    
    public static String getHashForURL(URL url) {
        return URL_HASHES.get(url);
    }
    
    public static void setHashForURL(URL url, String hash) {
        URL_HASHES.put(url, hash);
    }
    
    public static boolean removeHashForURL(URL url) {
        return URL_HASHES.remove(url) != null;
    }
    
    public static String getFilenameForURL(URL url) {
        return URL_FILENAMES.get(url);
    }
    
    public static void setFilenameForURL(URL url, String filename) {
        URL_FILENAMES.put(url, filename);
    }
    
    public static boolean removeFilenameForURL(URL url) {
        return URL_FILENAMES.remove(url) != null;
    }
    
    public static File getFileForHash(String hash) {
        return HASH_FILES.get(hash);
    }
    
    public static void setFileForHash(String hash, File file) {
        HASH_FILES.put(hash, file);
    }
    
    protected static Map.Entry<File, String> handleFile(URL url, File tempFile, String filename, boolean forceDownload) {
        final Map.Entry<File, String> entry = processTempFile(tempFile, forceDownload);
        if (entry == null) {
            return null;
        }
        if (entry.getValue() == null) {
            return new AbstractMap.SimpleEntry<>(tempFile, null);
        }
        if (!addHashAndFilenameToURL(url, entry.getValue(), filename)) {
            return null;
        }
        saveHashes();
        return entry;
    }
    
    private static Map.Entry<File, String> processTempFile(File tempFile, boolean forceDownload) {
        if (tempFile == null || !tempFile.exists()) {
            return null;
        }
        final byte[] hashBytes = Util.hashFile(tempFile);
        if (hashBytes == null) {
            return null;
        }
        final String hash = Util.base64URLencodeToString(hashBytes);
        final File file = createFileForHash(hash);
        try {
            tempFile.deleteOnExit();
            if (!file.exists() || forceDownload) {
                if (file.exists() && !file.delete()) {
                    System.err.printf("[WARNING][%s#processTempFile] Can't delete old \"%s\"%n", Downloader.class.getSimpleName(), file.getAbsolutePath()); //DEBUG
                }
                switch (1) {
                    case 0:
                        Files.move(tempFile.toPath(), file.toPath());
                        break;
                    case 1:
                        Files.copy(tempFile.toPath(), file.toPath());
                        break;
                    case 2:
                        try (final FileInputStream fileInputStream = new FileInputStream(tempFile)) {
                            try (final FileOutputStream fileOutputStream = new FileOutputStream(file, false)) {
                                fileInputStream.transferTo(fileOutputStream);
                            }
                        }
                        break;
                }
            } else {
                System.out.printf("[INFO ][%s#processTempFile] File \"%s\" already exists and shouldn't be overridden%n", Downloader.class.getSimpleName(), file.getAbsolutePath());
            }
            if (!tempFile.delete()) {
                System.err.printf("[WARNING][%s#processTempFile] Can't delete part \"%s\"%n", Downloader.class.getSimpleName(), tempFile.getAbsolutePath()); //DEBUG
            }
        } catch (Exception ex) {
            System.err.printf("[ERROR][%s#processTempFile] Failed moving \"%s\" to \"%s\"%n", Downloader.class.getSimpleName(), tempFile.getAbsolutePath(), file.getAbsolutePath()); //DEBUG
            ex.printStackTrace();
            return new AbstractMap.SimpleEntry<>(tempFile, null);
        }
        setFileForHash(hash, file);
        return new AbstractMap.SimpleEntry<>(file, hash);
    }
    
    private static boolean addHashAndFilenameToURL(URL url, String hash, String filename) {
        if (url == null || hash == null || hasHashForURL(url)) {
            return false;
        }
        setHashForURL(url, hash);
        setFilenameForURL(url, filename);
        return true;
    }
    
    public static void startDownloadContainerAsync(DownloadContainer downloadContainer) {
        EXECUTOR_SERVICE.submit(downloadContainer::startVoid);
    }
    
    public static DownloadContainer createDownloadContainer(URL url, boolean forceDownload) {
        final DownloadContainer downloadContainer = new DownloadContainer(url);
        downloadContainer.setForceDownload(forceDownload);
        DOWNLOAD_CONTAINERS.add(downloadContainer);
        return downloadContainer;
    }
    
    public static DownloadContainer getDownloadContainer(UUID uuid) {
        if (uuid == null) {
            return null;
        }
        return DOWNLOAD_CONTAINERS.stream().filter((downloadContainer) -> uuid.equals(downloadContainer.getDownloadInfo().getUuid())).findAny().orElse(null);
    }
    
    public static boolean removeDownloadContainer(UUID uuid) {
        if (uuid == null) {
            return false;
        }
        return DOWNLOAD_CONTAINERS.removeIf((downloadContainer) -> uuid.equals(downloadContainer.getDownloadInfo().getUuid()));
    }
    
    public static boolean removeFile(File file, URL url, boolean delete) {
        if (file == null || url == null) {
            return false;
        }
        final String hash = file.getName();
        Downloader.removeHashForURL(url);
        Downloader.removeFilenameForURL(url);
        Downloader.createFileForHash(hash);
        if (delete && file.exists()) {
            file.delete();
        }
        Downloader.saveHashes();
        System.out.printf("[DEBUG][%s#removeFile] Removed \"%s\" and \"%s\" (delete: %b)%n", Downloader.class.getSimpleName(), file.getAbsolutePath(), url, delete);
        return !(delete && file.exists());
    }
    
    public static List<DownloadContainer> getDownloadContainers() {
        return DOWNLOAD_CONTAINERS;
    }
    
}
