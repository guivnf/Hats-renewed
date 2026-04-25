package me.guivnf.mods.hats.common.hat;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import me.guivnf.mods.hats.HatsMod;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

public class HatPack
{
    public static final HatPack BUILTIN = new HatPack("builtin", "Built-in Hats", "Hats bundled with the mod.", "1.0", "iChun");

    public final String id;
    public final String name;
    public final String description;
    public final String version;
    public final String author;

    public HatPack(String id, String name, String description, String version, String author)
    {
        this.id = id;
        this.name = name;
        this.description = description;
        this.version = version;
        this.author = author;
    }

    public static HatPack fromDirectory(Path dir)
    {
        String id = dir.getFileName().toString();
        Path packJson = dir.resolve("pack.json");

        if (Files.exists(packJson)) {
            try (InputStream in = Files.newInputStream(packJson)) {
                return parsePackJson(id, in);
            } catch (IOException e) {
                HatsMod.LOGGER.warn("Failed to read pack.json for pack '{}': {}", id, e.getMessage());
            }
        }

        return new HatPack(id, id, "", "1.0", "unknown");
    }

    public static HatPack fromZipEntry(String id, InputStream packJsonStream)
    {
        try {
            return parsePackJson(id, packJsonStream);
        } catch (IOException e) {
            HatsMod.LOGGER.warn("Failed to parse pack.json from zip '{}': {}", id, e.getMessage());
        }
        return new HatPack(id, id, "", "1.0", "unknown");
    }

    private static HatPack parsePackJson(String id, InputStream in) throws IOException
    {
        JsonObject obj = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

        String name = obj.has("name") ? obj.get("name").getAsString() : id;
        String description = obj.has("description") ? obj.get("description").getAsString() : "";
        String version = obj.has("version") ? obj.get("version").getAsString() : "1.0";
        String author = obj.has("author") ? obj.get("author").getAsString() : "unknown";

        return new HatPack(id, name, description, version, author);
    }
}
