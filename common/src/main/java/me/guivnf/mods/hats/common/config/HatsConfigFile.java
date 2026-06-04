package me.guivnf.mods.hats.common.config;

import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.hat.HatRarity;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public final class HatsConfigFile
{
    private HatsConfigFile() {}

    public static void loadOrCreate(Path configFolder, HatsConfig cfg)
    {
        Path file = configFolder.resolve("hats.toml");
        try {
            if (!Files.exists(file)) {
                Files.createDirectories(configFolder);
                Files.writeString(file, defaultToml(cfg));
                HatsMod.LOGGER.info("Generated default hats.toml config");
                return;
            }
            parseInto(Files.readString(file), cfg);
        } catch (IOException e) {
            HatsMod.LOGGER.error("Failed to read/write hats.toml, using defaults", e);
        }
    }

    private static void parseInto(String text, HatsConfig cfg)
    {
        String section = "";
        for (String rawLine : text.split("\\r?\\n")) {
            String line = stripComment(rawLine).trim();
            if (line.isEmpty()) continue;

            if (line.startsWith("[") && line.endsWith("]")) {
                section = line.substring(1, line.length() - 1).trim();
                continue;
            }

            int eq = line.indexOf('=');
            if (eq <= 0) continue;
            String key = line.substring(0, eq).trim();
            String value = line.substring(eq + 1).trim();
            applyKey(cfg, section, key, value);
        }
    }

    private static String stripComment(String line)
    {
        boolean inString = false;
        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') inString = !inString;
            else if (!inString && c == '#') return line.substring(0, i);
        }
        return line;
    }

    private static void applyKey(HatsConfig cfg, String section, String key, String value)
    {
        String path = section.isEmpty() ? key : section + "." + key;
        switch (path) {
            case "server.hat_blacklist" -> cfg.hatBlacklist = parseStringList(value);
            case "server.hats_show_when_invisible" -> cfg.renderOnInvisible = parseBool(value);
            case "server.hats_prevent_undead_fire" -> cfg.preventUndeadFire = parseBool(value);
            case "server.disable_contributor_hats" -> cfg.disableContributorHats = parseBool(value);
            case "server.hat_unlock_chat_announcement" -> cfg.hatUnlockAnnouncement = parseBool(value);
            case "server.spawn_chance.common" -> cfg.rarityChances.put(HatRarity.COMMON, parseDouble(value));
            case "server.spawn_chance.uncommon" -> cfg.rarityChances.put(HatRarity.UNCOMMON, parseDouble(value));
            case "server.spawn_chance.rare" -> cfg.rarityChances.put(HatRarity.RARE, parseDouble(value));
            case "server.spawn_chance.epic" -> cfg.rarityChances.put(HatRarity.EPIC, parseDouble(value));
            case "server.spawn_chance.legendary" -> cfg.rarityChances.put(HatRarity.LEGENDARY, parseDouble(value));
            case "client.max_hats_on_screen" -> cfg.maxHatRendersPerFrame = parseInt(value, cfg.maxHatRendersPerFrame);
            case "client.display_hat_unlock_toast" -> cfg.displayHatUnlockToast = parseBool(value);
            case "server.hat_entity_blacklist" -> cfg.entityIdBlacklist = parseStringList(value);
            default -> HatsMod.LOGGER.warn("Unknown hats.toml key: {}", path);
        }
    }

    private static boolean parseBool(String s)
    {
        return s.trim().equalsIgnoreCase("true");
    }

    private static int parseInt(String s, int fallback)
    {
        try { return Integer.parseInt(s.trim()); } catch (NumberFormatException e) { return fallback; }
    }

    private static double parseDouble(String s)
    {
        String t = s.trim();
        if (t.endsWith("%")) {
            try { return Double.parseDouble(t.substring(0, t.length() - 1).trim()) / 100.0; }
            catch (NumberFormatException e) { return 0.0; }
        }
        try { return Double.parseDouble(t); } catch (NumberFormatException e) { return 0.0; }
    }

    private static List<String> parseStringList(String s)
    {
        List<String> result = new ArrayList<>();
        String t = s.trim();
        if (!t.startsWith("[") || !t.endsWith("]")) return result;
        t = t.substring(1, t.length() - 1).trim();
        if (t.isEmpty()) return result;

        StringBuilder cur = new StringBuilder();
        boolean inString = false;
        for (int i = 0; i < t.length(); i++) {
            char c = t.charAt(i);
            if (c == '"') { inString = !inString; continue; }
            if (!inString && c == ',') { addIfNotEmpty(result, cur); cur.setLength(0); continue; }
            if (inString) cur.append(c);
        }
        addIfNotEmpty(result, cur);
        return result;
    }

    private static void addIfNotEmpty(List<String> out, StringBuilder buf)
    {
        String v = buf.toString().trim();
        if (!v.isEmpty()) out.add(v);
    }

    private static String defaultToml(HatsConfig cfg)
    {
        StringBuilder sb = new StringBuilder();
        sb.append("# =============================================================\n");
        sb.append("# Hats-renewed configuration\n");
        sb.append("# Edit values and restart the game (or reload the world) for\n");
        sb.append("# changes to apply. Lines starting with # are comments.\n");
        sb.append("# =============================================================\n\n");

        sb.append("[server]\n\n");

        sb.append("# Hats that will never roll as a random mob hat. Use the hat's\n");
        sb.append("# display name as shown in the hat menu, e.g. [\"Top Hat\", \"Garland\"].\n");
        sb.append("hat_blacklist = []\n\n");

        sb.append("# Exclude Mobs of Mob groups from Spawning with hats. Use Mob IDs\n");
        sb.append("# to exclude them e.g. [\"minecraft\", \"minecraft:zombie\"]\n");
        sb.append("hat_entity_blacklist = []\n\n");

        sb.append("# If true, hats stay visible even when the wearing entity is invisible\n");
        sb.append("# (invisibility potion, spectator, etc). Default: false.\n");
        sb.append("hats_show_when_invisible = ").append(cfg.renderOnInvisible).append("\n\n");

        sb.append("# If true, undead mobs (zombies, skeletons, drowned, husks, etc)\n");
        sb.append("# that are wearing a hat will not burn in sunlight. Other fire\n");
        sb.append("# sources (lava, fire blocks, flame arrows, etc) still harm them.\n");
        sb.append("# Default: false.\n");
        sb.append("hats_prevent_undead_fire = ").append(cfg.preventUndeadFire).append("\n\n");

        sb.append("# If true, hats marked as contributor hats (the cyan-colored '(C)'\n");
        sb.append("# hats submitted by community contributors) are completely removed\n");
        sb.append("# from the registry: they won't spawn on mobs and won't show in the\n");
        sb.append("# hat menu. Default: false.\n");
        sb.append("disable_contributor_hats = ").append(cfg.disableContributorHats).append("\n\n");

        sb.append("# If true, a chat message is broadcast to all players whenever one\n");
        sb.append("# of them unlocks a new hat for the first time. The hat name is\n");
        sb.append("# colored by rarity and hovering shows its info. Default: false.\n");
        sb.append("hat_unlock_chat_announcement = ").append(cfg.hatUnlockAnnouncement).append("\n\n");

        sb.append("# Per-rarity hat spawn chance. Each mob rolls once; the rarity is\n");
        sb.append("# picked by cumulative probability from common -> legendary. The sum\n");
        sb.append("# of all values is the overall chance a mob receives any hat.\n");
        sb.append("# Values are fractions in 0.0-1.0 (0.10 = 10%). Accepts '10%' too.\n");
        sb.append("[server.spawn_chance]\n");
        sb.append("common    = ").append(cfg.rarityChances.getOrDefault(HatRarity.COMMON,    0.10)).append("\n");
        sb.append("uncommon  = ").append(cfg.rarityChances.getOrDefault(HatRarity.UNCOMMON,  0.08)).append("\n");
        sb.append("rare      = ").append(cfg.rarityChances.getOrDefault(HatRarity.RARE,      0.05)).append("\n");
        sb.append("epic      = ").append(cfg.rarityChances.getOrDefault(HatRarity.EPIC,      0.03)).append("\n");
        sb.append("legendary = ").append(cfg.rarityChances.getOrDefault(HatRarity.LEGENDARY, 0.02)).append("\n\n");

        sb.append("[client]\n\n");

        sb.append("# Maximum number of hats that may be rendered in a single frame.\n");
        sb.append("# Lower this if you see FPS drops in crowded areas. Default: 64.\n");
        sb.append("max_hats_on_screen = ").append(cfg.maxHatRendersPerFrame).append("\n\n");

        sb.append("# If true, show the 'New Hat Unlocked!' toast popup when you pick\n");
        sb.append("# up a hat you didn't own. Default: true.\n");
        sb.append("display_hat_unlock_toast = ").append(cfg.displayHatUnlockToast).append("\n");

        return sb.toString();
    }
}
