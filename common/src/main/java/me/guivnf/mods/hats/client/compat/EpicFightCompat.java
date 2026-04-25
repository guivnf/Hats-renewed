package me.guivnf.mods.hats.client.compat;

import dev.architectury.platform.Platform;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Optional;

public final class EpicFightCompat
{

    private static final String[] CAP_CLASS_NAMES = {
        "yesman.epicfight.api.client.forgeevent.EpicFightCapabilities",
        "yesman.epicfight.world.capabilities.EpicFightCapabilities",
        "yesman.epicfight.main.EpicFightCapabilities",
        "yesman.epicfight.api.utils.EpicFightCapabilities"
    };
    private static final String PATCH_CLASS    = "yesman.epicfight.world.capabilities.entitypatch.LivingEntityPatch";
    private static final String ARMATURE_CLASS = "yesman.epicfight.api.model.Armature";

    private static final String[] HEAD_JOINT_NAMES = { "Head", "head", "HEAD", "head_pivot" };


    public static final boolean PRESENT = Platform.isModLoaded("epicfight");

    public static volatile boolean GUI_PREVIEW_ACTIVE = false;

    // --- Cloth simulation save/restore ---
    private static volatile boolean clothReflDone;
    private static volatile boolean clothReflOk;
    private static Method  clothGetSimulator;
    private static Method  clothGetRunningObject;
    private static Object  clothPlayerCloakKey;
    private static Field   clothParticlesField;
    private static Field   clothParticlePosField;
    private static Field   clothParticleVelField;
    private static Field   clothVec3fX;
    private static Field   clothVec3fY;
    private static Field   clothVec3fZ;


    private static volatile boolean reflectDone, reflectOk;

    private static Class<?> patchClass;
    private static Method getEntityPatch;
    private static Method getArmature;
    private static Method getAnimator;
    private static Method getPose;
    private static Method searchJointByName;
    private static Method getBoundTransformFor;


    private static volatile boolean matFieldsDone;
    private static Field fM00, fM01, fM02;
    private static Field fM10, fM11, fM12;
    private static Field fM20, fM21, fM22;
    private static Field fM30, fM31, fM32;


    private static volatile boolean modelMatrixMethodDone;
    private static Method getModelMatrixMethod;


    private static volatile boolean poseMatFieldsDone;
    private static Field poseMatricesF;

    private static volatile boolean jointIdMethodDone;
    private static Method getJointIdMethod;

    private static final Logger LOGGER = LoggerFactory.getLogger("HatsDebug");
    private static volatile boolean loggedOnce = false;

    private EpicFightCompat() {}


    public static boolean isEpicFightEntity(LivingEntity entity)
    {
        if (!PRESENT || !ensureReflect()) return false;
        try {
            Object patch = getPatch(entity);
            if (patch == null) return false;
            Method isVanilla = findMethod(patch.getClass(), "isVanillaMode");
            if (isVanilla != null && (boolean) isVanilla.invoke(patch)) return false;
            return true;
        } catch (Exception e) { return false; }
    }

