package net.minecraft.world.entity.projectile;

import it.unimi.dsi.fastutil.doubles.DoubleDoubleImmutablePair;
import java.util.Iterator;
import java.util.List;
import java.util.OptionalInt;
import javax.annotation.Nullable;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.server.level.WorldServer;
import net.minecraft.sounds.SoundCategory;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.FireworkExplosion;
import net.minecraft.world.item.component.Fireworks;
import net.minecraft.world.level.RayTrace;
import net.minecraft.world.level.World;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.MovingObjectPosition;
import net.minecraft.world.phys.MovingObjectPositionBlock;
import net.minecraft.world.phys.MovingObjectPositionEntity;
import net.minecraft.world.phys.Vec3D;

public class EntityFireworks extends IProjectile implements ItemSupplier {

    public static final DataWatcherObject<ItemStack> DATA_ID_FIREWORKS_ITEM = DataWatcher.defineId(EntityFireworks.class, DataWatcherRegistry.ITEM_STACK);
    private static final DataWatcherObject<OptionalInt> DATA_ATTACHED_TO_TARGET = DataWatcher.defineId(EntityFireworks.class, DataWatcherRegistry.OPTIONAL_UNSIGNED_INT);
    public static final DataWatcherObject<Boolean> DATA_SHOT_AT_ANGLE = DataWatcher.defineId(EntityFireworks.class, DataWatcherRegistry.BOOLEAN);
    public int life;
    public int lifetime;
    @Nullable
    public EntityLiving attachedToEntity;

    public EntityFireworks(EntityTypes<? extends EntityFireworks> entitytypes, World world) {
        super(entitytypes, world);
    }

    public EntityFireworks(World world, double d0, double d1, double d2, ItemStack itemstack) {
        super(EntityTypes.FIREWORK_ROCKET, world);
        this.life = 0;
        this.setPos(d0, d1, d2);
        this.entityData.set(EntityFireworks.DATA_ID_FIREWORKS_ITEM, itemstack.copy());
        int i = 1;
        Fireworks fireworks = (Fireworks) itemstack.get(DataComponents.FIREWORKS);

        if (fireworks != null) {
            i += fireworks.flightDuration();
        }

        this.setDeltaMovement(this.random.triangle(0.0D, 0.002297D), 0.05D, this.random.triangle(0.0D, 0.002297D));
        this.lifetime = 10 * i + this.random.nextInt(6) + this.random.nextInt(7);
    }

    public EntityFireworks(World world, @Nullable Entity entity, double d0, double d1, double d2, ItemStack itemstack) {
        this(world, d0, d1, d2, itemstack);
        this.setOwner(entity);
    }

    public EntityFireworks(World world, ItemStack itemstack, EntityLiving entityliving) {
        this(world, entityliving, entityliving.getX(), entityliving.getY(), entityliving.getZ(), itemstack);
        this.entityData.set(EntityFireworks.DATA_ATTACHED_TO_TARGET, OptionalInt.of(entityliving.getId()));
        this.attachedToEntity = entityliving;
    }

    public EntityFireworks(World world, ItemStack itemstack, double d0, double d1, double d2, boolean flag) {
        this(world, d0, d1, d2, itemstack);
        this.entityData.set(EntityFireworks.DATA_SHOT_AT_ANGLE, flag);
    }

    public EntityFireworks(World world, ItemStack itemstack, Entity entity, double d0, double d1, double d2, boolean flag) {
        this(world, itemstack, d0, d1, d2, flag);
        this.setOwner(entity);
    }

    @Override
    protected void defineSynchedData(DataWatcher.a datawatcher_a) {
        datawatcher_a.define(EntityFireworks.DATA_ID_FIREWORKS_ITEM, getDefaultItem());
        datawatcher_a.define(EntityFireworks.DATA_ATTACHED_TO_TARGET, OptionalInt.empty());
        datawatcher_a.define(EntityFireworks.DATA_SHOT_AT_ANGLE, false);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double d0) {
        return d0 < 4096.0D && !this.isAttachedToEntity();
    }

    @Override
    public boolean shouldRender(double d0, double d1, double d2) {
        return super.shouldRender(d0, d1, d2) && !this.isAttachedToEntity();
    }

