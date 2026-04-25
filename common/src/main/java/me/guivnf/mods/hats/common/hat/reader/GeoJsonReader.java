package me.guivnf.mods.hats.common.hat.reader;

import com.google.gson.*;
import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.hat.HatMeta;
import me.guivnf.mods.hats.common.hat.HatModelData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public class GeoJsonReader
{
    public static HatModelData read(Path geoJson) throws IOException
    {
        try (InputStream in = Files.newInputStream(geoJson)) {
            return parseGeo(in);
        }
    }

    public static HatMeta readMeta(Path metaJson) throws IOException
    {
        try (InputStream in = Files.newInputStream(metaJson)) {
            JsonObject obj = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            return HatMeta.fromJson(obj);
        }
    }

    private static HatModelData parseGeo(InputStream in)
    {
        JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

        HatModelData model = new HatModelData();
        model.format = HatModelData.Format.GEO_JSON;

        if (!root.has("minecraft:geometry")) return model;

        JsonArray geometries = root.getAsJsonArray("minecraft:geometry");
        if (geometries.isEmpty()) return model;

        JsonObject geometry = geometries.get(0).getAsJsonObject();

        if (geometry.has("description")) {
            JsonObject desc = geometry.getAsJsonObject("description");
            model.textureWidth = desc.has("texture_width") ? desc.get("texture_width").getAsInt() : 64;
            model.textureHeight = desc.has("texture_height") ? desc.get("texture_height").getAsInt() : 64;
        }

        if (!geometry.has("bones")) return model;

        JsonArray bonesArray = geometry.getAsJsonArray("bones");

        Map<String, HatModelData.Bone> boneByName = new LinkedHashMap<>();
        Map<String, float[]> worldPivotByName = new LinkedHashMap<>();
        Map<String, String> parentByName = new LinkedHashMap<>();

        for (JsonElement el : bonesArray) {
            JsonObject boneObj = el.getAsJsonObject();

            HatModelData.Bone bone = new HatModelData.Bone();
            bone.name = boneObj.has("name") ? boneObj.get("name").getAsString() : "bone";

            float[] worldPivot = readVec3(boneObj, "pivot");
            worldPivotByName.put(bone.name, worldPivot);

            float[] rot = readVec3(boneObj, "rotation");
            bone.rotX = rot[0];
            bone.rotY = rot[1];
            bone.rotZ = rot[2];
            bone.mirror = boneObj.has("mirror") && boneObj.get("mirror").getAsBoolean();

            if (boneObj.has("cubes")) {
                for (JsonElement cubeEl : boneObj.getAsJsonArray("cubes")) {
                    JsonObject cubeObj = cubeEl.getAsJsonObject();
                    boolean hasPerCubeRot = cubeObj.has("rotation") || cubeObj.has("pivot");
                    if (hasPerCubeRot) {
                        float[] cubePivot = readVec3(cubeObj, "pivot");
                        float[] cubeRot   = readVec3(cubeObj, "rotation");
                        HatModelData.Bone wrapper = new HatModelData.Bone();
                        wrapper.name   = bone.name + "_cube" + bone.cubes.size() + bone.children.size();
                        wrapper.pivotX = cubePivot[0] - worldPivot[0];
                        wrapper.pivotY = cubePivot[1] - worldPivot[1];
                        wrapper.pivotZ = cubePivot[2] - worldPivot[2];
                        wrapper.rotX   = cubeRot[0];
                        wrapper.rotY   = cubeRot[1];
                        wrapper.rotZ   = cubeRot[2];
                        wrapper.cubes.add(parseCube(cubeObj, cubePivot));
                        bone.children.add(wrapper);
                    } else {
                        bone.cubes.add(parseCube(cubeObj, worldPivot));
                    }
                }
            }

            if (boneObj.has("parent")) {
                parentByName.put(bone.name, boneObj.get("parent").getAsString());
            }

            boneByName.put(bone.name, bone);
        }

        for (Map.Entry<String, HatModelData.Bone> entry : boneByName.entrySet()) {
            String boneName = entry.getKey();
            HatModelData.Bone bone = entry.getValue();

            String parentName = parentByName.get(boneName);
            if (parentName != null) {
                float[] myWorldPivot = worldPivotByName.get(boneName);
                float[] parentWorldPivot = worldPivotByName.getOrDefault(parentName, new float[]{0, 0, 0});

                bone.pivotX = myWorldPivot[0] - parentWorldPivot[0];
                bone.pivotY = myWorldPivot[1] - parentWorldPivot[1];
                bone.pivotZ = myWorldPivot[2] - parentWorldPivot[2];

                HatModelData.Bone parent = boneByName.get(parentName);
                if (parent != null) {
                    parent.children.add(bone);
                } else {
                    HatsMod.LOGGER.warn("Bone '{}' references unknown parent '{}'", boneName, parentName);
                    model.roots.add(bone);
                }
            } else {
                bone.pivotX = worldPivotByName.get(boneName)[0];
                bone.pivotY = worldPivotByName.get(boneName)[1];
                bone.pivotZ = worldPivotByName.get(boneName)[2];
                model.roots.add(bone);
            }
        }

        return model;
    }

    private static HatModelData.Cube parseCube(JsonObject cubeObj, float[] parentWorldPivot)
    {
        HatModelData.Cube cube = new HatModelData.Cube();

        float[] worldOrigin = readVec3(cubeObj, "origin");
        cube.originX = worldOrigin[0] - parentWorldPivot[0];
        cube.originY = worldOrigin[1] - parentWorldPivot[1];
        cube.originZ = worldOrigin[2] - parentWorldPivot[2];

        float[] size = readVec3(cubeObj, "size");
        cube.sizeX = size[0];
        cube.sizeY = size[1];
        cube.sizeZ = size[2];

        float inflate = cubeObj.has("inflate") ? cubeObj.get("inflate").getAsFloat() : 0f;
        cube.inflateX = inflate;
        cube.inflateY = inflate;
        cube.inflateZ = inflate;
        cube.mirror = cubeObj.has("mirror") && cubeObj.get("mirror").getAsBoolean();

        if (cubeObj.has("uv")) {
            JsonElement uvEl = cubeObj.get("uv");
            if (uvEl.isJsonArray()) {
                JsonArray uv = uvEl.getAsJsonArray();
                cube.texU = uv.get(0).getAsFloat();
                cube.texV = uv.get(1).getAsFloat();
            } else if (uvEl.isJsonObject()) {
                JsonObject perFace = uvEl.getAsJsonObject();
                if (perFace.has("north")) {
                    JsonArray northUv = perFace.getAsJsonObject("north").getAsJsonArray("uv");
                    cube.texU = northUv.get(0).getAsFloat();
                    cube.texV = northUv.get(1).getAsFloat();
                }
            }
        }

        return cube;
    }

    private static float[] readVec3(JsonObject obj, String key)
    {
        if (!obj.has(key)) return new float[]{0, 0, 0};

        JsonElement el = obj.get(key);
        if (el.isJsonArray()) {
            JsonArray arr = el.getAsJsonArray();
            return new float[]{
                arr.size() > 0 ? arr.get(0).getAsFloat() : 0,
                arr.size() > 1 ? arr.get(1).getAsFloat() : 0,
                arr.size() > 2 ? arr.get(2).getAsFloat() : 0
            };
        }
        return new float[]{0, 0, 0};
    }
}