    @Nullable
    public static Matrix4f getHeadTransform(LivingEntity entity, float partialTick)
    {
        if (!PRESENT || !ensureReflect()) {
            // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] getHeadTransform: present={} reflectOk={}", PRESENT, reflectOk); }
            return null;
        }
        try {
            Object patch = getPatch(entity);
            if (patch == null) {
                // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] getPatch returned null for entity={}", entity.getClass().getSimpleName()); }
                return null;
            }

            Object armature = getArmature.invoke(patch);
            if (armature == null) {
                // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] getArmature returned null"); }
                return null;
            }

            String foundJointName = null;
            Object headJoint = null;
            for (String name : HEAD_JOINT_NAMES) {
                headJoint = searchJointByName.invoke(armature, name);
                if (headJoint != null) { foundJointName = name; break; }
            }
            if (headJoint == null) {
                // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] no head joint found, tried names: {}", java.util.Arrays.toString(HEAD_JOINT_NAMES)); }
                return null;
            }

            Object mat = readFromPoseMatrices(armature, headJoint);

            if (mat == null) {
                Object animator = getAnimator.invoke(patch);
                if (animator == null) {
                    // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] poseMatrices unavailable and getAnimator returned null"); }
                    return null;
                }
                ensureGetPose(animator);
                if (getPose == null) {
                    // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] getPose method not found on {}", animator.getClass().getName()); }
                    return null;
                }
                Object pose = getPose.invoke(animator, partialTick);
                if (pose == null) {
                    // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] getPose returned null"); }
                    return null;
                }
                mat = getBoundTransformFor.invoke(armature, pose, headJoint);
                if (mat == null) {
                    // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] getBoundTransformFor returned null for joint={}", foundJointName); }
                    return null;
                }
            }

            ensureMatFields(mat);
            if (fM30 == null) {
                // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] fM30 field not found on {}", mat.getClass().getName()); }
                return null;
            }

            float m00 = fM00 != null ? fM00.getFloat(mat) : 1f;
            float m01 = fM01 != null ? fM01.getFloat(mat) : 0f;
            float m02 = fM02 != null ? fM02.getFloat(mat) : 0f;
            float m10 = fM10 != null ? fM10.getFloat(mat) : 0f;
            float m11 = fM11 != null ? fM11.getFloat(mat) : 1f;
            float m12 = fM12 != null ? fM12.getFloat(mat) : 0f;
            float m20 = fM20 != null ? fM20.getFloat(mat) : 0f;
            float m21 = fM21 != null ? fM21.getFloat(mat) : 0f;
            float m22 = fM22 != null ? fM22.getFloat(mat) : 1f;
            float tx  = fM30.getFloat(mat);
            float ty  = fM31 != null ? fM31.getFloat(mat) : 0f;
            float tz  = fM32 != null ? fM32.getFloat(mat) : 0f;

            // if (!loggedOnce) {
            //     loggedOnce = true;
            //     LOGGER.info("[Hats-EF] joint={} matClass={} tx={} ty={} tz={}", foundJointName, mat.getClass().getSimpleName(), tx, ty, tz);
            //     LOGGER.info("[Hats-EF] rot row0: [{}, {}, {}]", m00, m01, m02);
            //     LOGGER.info("[Hats-EF] rot row1: [{}, {}, {}]", m10, m11, m12);
            //     LOGGER.info("[Hats-EF] rot row2: [{}, {}, {}]", m20, m21, m22);
            // }

            return new Matrix4f(
                m00, m01, m02, 0f,
                m10, m11, m12, 0f,
                m20, m21, m22, 0f,
                tx,  ty,  tz,  1f
            );
        } catch (Exception e) {
            // if (!loggedOnce) { loggedOnce = true; LOGGER.info("[Hats-EF] exception in getHeadTransform: {}", e.toString()); }
            return null;
        }
    }

    @Nullable
    private static Object readFromPoseMatrices(Object armature, Object headJoint)
    {
        try {
            ensurePoseMatFields(armature);
            if (poseMatricesF == null) return null;
            ensureJointIdMethod(headJoint);
            if (getJointIdMethod == null) return null;
            Object[] arr = (Object[]) poseMatricesF.get(armature);
            if (arr == null) return null;
            int id = (int) getJointIdMethod.invoke(headJoint);
            if (id < 0 || id >= arr.length) return null;
            return arr[id];
        } catch (Exception e) { return null; }
    }

    public static boolean applyEFModelMatrix(PoseStack poseStack, LivingEntity entity, float partialTick)
    {
        if (!PRESENT || !ensureReflect()) return false;
        try {
            Object patch = getPatch(entity);
            if (patch == null) return false;
            ensureGetModelMatrix(patch);
            if (getModelMatrixMethod == null) return false;
            Object mat = getModelMatrixMethod.invoke(patch, partialTick);
            if (mat == null) return false;
            ensureMatFields(mat);
            if (fM00 == null) return false;
            float m00 = fM00.getFloat(mat), m01 = fM01.getFloat(mat), m02 = fM02.getFloat(mat);
            float m10 = fM10.getFloat(mat), m11 = fM11.getFloat(mat), m12 = fM12.getFloat(mat);
            float m20 = fM20.getFloat(mat), m21 = fM21.getFloat(mat), m22 = fM22.getFloat(mat);
            float tx  = fM30.getFloat(mat);
            float ty  = fM31 != null ? fM31.getFloat(mat) : 0f;
            float tz  = fM32 != null ? fM32.getFloat(mat) : 0f;
            org.joml.Matrix4f joml = new org.joml.Matrix4f(
                m00, m01, m02, 0f,
                m10, m11, m12, 0f,
                m20, m21, m22, 0f,
                tx,  ty,  tz,  1f);
            poseStack.last().pose().mul(joml);
            poseStack.last().normal().mul(new org.joml.Matrix3f(joml));
            return true;
        } catch (Exception e) { return false; }
    }


    // -------------------------------------------------------------------------
    // Cloth state save / restore (prevents cape tangling during GUI renders)
    // -------------------------------------------------------------------------

