package me.guivnf.mods.hats.client.render;

import com.mojang.blaze3d.vertex.PoseStack;
import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.client.cache.ClientHatCache;
import me.guivnf.mods.hats.client.compat.EpicFightCompat;
import me.guivnf.mods.hats.common.hat.placement.HatPlacementInfo;
import me.guivnf.mods.hats.common.hat.placement.HatPlacementRegistry;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.mixin.accessor.AgeableHierarchicalModelAccessor;
import me.guivnf.mods.hats.mixin.accessor.AgeableListModelAccessor;
import me.guivnf.mods.hats.mixin.accessor.ModelPartAccessor;
import net.minecraft.client.model.AgeableHierarchicalModel;
import net.minecraft.client.model.AgeableListModel;
import net.minecraft.client.model.EntityModel;
import net.minecraft.client.model.HeadedModel;
import net.minecraft.client.model.HierarchicalModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.LivingEntity;

import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

public class LayerHat<T extends LivingEntity, M extends EntityModel<T>>
        extends RenderLayer<T, M>
{
    private static final Logger LOGGER = LoggerFactory.getLogger("HatsDebug");

    private static final Set<String> LOGGED_TYPES = new HashSet<>();

    private static final List<String> HEAD_PART_NAMES = Arrays.asList(
        "head", "Head", "hat", "Hat", "helmet", "Helmet", "headWear", "headwear"
    );

    private static final float PLAYER_HEAD_UNITS = 8.0f;

    private static final float Z_FIGHT_INFLATE = 1.005f;
    private static final float HEAD_HALF = 4.0f / 16.0f;

    public static final ThreadLocal<Boolean> HAT_RENDERED_THIS_ENTITY = ThreadLocal.withInitial(() -> false);

    public static final ThreadLocal<Boolean> RENDERING_HAT_GUI_PREVIEW = ThreadLocal.withInitial(() -> false);

    private static int rendersThisFrame = 0;

    public static void resetFrameCounter()
    {
        rendersThisFrame = 0;
    }

    private static final Map<Class<?>, float[]> HEAD_OFFSET_CACHE = new WeakHashMap<>();

    public LayerHat(RenderLayerParent<T, M> renderer)
    {
        super(renderer);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffers, int light, T entity,
            float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
            float netHeadYaw, float headPitch)
    {
        if (EpicFightCompat.PRESENT && EpicFightCompat.isEpicFightEntity(entity)) return;

        HAT_RENDERED_THIS_ENTITY.set(true);

        if (entity.isInvisible() && !HatsMod.getConfig().renderOnInvisible) return;

        HatPart hat = ClientHatCache.getEntityHat(entity.getUUID());
        if (hat == null || !hat.isShowing) return;

        if (!RENDERING_HAT_GUI_PREVIEW.get()
                && rendersThisFrame >= HatsMod.getConfig().maxHatRendersPerFrame) return;
        rendersThisFrame++;

        HatPlacementInfo placement = HatPlacementRegistry.get(entity);

        String entityKey = entity.getClass().getName();
        boolean firstLog = LOGGED_TYPES.add(entityKey);

        // if (firstLog) {
        //     M model = getParentModel();
        //     LOGGER.info("[Hats] LayerHat running for entity={} model={} netHeadYaw={} headPitch={}",
        //         entity.getClass().getSimpleName(),
        //         model.getClass().getName(),
        //         netHeadYaw, headPitch);
        // }

        poseStack.pushPose();

        float[] headData = tryPositionAtHead(poseStack, entity, firstLog);

        float geometryScale = headData != null ? headData[0] : 1.0f;
        float finalScale = placement.scale * geometryScale;

        if (headData == null) {
            poseStack.translate(0.0, entity.getBbHeight() + 0.1, 0.0);
        }

        poseStack.translate(0.0, HEAD_HALF, 0.0);
        poseStack.scale(Z_FIGHT_INFLATE, Z_FIGHT_INFLATE, Z_FIGHT_INFLATE);
        poseStack.translate(0.0, -HEAD_HALF, 0.0);
        HatRenderer.render(poseStack, buffers, light, overlay(entity), hat,
            placement.offsetX, placement.offsetY, placement.offsetZ,
            placement.rotX, placement.rotY, placement.rotZ, finalScale);

        poseStack.popPose();
    }


    @Nullable
    private float[] tryPositionAtHead(PoseStack poseStack, T entity, boolean doLog)
    {
        M model = getParentModel();

        if (model instanceof HeadedModel headed) {
            applyBabyTransformsIfNeeded(poseStack, model);
            ModelPart head = headed.getHead();
            // if (doLog) LOGGER.info("[Hats]  -> HeadedModel path. head x={} y={} z={} xRot={} yRot={} zRot={}",
            //     head.x, head.y, head.z, head.xRot, head.yRot, head.zRot);
            head.translateAndRotate(poseStack);
            return new float[]{ applyGeometryOffset(poseStack, head) };
        }

        if (model instanceof HierarchicalModel<?> hierarchical) {
            for (String name : HEAD_PART_NAMES) {
                java.util.Optional<ModelPart> found = hierarchical.getAnyDescendantWithName(name);
                if (found.isPresent()) {
                    applyAgeableHierarchicalBabyTransforms(poseStack, model);
                    ModelPart root = hierarchical.root();
                    List<ModelPart> chain = buildChain(root, found.get());
                    // if (doLog) {
                    //     LOGGER.info("[Hats]  -> HierarchicalModel path, bone name='{}', chain length={}",
                    //         name, chain != null ? chain.size() : 1);
                    //     if (chain != null) {
                    //         for (int i = 0; i < chain.size(); i++) {
                    //             ModelPart b = chain.get(i);
                    //             LOGGER.info("[Hats]     chain[{}]: x={} y={} z={} xRot={} yRot={} zRot={}",
                    //                 i, b.x, b.y, b.z, b.xRot, b.yRot, b.zRot);
                    //         }
                    //     } else {
                    //         ModelPart h = found.get();
                    //         LOGGER.info("[Hats]     (no chain) head: x={} y={} z={} xRot={} yRot={} zRot={}",
                    //             h.x, h.y, h.z, h.xRot, h.yRot, h.zRot);
                    //     }
                    //     LOGGER.info("[Hats]     root: x={} y={} z={} xRot={} yRot={} zRot={}",
                    //         root.x, root.y, root.z, root.xRot, root.yRot, root.zRot);
                    // }
                    if (chain != null) {
                        for (ModelPart bone : chain) bone.translateAndRotate(poseStack);
                    } else {
                        found.get().translateAndRotate(poseStack);
                    }
                    return new float[]{ applyGeometryOffset(poseStack, found.get()) };
                }
            }
            // if (doLog) LOGGER.info("[Hats]  -> HierarchicalModel fallback (no head bone found by name)");
            applyAgeableHierarchicalBabyTransforms(poseStack, model);
            ModelPart root = hierarchical.root();
            ModelPart largest = largestChildWithCubes(root);
            if (largest != null) {
                root.translateAndRotate(poseStack);
                largest.translateAndRotate(poseStack);
                return new float[]{ applyGeometryOffset(poseStack, largest) };
            }
        }

        if (model instanceof AgeableListModel<?>) {
            AgeableListModelAccessor acc = (AgeableListModelAccessor)(Object) model;
            Iterable<ModelPart> parts = acc.hats$headParts();
            if (parts != null) {
                java.util.Iterator<ModelPart> it = parts.iterator();
                if (it.hasNext()) {
                    ModelPart head = it.next();
                    // if (doLog) LOGGER.info("[Hats]  -> AgeableListModel headParts path. head x={} y={} z={} xRot={} yRot={} zRot={}",
                    //     head.x, head.y, head.z, head.xRot, head.yRot, head.zRot);
                    applyBabyTransformsIfNeeded(poseStack, model);
                    head.translateAndRotate(poseStack);
                    return new float[]{ applyGeometryOffset(poseStack, head) };
                }
            }
        }

        ModelPart root = getRootPart(model);
        if (root != null) {
            List<ModelPart> chain = buildChainByName(root, HEAD_PART_NAMES);
            if (chain != null && !chain.isEmpty()) {
                // if (doLog) LOGGER.info("[Hats]  -> DFS root path, chain length={}", chain.size());
                applyBabyTransformsIfNeeded(poseStack, model);
                for (ModelPart bone : chain) bone.translateAndRotate(poseStack);
                return new float[]{ applyGeometryOffset(poseStack, chain.get(chain.size() - 1)) };
            }
        }

        if (model instanceof AgeableListModel<?>) {
            AgeableListModelAccessor acc = (AgeableListModelAccessor)(Object) model;
            Iterable<ModelPart> bParts = acc.hats$bodyParts();
            if (bParts != null) {
                for (ModelPart bodyRoot : bParts) {
                    List<ModelPart> chain = buildChainByName(bodyRoot, HEAD_PART_NAMES);
                    if (chain != null && !chain.isEmpty()) {
                        // if (doLog) LOGGER.info("[Hats]  -> AgeableListModel bodyParts DFS path");
                        applyBabyBodyTransformsIfNeeded(poseStack, model);
                        for (ModelPart bone : chain) bone.translateAndRotate(poseStack);
                        return new float[]{ applyGeometryOffset(poseStack, chain.get(chain.size() - 1)) };
                    }
                }
            }
        }

        ModelPart headByField = findHeadByFieldReflection(model);
        if (headByField != null) {
            // if (doLog) LOGGER.info("[Hats]  -> Field-reflection path. head x={} y={} z={} xRot={} yRot={} zRot={}",
            //     headByField.x, headByField.y, headByField.z, headByField.xRot, headByField.yRot, headByField.zRot);
            applyBabyTransformsIfNeeded(poseStack, model);
            headByField.translateAndRotate(poseStack);
            return new float[]{ applyGeometryOffset(poseStack, headByField) };
        }

        if (model instanceof AgeableListModel<?>) {
            AgeableListModelAccessor acc = (AgeableListModelAccessor)(Object) model;
            Iterable<ModelPart> bParts = acc.hats$bodyParts();
            if (bParts != null) {
                ModelPart bestRoot = null;
                float bestVol = 0f;
                for (ModelPart bp : bParts) {
                    ModelPart largest = largestChildWithCubes(bp);
                    if (largest == null) largest = bp;
                    float vol = totalVolume(largest);
                    if (vol > bestVol) { bestVol = vol; bestRoot = bp; }
                }
                if (bestRoot != null) {
                    // if (doLog) LOGGER.info("[Hats]  -> AgeableListModel body-as-head fallback");
                    applyBabyBodyTransformsIfNeeded(poseStack, model);
                    bestRoot.translateAndRotate(poseStack);
                    ModelPart bodyPart = largestChildWithCubes(bestRoot);
                    if (bodyPart != null) {
                        bodyPart.translateAndRotate(poseStack);
                        return new float[]{ applyGeometryOffset(poseStack, bodyPart) };
                    }
                    return new float[]{ applyGeometryOffset(poseStack, bestRoot) };
                }
            }
        }

        // if (doLog) LOGGER.info("[Hats]  -> All paths exhausted, returning null (bb-top fallback)");
        return null;
    }


    private void applyBabyTransformsIfNeeded(PoseStack poseStack, M model)
    {
        if (!(model instanceof AgeableListModel<?>)) return;
        if (!model.young) return;

        AgeableListModelAccessor acc = (AgeableListModelAccessor)(Object) model;
        boolean scaleHead     = acc.hats$getScaleHead();
        float babyHeadScale   = acc.hats$getBabyHeadScale();
        float babyYHeadOffset = acc.hats$getBabyYHeadOffset();
        float babyZHeadOffset = acc.hats$getBabyZHeadOffset();

        if (scaleHead) {
            float s = 1.5f / babyHeadScale;
            poseStack.scale(s, s, s);
        }
        poseStack.translate(0.0, babyYHeadOffset / 16.0, babyZHeadOffset / 16.0);
    }

    private void applyBabyBodyTransformsIfNeeded(PoseStack poseStack, M model)
    {
        if (!(model instanceof AgeableListModel<?>)) return;
        if (!model.young) return;

        AgeableListModelAccessor acc = (AgeableListModelAccessor)(Object) model;
        float babyBodyScale = acc.hats$getBabyBodyScale();
        float bodyYOffset   = acc.hats$getBodyYOffset();

        if (babyBodyScale != 0f && babyBodyScale != 1f) {
            float s = 1f / babyBodyScale;
            poseStack.scale(s, s, s);
        }
        poseStack.translate(0.0, bodyYOffset / 16.0, 0.0);
    }

    private void applyAgeableHierarchicalBabyTransforms(PoseStack poseStack, M model)
    {
        if (!(model instanceof AgeableHierarchicalModel<?>)) return;
        if (!model.young) return;

        AgeableHierarchicalModelAccessor acc = (AgeableHierarchicalModelAccessor)(Object) model;
        float scale = acc.hats$getYoungScaleFactor();
        float yOff  = acc.hats$getBodyYOffset();
        if (scale != 1f && scale != 0f) poseStack.scale(scale, scale, scale);
        if (yOff  != 0f) poseStack.translate(0.0, yOff / 16.0, 0.0);
    }


    private float applyGeometryOffset(PoseStack poseStack, ModelPart head)
    {
        float[] off = HEAD_OFFSET_CACHE.computeIfAbsent(
            getParentModel().getClass(), k -> computeHeadOffsets(head));
        poseStack.translate(
            Float.isNaN(off[0]) ? 0f        : off[0],
            Float.isNaN(off[1]) ? -8f / 16f : off[1],
            Float.isNaN(off[2]) ? 0f        : off[2]);
        return Float.isNaN(off[3]) ? 1f : off[3];
    }

    private float[] computeHeadOffsets(ModelPart head)
    {
        float[] nan = { Float.NaN, Float.NaN, Float.NaN, Float.NaN };

        ModelPartAccessor headAcc = (ModelPartAccessor)(Object) head;
        List<ModelPart.Cube> cubes = headAcc.hats$getCubes();
        if (!cubes.isEmpty()) {
            return offsetsFromCubes(cubes, 0f, 0f, 0f);
        }

        Map<String, ModelPart> children = headAcc.hats$getChildren();
        if (children.isEmpty()) return nan;

        ModelPart bestChild = null;
        float bestVol = 0f;
        for (ModelPart child : children.values()) {
            float vol = totalVolume(child);
            if (vol > bestVol) { bestVol = vol; bestChild = child; }
        }

        if (bestChild != null) {
            ModelPartAccessor childAcc = (ModelPartAccessor)(Object) bestChild;
            return offsetsFromCubes(childAcc.hats$getCubes(),
                bestChild.x / 16f, bestChild.y / 16f, bestChild.z / 16f);
        }

        return nan;
    }

    private static final float MIN_HEAD_CUBE_VOL = 32f;

    private static float[] offsetsFromCubes(List<ModelPart.Cube> cubes, float px, float py, float pz)
    {
        ModelPart.Cube primary = null;
        for (ModelPart.Cube c : cubes) {
            if (volume(c) < MIN_HEAD_CUBE_VOL) continue;
            if (primary == null || c.minY < primary.minY
                    || (c.minY == primary.minY && volume(c) > volume(primary))) {
                primary = c;
            }
        }
        if (primary != null) {
            float headWidthUnits = primary.maxX - primary.minX;
            float scale = Mth.clamp(headWidthUnits / PLAYER_HEAD_UNITS, 0.2f, 3.0f);
            return new float[]{
                (primary.minX + primary.maxX) / 2f / 16f + px,
                 primary.minY                       / 16f + py,
                (primary.minZ + primary.maxZ) / 2f / 16f + pz,
                scale
            };
        }

        if (cubes.isEmpty()) return new float[]{ Float.NaN, Float.NaN, Float.NaN, Float.NaN };

        float topY = Float.MAX_VALUE;
        for (ModelPart.Cube c : cubes) topY = Math.min(topY, c.minY);

        float sumX = 0, sumZ = 0, minX = Float.MAX_VALUE, maxX = -Float.MAX_VALUE;
        int count = 0;
        for (ModelPart.Cube c : cubes) {
            if (c.minY > topY + 2f) continue;
            sumX += (c.minX + c.maxX) / 2f;
            sumZ += (c.minZ + c.maxZ) / 2f;
            minX = Math.min(minX, c.minX);
            maxX = Math.max(maxX, c.maxX);
            count++;
        }
        float cx = count > 0 ? sumX / count : 0f;
        float cz = count > 0 ? sumZ / count : 0f;
        float span = (maxX > minX) ? (maxX - minX) : PLAYER_HEAD_UNITS;
        float scale = Mth.clamp(span / PLAYER_HEAD_UNITS, 0.2f, 3.0f);
        return new float[]{
            cx / 16f + px,
            topY / 16f + py,
            cz / 16f + pz,
            scale
        };
    }


    @Nullable
    private List<ModelPart> buildChain(ModelPart root, ModelPart target)
    {
        List<ModelPart> chain = new ArrayList<>();
        if (buildChainInto(root, target, chain)) return chain;
        return null;
    }

    private boolean buildChainInto(ModelPart current, ModelPart target, List<ModelPart> chain)
    {
        chain.add(current);
        if (current == target) return true;
        for (ModelPart child : ((ModelPartAccessor)(Object) current).hats$getChildren().values()) {
            if (buildChainInto(child, target, chain)) return true;
        }
        chain.remove(chain.size() - 1);
        return false;
    }

    @Nullable
    private List<ModelPart> buildChainByName(ModelPart root, List<String> names)
    {
        return buildChainByNameInto(root, names, new ArrayList<>());
    }

    @Nullable
    private List<ModelPart> buildChainByNameInto(ModelPart current, List<String> names, List<ModelPart> chain)
    {
        chain.add(current);
        for (Map.Entry<String, ModelPart> entry :
                ((ModelPartAccessor)(Object) current).hats$getChildren().entrySet()) {
            if (names.contains(entry.getKey())) {
                chain.add(entry.getValue());
                return chain;
            }
            List<ModelPart> found = buildChainByNameInto(entry.getValue(), names, chain);
            if (found != null) return found;
        }
        chain.remove(chain.size() - 1);
        return null;
    }

    @Nullable
    private ModelPart getRootPart(M model)
    {
        if (model instanceof HierarchicalModel<?> h) return h.root();
        try {
            java.lang.reflect.Method rootMethod = model.getClass().getMethod("root");
            return (ModelPart) rootMethod.invoke(model);
        } catch (Exception ignored) {}
        Class<?> cls = model.getClass();
        while (cls != null && cls != Object.class) {
            try {
                Field f = cls.getDeclaredField("root");
                if (ModelPart.class.isAssignableFrom(f.getType())) {
                    f.setAccessible(true);
                    return (ModelPart) f.get(model);
                }
            } catch (Exception ignored2) {}
            cls = cls.getSuperclass();
        }
        return null;
    }

    @Nullable
    private ModelPart largestChildWithCubes(ModelPart part)
    {
        ModelPart best = null;
        float bestVol = 0f;
        for (ModelPart child : ((ModelPartAccessor)(Object) part).hats$getChildren().values()) {
            float vol = totalVolume(child);
            if (vol > bestVol) { bestVol = vol; best = child; }
        }
        return best;
    }

    @Nullable
    private ModelPart findHeadByFieldReflection(M model)
    {
        Class<?> cls = model.getClass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                if (!ModelPart.class.isAssignableFrom(field.getType())) continue;
                if (!HEAD_PART_NAMES.contains(field.getName())) continue;
                try {
                    field.setAccessible(true);
                    return (ModelPart) field.get(model);
                } catch (Exception ignored) {}
            }
            cls = cls.getSuperclass();
        }

        ModelPart best = null;
        float bestMinY = Float.MAX_VALUE;

        cls = model.getClass();
        while (cls != null && cls != Object.class) {
            for (Field field : cls.getDeclaredFields()) {
                if (!ModelPart.class.isAssignableFrom(field.getType())) continue;
                try {
                    field.setAccessible(true);
                    ModelPart part = (ModelPart) field.get(model);
                    if (part == null) continue;
                    float minY = highestSignificantCubeMinY(part);
                    if (minY < bestMinY) {
                        bestMinY = minY;
                        best = part;
                    }
                } catch (Exception ignored) {}
            }
            cls = cls.getSuperclass();
        }
        return best;
    }

    private float highestSignificantCubeMinY(ModelPart part)
    {
        float minY = Float.MAX_VALUE;
        for (ModelPart.Cube cube : ((ModelPartAccessor)(Object) part).hats$getCubes()) {
            if (volume(cube) >= MIN_HEAD_CUBE_VOL) {
                minY = Math.min(minY, cube.minY);
            }
        }
        return minY;
    }


    private static float totalVolume(ModelPart part)
    {
        float vol = 0f;
        for (ModelPart.Cube c : ((ModelPartAccessor)(Object) part).hats$getCubes()) vol += volume(c);
        return vol;
    }

    private static float volume(ModelPart.Cube c)
    {
        return (c.maxX - c.minX) * (c.maxY - c.minY) * (c.maxZ - c.minZ);
    }

    private int overlay(T entity)
    {
        return net.minecraft.client.renderer.texture.OverlayTexture.pack(0, 10);
    }
}
