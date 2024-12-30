package net.minecraft.world.entity.vehicle;

import com.google.common.collect.Lists;
import com.google.common.collect.UnmodifiableIterator;
import java.util.Iterator;
import java.util.List;
import java.util.function.Supplier;
import javax.annotation.Nullable;
import net.minecraft.BlockUtil;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.core.particles.Particles;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.network.protocol.game.PacketPlayInBoatMove;
import net.minecraft.network.syncher.DataWatcher;
import net.minecraft.network.syncher.DataWatcherObject;
import net.minecraft.network.syncher.DataWatcherRegistry;
import net.minecraft.sounds.SoundEffect;
import net.minecraft.sounds.SoundEffects;
import net.minecraft.tags.TagsEntity;
import net.minecraft.tags.TagsFluid;
import net.minecraft.util.MathHelper;
import net.minecraft.world.EnumHand;
import net.minecraft.world.EnumInteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityLiving;
import net.minecraft.world.entity.EntityPose;
import net.minecraft.world.entity.EntitySize;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.EnumMoveType;
import net.minecraft.world.entity.IEntitySelector;
import net.minecraft.world.entity.Leashable;
import net.minecraft.world.entity.animal.EntityAnimal;
import net.minecraft.world.entity.animal.EntityWaterAnimal;
import net.minecraft.world.entity.monster.creaking.Creaking;
import net.minecraft.world.entity.player.EntityHuman;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.IMaterial;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.BlockWaterLily;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AxisAlignedBB;
import net.minecraft.world.phys.Vec3D;
import net.minecraft.world.phys.shapes.OperatorBoolean;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapes;

public abstract class AbstractBoat extends VehicleEntity implements Leashable {

