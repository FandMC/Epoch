package cn.fandmc.epoch.asset;

import com.google.gson.*;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import java.io.*;
import java.nio.charset.StandardCharsets;

/** Shared strict UTF-8 JSON boundary with resource-aware errors. */
public final class JsonResources {
    private JsonResources() {}
    public static JsonObject object(byte[] bytes, String source) throws IOException {
        if (bytes.length > 32 * 1024 * 1024) throw new IOException("JSON too large: " + source);
        try (JsonReader reader = new JsonReader(new InputStreamReader(new ByteArrayInputStream(bytes), StandardCharsets.UTF_8))) {
            reader.setStrictness(Strictness.STRICT);
            JsonElement element = JsonParser.parseReader(reader);
            if (!element.isJsonObject() || reader.peek() != JsonToken.END_DOCUMENT)
                throw new IOException("Expected one JSON object: " + source);
            return element.getAsJsonObject();
        } catch (JsonParseException | IllegalStateException e) { throw new IOException("Invalid JSON: " + source, e); }
    }

    public static JsonObject bundled(String path) throws IOException {
        try (InputStream input = JsonResources.class.getResourceAsStream(path)) {
            if (input == null) throw new IOException("Missing bundled resource: " + path);
            return object(input.readNBytes(32 * 1024 * 1024 + 1), path);
        }
    }
}
