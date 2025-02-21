package com.goofy.GoofyDocs.chunking;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.util.StopWatch;

public class ChunkingPerformanceTest {

    private ChunkingService chunkingService;
    private static final Path TEST_FILES_DIR = Paths.get("src/test/resources/test-files");
    private static final Path PROJECT_ROOT = Paths.get(".").toAbsolutePath().normalize();

    private static final Map<String, String[]> FILE_PATTERNS = new HashMap<>();
    static {
        FILE_PATTERNS.put("Text", new String[] { ".txt", ".md", ".json" });
        FILE_PATTERNS.put("CSV", new String[] { ".csv" });
        FILE_PATTERNS.put("Log", new String[] { ".log" });
        FILE_PATTERNS.put("Binary", new String[] { ".bin", ".dat" });
        FILE_PATTERNS.put("Archive", new String[] { ".zip", ".jar" });
    }

    @BeforeEach
    void setUp() throws NoSuchAlgorithmException, IOException {
        chunkingService = new ChunkingService();
        System.out.println("Searching for files in: " + PROJECT_ROOT);

        Files.createDirectories(TEST_FILES_DIR);
    }

    @Test
    void testChunkingPerformanceOnFiles() throws IOException {
        System.out.println("\n=== Performance Test on File ===");
        System.out.println("Format: Type | File | Size | Time | Speed | Chunks (Total/Uniques) | Storage Gain");
        System.out.println("------------------------------------------------------------------------");

        for (Map.Entry<String, String[]> entry : FILE_PATTERNS.entrySet()) {
            String fileType = entry.getKey();
            String[] extensions = entry.getValue();

            System.out.println("\n=== Testing type: " + fileType + " ===");

            List<File> realFiles = findRealFiles(extensions);

            if (!realFiles.isEmpty()) {
                System.out.println("Using real files found in the project:");
                for (File file : realFiles) {
                    testFilePerformance(fileType, file);
                }
            } else {
                System.out.println("No real files found, using generated files:");
                List<File> testFiles = new ArrayList<>();
                for (int size : new int[] { 1, 10, 50, 100 }) {
                    File testFile = createTestFile(fileType, size);
                    testFiles.add(testFile);
                    testFilePerformance(fileType, testFile);
                }

                for (File file : testFiles) {
                    Files.deleteIfExists(file.toPath());
                }
            }
        }
    }