    private static synchronized boolean ensureClothReflect()
    {
        if (clothReflDone) return clothReflOk;
        clothReflDone = true;
        if (!PRESENT || !ensureReflect()) return false;
        try {
            Class<?> clothSimCls = Class.forName("yesman.epicfight.api.client.physics.cloth.ClothSimulator");
            clothPlayerCloakKey = clothSimCls.getField("PLAYER_CLOAK").get(null);
            clothGetRunningObject = findMethodByNameAndParamCount(clothSimCls, "getRunningObject", 1);
            if (clothGetRunningObject == null) return false;

            Class<?> clothObjCls = Class.forName("yesman.epicfight.api.client.physics.cloth.ClothSimulator$ClothObject");
            clothParticlesField = findField(clothObjCls, "particles");
            if (clothParticlesField == null) return false;

            Class<?> particleCls = null;
            for (Class<?> inner : clothObjCls.getDeclaredClasses()) {
                if ("Particle".equals(inner.getSimpleName())) { particleCls = inner; break; }
            }
            if (particleCls == null) return false;

            clothParticlePosField = findField(particleCls, "position");
            clothParticleVelField = findField(particleCls, "velocity");
            if (clothParticlePosField == null || clothParticleVelField == null) return false;

            Class<?> vec3fCls = Class.forName("yesman.epicfight.api.utils.math.Vec3f");
            clothVec3fX = findField(vec3fCls, "x");
            clothVec3fY = findField(vec3fCls, "y");
            clothVec3fZ = findField(vec3fCls, "z");
            if (clothVec3fX == null || clothVec3fY == null || clothVec3fZ == null) return false;

            clothReflOk = true;
        } catch (Exception ignored) {}
        return clothReflOk;
    }

    @Nullable
    private static Object getClothObject(LivingEntity entity)
    {
        try {
            Object patch = getPatch(entity);
            if (patch == null) return null;
            if (clothGetSimulator == null) {
                clothGetSimulator = findMethod(patch.getClass(), "getClothSimulator");
                if (clothGetSimulator == null) return null;
            }
            Object clothSim = clothGetSimulator.invoke(patch);
            if (clothSim == null) return null;
            Object result = clothGetRunningObject.invoke(clothSim, clothPlayerCloakKey);
            if (result instanceof Optional<?> opt) return opt.orElse(null);
            return result;
        } catch (Exception e) { return null; }
    }

    /**
     * Captures particle positions and velocities for the entity's EF cape.
     * Returns a float[] snapshot, or null if EF/cape is unavailable.
     */
    @Nullable
    public static float[] captureClothState(LivingEntity entity)
    {
        if (!ensureClothReflect()) return null;
        try {
            Object clothObj = getClothObject(entity);
            if (clothObj == null) return null;
            @SuppressWarnings("unchecked")
            java.util.Map<?, Object> particles = (java.util.Map<?, Object>) clothParticlesField.get(clothObj);
            float[] snap = new float[particles.size() * 6];
            int i = 0;
            for (Object p : particles.values()) {
                Object pos = clothParticlePosField.get(p);
                Object vel = clothParticleVelField.get(p);
                snap[i++] = clothVec3fX.getFloat(pos);
                snap[i++] = clothVec3fY.getFloat(pos);
                snap[i++] = clothVec3fZ.getFloat(pos);
                snap[i++] = clothVec3fX.getFloat(vel);
                snap[i++] = clothVec3fY.getFloat(vel);
                snap[i++] = clothVec3fZ.getFloat(vel);
            }
            return snap;
        } catch (Exception e) { return null; }
    }

    /**
     * Restores particle positions and velocities from a snapshot produced by
     * {@link #captureClothState}.  No-op if snap is null or EF is unavailable.
     */
    public static void restoreClothState(LivingEntity entity, float[] snap)
    {
        if (snap == null || !clothReflOk) return;
        try {
            Object clothObj = getClothObject(entity);
            if (clothObj == null) return;
            @SuppressWarnings("unchecked")
            java.util.Map<?, Object> particles = (java.util.Map<?, Object>) clothParticlesField.get(clothObj);
            if (particles.size() * 6 != snap.length) return;
            int i = 0;
            for (Object p : particles.values()) {
                Object pos = clothParticlePosField.get(p);
                Object vel = clothParticleVelField.get(p);
                clothVec3fX.setFloat(pos, snap[i++]);
                clothVec3fY.setFloat(pos, snap[i++]);
                clothVec3fZ.setFloat(pos, snap[i++]);
                clothVec3fX.setFloat(vel, snap[i++]);
                clothVec3fY.setFloat(vel, snap[i++]);
                clothVec3fZ.setFloat(vel, snap[i++]);
            }
        } catch (Exception ignored) {}
    }


    @Nullable
    private static Object getPatch(LivingEntity entity)
    {
        try {
            Object result = getEntityPatch.invoke(null, entity, patchClass);
            if (result instanceof Optional<?> opt) return opt.orElse(null);
            return result;
        } catch (Exception e) { return null; }
    }


