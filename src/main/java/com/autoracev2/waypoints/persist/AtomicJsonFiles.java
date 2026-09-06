package com.autoracev2.waypoints.persist;

import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;

/**
 * Writes JSON through a temp file so a crash cannot leave a half-written target.
 */
public final class AtomicJsonFiles {
    private AtomicJsonFiles() {
    }

    public static void write(Path target, String json) throws IOException {
        Path directory = target.getParent();
        if (directory != null) {
            Files.createDirectories(directory);
        }
        Path temp = target.resolveSibling(target.getFileName().toString() + ".tmp");
        try {
            Files.writeString(temp, json, StandardCharsets.UTF_8,
                    StandardOpenOption.CREATE,
                    StandardOpenOption.TRUNCATE_EXISTING,
                    StandardOpenOption.WRITE);
            try (FileChannel channel = FileChannel.open(temp, StandardOpenOption.WRITE)) {
                channel.force(true);
            }
            try {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (AtomicMoveNotSupportedException ignored) {
                Files.move(temp, target, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException exception) {
            try {
                Files.deleteIfExists(temp);
            } catch (IOException ignored) {
                // Best-effort cleanup only.
            }
            throw exception;
        }
    }

    public static String readIfPresent(Path target) throws IOException {
        if (!Files.exists(target)) {
            return null;
        }
        return Files.readString(target, StandardCharsets.UTF_8);
    }

    public static void backupCorrupt(Path target, String suffix) {
        if (!Files.exists(target)) {
            return;
        }
        Path backup = target.resolveSibling(target.getFileName().toString() + suffix);
        try {
            Files.move(target, backup, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            try {
                Files.copy(target, backup, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignoredAgain) {
                // Keep the original file if backup also fails.
            }
        }
    }
}
