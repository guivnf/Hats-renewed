package me.guivnf.mods.hats.common.network.packet;

import com.google.gson.*;
import dev.architectury.networking.NetworkManager;
import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.hat.*;


import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.core.RegistryAccess;

import org.jetbrains.annotations.Nullable;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class PacketHatDataFragment
{
    private static final Gson GSON = new Gson();

    public static RegistryFriendlyByteBuf encode(
            String hatName, String packId,
            int fragmentIndex, int totalFragments,
            byte[] textureChunk,
            @Nullable HatModelData model,
            @Nullable HatMeta meta)
    {
        RegistryFriendlyByteBuf buf = new net.minecraft.network.RegistryFriendlyByteBuf(io.netty.buffer.Unpooled.buffer(), net.minecraft.core.RegistryAccess.EMPTY);
        buf.writeUtf(hatName);
        buf.writeUtf(packId);
        buf.writeInt(fragmentIndex);
        buf.writeInt(totalFragments);
        buf.writeByteArray(textureChunk);

        buf.writeBoolean(model != null);
        if (model != null) {
            buf.writeUtf(serializeModel(model));
        }

        buf.writeBoolean(meta != null);
        if (meta != null) {
            buf.writeUtf(serializeMeta(meta));
        }

        return buf;
    }

    public static void handle(RegistryFriendlyByteBuf buf, NetworkManager.PacketContext context)
    {
        String hatName = buf.readUtf();
        String packId = buf.readUtf();
        int fragmentIndex = buf.readInt();
        int totalFragments = buf.readInt();
        byte[] textureChunk = buf.readByteArray();

        HatModelData model = null;
        if (buf.readBoolean()) {
            try {
                model = deserializeModel(buf.readUtf());
            } catch (Exception e) {
                HatsMod.LOGGER.error("Failed to deserialize model for hat '{}': {}", hatName, e.getMessage());
            }
        }

        HatMeta meta = null;
        if (buf.readBoolean()) {
            try {
                meta = HatMeta.fromJson(JsonParser.parseString(buf.readUtf()).getAsJsonObject());
            } catch (Exception e) {
                HatsMod.LOGGER.error("Failed to deserialize meta for hat '{}': {}", hatName, e.getMessage());
            }
        }

        HatModelData finalModel = model;
        HatMeta finalMeta = meta;
        context.queue(() -> handleClient(hatName, packId, fragmentIndex, totalFragments, textureChunk, finalModel, finalMeta));
    }


    private static void handleClient(String hatName, String packId, int fragmentIndex, int totalFragments,
            byte[] textureChunk, @Nullable HatModelData model, @Nullable HatMeta meta)
    {
        me.guivnf.mods.hats.client.cache.ClientHatCache.receiveFragment(
            hatName, packId, fragmentIndex, totalFragments, textureChunk, model, meta);
    }

    private static String serializeModel(HatModelData model)
    {
        JsonObject root = new JsonObject();
        root.addProperty("format", model.format.name());
        root.addProperty("textureWidth", model.textureWidth);
        root.addProperty("textureHeight", model.textureHeight);

        JsonArray bones = new JsonArray();
        for (HatModelData.Bone bone : model.getAllBones()) {
            bones.add(serializeBone(bone));
        }
        root.add("bones", bones);

        return GSON.toJson(root);
    }

    private static JsonObject serializeBone(HatModelData.Bone bone)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("name", bone.name);
        obj.addProperty("pivotX", bone.pivotX);
        obj.addProperty("pivotY", bone.pivotY);
        obj.addProperty("pivotZ", bone.pivotZ);
        obj.addProperty("rotX", bone.rotX);
        obj.addProperty("rotY", bone.rotY);
        obj.addProperty("rotZ", bone.rotZ);
        obj.addProperty("mirror", bone.mirror);
        obj.addProperty("visible", bone.visible);

        JsonArray cubes = new JsonArray();
        for (HatModelData.Cube cube : bone.cubes) {
            cubes.add(serializeCube(cube));
        }
        obj.add("cubes", cubes);

        JsonArray children = new JsonArray();
        for (HatModelData.Bone child : bone.children) {
            children.add(child.name);
        }
        obj.add("children", children);

        return obj;
    }

    private static JsonObject serializeCube(HatModelData.Cube cube)
    {
        JsonObject obj = new JsonObject();
        obj.addProperty("originX", cube.originX);
        obj.addProperty("originY", cube.originY);
        obj.addProperty("originZ", cube.originZ);
        obj.addProperty("sizeX", cube.sizeX);
        obj.addProperty("sizeY", cube.sizeY);
        obj.addProperty("sizeZ", cube.sizeZ);
        obj.addProperty("texU", cube.texU);
        obj.addProperty("texV", cube.texV);
        obj.addProperty("inflateX", cube.inflateX);
        obj.addProperty("inflateY", cube.inflateY);
        obj.addProperty("inflateZ", cube.inflateZ);
        obj.addProperty("mirror", cube.mirror);
        return obj;
    }

    private static String serializeMeta(HatMeta meta)
    {
        JsonObject obj = new JsonObject();
        if (meta.forcedRarity != null) obj.addProperty("rarity", meta.forcedRarity.name().toLowerCase());
        if (meta.forcedPool != null) obj.addProperty("pool", meta.forcedPool);
        if (meta.forcedWorth >= 0) obj.addProperty("worth", meta.forcedWorth);
        if (meta.contributorUuid != null) obj.addProperty("contributor_uuid", meta.contributorUuid.toString());
        if (meta.description != null) obj.addProperty("description", meta.description);
        if (meta.accessoryFor != null) obj.addProperty("accessory_for", meta.accessoryFor);
        if (meta.accessoryParent != null) obj.addProperty("accessory_parent", meta.accessoryParent);

        JsonArray layers = new JsonArray();
        for (String layer : meta.accessoryLayers) layers.add(layer);
        obj.add("accessory_layers", layers);

        JsonArray hide = new JsonArray();
        for (String part : meta.hideParentParts) hide.add(part);
        obj.add("hide_parent_parts", hide);

        return GSON.toJson(obj);
    }

    private static HatModelData deserializeModel(String json)
    {
        JsonObject root = JsonParser.parseString(json).getAsJsonObject();
        HatModelData model = new HatModelData();
        model.format = HatModelData.Format.valueOf(root.get("format").getAsString());
        model.textureWidth = root.get("textureWidth").getAsInt();
        model.textureHeight = root.get("textureHeight").getAsInt();

        Map<String, HatModelData.Bone> boneByName = new LinkedHashMap<>();
        Map<String, List<String>> childrenByName = new LinkedHashMap<>();

        for (JsonElement el : root.getAsJsonArray("bones")) {
            JsonObject obj = el.getAsJsonObject();
            HatModelData.Bone bone = new HatModelData.Bone();
            bone.name = obj.get("name").getAsString();
            bone.pivotX = obj.get("pivotX").getAsFloat();
            bone.pivotY = obj.get("pivotY").getAsFloat();
            bone.pivotZ = obj.get("pivotZ").getAsFloat();
            bone.rotX = obj.get("rotX").getAsFloat();
            bone.rotY = obj.get("rotY").getAsFloat();
            bone.rotZ = obj.get("rotZ").getAsFloat();
            bone.mirror = obj.get("mirror").getAsBoolean();
            bone.visible = obj.get("visible").getAsBoolean();

            for (JsonElement cubeEl : obj.getAsJsonArray("cubes")) {
                JsonObject c = cubeEl.getAsJsonObject();
                HatModelData.Cube cube = new HatModelData.Cube();
                cube.originX = c.get("originX").getAsFloat();
                cube.originY = c.get("originY").getAsFloat();
                cube.originZ = c.get("originZ").getAsFloat();
                cube.sizeX = c.get("sizeX").getAsFloat();
                cube.sizeY = c.get("sizeY").getAsFloat();
                cube.sizeZ = c.get("sizeZ").getAsFloat();
                cube.texU = c.get("texU").getAsFloat();
                cube.texV = c.get("texV").getAsFloat();
                cube.inflateX = c.has("inflateX") ? c.get("inflateX").getAsFloat() : (c.has("inflate") ? c.get("inflate").getAsFloat() : 0f);
                cube.inflateY = c.has("inflateY") ? c.get("inflateY").getAsFloat() : cube.inflateX;
                cube.inflateZ = c.has("inflateZ") ? c.get("inflateZ").getAsFloat() : cube.inflateX;
                cube.mirror = c.get("mirror").getAsBoolean();
                bone.cubes.add(cube);
            }

            List<String> children = new ArrayList<>();
            for (JsonElement childEl : obj.getAsJsonArray("children")) {
                children.add(childEl.getAsString());
            }

            boneByName.put(bone.name, bone);
            childrenByName.put(bone.name, children);
        }

        Set<String> nonRoots = new HashSet<>();
        for (List<String> children : childrenByName.values()) {
            nonRoots.addAll(children);
        }

        for (Map.Entry<String, HatModelData.Bone> entry : boneByName.entrySet()) {
            for (String childName : childrenByName.getOrDefault(entry.getKey(), List.of())) {
                HatModelData.Bone child = boneByName.get(childName);
                if (child != null) entry.getValue().children.add(child);
            }
            if (!nonRoots.contains(entry.getKey())) {
                model.roots.add(entry.getValue());
            }
        }

        return model;
    }
}