    private static synchronized boolean ensureReflect()
    {
        if (reflectDone) return reflectOk;
        reflectDone = true;
        try {
            patchClass = Class.forName(PATCH_CLASS);

            for (String capName : CAP_CLASS_NAMES) {
                try {
                    Class<?> capClass = Class.forName(capName);
                    for (Method m : capClass.getMethods()) {
                        if ("getEntityPatch".equals(m.getName()) && Modifier.isStatic(m.getModifiers())) {
                            m.setAccessible(true);
                            getEntityPatch = m;
                            break;
                        }
                    }
                    if (getEntityPatch != null) break;
                } catch (ClassNotFoundException ignored) {}
            }
            if (getEntityPatch == null) return reflectOk;

            getArmature = findMethod(patchClass, "getArmature");
            if (getArmature == null) return reflectOk;

            getAnimator = findMethod(patchClass, "getAnimator");
            if (getAnimator == null) return reflectOk;

            Class<?> armatureClass;
            try { armatureClass = Class.forName(ARMATURE_CLASS); }
            catch (ClassNotFoundException e) { return reflectOk; }

            searchJointByName = findMethodWithParams(armatureClass, "searchJointByName", String.class);
            if (searchJointByName == null) return reflectOk;

            getBoundTransformFor = findMethodByNameAndParamCount(armatureClass, "getBoundTransformFor", 2);
            if (getBoundTransformFor == null) return reflectOk;

            reflectOk = true;
        } catch (Exception ignored) {}
        return reflectOk;
    }

    private static synchronized void ensureGetPose(Object animatorInstance)
    {
        if (getPose != null) return;
        getPose = findMethodWithParamType(animatorInstance.getClass(), "getPose", float.class);
        if (getPose == null)
            getPose = findMethodByNameAndParamCount(animatorInstance.getClass(), "getPose", 1);
        if (getPose != null) getPose.setAccessible(true);
    }

    private static synchronized void ensureMatFields(Object openMatInstance)
    {
        if (matFieldsDone) return;
        matFieldsDone = true;
        Class<?> cls = openMatInstance.getClass();
        fM00 = findField(cls, "m00"); fM01 = findField(cls, "m01"); fM02 = findField(cls, "m02");
        fM10 = findField(cls, "m10"); fM11 = findField(cls, "m11"); fM12 = findField(cls, "m12");
        fM20 = findField(cls, "m20"); fM21 = findField(cls, "m21"); fM22 = findField(cls, "m22");
        fM30 = findField(cls, "m30"); fM31 = findField(cls, "m31"); fM32 = findField(cls, "m32");
    }

    private static synchronized void ensureGetModelMatrix(Object patchInstance)
    {
        if (modelMatrixMethodDone) return;
        modelMatrixMethodDone = true;
        getModelMatrixMethod = findMethodWithParamType(patchInstance.getClass(), "getModelMatrix", float.class);
    }

    private static synchronized void ensurePoseMatFields(Object armatureInstance)
    {
        if (poseMatFieldsDone) return;
        poseMatFieldsDone = true;
        poseMatricesF = findField(armatureInstance.getClass(), "poseMatrices");
    }

    private static synchronized void ensureJointIdMethod(Object jointInstance)
    {
        if (jointIdMethodDone) return;
        jointIdMethodDone = true;
        getJointIdMethod = findMethod(jointInstance.getClass(), "getId");
    }


    @Nullable
    private static Method findMethod(Class<?> cls, String name)
    {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == 0) {
                    m.setAccessible(true); return m;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static Method findMethodWithParams(Class<?> cls, String name, Class<?>... params)
    {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try { Method m = c.getDeclaredMethod(name, params); m.setAccessible(true); return m; }
            catch (NoSuchMethodException ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static Method findMethodWithParamType(Class<?> cls, String name, Class<?> paramType)
    {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (!m.getName().equals(name) || m.getParameterCount() != 1) continue;
                Class<?> p = m.getParameterTypes()[0];
                if (p == paramType || p == Float.class) { m.setAccessible(true); return m; }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static Method findMethodByNameAndParamCount(Class<?> cls, String name, int count)
    {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            for (Method m : c.getDeclaredMethods()) {
                if (m.getName().equals(name) && m.getParameterCount() == count) {
                    m.setAccessible(true); return m;
                }
            }
            c = c.getSuperclass();
        }
        return null;
    }

    @Nullable
    private static Field findField(Class<?> cls, String name)
    {
        Class<?> c = cls;
        while (c != null && c != Object.class) {
            try { Field f = c.getDeclaredField(name); f.setAccessible(true); return f; }
            catch (NoSuchFieldException ignored) {}
            c = c.getSuperclass();
        }
        return null;
    }

}