    private static final DataWatcherObject<Boolean> DATA_ID_PADDLE_LEFT = DataWatcher.defineId(AbstractBoat.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Boolean> DATA_ID_PADDLE_RIGHT = DataWatcher.defineId(AbstractBoat.class, DataWatcherRegistry.BOOLEAN);
    private static final DataWatcherObject<Integer> DATA_ID_BUBBLE_TIME = DataWatcher.defineId(AbstractBoat.class, DataWatcherRegistry.INT);
    public static final int PADDLE_LEFT = 0;
    public static final int PADDLE_RIGHT = 1;
    private static final int TIME_TO_EJECT = 60;
    private static final float PADDLE_SPEED = 0.3926991F;
    public static final double PADDLE_SOUND_TIME = 0.7853981852531433D;
    public static final int BUBBLE_TIME = 60;
    private final float[] paddlePositions = new float[2];
    private float invFriction;
    private float outOfControlTicks;
    private float deltaRotation;
    private int lerpSteps;
    private double lerpX;
    private double lerpY;
    private double lerpZ;
    private double lerpYRot;
    private double lerpXRot;
    private boolean inputLeft;
    private boolean inputRight;
    private boolean inputUp;
    private boolean inputDown;
    private double waterLevel;
    private float landFriction;
    public AbstractBoat.EnumStatus status;
    private AbstractBoat.EnumStatus oldStatus;
    private double lastYd;
    private boolean isAboveBubbleColumn;
    private boolean bubbleColumnDirectionIsDown;
    private float bubbleMultiplier;
    private float bubbleAngle;
    private float bubbleAngleO;
    @Nullable
    private Leashable.a leashData;
    private final Supplier<Item> dropItem;

    public AbstractBoat(EntityTypes<? extends AbstractBoat> entitytypes, World world, Supplier<Item> supplier) {
        super(entitytypes, world);
        this.dropItem = supplier;
        this.blocksBuilding = true;
    }

    public void setInitialPos(double d0, double d1, double d2) {
        this.setPos(d0, d1, d2);
        this.xo = d0;
        this.yo = d1;
        this.zo = d2;
    }

    @Override
    protected Entity.MovementEmission getMovementEmission() {
        return Entity.MovementEmission.EVENTS;
    }

    @Override
    protected void defineSynchedData(DataWatcher.a datawatcher_a) {
        super.defineSynchedData(datawatcher_a);
        datawatcher_a.define(AbstractBoat.DATA_ID_PADDLE_LEFT, false);
        datawatcher_a.define(AbstractBoat.DATA_ID_PADDLE_RIGHT, false);
        datawatcher_a.define(AbstractBoat.DATA_ID_BUBBLE_TIME, 0);
    }

    @Override
    public boolean canCollideWith(Entity entity) {
        return canVehicleCollide(this, entity);
    }

    public static boolean canVehicleCollide(Entity entity, Entity entity1) {
        return (entity1.canBeCollidedWith() || entity1.isPushable()) && !entity.isPassengerOfSameVehicle(entity1);
    }

    @Override
    public boolean canBeCollidedWith() {
        return true;
    }

    @Override
    public boolean isPushable() {
        return true;
    }

    @Override
    public Vec3D getRelativePortalPosition(EnumDirection.EnumAxis enumdirection_enumaxis, BlockUtil.Rectangle blockutil_rectangle) {
        return EntityLiving.resetForwardDirectionOfRelativePortalPosition(super.getRelativePortalPosition(enumdirection_enumaxis, blockutil_rectangle));
    }

    protected abstract double rideHeight(EntitySize entitysize);

    @Override
    protected Vec3D getPassengerAttachmentPoint(Entity entity, EntitySize entitysize, float f) {
        float f1 = this.getSinglePassengerXOffset();

        if (this.getPassengers().size() > 1) {
            int i = this.getPassengers().indexOf(entity);

            if (i == 0) {
                f1 = 0.2F;
            } else {
                f1 = -0.6F;
            }

            if (entity instanceof EntityAnimal) {
                f1 += 0.2F;
            }
        }

        return (new Vec3D(0.0D, this.rideHeight(entitysize), (double) f1)).yRot(-this.getYRot() * 0.017453292F);
    }

    @Override
    public void onAboveBubbleCol(boolean flag) {
        if (!this.level().isClientSide) {
            this.isAboveBubbleColumn = true;
            this.bubbleColumnDirectionIsDown = flag;
            if (this.getBubbleTime() == 0) {
                this.setBubbleTime(60);
            }
        }

        this.level().addParticle(Particles.SPLASH, this.getX() + (double) this.random.nextFloat(), this.getY() + 0.7D, this.getZ() + (double) this.random.nextFloat(), 0.0D, 0.0D, 0.0D);
        if (this.random.nextInt(20) == 0) {
            this.level().playLocalSound(this.getX(), this.getY(), this.getZ(), this.getSwimSplashSound(), this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat(), false);
            this.gameEvent(GameEvent.SPLASH, this.getControllingPassenger());
        }

    }

    @Override
    public void push(Entity entity) {
        if (entity instanceof AbstractBoat) {
            if (entity.getBoundingBox().minY < this.getBoundingBox().maxY) {
                super.push(entity);
            }
        } else if (entity.getBoundingBox().minY <= this.getBoundingBox().minY) {
            super.push(entity);
        }

    }

    @Override
    public void animateHurt(float f) {
        this.setHurtDir(-this.getHurtDir());
        this.setHurtTime(10);
        this.setDamage(this.getDamage() * 11.0F);
    }

    @Override
    public boolean isPickable() {
        return !this.isRemoved();
    }

    @Override
    public void cancelLerp() {
        this.lerpSteps = 0;
    }

    @Override
    public void lerpTo(double d0, double d1, double d2, float f, float f1, int i) {
        this.lerpX = d0;
        this.lerpY = d1;
        this.lerpZ = d2;
        this.lerpYRot = (double) f;
        this.lerpXRot = (double) f1;
        this.lerpSteps = i;
    }

    @Override
    public double lerpTargetX() {
        return this.lerpSteps > 0 ? this.lerpX : this.getX();
    }

    @Override
    public double lerpTargetY() {
        return this.lerpSteps > 0 ? this.lerpY : this.getY();
    }

    @Override
    public double lerpTargetZ() {
        return this.lerpSteps > 0 ? this.lerpZ : this.getZ();
    }

    @Override
    public float lerpTargetXRot() {
        return this.lerpSteps > 0 ? (float) this.lerpXRot : this.getXRot();
    }

    @Override
    public float lerpTargetYRot() {
        return this.lerpSteps > 0 ? (float) this.lerpYRot : this.getYRot();
    }

    @Override
    public EnumDirection getMotionDirection() {
        return this.getDirection().getClockWise();
    }

    @Override
    public void tick() {
        this.oldStatus = this.status;
        this.status = this.getStatus();
        if (this.status != AbstractBoat.EnumStatus.UNDER_WATER && this.status != AbstractBoat.EnumStatus.UNDER_FLOWING_WATER) {
            this.outOfControlTicks = 0.0F;
        } else {
            ++this.outOfControlTicks;
        }

        if (!this.level().isClientSide && this.outOfControlTicks >= 60.0F) {
            this.ejectPassengers();
        }

        if (this.getHurtTime() > 0) {
            this.setHurtTime(this.getHurtTime() - 1);
        }

        if (this.getDamage() > 0.0F) {
            this.setDamage(this.getDamage() - 1.0F);
        }

        super.tick();
        this.tickLerp();
        if (this.isControlledByLocalInstance()) {
            if (!(this.getFirstPassenger() instanceof EntityHuman)) {
                this.setPaddleState(false, false);
            }

            this.floatBoat();
            if (this.level().isClientSide) {
                this.controlBoat();
                this.level().sendPacketToServer(new PacketPlayInBoatMove(this.getPaddleState(0), this.getPaddleState(1)));
            }

            this.move(EnumMoveType.SELF, this.getDeltaMovement());
        } else {
            this.setDeltaMovement(Vec3D.ZERO);
        }

        this.applyEffectsFromBlocks();
        this.applyEffectsFromBlocks();
        this.tickBubbleColumn();

        for (int i = 0; i <= 1; ++i) {
            if (this.getPaddleState(i)) {
                if (!this.isSilent() && (double) (this.paddlePositions[i] % 6.2831855F) <= 0.7853981852531433D && (double) ((this.paddlePositions[i] + 0.3926991F) % 6.2831855F) >= 0.7853981852531433D) {
                    SoundEffect soundeffect = this.getPaddleSound();

                    if (soundeffect != null) {
                        Vec3D vec3d = this.getViewVector(1.0F);
                        double d0 = i == 1 ? -vec3d.z : vec3d.z;
                        double d1 = i == 1 ? vec3d.x : -vec3d.x;

                        this.level().playSound((EntityHuman) null, this.getX() + d0, this.getY(), this.getZ() + d1, soundeffect, this.getSoundSource(), 1.0F, 0.8F + 0.4F * this.random.nextFloat());
                    }
                }

                this.paddlePositions[i] += 0.3926991F;
            } else {
                this.paddlePositions[i] = 0.0F;
            }
        }

        List<Entity> list = this.level().getEntities((Entity) this, this.getBoundingBox().inflate(0.20000000298023224D, -0.009999999776482582D, 0.20000000298023224D), IEntitySelector.pushableBy(this));

        if (!list.isEmpty()) {
            boolean flag = !this.level().isClientSide && !(this.getControllingPassenger() instanceof EntityHuman);
            Iterator iterator = list.iterator();

            while (iterator.hasNext()) {
                Entity entity = (Entity) iterator.next();

                if (!entity.hasPassenger((Entity) this)) {
                    if (flag && this.getPassengers().size() < this.getMaxPassengers() && !entity.isPassenger() && this.hasEnoughSpaceFor(entity) && entity instanceof EntityLiving && !(entity instanceof EntityWaterAnimal) && !(entity instanceof EntityHuman) && !(entity instanceof Creaking)) {
                        entity.startRiding(this);
                    } else {
                        this.push(entity);
                    }
                }
            }
        }

    }

    private void tickBubbleColumn() {
        int i;

        if (this.level().isClientSide) {
            i = this.getBubbleTime();
            if (i > 0) {
                this.bubbleMultiplier += 0.05F;
            } else {
                this.bubbleMultiplier -= 0.1F;
            }

            this.bubbleMultiplier = MathHelper.clamp(this.bubbleMultiplier, 0.0F, 1.0F);
            this.bubbleAngleO = this.bubbleAngle;
            this.bubbleAngle = 10.0F * (float) Math.sin((double) (0.5F * (float) this.level().getGameTime())) * this.bubbleMultiplier;
        } else {
            if (!this.isAboveBubbleColumn) {
                this.setBubbleTime(0);
            }

            i = this.getBubbleTime();
            if (i > 0) {
                --i;
                this.setBubbleTime(i);
                int j = 60 - i - 1;

                if (j > 0 && i == 0) {
                    this.setBubbleTime(0);
                    Vec3D vec3d = this.getDeltaMovement();

                    if (this.bubbleColumnDirectionIsDown) {
                        this.setDeltaMovement(vec3d.add(0.0D, -0.7D, 0.0D));
                        this.ejectPassengers();
                    } else {
                        this.setDeltaMovement(vec3d.x, this.hasPassenger((entity) -> {
                            return entity instanceof EntityHuman;
                        }) ? 2.7D : 0.6D, vec3d.z);
                    }
                }

                this.isAboveBubbleColumn = false;
            }
        }

    }

    @Nullable
    protected SoundEffect getPaddleSound() {
        switch (this.getStatus().ordinal()) {
            case 0:
            case 1:
            case 2:
                return SoundEffects.BOAT_PADDLE_WATER;
            case 3:
                return SoundEffects.BOAT_PADDLE_LAND;
            case 4:
            default:
                return null;
        }
    }

    private void tickLerp() {
        if (this.lerpSteps > 0) {
            this.lerpPositionAndRotationStep(this.lerpSteps, this.lerpX, this.lerpY, this.lerpZ, this.lerpYRot, this.lerpXRot);
            --this.lerpSteps;
        }
    }

    public void setPaddleState(boolean flag, boolean flag1) {
        this.entityData.set(AbstractBoat.DATA_ID_PADDLE_LEFT, flag);
        this.entityData.set(AbstractBoat.DATA_ID_PADDLE_RIGHT, flag1);
    }

    public float getRowingTime(int i, float f) {
        return this.getPaddleState(i) ? MathHelper.clampedLerp(this.paddlePositions[i] - 0.3926991F, this.paddlePositions[i], f) : 0.0F;
    }

    @Nullable
    @Override
    public Leashable.a getLeashData() {
        return this.leashData;
    }

    @Override
    public void setLeashData(@Nullable Leashable.a leashable_a) {
        this.leashData = leashable_a;
    }

    @Override
    public Vec3D getLeashOffset() {
        return new Vec3D(0.0D, (double) (0.88F * this.getEyeHeight()), (double) (this.getBbWidth() * 0.64F));
    }

    @Override
    public void elasticRangeLeashBehaviour(Entity entity, float f) {
        Vec3D vec3d = entity.position().subtract(this.position()).normalize().scale((double) f - 6.0D);
        Vec3D vec3d1 = this.getDeltaMovement();
        boolean flag = vec3d1.dot(vec3d) > 0.0D;

        this.setDeltaMovement(vec3d1.add(vec3d.scale(flag ? 0.15000000596046448D : 0.20000000298023224D)));
    }

    private AbstractBoat.EnumStatus getStatus() {
        AbstractBoat.EnumStatus abstractboat_enumstatus = this.isUnderwater();

        if (abstractboat_enumstatus != null) {
            this.waterLevel = this.getBoundingBox().maxY;
            return abstractboat_enumstatus;
        } else if (this.checkInWater()) {
            return AbstractBoat.EnumStatus.IN_WATER;
        } else {
            float f = this.getGroundFriction();

            if (f > 0.0F) {
                this.landFriction = f;
                return AbstractBoat.EnumStatus.ON_LAND;
            } else {
                return AbstractBoat.EnumStatus.IN_AIR;
            }
        }
    }

    public float getWaterLevelAbove() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.maxY);
        int l = MathHelper.ceil(axisalignedbb.maxY - this.lastYd);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();
        int k1 = k;

