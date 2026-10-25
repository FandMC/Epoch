package cn.fandmc.epoch.asset;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.Optional;

public final class AssetDirectory {
    private static final int MAX_RESOURCE_BYTES = 32 * 1024 * 1024;
    private final Path root;

    public AssetDirectory(Path directory) throws IOException {
        root = directory.toRealPath();
        if (!Files.isDirectory(root)) throw new IOException("Not an asset directory: " + directory);
    }

    public byte[] read(String resource) throws IOException {
        return find(resource).orElseThrow(() -> new IOException("Missing Epoch resource: " + resource));
    }

    public Optional<byte[]> find(String resource) throws IOException {
        if (resource == null || resource.isBlank() || resource.startsWith("/")
                || resource.contains("\\") || resource.contains(":"))
            throw new IOException("Invalid resource path");
        for (String segment : resource.split("/", -1)) {
            if (!segment.matches("[A-Za-z0-9_.-]+") || segment.equals(".") || segment.equals("..")
                    || segment.endsWith(".")) throw new IOException("Unsafe resource path");
        }
        Path file = root.resolve(resource).normalize();
        if (!file.startsWith(root)) throw new IOException("Resource path escapes asset directory");
        if (!Files.exists(file, LinkOption.NOFOLLOW_LINKS)) return Optional.empty();
        Path realFile = file.toRealPath();
        if (!realFile.startsWith(root)) throw new IOException("Resource link escapes asset directory");
        if (!Files.isRegularFile(realFile)) return Optional.empty();
        if (Files.size(realFile) > MAX_RESOURCE_BYTES) throw new IOException("Resource exceeds size limit");
        try (InputStream input = Files.newInputStream(realFile)) {
            byte[] data = input.readNBytes(MAX_RESOURCE_BYTES + 1);
            if (data.length > MAX_RESOURCE_BYTES) throw new IOException("Resource exceeds size limit");
            return Optional.of(data);
        }
    }
}