    @Override
    public void tick() {
        super.tick();
        Vec3D vec3d;
        MovingObjectPosition movingobjectposition;

        if (this.isAttachedToEntity()) {
            if (this.attachedToEntity == null) {
                ((OptionalInt) this.entityData.get(EntityFireworks.DATA_ATTACHED_TO_TARGET)).ifPresent((i) -> {
                    Entity entity = this.level().getEntity(i);

                    if (entity instanceof EntityLiving) {
                        this.attachedToEntity = (EntityLiving) entity;
                    }

                });
            }

            if (this.attachedToEntity != null) {
                if (this.attachedToEntity.isFallFlying()) {
                    Vec3D vec3d1 = this.attachedToEntity.getLookAngle();
                    double d0 = 1.5D;
                    double d1 = 0.1D;
                    Vec3D vec3d2 = this.attachedToEntity.getDeltaMovement();

                    this.attachedToEntity.setDeltaMovement(vec3d2.add(vec3d1.x * 0.1D + (vec3d1.x * 1.5D - vec3d2.x) * 0.5D, vec3d1.y * 0.1D + (vec3d1.y * 1.5D - vec3d2.y) * 0.5D, vec3d1.z * 0.1D + (vec3d1.z * 1.5D - vec3d2.z) * 0.5D));
                    vec3d = this.attachedToEntity.getHandHoldingItemAngle(Items.FIREWORK_ROCKET);
                } else {
                    vec3d = Vec3D.ZERO;
                }

                this.setPos(this.attachedToEntity.getX() + vec3d.x, this.attachedToEntity.getY() + vec3d.y, this.attachedToEntity.getZ() + vec3d.z);
                this.setDeltaMovement(this.attachedToEntity.getDeltaMovement());
            }

            movingobjectposition = ProjectileHelper.getHitResultOnMoveVector(this, this::canHitEntity);
        } else {
            if (!this.isShotAtAngle()) {
                double d2 = this.horizontalCollision ? 1.0D : 1.15D;

                this.setDeltaMovement(this.getDeltaMovement().multiply(d2, 1.0D, d2).add(0.0D, 0.04D, 0.0D));
            }

            vec3d = this.getDeltaMovement();
            movingobjectposition = ProjectileHelper.getHitResultOnMoveVector(this, this::canHitEntity);
            this.move(EnumMoveType.SELF, vec3d);
            this.applyEffectsFromBlocks();
            this.setDeltaMovement(vec3d);
        }

        if (!this.noPhysics && this.isAlive() && movingobjectposition.getType() != MovingObjectPosition.EnumMovingObjectType.MISS) {
            this.hitTargetOrDeflectSelf(movingobjectposition);
            this.hasImpulse = true;
        }

        this.updateRotation();
        if (this.life == 0 && !this.isSilent()) {
            this.level().playSound((EntityHuman) null, this.getX(), this.getY(), this.getZ(), SoundEffects.FIREWORK_ROCKET_LAUNCH, SoundCategory.AMBIENT, 3.0F, 1.0F);
        }

        ++this.life;
        if (this.level().isClientSide && this.life % 2 < 2) {
            this.level().addParticle(Particles.FIREWORK, this.getX(), this.getY(), this.getZ(), this.random.nextGaussian() * 0.05D, -this.getDeltaMovement().y * 0.5D, this.random.nextGaussian() * 0.05D);
        }

        if (this.life > this.lifetime) {
            World world = this.level();

            if (world instanceof WorldServer) {
                WorldServer worldserver = (WorldServer) world;

                this.explode(worldserver);
            }
        }

    }

    private void explode(WorldServer worldserver) {
        worldserver.broadcastEntityEvent(this, (byte) 17);
        this.gameEvent(GameEvent.EXPLODE, this.getOwner());
        this.dealExplosionDamage(worldserver);
        this.discard();
    }

    @Override
    protected void onHitEntity(MovingObjectPositionEntity movingobjectpositionentity) {
        super.onHitEntity(movingobjectpositionentity);
        World world = this.level();

        if (world instanceof WorldServer worldserver) {
            this.explode(worldserver);
        }

    }

    @Override
    protected void onHitBlock(MovingObjectPositionBlock movingobjectpositionblock) {
        BlockPosition blockposition = new BlockPosition(movingobjectpositionblock.getBlockPos());

        this.level().getBlockState(blockposition).entityInside(this.level(), blockposition, this);
        World world = this.level();

        if (world instanceof WorldServer worldserver) {
            if (this.hasExplosion()) {
                this.explode(worldserver);
            }
        }

        super.onHitBlock(movingobjectpositionblock);
    }

    private boolean hasExplosion() {
        return !this.getExplosions().isEmpty();
    }

