package me.guivnf.mods.hats.client.gui;

import me.guivnf.mods.hats.HatsMod;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashSet;
import java.util.Set;

public class ClientFavourites
{
    private static final Set<String> FAVS = new LinkedHashSet<>();
    private static boolean loaded = false;

    private static Path file()
    {
        return HatsMod.hatsDirectory.resolve("favourites.txt");
    }

    public static void load()
    {
        FAVS.clear();
        loaded = true;
        Path f = file();
        if (!Files.exists(f)) return;
        try {
            for (String line : Files.readAllLines(f)) {
                String trimmed = line.trim();
                if (!trimmed.isEmpty()) FAVS.add(trimmed);
            }
        } catch (IOException e) {
            HatsMod.LOGGER.warn("Failed to load hat favourites: {}", e.getMessage());
        }
    }

    public static boolean isFavourite(String name)
    {
        if (!loaded) load();
        return FAVS.contains(name);
    }

    public static void toggle(String name)
    {
        if (!loaded) load();
        if (!FAVS.remove(name)) FAVS.add(name);
        save();
    }

    private static void save()
    {
        try {
            Files.createDirectories(file().getParent());
            Files.writeString(file(), String.join("\n", FAVS));
        } catch (IOException e) {
            HatsMod.LOGGER.warn("Failed to save hat favourites: {}", e.getMessage());
        }
    }
}
