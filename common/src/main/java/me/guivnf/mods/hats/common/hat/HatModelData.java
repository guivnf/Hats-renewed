package me.guivnf.mods.hats.common.hat;

import java.util.ArrayList;
import java.util.List;

public class HatModelData
{
    public enum Format { TBL, GEO_JSON }

    public Format format = Format.GEO_JSON;
    public int textureWidth = 64;
    public int textureHeight = 64;
    public final List<Bone> roots = new ArrayList<>();

    public List<Bone> getAllBones()
    {
        List<Bone> all = new ArrayList<>();
        for (Bone root : roots) {
            collectBones(root, all);
        }
        return all;
    }

    public Bone findBone(String name)
    {
        for (Bone root : roots) {
            Bone found = findBoneIn(root, name);
            if (found != null) return found;
        }
        return null;
    }

    private void collectBones(Bone bone, List<Bone> out)
    {
        out.add(bone);
        for (Bone child : bone.children) {
            collectBones(child, out);
        }
    }

    private Bone findBoneIn(Bone bone, String name)
    {
        if (bone.name.equals(name)) return bone;
        for (Bone child : bone.children) {
            Bone found = findBoneIn(child, name);
            if (found != null) return found;
        }
        return null;
    }

    public static class Bone
    {
        public String name = "";
        public float pivotX = 0;
        public float pivotY = 0;
        public float pivotZ = 0;
        public float rotX = 0;
        public float rotY = 0;
        public float rotZ = 0;
        public boolean mirror = false;
        public boolean visible = true;
        public int texW = 0;
        public int texH = 0;
        public final List<Cube> cubes = new ArrayList<>();
        public final List<Bone> children = new ArrayList<>();
    }

    public static class Cube
    {
        public float originX = 0;
        public float originY = 0;
        public float originZ = 0;
        public float sizeX = 0;
        public float sizeY = 0;
        public float sizeZ = 0;
        public float texU = 0;
        public float texV = 0;
        public float inflateX = 0;
        public float inflateY = 0;
        public float inflateZ = 0;
        public boolean mirror = false;
    }
}