    private void dealExplosionDamage(WorldServer worldserver) {
        float f = 0.0F;
        List<FireworkExplosion> list = this.getExplosions();

        if (!list.isEmpty()) {
            f = 5.0F + (float) (list.size() * 2);
        }

        if (f > 0.0F) {
            if (this.attachedToEntity != null) {
                this.attachedToEntity.hurtServer(worldserver, this.damageSources().fireworks(this, this.getOwner()), 5.0F + (float) (list.size() * 2));
            }

            double d0 = 5.0D;
            Vec3D vec3d = this.position();
            List<EntityLiving> list1 = this.level().getEntitiesOfClass(EntityLiving.class, this.getBoundingBox().inflate(5.0D));
            Iterator iterator = list1.iterator();

            while (iterator.hasNext()) {
                EntityLiving entityliving = (EntityLiving) iterator.next();

                if (entityliving != this.attachedToEntity && this.distanceToSqr((Entity) entityliving) <= 25.0D) {
                    boolean flag = false;

                    for (int i = 0; i < 2; ++i) {
                        Vec3D vec3d1 = new Vec3D(entityliving.getX(), entityliving.getY(0.5D * (double) i), entityliving.getZ());
                        MovingObjectPositionBlock movingobjectpositionblock = this.level().clip(new RayTrace(vec3d, vec3d1, RayTrace.BlockCollisionOption.COLLIDER, RayTrace.FluidCollisionOption.NONE, this));

                        if (movingobjectpositionblock.getType() == MovingObjectPosition.EnumMovingObjectType.MISS) {
                            flag = true;
                            break;
                        }
                    }

                    if (flag) {
                        float f1 = f * (float) Math.sqrt((5.0D - (double) this.distanceTo(entityliving)) / 5.0D);

                        entityliving.hurtServer(worldserver, this.damageSources().fireworks(this, this.getOwner()), f1);
                    }
                }
            }
        }

    }

    private boolean isAttachedToEntity() {
        return ((OptionalInt) this.entityData.get(EntityFireworks.DATA_ATTACHED_TO_TARGET)).isPresent();
    }

    public boolean isShotAtAngle() {
        return (Boolean) this.entityData.get(EntityFireworks.DATA_SHOT_AT_ANGLE);
    }

    @Override
    public void handleEntityEvent(byte b0) {
        if (b0 == 17 && this.level().isClientSide) {
            Vec3D vec3d = this.getDeltaMovement();

            this.level().createFireworks(this.getX(), this.getY(), this.getZ(), vec3d.x, vec3d.y, vec3d.z, this.getExplosions());
        }

        super.handleEntityEvent(b0);
    }

    @Override
    public void addAdditionalSaveData(NBTTagCompound nbttagcompound) {
        super.addAdditionalSaveData(nbttagcompound);
        nbttagcompound.putInt("Life", this.life);
        nbttagcompound.putInt("LifeTime", this.lifetime);
        nbttagcompound.put("FireworksItem", this.getItem().save(this.registryAccess()));
        nbttagcompound.putBoolean("ShotAtAngle", (Boolean) this.entityData.get(EntityFireworks.DATA_SHOT_AT_ANGLE));
    }

    @Override
    public void readAdditionalSaveData(NBTTagCompound nbttagcompound) {
        super.readAdditionalSaveData(nbttagcompound);
        this.life = nbttagcompound.getInt("Life");
        this.lifetime = nbttagcompound.getInt("LifeTime");
        if (nbttagcompound.contains("FireworksItem", 10)) {
            this.entityData.set(EntityFireworks.DATA_ID_FIREWORKS_ITEM, (ItemStack) ItemStack.parse(this.registryAccess(), nbttagcompound.getCompound("FireworksItem")).orElseGet(EntityFireworks::getDefaultItem));
        } else {
            this.entityData.set(EntityFireworks.DATA_ID_FIREWORKS_ITEM, getDefaultItem());
        }

        if (nbttagcompound.contains("ShotAtAngle")) {
            this.entityData.set(EntityFireworks.DATA_SHOT_AT_ANGLE, nbttagcompound.getBoolean("ShotAtAngle"));
        }

    }

    private List<FireworkExplosion> getExplosions() {
        ItemStack itemstack = (ItemStack) this.entityData.get(EntityFireworks.DATA_ID_FIREWORKS_ITEM);
        Fireworks fireworks = (Fireworks) itemstack.get(DataComponents.FIREWORKS);

        return fireworks != null ? fireworks.explosions() : List.of();
    }

    @Override
    public ItemStack getItem() {
        return (ItemStack) this.entityData.get(EntityFireworks.DATA_ID_FIREWORKS_ITEM);
    }

    @Override
    public boolean isAttackable() {
        return false;
    }

    private static ItemStack getDefaultItem() {
        return new ItemStack(Items.FIREWORK_ROCKET);
    }

    @Override
    public DoubleDoubleImmutablePair calculateHorizontalHurtKnockbackDirection(EntityLiving entityliving, DamageSource damagesource) {
        double d0 = entityliving.position().x - this.position().x;
        double d1 = entityliving.position().z - this.position().z;

        return DoubleDoubleImmutablePair.of(d0, d1);
    }
}
