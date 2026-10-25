package cn.fandmc.epoch.config;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public record ClientConfig(String host, int port, String username, String assetDirectory,
                           int width, int height, boolean vsync, int viewDistance,boolean modernLadderCollision) {
    private static final Gson JSON = new GsonBuilder().setPrettyPrinting().create();

    public ClientConfig {
        if (host == null || host.isBlank() || host.length() > 255 || host.indexOf('\0') >= 0)
            throw new IllegalArgumentException("Invalid server host");
        if (port < 1 || port > 65535) throw new IllegalArgumentException("Port must be 1..65535");
        if (username == null || !username.matches("[A-Za-z0-9_]{1,16}"))
            throw new IllegalArgumentException("Username must contain 1..16 letters, numbers or underscores");
        if (assetDirectory == null || assetDirectory.isBlank())
            throw new IllegalArgumentException("Epoch asset directory is required");
        if (width < 640 || width > 7680 || height < 480 || height > 4320)
            throw new IllegalArgumentException("Invalid window dimensions");
        if (viewDistance < 2 || viewDistance > 16) throw new IllegalArgumentException("View distance must be 2..16");
    }

    public static ClientConfig defaults() {
        return new ClientConfig("127.0.0.1", 25566, "EpochPlayer", "assets",
                1280, 720, true, 6,false);
    }

    public static ClientConfig load(Path file) throws IOException {
        if (!Files.exists(file)) {
            ClientConfig config = defaults();
            config.save(file);
            return config;
        }
        if (Files.size(file) > 65536) throw new IOException("Configuration is too large: " + file);
        try {
            ClientConfig config = JSON.fromJson(Files.readString(file), ClientConfig.class);
            if (config == null) throw new IOException("Empty configuration: " + file);
            return config;
        } catch (JsonParseException | IllegalArgumentException e) {
            throw new IOException("Invalid configuration, existing file preserved: " + file, e);
        }
    }

    public void save(Path file) throws IOException {
        Path parent = file.toAbsolutePath().getParent();
        Files.createDirectories(parent);
        Path temporary = Files.createTempFile(parent, "epoch-config-", ".tmp");
        try {
            Files.writeString(temporary, JSON.toJson(this) + System.lineSeparator());
            try {
                Files.move(temporary, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException e) {
                Files.move(temporary, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temporary);
        }
    }

    public ClientConfig withServer(String host, int port, String username) {
        return new ClientConfig(host.trim(), port, username.trim(), assetDirectory, width, height, vsync, viewDistance,modernLadderCollision);
    }

    public ClientConfig withVsync(boolean enabled) {
        return new ClientConfig(host, port, username, assetDirectory, width, height, enabled, viewDistance,modernLadderCollision);
    }
}