    private List<File> findRealFiles(String[] extensions) throws IOException {
        List<File> files = new ArrayList<>();
        try (var walk = Files.walk(PROJECT_ROOT)) {
            files = walk
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.toString().toLowerCase();
                        return Arrays.stream(extensions)
                                .anyMatch(ext -> name.endsWith(ext.toLowerCase()));
                    })
                    .map(Path::toFile)
                    .filter(f -> f.length() > 0 && f.length() <= 50 * 1024 * 1024)
                    .sorted((f1, f2) -> Long.compare(f2.length(), f1.length()))
                    .limit(2)
                    .collect(Collectors.toList());
        }
        return files;
    }

    private File createTestFile(String fileType, int sizeMB) throws IOException {
        String fileName = String.format("%s-%dMB%s",
                fileType.toLowerCase(),
                sizeMB,
                FILE_PATTERNS.get(fileType)[0]);

        File file = TEST_FILES_DIR.resolve(fileName).toFile();

        switch (fileType) {
            case "Text":
                createTextFile(file, sizeMB);
                break;
            case "CSV":
                createCsvFile(file, sizeMB);
                break;
            case "Log":
                createLogFile(file, sizeMB);
                break;
            case "Binary":
                createBinaryFile(file, sizeMB);
                break;
            case "Archive":
                createZipFile(file, sizeMB);
                break;
            default:
                throw new IllegalArgumentException("Unsupported file type: " + fileType);
        }
        return file;
    }

    private void createTextFile(File file, int sizeMB) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            String[] paragraphs = {
                    "Lorem ipsum dolor sit amet, consectetur adipiscing elit. ",
                    "Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. ",
                    "Ut enim ad minim veniam, quis nostrud exercitation ullamco. ",
                    "Duis aute irure dolor in reprehenderit in voluptate velit esse. "
            };

            long targetSize = sizeMB * 1024L * 1024L;
            while (file.length() < targetSize) {
                for (String para : paragraphs) {
                    writer.write(para);
                    if (Math.random() < 0.3) {
                        writer.write(para);
                    }
                }
                writer.write("\n");
            }
        }
    }

    private void createCsvFile(File file, int sizeMB) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writer.write("id,date,user,action,status,amount\n");

            long targetSize = sizeMB * 1024L * 1024L;
            int id = 1;
            String[] actions = { "LOGIN", "PURCHASE", "VIEW", "LOGOUT" };
            String[] statuses = { "SUCCESS", "PENDING", "FAILED" };

            while (file.length() < targetSize) {
                String date = String.format("2024-02-%02d", (id % 28) + 1);
                String action = actions[id % actions.length];
                String status = statuses[id % statuses.length];
                double amount = Math.round(Math.random() * 1000 * 100.0) / 100.0;

                writer.write(String.format("%d,%s,user%d,%s,%s,%.2f\n",
                        id, date, (id % 100) + 1, action, status, amount));
                id++;
            }
        }
    }

    private void createLogFile(File file, int sizeMB) throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            String[] levels = { "INFO", "WARN", "ERROR", "DEBUG" };
            String[] messages = {
                    "User authentication successful",
                    "Database connection timeout",
                    "Invalid request parameters",
                    "Cache miss for key: %s",
                    "Processing batch job #%d"
            };

            long targetSize = sizeMB * 1024L * 1024L;
            int lineCount = 0;

            while (file.length() < targetSize) {
                String timestamp = String.format("2024-02-%02d %02d:%02d:%02d.%03d",
                        (lineCount % 28) + 1,
                        lineCount % 24,
                        (lineCount * 7) % 60,
                        (lineCount * 13) % 60,
                        lineCount % 1000);

                String level = levels[lineCount % levels.length];
                String message = messages[lineCount % messages.length];
                if (message.contains("%s")) {
                    message = String.format(message, "cache_" + lineCount);
                } else if (message.contains("%d")) {
                    message = String.format(message, lineCount);
                }

                writer.write(String.format("%s [%s] %s%n", timestamp, level, message));
                lineCount++;
            }
        }
    }

    private void createBinaryFile(File file, int sizeMB) throws IOException {
        try (FileOutputStream fos = new FileOutputStream(file)) {
            byte[] pattern = new byte[4096];
            new Random().nextBytes(pattern);

            long targetSize = sizeMB * 1024L * 1024L;
            while (file.length() < targetSize) {
                if (Math.random() < 0.7) {
                    fos.write(pattern);
                } else {
                    byte[] random = new byte[4096];
                    new Random().nextBytes(random);
                    fos.write(random);
                }
            }
        }
    }

    private void createZipFile(File file, int sizeMB) throws IOException {
        try (ZipOutputStream zos = new ZipOutputStream(new FileOutputStream(file))) {
            byte[] data = "Repetitive content for testing ZIP compression".getBytes();

            int fileCount = 1;
            while (file.length() < sizeMB * 1024L * 1024L) {
                ZipEntry entry = new ZipEntry(String.format("file%d.txt", fileCount++));
                zos.putNextEntry(entry);
                zos.write(data);
                zos.closeEntry();

                if (Math.random() < 0.3) {
                    entry = new ZipEntry(String.format("file%d_copy.txt", fileCount - 1));
                    zos.putNextEntry(entry);
                    zos.write(data);
                    zos.closeEntry();
                }
            }
        }
    }

    private void testFilePerformance(String fileType, File testFile) throws IOException {
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        List<Chunk> chunks = chunkingService.chunkFile(testFile);
        stopWatch.stop();

        long fileSize = testFile.length();
        double timeInSeconds = stopWatch.getTotalTimeSeconds();
        double speedMBps = (fileSize / (1024.0 * 1024.0)) / timeInSeconds;
        double avgChunkSize = fileSize / (double) chunks.size();

        Map<String, Chunk> uniqueChunks = chunks.stream()
                .collect(Collectors.toMap(
                        Chunk::getHash,
                        chunk -> chunk,
                        (existing, replacement) -> existing));

        long totalUniqueSize = uniqueChunks.values().stream()
                .mapToInt(chunk -> chunk.getData().length)
                .sum();

        double storageGain = ((fileSize - totalUniqueSize) / (double) fileSize) * 100;

        System.out.printf("%-8s | %-20s | %5.1f MB | %6.3f s | %6.1f MB/s | %4d/%4d | %13.2f%% | %7.0f B%n",
                fileType,
                testFile.getName(),
                fileSize / (1024.0 * 1024.0),
                timeInSeconds,
                speedMBps,
                chunks.size(),
                uniqueChunks.size(),
                storageGain,
                avgChunkSize);
    }
}
