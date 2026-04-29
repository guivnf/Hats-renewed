package me.guivnf.mods.hats.common.entity;

import me.guivnf.mods.hats.HatsMod;
import me.guivnf.mods.hats.common.hat.HatPart;
import me.guivnf.mods.hats.common.hat.placement.HatPlacementRegistry;
import me.guivnf.mods.hats.common.network.NetworkHelper;
import me.guivnf.mods.hats.common.registry.HatsRegistries;
import me.guivnf.mods.hats.common.world.HatsSavedData;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import org.jetbrains.annotations.Nullable;
import java.util.List;
import java.util.UUID;

public class HatEntity
        extends Entity
{
    private static final EntityDataAccessor<CompoundTag> HAT_DATA =
            SynchedEntityData.defineId(HatEntity.class, EntityDataSerializers.COMPOUND_TAG);

    @Nullable
    private HatPart hatPart;

    @Nullable
    private UUID launcherId;
    private boolean leftLauncher = false;
    private boolean isRogue      = false;

    public int age = 0;

    public float rotX;
    public float rotY;
    public float lastRotX;
    public float lastRotY;
    public float rotFactorX;
    public float rotFactorY;

    public HatEntity(EntityType<HatEntity> type, Level level)
    {
        super(type, level);
        this.noPhysics = false;
        rotFactorX = (random.nextFloat() * 2f - 1f) * 45f;
        rotFactorY = (random.nextFloat() * 2f - 1f) * 45f;
    }

    public static HatEntity launch(Level level, Player shooter, HatPart hat)
    {
        HatEntity entity = new HatEntity(HatsRegistries.HAT_ENTITY.get(), level);
        entity.hatPart   = hat.copy();
        entity.entityData.set(HAT_DATA, hat.save());
        entity.launcherId    = shooter.getUUID();
        entity.leftLauncher  = false;

        Vec3 eye  = shooter.getEyePosition();
        Vec3 look = shooter.getLookAngle();
        entity.setPos(eye.x + look.x * 0.5, eye.y - 0.15, eye.z + look.z * 0.5);
        entity.setDeltaMovement(look.scale(1.2));
        entity.setYRot(shooter.getYRot());
        entity.setXRot(shooter.getXRot());
        return entity;
    }

    @Override
    protected void defineSynchedData(net.minecraft.network.syncher.SynchedEntityData.Builder builder)
    {
        builder.define(HAT_DATA, new CompoundTag());
    }

    @Override
    public void tick()
    {
        lastRotX = rotX;
        lastRotY = rotY;

        super.tick();
        age++;

        if (!level().isClientSide) {
            int lifespan = HatsMod.getConfig().hatEntityLifespan;
            if (age >= lifespan) {
                discard();
                return;
            }
        }

        Vec3 motion = getDeltaMovement();

        if (!level().isClientSide && hatPart != null) {
            if (!leftLauncher && launcherId != null) {
                Entity launcher = null;
                for (Entity e : level().getEntities(this, getBoundingBox().inflate(1.5), e -> e instanceof LivingEntity)) {
                    if (e.getUUID().equals(launcherId)) { launcher = e; break; }
                }
                if (launcher == null) leftLauncher = true;
            }

            if (leftLauncher) {
                AABB searchBox = getBoundingBox().expandTowards(motion).inflate(0.5);
                List<Entity> nearby = level().getEntities(this, searchBox,
                        e -> e instanceof LivingEntity living && canHitEntity(living));

                LivingEntity target = null;
                if (!nearby.isEmpty()) {
                    target = (LivingEntity) nearby.get(0);
                }

                if (target != null) {
                    hitEntity(target);
                    return;
                }
            }
        }

        boolean wasHorizontalCollision = horizontalCollision;
        boolean wasOnGround = onGround();
        move(MoverType.SELF, motion);

        if ((horizontalCollision && !wasHorizontalCollision) || (onGround() && !wasOnGround)) {
            if (!level().isClientSide) {
                isRogue = false;
                leftLauncher = true;
            } else {
                rotFactorX += (random.nextFloat() * 2f - 1f) * 45f;
                rotFactorY += (random.nextFloat() * 2f - 1f) * 45f;
            }
        }

        motion = getDeltaMovement();
        setDeltaMovement(motion.add(0, -0.02, 0));

        motion = getDeltaMovement();
        if (onGround()) {
            setDeltaMovement(motion.multiply(0.8, 0.8, 0.8));
            rotFactorX *= 0.8f;
            rotFactorY *= 0.8f;
        } else {
            setDeltaMovement(motion.multiply(0.98, 0.98, 0.98));
            rotFactorX *= 0.98f;
            rotFactorY *= 0.98f;
        }

        rotX += rotFactorX;
        rotY += rotFactorY;
    }

    private boolean canHitEntity(LivingEntity e)
    {
        if (e.isSpectator()) return false;
        if (!leftLauncher && launcherId != null && e.getUUID().equals(launcherId)) return false;
        return HatPlacementRegistry.get(e) != null;
    }

    private void hitEntity(LivingEntity target)
    {
        HatsSavedData data = HatsSavedData.get(level());
        HatPart incoming  = this.hatPart;

        spawnPickupParticles(target);
        if (HatsRegistries.POOF.isPresent()) {
            level().playSound(null, target.blockPosition(), HatsRegistries.POOF.get(),
                SoundSource.NEUTRAL, 0.6f, 0.85f + random.nextFloat() * 0.3f);
        }

        if (target instanceof ServerPlayer sp) {
            data.addHatToInventory(sp.getUUID(), incoming);
            NetworkHelper.syncInventory(sp, data);
            discard();
        } else {
            HatPart existingHat = data.getEntityHat(target.getUUID());
            incoming.isShowing = true;
            for (HatPart acc : incoming.accessories) acc.isShowing = true;
            data.setEntityHat(target.getUUID(), incoming);
            NetworkHelper.broadcastEntityHat(target);

            if (existingHat != null && !existingHat.name.isEmpty()) {
                isRogue = true;
                setHatPart(existingHat);
                setLastInteracted(target);
                setDeltaMovement(
                        random.nextGaussian() * 0.2,
                        0.2 + random.nextFloat() * 0.2,
                        random.nextGaussian() * 0.2);
            } else {
                discard();
            }
        }
    }

    private void spawnPickupParticles(LivingEntity target)
    {
        if (!(level() instanceof ServerLevel sl)) return;
        double x = target.getX();
        double y = target.getY() + target.getBbHeight() * 0.5;
        double z = target.getZ();
        for (int i = 0; i < 8; i++) {
            sl.sendParticles(ParticleTypes.POOF,
                x + (random.nextDouble() - 0.5) * 0.6,
                y + (random.nextDouble() - 0.5) * 0.6,
                z + (random.nextDouble() - 0.5) * 0.6,
                1, 0, 0, 0, 0.05);
        }
        for (int i = 0; i < 5; i++) {
            sl.sendParticles(ParticleTypes.ELECTRIC_SPARK,
                x + (random.nextDouble() - 0.5) * 0.5,
                y + (random.nextDouble() - 0.5) * 0.5,
                z + (random.nextDouble() - 0.5) * 0.5,
                1, 0, 0, 0, 0.1);
        }
    }

    private void setLastInteracted(LivingEntity entity)
    {
        launcherId   = entity.getUUID();
        leftLauncher = false;
    }

    @Override
    public void onSyncedDataUpdated(EntityDataAccessor<?> key)
    {
        super.onSyncedDataUpdated(key);
        if (HAT_DATA.equals(key) && level().isClientSide) {
            hatPart = null;
        }
    }

    @Nullable
    public HatPart getHatPart()
    {
        if (hatPart == null) {
            CompoundTag tag = entityData.get(HAT_DATA);
            if (!tag.isEmpty()) {
                hatPart = HatPart.load(tag);
            }
        }
        return hatPart;
    }

    public void setHatPart(HatPart part)
    {
        hatPart = part;
        entityData.set(HAT_DATA, part.save());
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand)
    {
        if (level().isClientSide) return InteractionResult.SUCCESS;

        HatPart hat = getHatPart();
        if (hat == null) {
            discard();
            return InteractionResult.CONSUME;
        }

        HatsSavedData data = HatsSavedData.get(player.level());
        data.addHatToInventory(player.getUUID(), hat);
        if (player instanceof ServerPlayer sp) {
            NetworkHelper.syncInventory(sp, data);
        }
        discard();
        return InteractionResult.CONSUME;
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag tag)
    {
        if (tag.contains("hatPart")) {
            hatPart = HatPart.load(tag.getCompound("hatPart"));
            entityData.set(HAT_DATA, tag.getCompound("hatPart"));
        }
        if (tag.hasUUID("launcherId"))  launcherId   = tag.getUUID("launcherId");
        leftLauncher = tag.getBoolean("leftLauncher");
        isRogue      = tag.getBoolean("isRogue");
        age          = tag.getInt("age");
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag tag)
    {
        HatPart hat = getHatPart();
        if (hat != null) tag.put("hatPart", hat.save());
        if (launcherId != null) tag.putUUID("launcherId", launcherId);
        tag.putBoolean("leftLauncher", leftLauncher);
        tag.putBoolean("isRogue", isRogue);
        tag.putInt("age", age);
    }

    @Override
    public boolean isPickable()  { return !isRemoved(); }
    @Override
    public boolean isPushable()  { return false; }
    @Override
    public boolean shouldRenderAtSqrDistance(double distanceSq) { return distanceSq < 4096; }
}
