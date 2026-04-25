package me.guivnf.mods.hats.common.hat;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import me.guivnf.mods.hats.HatsMod;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

public class HatMeta
{
    public HatRarity forcedRarity;
    public String forcedPool;
    public int forcedWorth = -1;
    public UUID contributorUuid;
    public String description;
    public String accessoryFor;
    public final List<String> accessoryLayers = new ArrayList<>();
    public String accessoryParent;
    public final List<String> hideParentParts = new ArrayList<>();

    public final Map<String, float[]> boneColorOverrides = new LinkedHashMap<>();

    public final Map<String, String> boneTextureOverrides = new LinkedHashMap<>();

    public Boolean cull = null;

    public static HatMeta fromJson(JsonObject json)
    {
        HatMeta meta = new HatMeta();

        if (json.has("rarity")) {
            HatRarity r = HatRarity.fromString(json.get("rarity").getAsString());
            if (r != null) meta.forcedRarity = r;
        }
        if (json.has("pool")) {
            String v = json.get("pool").getAsString().trim();
            if (!v.isEmpty()) meta.forcedPool = v;
        }
        if (json.has("worth")) {
            try {
                meta.forcedWorth = json.get("worth").getAsInt();
            } catch (NumberFormatException e) {
                HatsMod.LOGGER.warn("Invalid worth value in hat meta: {}", json.get("worth"));
            }
        }
        if (json.has("contributor_uuid")) {
            String v = json.get("contributor_uuid").getAsString().trim();
            if (!v.isEmpty()) {
                try {
                    meta.contributorUuid = UUID.fromString(v);
                } catch (IllegalArgumentException e) {
                    HatsMod.LOGGER.warn("Invalid contributor_uuid in hat meta");
                }
            }
        }
        if (json.has("description")) {
            String v = json.get("description").getAsString().trim();
            if (!v.isEmpty()) meta.description = v;
        }
        if (json.has("accessory_for")) {
            String v = json.get("accessory_for").getAsString().trim();
            if (!v.isEmpty()) meta.accessoryFor = v;
        }
        if (json.has("accessory_parent")) {
            String v = json.get("accessory_parent").getAsString().trim();
            if (!v.isEmpty()) meta.accessoryParent = v;
        }

        if (json.has("accessory_layers")) {
            JsonArray arr = json.getAsJsonArray("accessory_layers");
            for (JsonElement el : arr) {
                meta.accessoryLayers.add(el.getAsString().trim());
            }
        }
        if (json.has("hide_parent_parts")) {
            JsonArray arr = json.getAsJsonArray("hide_parent_parts");
            for (JsonElement el : arr) {
                meta.hideParentParts.add(el.getAsString().trim());
            }
        }

        if (json.has("bone_colors")) {
            JsonObject obj = json.getAsJsonObject("bone_colors");
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                JsonArray arr = entry.getValue().getAsJsonArray();
                float[] rgba = new float[]{
                    arr.size() > 0 ? arr.get(0).getAsFloat() : 1f,
                    arr.size() > 1 ? arr.get(1).getAsFloat() : 1f,
                    arr.size() > 2 ? arr.get(2).getAsFloat() : 1f,
                    arr.size() > 3 ? arr.get(3).getAsFloat() : 1f
                };
                meta.boneColorOverrides.put(entry.getKey(), rgba);
            }
        }

        if (json.has("cull")) {
            meta.cull = json.get("cull").getAsBoolean();
        }
        if (json.has("bone_textures")) {
            JsonObject obj = json.getAsJsonObject("bone_textures");
            for (Map.Entry<String, JsonElement> entry : obj.entrySet()) {
                meta.boneTextureOverrides.put(entry.getKey(), entry.getValue().getAsString().trim());
            }
        }

        return meta;
    }

    public static HatMeta fromTblNotes(List<String> notes)
    {
        HatMeta meta = new HatMeta();

        for (String note : notes) {
            if (note.startsWith("hats-rarity:")) {
                HatRarity r = HatRarity.fromString(note.substring("hats-rarity:".length()).trim());
                if (r != null) meta.forcedRarity = r;
            } else if (note.startsWith("hats-pool:")) {
                meta.forcedPool = note.substring("hats-pool:".length()).trim();
            } else if (note.startsWith("hats-worth:")) {
                try {
                    meta.forcedWorth = Integer.parseInt(note.substring("hats-worth:".length()).trim());
                } catch (NumberFormatException e) {
                    HatsMod.LOGGER.warn("Invalid hats-worth value in tbl notes: {}", note);
                }
            } else if (note.startsWith("hats-contributor-uuid:")) {
                try {
                    meta.contributorUuid = UUID.fromString(note.substring("hats-contributor-uuid:".length()).trim());
                } catch (IllegalArgumentException e) {
                    HatsMod.LOGGER.warn("Invalid hats-contributor-uuid in tbl notes");
                }
            } else if (note.startsWith("hats-contributor-mini-me:")) {
                if (meta.contributorUuid == null) {
                    meta.contributorUuid = new UUID(0L, 0L);
                }
            } else if (note.startsWith("hats-accessory:")) {
                meta.accessoryFor = note.substring("hats-accessory:".length()).trim();
            } else if (note.startsWith("hats-accessory-layer:")) {
                meta.accessoryLayers.add(note.substring("hats-accessory-layer:".length()).trim());
            } else if (note.startsWith("hats-accessory-parent:")) {
                meta.accessoryParent = note.substring("hats-accessory-parent:".length()).trim();
            } else if (note.startsWith("hats-accessory-hide-parent-part:")) {
                meta.hideParentParts.add(note.substring("hats-accessory-hide-parent-part:".length()).trim());
            } else if (note.startsWith("hats-description:")) {
                meta.description = note.substring("hats-description:".length()).trim();
            } else if (note.startsWith("hats-cull:")) {
                String v = note.substring("hats-cull:".length()).trim().toLowerCase(Locale.ROOT);
                if (v.equals("true") || v.equals("false")) meta.cull = v.equals("true");
            } else if (note.toLowerCase(Locale.ROOT).startsWith("hat")) {
                HatsMod.LOGGER.warn("Unknown hat meta note in tbl: {}", note);
            }
        }

        return meta;
    }
}
