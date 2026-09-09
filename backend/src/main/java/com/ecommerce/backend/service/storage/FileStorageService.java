package com.ecommerce.backend.service.storage;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.UUID;

@Service
@Slf4j
public class FileStorageService {

    private final Path uploadDir;
    private final String baseUrl;

    private static final List<String> ALLOWED_CONTENT_TYPES = List.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp",
            "image/gif"
    );

    private static final List<String> ALLOWED_EXTENSIONS = List.of(
            "jpg", "jpeg", "png", "webp", "gif"
    );

    public FileStorageService(
            @Value("${app.upload.dir:uploads/}") String uploadDir,
            @Value("${app.upload.base-url:http://localhost:8081/uploads/}") String baseUrl) {
        this.uploadDir = Paths.get(uploadDir).toAbsolutePath().normalize();
        this.baseUrl = baseUrl.endsWith("/") ? baseUrl : baseUrl + "/";
        try {
            Files.createDirectories(this.uploadDir);
            log.info("File storage initialized at: {}", this.uploadDir);
        } catch (IOException e) {
            log.error("Failed to create upload directory: {}", this.uploadDir, e);
            throw new IllegalStateException("Could not create upload directory", e);
        }
    }

    /**
     * Store file and return public URL.
     *
     * @param file      multipart file (required, validated)
     * @param subfolder subfolder inside uploadDir, e.g. "products/{productId}"
     * @return public URL like http://localhost:8080/uploads/products/{productId}/uuid.jpg
     */
    public String storeFile(MultipartFile file, String subfolder) {
        log.info("Storing file: originalName={}, contentType={}, size={}, subfolder={}",
                file.getOriginalFilename(), file.getContentType(), file.getSize(), subfolder);

        if (file.isEmpty()) {
            log.warn("Attempt to store empty file");
            throw new IllegalArgumentException("File is empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            log.warn("Invalid content type: {}", contentType);
            throw new IllegalArgumentException("Invalid file type. Allowed: " + ALLOWED_CONTENT_TYPES);
        }

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename() != null ? file.getOriginalFilename() : "file");
        if (originalFilename.contains("..")) {
            log.warn("Invalid path sequence in filename: {}", originalFilename);
            throw new IllegalArgumentException("Invalid file path");
        }

        String extension = StringUtils.getFilenameExtension(originalFilename);
        if (extension == null) {
            extension = "";
        }
        extension = extension.toLowerCase();

        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            log.warn("Invalid file extension: {}", extension);
            throw new IllegalArgumentException("Invalid file extension. Allowed: " + ALLOWED_EXTENSIONS);
        }

        validateMagicBytes(file, contentType, extension);

        String sanitizedSubfolder = sanitizeSubfolder(subfolder);

        String newFilename = UUID.randomUUID() + "." + extension;

        try {
            Path targetDir = sanitizedSubfolder.isBlank()
                    ? this.uploadDir
                    : this.uploadDir.resolve(sanitizedSubfolder).normalize();

            if (!targetDir.startsWith(this.uploadDir)) {
                log.warn("Path traversal attempt with subfolder: {}", subfolder);
                throw new IllegalArgumentException("Invalid subfolder");
            }

            Files.createDirectories(targetDir);

            Path targetPath = targetDir.resolve(newFilename).normalize();
            if (!targetPath.startsWith(this.uploadDir)) {
                log.warn("Path traversal attempt with filename: {}", newFilename);
                throw new IllegalArgumentException("Invalid file path");
            }

            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }

            String relativePath = sanitizedSubfolder.isBlank()
                    ? newFilename
                    : sanitizedSubfolder + "/" + newFilename;

            relativePath = relativePath.replace("\\", "/");

            String url = this.baseUrl + relativePath;
            log.info("Successfully stored file at: {} -> URL: {}", targetPath, url);
            return url;
        } catch (IOException e) {
            log.error("Failed to store file: {}", originalFilename, e);
            throw new IllegalStateException("Failed to store file", e);
        }
    }

    /**
     * Delete physical file given its public URL or relative path.
     * If file does not exist, logs and returns silently.
     */
    public void deleteFile(String fileNameOrUrl) {
        log.info("Deleting file: {}", fileNameOrUrl);
        if (fileNameOrUrl == null || fileNameOrUrl.isBlank()) {
            log.warn("Delete called with blank fileName");
            return;
        }

        try {
            String relativePath = extractRelativePath(fileNameOrUrl);
            if (relativePath == null || relativePath.isBlank()) {
                log.warn("Could not extract relative path from: {}", fileNameOrUrl);
                return;
            }

            Path filePath = this.uploadDir.resolve(relativePath).normalize();

            if (!filePath.startsWith(this.uploadDir)) {
                log.warn("Path traversal attempt on delete: {}", fileNameOrUrl);
                throw new IllegalArgumentException("Invalid file path");
            }

            boolean deleted = Files.deleteIfExists(filePath);
            if (deleted) {
                log.info("Successfully deleted file: {}", filePath);
            } else {
                log.warn("File not found for deletion: {}", filePath);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (IOException e) {
            log.error("Failed to delete file: {}", fileNameOrUrl, e);
            throw new IllegalStateException("Failed to delete file", e);
        }
    }

    /**
     * Load file as Resource for serving.
     */
    public Resource loadFile(String fileNameOrUrl) {
        log.debug("Loading file: {}", fileNameOrUrl);
        try {
            String relativePath = extractRelativePath(fileNameOrUrl);
            if (relativePath == null || relativePath.isBlank()) {
                log.warn("Could not extract relative path from: {}", fileNameOrUrl);
                throw new IllegalArgumentException("Invalid file path");
            }

            Path filePath = this.uploadDir.resolve(relativePath).normalize();

            if (!filePath.startsWith(this.uploadDir)) {
                log.warn("Path traversal attempt on load: {}", fileNameOrUrl);
                throw new IllegalArgumentException("Invalid file path");
            }

            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                log.debug("Loaded file: {}", filePath);
                return resource;
            } else {
                log.warn("File not found or not readable: {}", filePath);
                throw new IllegalArgumentException("File not found: " + fileNameOrUrl);
            }
        } catch (MalformedURLException e) {
            log.error("Malformed URL for file: {}", fileNameOrUrl, e);
            throw new IllegalArgumentException("Invalid file path", e);
        }
    }

    private String sanitizeSubfolder(String subfolder) {
        if (subfolder == null || subfolder.isBlank()) {
            return "";
        }
        String cleaned = StringUtils.cleanPath(subfolder.trim());
        cleaned = cleaned.replaceAll("^/+", "").replaceAll("/+$", "");
        if (cleaned.contains("..")) {
            log.warn("Subfolder contains path traversal: {}", subfolder);
            throw new IllegalArgumentException("Invalid subfolder");
        }
        if (!cleaned.matches("[a-zA-Z0-9/_\\-]+")) {
            log.warn("Subfolder contains invalid characters: {}", subfolder);
            throw new IllegalArgumentException("Invalid subfolder characters");
        }
        return cleaned;
    }

    /**
     * Verify that the uploaded bytes actually look like the claimed raster image
     * type. This prevents serving arbitrary HTML/script payloads that were merely
     * labeled with an image Content-Type / extension.
     */
    private void validateMagicBytes(MultipartFile file, String contentType, String extension) {
        byte[] bytes;
        try {
            bytes = file.getBytes();
        } catch (IOException e) {
            throw new IllegalArgumentException("Failed to read uploaded file", e);
        }
        if (bytes == null || bytes.length < 4) {
            throw new IllegalArgumentException("Invalid or empty image file");
        }

        boolean match;
        switch (extension) {
            case "jpg":
            case "jpeg":
                match = (bytes[0] & 0xFF) == 0xFF
                        && (bytes[1] & 0xFF) == 0xD8
                        && (bytes[2] & 0xFF) == 0xFF;
                break;
            case "png":
                match = (bytes[0] & 0xFF) == 0x89
                        && bytes[1] == 'P' && bytes[2] == 'N' && bytes[3] == 'G';
                break;
            case "gif":
                match = bytes[0] == 'G' && bytes[1] == 'I' && bytes[2] == 'F';
                break;
            case "webp":
                match = bytes[0] == 'R' && bytes[1] == 'I' && bytes[2] == 'F' && bytes[3] == 'F';
                break;
            default:
                match = true;
        }

        if (!match) {
            log.warn("File content does not match claimed image type: ext={} contentType={}", extension, contentType);
            throw new IllegalArgumentException("File content does not match its declared image type");
        }
    }

    private String extractRelativePath(String fileNameOrUrl) {
        if (fileNameOrUrl == null || fileNameOrUrl.isBlank()) {
            return null;
        }
        String trimmed = fileNameOrUrl.trim();

        if (trimmed.startsWith(this.baseUrl)) {
            return trimmed.substring(this.baseUrl.length());
        }

        int uploadsIdx = trimmed.indexOf("/uploads/");
        if (uploadsIdx != -1) {
            return trimmed.substring(uploadsIdx + "/uploads/".length());
        }

        if (trimmed.startsWith("/")) {
            trimmed = trimmed.replaceFirst("^/+", "");
        }

        if (trimmed.startsWith("http://") || trimmed.startsWith("https://")) {
            try {
                java.net.URL url = new java.net.URL(trimmed);
                String path = url.getPath();
                if (path.startsWith("/uploads/")) {
                    return path.substring("/uploads/".length());
                }
                if (path.startsWith("/")) {
                    path = path.substring(1);
                }
                return path;
            } catch (MalformedURLException e) {
                log.warn("Failed to parse URL: {}", trimmed);
                return null;
            }
        }

        return trimmed;
    }

    public Path getUploadDir() {
        return uploadDir;
    }

    public String getBaseUrl() {
        return baseUrl;
    }
}