        while (k1 < l) {
            float f = 0.0F;
            int l1 = i;

            label35:
            while (true) {
                if (l1 < j) {
                    int i2 = i1;

                    while (true) {
                        if (i2 >= j1) {
                            ++l1;
                            continue label35;
                        }

                        blockposition_mutableblockposition.set(l1, k1, i2);
                        Fluid fluid = this.level().getFluidState(blockposition_mutableblockposition);

                        if (fluid.is(TagsFluid.WATER)) {
                            f = Math.max(f, fluid.getHeight(this.level(), blockposition_mutableblockposition));
                        }

                        if (f >= 1.0F) {
                            break;
                        }

                        ++i2;
                    }
                } else if (f < 1.0F) {
                    return (float) blockposition_mutableblockposition.getY() + f;
                }

                ++k1;
                break;
            }
        }

        return (float) (l + 1);
    }

    public float getGroundFriction() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        AxisAlignedBB axisalignedbb1 = new AxisAlignedBB(axisalignedbb.minX, axisalignedbb.minY - 0.001D, axisalignedbb.minZ, axisalignedbb.maxX, axisalignedbb.minY, axisalignedbb.maxZ);
        int i = MathHelper.floor(axisalignedbb1.minX) - 1;
        int j = MathHelper.ceil(axisalignedbb1.maxX) + 1;
        int k = MathHelper.floor(axisalignedbb1.minY) - 1;
        int l = MathHelper.ceil(axisalignedbb1.maxY) + 1;
        int i1 = MathHelper.floor(axisalignedbb1.minZ) - 1;
        int j1 = MathHelper.ceil(axisalignedbb1.maxZ) + 1;
        VoxelShape voxelshape = VoxelShapes.create(axisalignedbb1);
        float f = 0.0F;
        int k1 = 0;
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

        for (int l1 = i; l1 < j; ++l1) {
            for (int i2 = i1; i2 < j1; ++i2) {
                int j2 = (l1 != i && l1 != j - 1 ? 0 : 1) + (i2 != i1 && i2 != j1 - 1 ? 0 : 1);

                if (j2 != 2) {
                    for (int k2 = k; k2 < l; ++k2) {
                        if (j2 <= 0 || k2 != k && k2 != l - 1) {
                            blockposition_mutableblockposition.set(l1, k2, i2);
                            IBlockData iblockdata = this.level().getBlockState(blockposition_mutableblockposition);

                            if (!(iblockdata.getBlock() instanceof BlockWaterLily) && VoxelShapes.joinIsNotEmpty(iblockdata.getCollisionShape(this.level(), blockposition_mutableblockposition).move((double) l1, (double) k2, (double) i2), voxelshape, OperatorBoolean.AND)) {
                                f += iblockdata.getBlock().getFriction();
                                ++k1;
                            }
                        }
                    }
                }
            }
        }

        return f / (float) k1;
    }

    private boolean checkInWater() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.minY);
        int l = MathHelper.ceil(axisalignedbb.minY + 0.001D);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        boolean flag = false;

        this.waterLevel = -1.7976931348623157E308D;
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockposition_mutableblockposition.set(k1, l1, i2);
                    Fluid fluid = this.level().getFluidState(blockposition_mutableblockposition);

                    if (fluid.is(TagsFluid.WATER)) {
                        float f = (float) l1 + fluid.getHeight(this.level(), blockposition_mutableblockposition);

                        this.waterLevel = Math.max((double) f, this.waterLevel);
                        flag |= axisalignedbb.minY < (double) f;
                    }
                }
            }
        }

        return flag;
    }

    @Nullable
    private AbstractBoat.EnumStatus isUnderwater() {
        AxisAlignedBB axisalignedbb = this.getBoundingBox();
        double d0 = axisalignedbb.maxY + 0.001D;
        int i = MathHelper.floor(axisalignedbb.minX);
        int j = MathHelper.ceil(axisalignedbb.maxX);
        int k = MathHelper.floor(axisalignedbb.maxY);
        int l = MathHelper.ceil(d0);
        int i1 = MathHelper.floor(axisalignedbb.minZ);
        int j1 = MathHelper.ceil(axisalignedbb.maxZ);
        boolean flag = false;
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = new BlockPosition.MutableBlockPosition();

        for (int k1 = i; k1 < j; ++k1) {
            for (int l1 = k; l1 < l; ++l1) {
                for (int i2 = i1; i2 < j1; ++i2) {
                    blockposition_mutableblockposition.set(k1, l1, i2);
                    Fluid fluid = this.level().getFluidState(blockposition_mutableblockposition);

                    if (fluid.is(TagsFluid.WATER) && d0 < (double) ((float) blockposition_mutableblockposition.getY() + fluid.getHeight(this.level(), blockposition_mutableblockposition))) {
                        if (!fluid.isSource()) {
                            return AbstractBoat.EnumStatus.UNDER_FLOWING_WATER;
                        }

                        flag = true;
                    }
                }
            }
        }

        return flag ? AbstractBoat.EnumStatus.UNDER_WATER : null;
    }

    @Override
    protected double getDefaultGravity() {
        return 0.04D;
    }

    private void floatBoat() {
        double d0 = -this.getGravity();
        double d1 = 0.0D;

        this.invFriction = 0.05F;
        if (this.oldStatus == AbstractBoat.EnumStatus.IN_AIR && this.status != AbstractBoat.EnumStatus.IN_AIR && this.status != AbstractBoat.EnumStatus.ON_LAND) {
            this.waterLevel = this.getY(1.0D);
            double d2 = (double) (this.getWaterLevelAbove() - this.getBbHeight()) + 0.101D;

            if (this.level().noCollision(this, this.getBoundingBox().move(0.0D, d2 - this.getY(), 0.0D))) {
                this.setPos(this.getX(), d2, this.getZ());
                this.setDeltaMovement(this.getDeltaMovement().multiply(1.0D, 0.0D, 1.0D));
                this.lastYd = 0.0D;
            }

            this.status = AbstractBoat.EnumStatus.IN_WATER;
        } else {
            if (this.status == AbstractBoat.EnumStatus.IN_WATER) {
                d1 = (this.waterLevel - this.getY()) / (double) this.getBbHeight();
                this.invFriction = 0.9F;
            } else if (this.status == AbstractBoat.EnumStatus.UNDER_FLOWING_WATER) {
                d0 = -7.0E-4D;
                this.invFriction = 0.9F;
            } else if (this.status == AbstractBoat.EnumStatus.UNDER_WATER) {
                d1 = 0.009999999776482582D;
                this.invFriction = 0.45F;
            } else if (this.status == AbstractBoat.EnumStatus.IN_AIR) {
                this.invFriction = 0.9F;
            } else if (this.status == AbstractBoat.EnumStatus.ON_LAND) {
                this.invFriction = this.landFriction;
                if (this.getControllingPassenger() instanceof EntityHuman) {
                    this.landFriction /= 2.0F;
                }
            }

            Vec3D vec3d = this.getDeltaMovement();

            this.setDeltaMovement(vec3d.x * (double) this.invFriction, vec3d.y + d0, vec3d.z * (double) this.invFriction);
            this.deltaRotation *= this.invFriction;
            if (d1 > 0.0D) {
                Vec3D vec3d1 = this.getDeltaMovement();

                this.setDeltaMovement(vec3d1.x, (vec3d1.y + d1 * (this.getDefaultGravity() / 0.65D)) * 0.75D, vec3d1.z);
            }
        }

    }

    private void controlBoat() {
        if (this.isVehicle()) {
            float f = 0.0F;

            if (this.inputLeft) {
                --this.deltaRotation;
            }

            if (this.inputRight) {
                ++this.deltaRotation;
            }

            if (this.inputRight != this.inputLeft && !this.inputUp && !this.inputDown) {
                f += 0.005F;
            }

            this.setYRot(this.getYRot() + this.deltaRotation);
            if (this.inputUp) {
                f += 0.04F;
            }

            if (this.inputDown) {
                f -= 0.005F;
            }

            this.setDeltaMovement(this.getDeltaMovement().add((double) (MathHelper.sin(-this.getYRot() * 0.017453292F) * f), 0.0D, (double) (MathHelper.cos(this.getYRot() * 0.017453292F) * f)));
            this.setPaddleState(this.inputRight && !this.inputLeft || this.inputUp, this.inputLeft && !this.inputRight || this.inputUp);
        }
    }

    protected float getSinglePassengerXOffset() {
        return 0.0F;
    }

    public boolean hasEnoughSpaceFor(Entity entity) {
        return entity.getBbWidth() < this.getBbWidth();
    }

    @Override
    protected void positionRider(Entity entity, Entity.MoveFunction entity_movefunction) {
        super.positionRider(entity, entity_movefunction);
        if (!entity.getType().is(TagsEntity.CAN_TURN_IN_BOATS)) {
            entity.setYRot(entity.getYRot() + this.deltaRotation);
            entity.setYHeadRot(entity.getYHeadRot() + this.deltaRotation);
            this.clampRotation(entity);
            if (entity instanceof EntityAnimal && this.getPassengers().size() == this.getMaxPassengers()) {
                int i = entity.getId() % 2 == 0 ? 90 : 270;

                entity.setYBodyRot(((EntityAnimal) entity).yBodyRot + (float) i);
                entity.setYHeadRot(entity.getYHeadRot() + (float) i);
            }

        }
    }

    @Override
    public Vec3D getDismountLocationForPassenger(EntityLiving entityliving) {
        Vec3D vec3d = getCollisionHorizontalEscapeVector((double) (this.getBbWidth() * MathHelper.SQRT_OF_TWO), (double) entityliving.getBbWidth(), entityliving.getYRot());
        double d0 = this.getX() + vec3d.x;
        double d1 = this.getZ() + vec3d.z;
        BlockPosition blockposition = BlockPosition.containing(d0, this.getBoundingBox().maxY, d1);
        BlockPosition blockposition1 = blockposition.below();

        if (!this.level().isWaterAt(blockposition1)) {
            List<Vec3D> list = Lists.newArrayList();
            double d2 = this.level().getBlockFloorHeight(blockposition);

            if (DismountUtil.isBlockFloorValid(d2)) {
                list.add(new Vec3D(d0, (double) blockposition.getY() + d2, d1));
            }

            double d3 = this.level().getBlockFloorHeight(blockposition1);

            if (DismountUtil.isBlockFloorValid(d3)) {
                list.add(new Vec3D(d0, (double) blockposition1.getY() + d3, d1));
            }

            UnmodifiableIterator unmodifiableiterator = entityliving.getDismountPoses().iterator();

            while (unmodifiableiterator.hasNext()) {
                EntityPose entitypose = (EntityPose) unmodifiableiterator.next();
                Iterator iterator = list.iterator();

                while (iterator.hasNext()) {
                    Vec3D vec3d1 = (Vec3D) iterator.next();

                    if (DismountUtil.canDismountTo(this.level(), vec3d1, entityliving, entitypose)) {
                        entityliving.setPose(entitypose);
                        return vec3d1;
                    }
                }
            }
        }

        return super.getDismountLocationForPassenger(entityliving);
    }

    protected void clampRotation(Entity entity) {
        entity.setYBodyRot(this.getYRot());
        float f = MathHelper.wrapDegrees(entity.getYRot() - this.getYRot());
        float f1 = MathHelper.clamp(f, -105.0F, 105.0F);

        entity.yRotO += f1 - f;
        entity.setYRot(entity.getYRot() + f1 - f);
        entity.setYHeadRot(entity.getYRot());
    }

    @Override
    public void onPassengerTurned(Entity entity) {
        this.clampRotation(entity);
    }

    @Override
    protected void addAdditionalSaveData(NBTTagCompound nbttagcompound) {
        this.writeLeashData(nbttagcompound, this.leashData);
    }

    @Override
    protected void readAdditionalSaveData(NBTTagCompound nbttagcompound) {
        this.readLeashData(nbttagcompound);
    }

    @Override
    public EnumInteractionResult interact(EntityHuman entityhuman, EnumHand enumhand) {
        EnumInteractionResult enuminteractionresult = super.interact(entityhuman, enumhand);

        return (EnumInteractionResult) (enuminteractionresult != EnumInteractionResult.PASS ? enuminteractionresult : (!entityhuman.isSecondaryUseActive() && this.outOfControlTicks < 60.0F && (this.level().isClientSide || entityhuman.startRiding(this)) ? EnumInteractionResult.SUCCESS : EnumInteractionResult.PASS));
    }

    @Override
    public void remove(Entity.RemovalReason entity_removalreason) {
        if (!this.level().isClientSide && entity_removalreason.shouldDestroy() && this.isLeashed()) {
            this.dropLeash();
        }

        super.remove(entity_removalreason);
    }

    @Override
    protected void checkFallDamage(double d0, boolean flag, IBlockData iblockdata, BlockPosition blockposition) {
        this.lastYd = this.getDeltaMovement().y;
        if (!this.isPassenger()) {
            if (flag) {
                this.resetFallDistance();
            } else if (!this.level().getFluidState(this.blockPosition().below()).is(TagsFluid.WATER) && d0 < 0.0D) {
                this.fallDistance -= (float) d0;
            }

        }
    }

    public boolean getPaddleState(int i) {
        return (Boolean) this.entityData.get(i == 0 ? AbstractBoat.DATA_ID_PADDLE_LEFT : AbstractBoat.DATA_ID_PADDLE_RIGHT) && this.getControllingPassenger() != null;
    }

    private void setBubbleTime(int i) {
        this.entityData.set(AbstractBoat.DATA_ID_BUBBLE_TIME, i);
    }

    private int getBubbleTime() {
        return (Integer) this.entityData.get(AbstractBoat.DATA_ID_BUBBLE_TIME);
    }

    public float getBubbleAngle(float f) {
        return MathHelper.lerp(f, this.bubbleAngleO, this.bubbleAngle);
    }

    @Override
    protected boolean canAddPassenger(Entity entity) {
        return this.getPassengers().size() < this.getMaxPassengers() && !this.isEyeInFluid(TagsFluid.WATER);
    }

    protected int getMaxPassengers() {
        return 2;
    }

    @Nullable
    @Override
    public EntityLiving getControllingPassenger() {
        Entity entity = this.getFirstPassenger();
        EntityLiving entityliving;

        if (entity instanceof EntityLiving entityliving1) {
            entityliving = entityliving1;
        } else {
            entityliving = super.getControllingPassenger();
        }

        return entityliving;
    }

    public void setInput(boolean flag, boolean flag1, boolean flag2, boolean flag3) {
        this.inputLeft = flag;
        this.inputRight = flag1;
        this.inputUp = flag2;
        this.inputDown = flag3;
    }

    @Override
    public boolean isUnderWater() {
        return this.status == AbstractBoat.EnumStatus.UNDER_WATER || this.status == AbstractBoat.EnumStatus.UNDER_FLOWING_WATER;
    }

    @Override
    protected final Item getDropItem() {
        return (Item) this.dropItem.get();
    }

    @Override
    public final ItemStack getPickResult() {
        return new ItemStack((IMaterial) this.dropItem.get());
    }

    public static enum EnumStatus {

        IN_WATER, UNDER_WATER, UNDER_FLOWING_WATER, ON_LAND, IN_AIR;

        private EnumStatus() {}
    }
}
