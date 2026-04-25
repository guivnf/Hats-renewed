package me.guivnf.mods.hats.client.compat;

import dev.architectury.platform.Platform;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class GeckoLibCompat
{

    private static final String GECKO_RENDERER    = "software.bernie.geckolib.renderer.GeoEntityRenderer";
    private static final String GECKO_BONE        = "software.bernie.geckolib.cache.object.GeoBone";

    private static final String AZURE_RENDERER    = "mod.azure.azurelib.render.entity.AzEntityRenderer";
    private static final String AZURE_BONE        = "mod.azure.azurelib.model.AzBone";
    private static final String AZURE_BAKED_MODEL = "mod.azure.azurelib.model.AzBakedModel";


    private static final boolean GECKO_PRESENT = Platform.isModLoaded("geckolib");
    private static final boolean AZURE_PRESENT = Platform.isModLoaded("azurelib");
    public  static final boolean PRESENT        = GECKO_PRESENT || AZURE_PRESENT;


    private static volatile boolean geckoReflectDone, geckoReflectOk;
    private static Class<?> geckoRendererClass;
    private static Method   geckoGetGeoModel;
    private static final Method[] geckoGetBone = new Method[1];
    private static Method geckoPivotX, geckoPivotY, geckoPivotZ;
    private static Method geckoPosX,   geckoPosY,   geckoPosZ;
    private static Method geckoRotX,   geckoRotY,   geckoRotZ;
    private static Method geckoParent;
    private static Method geckoGetCubes;


    private static volatile boolean azureReflectDone, azureReflectOk;
    private static Class<?> azureRendererClass;
    private static Field    azureProviderField;
    private static Method   azureProvideBakedModel;
    private static Method   azureBakedModelGetBone;
    private static Method azurePivotX, azurePivotY, azurePivotZ;
    private static Method azurePosX,   azurePosY,   azurePosZ;
    private static Method azureRotX,   azureRotY,   azureRotZ;
    private static Method azureParent;
    private static Method azureGetCubes;


    private static final List<String> HEAD_NAMES = List.of(
        "head", "Head", "skull", "Skull", "neck", "Neck", "head_pivot", "HEAD"
    );

    private GeckoLibCompat() {}


    public static boolean isGeckoLibEntity(LivingEntity entity)
    {
        try {
            var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if (GECKO_PRESENT && ensureGecko() && geckoRendererClass.isInstance(renderer)) return true;
            if (AZURE_PRESENT && ensureAzure() && azureRendererClass.isInstance(renderer)) return true;
        } catch (Exception ignored) {}
        return false;
    }

    @Nullable
    public static Matrix4f getHeadMatrix(LivingEntity entity)
    {
        if (GECKO_PRESENT && ensureGecko()) {
            Matrix4f m = tryGetHeadMatrixGecko(entity);
            if (m != null) return m;
        }
        if (AZURE_PRESENT && ensureAzure()) {
            return tryGetHeadMatrixAzure(entity);
        }
        return null;
    }


    @Nullable
    private static Matrix4f tryGetHeadMatrixGecko(LivingEntity entity)
    {
        try {
            var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if (!geckoRendererClass.isInstance(renderer)) return null;

            Object model = geckoGetGeoModel.invoke(renderer);
            if (model == null) return null;

            if (geckoGetBone[0] == null) {
                synchronized (GeckoLibCompat.class) {
                    if (geckoGetBone[0] == null) {
                        geckoGetBone[0] = findInHierarchy(model.getClass(), "getBone", String.class);
                    }
                }
            }
            if (geckoGetBone[0] == null) return null;

            Object headBone = findHeadBone(model, geckoGetBone[0]);
            if (headBone == null) return null;

            return buildMatrix(headBone,
                geckoPivotX, geckoPivotY, geckoPivotZ,
                geckoPosX,   geckoPosY,   geckoPosZ,
                geckoRotX,   geckoRotY,   geckoRotZ,
                geckoParent, geckoGetCubes);
        } catch (Exception e) { return null; }
    }


    @Nullable
    private static Matrix4f tryGetHeadMatrixAzure(LivingEntity entity)
    {
        try {
            var renderer = Minecraft.getInstance().getEntityRenderDispatcher().getRenderer(entity);
            if (!azureRendererClass.isInstance(renderer)) return null;

            Object provider = azureProviderField.get(renderer);
            if (provider == null) return null;
            Object bakedModel = azureProvideBakedModel.invoke(provider, entity, entity);
            if (bakedModel == null) return null;

            Object headBone = findHeadBone(bakedModel, azureBakedModelGetBone);
            if (headBone == null) return null;

            return buildMatrix(headBone,
                azurePivotX, azurePivotY, azurePivotZ,
                azurePosX,   azurePosY,   azurePosZ,
                azureRotX,   azureRotY,   azureRotZ,
                azureParent, azureGetCubes);
        } catch (Exception e) { return null; }
    }


    @Nullable
    private static Object findHeadBone(Object model, Method getBoneMethod)
    {
        try {
            for (String name : HEAD_NAMES) {
                @SuppressWarnings("unchecked")
                Optional<Object> opt = (Optional<Object>) getBoneMethod.invoke(model, name);
                if (opt != null && opt.isPresent()) return opt.get();
            }
        } catch (Exception ignored) {}
        return null;
    }

    @Nullable
    private static Matrix4f buildMatrix(
        Object headBone,
        Method pivX, Method pivY, Method pivZ,
        @Nullable Method posX, @Nullable Method posY, @Nullable Method posZ,
        Method rotX, Method rotY, Method rotZ,
        Method parent, @Nullable Method getCubes)
    {
        try {
            List<Object> chain = new ArrayList<>();
            Object cur = headBone;
            while (cur != null) {
                chain.add(0, cur);
                cur = parent.invoke(cur);
            }

            Matrix4f mat = new Matrix4f();
            for (int i = 0; i < chain.size(); i++) {
                Object bone = chain.get(i);
                boolean isHead = (i == chain.size() - 1);

                float px = p(pivX, bone), py = p(pivY, bone), pz = p(pivZ, bone);
                float tx = (-a(posX, bone) + px) / 16f;
                float ty = ( a(posY, bone) + py) / 16f;
                float tz = ( a(posZ, bone) + pz) / 16f;
                float rx = (float) rotX.invoke(bone);
                float ry = (float) rotY.invoke(bone);
                float rz = (float) rotZ.invoke(bone);

                mat.translate(tx, ty, tz);
                mat.rotateZ(rz);
                mat.rotateY(ry);
                mat.rotateX(rx);
                if (!isHead) {
                    mat.translate(-px / 16f, -py / 16f, -pz / 16f);
                }
            }

            float cubeH = getCubes != null ? getMaxCubeY(headBone, getCubes) : 8f;
            if (cubeH <= 0f) cubeH = 8f;
            mat.translate(0f, cubeH / 16f, 0f);

            return mat;
        } catch (Exception e) { return null; }
    }

    private static float getMaxCubeY(Object bone, Method getCubes)
    {
        try {
            List<?> cubes = (List<?>) getCubes.invoke(bone);
            float maxH = 0f;
            for (Object cube : cubes) {
                float h = getVecY(cube.getClass().getMethod("size").invoke(cube));
                if (h > maxH) maxH = h;
            }
            return maxH;
        } catch (Exception e) { return 0f; }
    }

    private static float getVecY(Object vec)
    {
        try {
            Field f = vec.getClass().getField("y");
            return ((Number) f.get(vec)).floatValue();
        } catch (Exception ignored) {}
        try {
            Method m = vec.getClass().getMethod("y");
            return ((Number) m.invoke(vec)).floatValue();
        } catch (Exception ignored) {}
        return 0f;
    }


    private static synchronized boolean ensureGecko()
    {
        if (geckoReflectDone) return geckoReflectOk;
        geckoReflectDone = true;
        try {
            geckoRendererClass = Class.forName(GECKO_RENDERER);
            geckoGetGeoModel   = geckoRendererClass.getMethod("getGeoModel");
            geckoGetGeoModel.setAccessible(true);
            Class<?> bone = Class.forName(GECKO_BONE);
            geckoPivotX = bone.getMethod("getPivotX"); geckoPivotX.setAccessible(true);
            geckoPivotY = bone.getMethod("getPivotY"); geckoPivotY.setAccessible(true);
            geckoPivotZ = bone.getMethod("getPivotZ"); geckoPivotZ.setAccessible(true);
            geckoRotX   = bone.getMethod("getRotX");   geckoRotX.setAccessible(true);
            geckoRotY   = bone.getMethod("getRotY");   geckoRotY.setAccessible(true);
            geckoRotZ   = bone.getMethod("getRotZ");   geckoRotZ.setAccessible(true);
            geckoParent = bone.getMethod("getParent"); geckoParent.setAccessible(true);
            try { geckoGetCubes = bone.getMethod("getCubes"); geckoGetCubes.setAccessible(true); } catch (NoSuchMethodException ignored) {}
            try {
                geckoPosX = bone.getMethod("getPosX"); geckoPosX.setAccessible(true);
                geckoPosY = bone.getMethod("getPosY"); geckoPosY.setAccessible(true);
                geckoPosZ = bone.getMethod("getPosZ"); geckoPosZ.setAccessible(true);
            } catch (NoSuchMethodException ignored) {}
            geckoReflectOk = true;
        } catch (Exception ignored) {}
        return geckoReflectOk;
    }

    private static synchronized boolean ensureAzure()
    {
        if (azureReflectDone) return azureReflectOk;
        azureReflectDone = true;
        try {
            azureRendererClass = Class.forName(AZURE_RENDERER);

            Field pf = findFieldInHierarchy(azureRendererClass, "provider");
            if (pf == null) return azureReflectOk;
            pf.setAccessible(true);
            azureProviderField = pf;

            Method pbm = findMethodByName(pf.getType(), "provideBakedModel");
            if (pbm == null) return azureReflectOk;
            pbm.setAccessible(true);
            azureProvideBakedModel = pbm;

            Class<?> bakedModelClass = Class.forName(AZURE_BAKED_MODEL);
            azureBakedModelGetBone = bakedModelClass.getMethod("getBone", String.class);
            azureBakedModelGetBone.setAccessible(true);

            Class<?> bone = Class.forName(AZURE_BONE);
            azurePivotX = bone.getMethod("getPivotX"); azurePivotX.setAccessible(true);
            azurePivotY = bone.getMethod("getPivotY"); azurePivotY.setAccessible(true);
            azurePivotZ = bone.getMethod("getPivotZ"); azurePivotZ.setAccessible(true);
            azureRotX   = bone.getMethod("getRotX");   azureRotX.setAccessible(true);
            azureRotY   = bone.getMethod("getRotY");   azureRotY.setAccessible(true);
            azureRotZ   = bone.getMethod("getRotZ");   azureRotZ.setAccessible(true);
            azureParent = bone.getMethod("getParent"); azureParent.setAccessible(true);
            try { azureGetCubes = bone.getMethod("getCubes"); azureGetCubes.setAccessible(true); } catch (NoSuchMethodException ignored) {}
            try {
                azurePosX = bone.getMethod("getPosX"); azurePosX.setAccessible(true);
                azurePosY = bone.getMethod("getPosY"); azurePosY.setAccessible(true);
                azurePosZ = bone.getMethod("getPosZ"); azurePosZ.setAccessible(true);
            } catch (NoSuchMethodException ignored) {}
            azureReflectOk = true;
        } catch (Exception ignored) {}
        return azureReflectOk;
    }


    private static float p(Method m, Object bone) throws Exception { return (float) m.invoke(bone); }

    private static float a(@Nullable Method m, Object bone)
    {
        if (m == null) return 0f;
        try { return (float) m.invoke(bone); } catch (Exception e) { return 0f; }
    }

    @Nullable
    private static Method findInHierarchy(Class<?> cls, String name, Class<?>... params)
    {
        while (cls != null) {
            try { return cls.getMethod(name, params); } catch (NoSuchMethodException ignored) {}
            cls = cls.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static Method findMethodByName(Class<?> cls, String name)
    {
        while (cls != null) {
            for (Method m : cls.getDeclaredMethods()) {
                if (m.getName().equals(name)) return m;
            }
            cls = cls.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static Field findFieldInHierarchy(Class<?> cls, String name)
    {
        while (cls != null) {
            try { return cls.getDeclaredField(name); } catch (NoSuchFieldException ignored) {}
            cls = cls.getSuperclass();
        }
        return null;
    }
}
