package me.guivnf.mods.hats.common.hat.placement;

public class HatPlacementInfo
{
    public static final HatPlacementInfo DEFAULT = new HatPlacementInfo(0f, 0f, 0f, 0f, 0f, 0f, 1f, false);

    public final float offsetX;
    public final float offsetY;
    public final float offsetZ;
    public final float rotX;
    public final float rotY;
    public final float rotZ;
    public final float scale;
    public final boolean isBoss;

    public HatPlacementInfo(float offsetX, float offsetY, float offsetZ,
                             float rotX, float rotY, float rotZ,
                             float scale, boolean isBoss)
    {
        this.offsetX = offsetX;
        this.offsetY = offsetY;
        this.offsetZ = offsetZ;
        this.rotX    = rotX;
        this.rotY    = rotY;
        this.rotZ    = rotZ;
        this.scale   = scale;
        this.isBoss  = isBoss;
    }

    public HatPlacementInfo(float offsetX, float offsetY, float offsetZ, float scale, boolean isBoss)
    {
        this(offsetX, offsetY, offsetZ, 0f, 0f, 0f, scale, isBoss);
    }

    public static Builder builder()
    {
        return new Builder();
    }

    public static class Builder
    {
        private float x = 0, y = 0, z = 0;
        private float rx = 0, ry = 0, rz = 0;
        private float scale = 1f;
        private boolean boss = false;

        public Builder offset(float x, float y, float z) { this.x = x; this.y = y; this.z = z; return this; }
        public Builder rotation(float rx, float ry, float rz) { this.rx = rx; this.ry = ry; this.rz = rz; return this; }
        public Builder scale(float scale) { this.scale = scale; return this; }
        public Builder boss() { this.boss = true; return this; }

        public HatPlacementInfo build()
        {
            return new HatPlacementInfo(x, y, z, rx, ry, rz, scale, boss);
        }
    }
}
