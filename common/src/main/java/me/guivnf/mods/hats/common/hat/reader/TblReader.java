package me.guivnf.mods.hats.common.hat.reader;

import com.google.gson.*;
import me.guivnf.mods.hats.common.hat.HatModelData;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class TblReader
{
    private static final String MODEL_ENTRY  = "model.json";
    private static final String TEXTURE_ENTRY = "texture.png";

    public static HatModelData readModel(Path tbl) throws IOException
    {
        try (ZipInputStream zip = openZip(tbl)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(MODEL_ENTRY)) {
                    return parseModel(zip);
                }
            }
        }
        throw new IOException("No model.json in " + tbl.getFileName());
    }

    public static byte[] readTexture(Path tbl) throws IOException
    {
        try (ZipInputStream zip = openZip(tbl)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(TEXTURE_ENTRY)) {
                    return zip.readAllBytes();
                }
            }
        }
        throw new IOException("No texture.png in " + tbl.getFileName());
    }

    public static List<String> readNotes(Path tbl) throws IOException
    {
        try (ZipInputStream zip = openZip(tbl)) {
            ZipEntry entry;
            while ((entry = zip.getNextEntry()) != null) {
                if (entry.getName().equals(MODEL_ENTRY)) {
                    return parseNotes(zip);
                }
            }
        }
        return Collections.emptyList();
    }

    private static HatModelData parseModel(InputStream in)
    {
        JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

        HatModelData model = new HatModelData();
        model.format = HatModelData.Format.TBL;
        model.textureWidth  = getInt(root, "texWidth",  64);
        model.textureHeight = getInt(root, "texHeight", 64);

        if (root.has("parts")) {
            for (JsonElement el : root.getAsJsonArray("parts")) {
                model.roots.add(parsePart(el.getAsJsonObject(), model.textureWidth, model.textureHeight));
            }
        }

        return model;
    }

    private static HatModelData.Bone parsePart(JsonObject part, int projectTexW, int projectTexH)
    {
        HatModelData.Bone bone = new HatModelData.Bone();

        bone.name    = part.has("name") ? part.get("name").getAsString() : "";
        bone.pivotX  = getFloat(part, "rotPX");
        bone.pivotY  = getFloat(part, "rotPY");
        bone.pivotZ  = getFloat(part, "rotPZ");
        bone.rotX    = (float) Math.toRadians(getFloat(part, "rotAX"));
        bone.rotY    = (float) Math.toRadians(getFloat(part, "rotAY"));
        bone.rotZ    = (float) Math.toRadians(getFloat(part, "rotAZ"));
        bone.mirror  = getBool(part, "mirror");
        bone.visible = getBoolDef(part, "showModel", true);

        boolean matchProject = getBoolDef(part, "matchProject", true);
        if (!matchProject) {
            bone.texW = getInt(part, "texWidth",  projectTexW);
            bone.texH = getInt(part, "texHeight", projectTexH);
        }

        int partTexU = getInt(part, "texOffX", 0);
        int partTexV = getInt(part, "texOffY", 0);

        if (part.has("boxes")) {
            for (JsonElement boxEl : part.getAsJsonArray("boxes")) {
                JsonObject box = boxEl.getAsJsonObject();
                HatModelData.Cube cube = new HatModelData.Cube();

                cube.originX  = getFloat(box, "posX");
                cube.originY  = getFloat(box, "posY");
                cube.originZ  = getFloat(box, "posZ");
                cube.sizeX    = getFloat(box, "dimX");
                cube.sizeY    = getFloat(box, "dimY");
                cube.sizeZ    = getFloat(box, "dimZ");
                cube.inflateX = getFloat(box, "expandX");
                cube.inflateY = getFloat(box, "expandY");
                cube.inflateZ = getFloat(box, "expandZ");
                cube.mirror   = bone.mirror;

                cube.texU = partTexU + getInt(box, "texOffX", 0);
                cube.texV = partTexV + getInt(box, "texOffY", 0);

                bone.cubes.add(cube);
            }
        }

        if (part.has("children")) {
            for (JsonElement childEl : part.getAsJsonArray("children")) {
                if (childEl.isJsonObject()) {
                    bone.children.add(parsePart(childEl.getAsJsonObject(), projectTexW, projectTexH));
                }
            }
        }

        return bone;
    }

    private static List<String> parseNotes(InputStream in)
    {
        JsonObject root = JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();

        if (!root.has("notes")) return Collections.emptyList();

        List<String> notes = new ArrayList<>();
        for (JsonElement el : root.getAsJsonArray("notes")) {
            notes.add(el.getAsString());
        }
        return notes;
    }

    private static ZipInputStream openZip(Path path) throws IOException
    {
        return new ZipInputStream(Files.newInputStream(path));
    }

    private static float getFloat(JsonObject obj, String key)
    {
        return obj.has(key) ? obj.get(key).getAsFloat() : 0f;
    }

    private static int getInt(JsonObject obj, String key, int def)
    {
        return obj.has(key) ? obj.get(key).getAsInt() : def;
    }

    private static boolean getBool(JsonObject obj, String key)
    {
        return obj.has(key) && obj.get(key).getAsBoolean();
    }

    private static boolean getBoolDef(JsonObject obj, String key, boolean def)
    {
        return obj.has(key) ? obj.get(key).getAsBoolean() : def;
    }
}
