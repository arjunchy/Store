package com.ecommerce.backend.service.storage;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service;
    private String baseUrl = "http://localhost:8080/uploads/";

    @BeforeEach
    void setUp() {
        String uploadDir = tempDir.toString() + "/";
        service = new FileStorageService(uploadDir, baseUrl);
    }

    @AfterEach
    void tearDown() throws IOException {
        // cleanup handled by @TempDir
    }

    @Test
    void storeFile_validImage_returnsUrlAndCreatesFile() throws Exception {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10, 0x4A, 0x46, 0x49, 0x46, 0x00, 0x01};
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", jpegBytes
        );
        String url = service.storeFile(file, "products/123");
        assertNotNull(url);
        assertTrue(url.startsWith(baseUrl + "products/123/"));
        assertTrue(url.endsWith(".jpg"));

        // Verify file exists via load
        Resource res = service.loadFile(url);
        assertTrue(res.exists());
        assertTrue(res.isReadable());
    }

    @Test
    void storeFile_invalidContentType_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.txt", "text/plain", "hello".getBytes()
        );
        assertThrows(IllegalArgumentException.class, () -> service.storeFile(file, "products/123"));
    }

    @Test
    void storeFile_invalidExtension_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.exe", "image/jpeg", "hello".getBytes()
        );
        assertThrows(IllegalArgumentException.class, () -> service.storeFile(file, "products/123"));
    }

    @Test
    void storeFile_emptyFile_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "empty.jpg", "image/jpeg", new byte[0]
        );
        assertThrows(IllegalArgumentException.class, () -> service.storeFile(file, "products/123"));
    }

    @Test
    void storeFile_pathTraversalInSubfolder_throws() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes()
        );
        assertThrows(IllegalArgumentException.class, () -> service.storeFile(file, "../etc"));
        assertThrows(IllegalArgumentException.class, () -> service.storeFile(file, "products/../../etc"));
    }

    @Test
    void deleteFile_deletesPhysicalFile() throws Exception {
        byte[] pngBytes = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A};
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", pngBytes
        );
        String url = service.storeFile(file, "products/123");
        Resource before = service.loadFile(url);
        assertTrue(before.exists());

        service.deleteFile(url);

        assertThrows(IllegalArgumentException.class, () -> service.loadFile(url));
    }

    @Test
    void deleteFile_nonExistent_doesNotThrow() {
        assertDoesNotThrow(() -> service.deleteFile(baseUrl + "nonexistent/file.jpg"));
        assertDoesNotThrow(() -> service.deleteFile(""));
        assertDoesNotThrow(() -> service.deleteFile(null));
    }

    @Test
    void loadFile_fromRelativePath_works() throws Exception {
        byte[] webpBytes = new byte[]{0x52, 0x49, 0x46, 0x46, 0x00, 0x00, 0x00, 0x00, 0x57, 0x45, 0x42, 0x50};
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.webp", "image/webp", webpBytes
        );
        String url = service.storeFile(file, "products/123");
        String relative = url.replace(baseUrl, "");
        Resource res = service.loadFile(relative);
        assertTrue(res.exists());
    }

    @Test
    void storeFile_sanitizesSubfolderAndCreatesDirectories() throws Exception {
        byte[] jpegBytes = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, (byte) 0xE0, 0x00, 0x10};
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", jpegBytes
        );
        String url = service.storeFile(file, "products/abc-123");
        assertTrue(url.contains("products/abc-123"));
        // Verify directory created
        assertTrue(Files.exists(tempDir.resolve("products/abc-123")));
    }
}
