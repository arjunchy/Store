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
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "fake-image-content".getBytes()
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
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.png", "image/png", "content".getBytes()
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
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.webp", "image/webp", "webpcontent".getBytes()
        );
        String url = service.storeFile(file, "products/123");
        String relative = url.replace(baseUrl, "");
        Resource res = service.loadFile(relative);
        assertTrue(res.exists());
    }

    @Test
    void storeFile_sanitizesSubfolderAndCreatesDirectories() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "content".getBytes()
        );
        String url = service.storeFile(file, "products/abc-123");
        assertTrue(url.contains("products/abc-123"));
        // Verify directory created
        assertTrue(Files.exists(tempDir.resolve("products/abc-123")));
    }
}
