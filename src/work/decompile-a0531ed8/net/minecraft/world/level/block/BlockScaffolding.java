package net.minecraft.world.level.block;

import com.mojang.serialization.MapCodec;
import java.util.Iterator;
import net.minecraft.core.BlockPosition;
import net.minecraft.core.EnumDirection;
import net.minecraft.server.level.WorldServer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.item.EntityFallingBlock;
import net.minecraft.world.item.context.BlockActionContext;
import net.minecraft.world.level.IBlockAccess;
import net.minecraft.world.level.IWorldReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.World;
import net.minecraft.world.level.block.state.BlockBase;
import net.minecraft.world.level.block.state.BlockStateList;
import net.minecraft.world.level.block.state.IBlockData;
import net.minecraft.world.level.block.state.properties.BlockProperties;
import net.minecraft.world.level.block.state.properties.BlockStateBoolean;
import net.minecraft.world.level.block.state.properties.BlockStateInteger;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidType;
import net.minecraft.world.level.material.FluidTypes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraft.world.phys.shapes.VoxelShapeCollision;
import net.minecraft.world.phys.shapes.VoxelShapes;

public class BlockScaffolding extends Block implements IBlockWaterlogged {

    public static final MapCodec<BlockScaffolding> CODEC = simpleCodec(BlockScaffolding::new);
    private static final int TICK_DELAY = 1;
    private static final VoxelShape STABLE_SHAPE;
    private static final VoxelShape UNSTABLE_SHAPE;
    private static final VoxelShape UNSTABLE_SHAPE_BOTTOM = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
    private static final VoxelShape BELOW_BLOCK = VoxelShapes.block().move(0.0D, -1.0D, 0.0D);
    public static final int STABILITY_MAX_DISTANCE = 7;
    public static final BlockStateInteger DISTANCE = BlockProperties.STABILITY_DISTANCE;
    public static final BlockStateBoolean WATERLOGGED = BlockProperties.WATERLOGGED;
    public static final BlockStateBoolean BOTTOM = BlockProperties.BOTTOM;

    @Override
    public MapCodec<BlockScaffolding> codec() {
        return BlockScaffolding.CODEC;
    }

    protected BlockScaffolding(BlockBase.Info blockbase_info) {
        super(blockbase_info);
        this.registerDefaultState((IBlockData) ((IBlockData) ((IBlockData) ((IBlockData) this.stateDefinition.any()).setValue(BlockScaffolding.DISTANCE, 7)).setValue(BlockScaffolding.WATERLOGGED, false)).setValue(BlockScaffolding.BOTTOM, false));
    }

    @Override
    protected void createBlockStateDefinition(BlockStateList.a<Block, IBlockData> blockstatelist_a) {
        blockstatelist_a.add(BlockScaffolding.DISTANCE, BlockScaffolding.WATERLOGGED, BlockScaffolding.BOTTOM);
    }

    @Override
    protected VoxelShape getShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return !voxelshapecollision.isHoldingItem(iblockdata.getBlock().asItem()) ? ((Boolean) iblockdata.getValue(BlockScaffolding.BOTTOM) ? BlockScaffolding.UNSTABLE_SHAPE : BlockScaffolding.STABLE_SHAPE) : VoxelShapes.block();
    }

    @Override
    protected VoxelShape getInteractionShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition) {
        return VoxelShapes.block();
    }

    @Override
    protected boolean canBeReplaced(IBlockData iblockdata, BlockActionContext blockactioncontext) {
        return blockactioncontext.getItemInHand().is(this.asItem());
    }

    @Override
    public IBlockData getStateForPlacement(BlockActionContext blockactioncontext) {
        BlockPosition blockposition = blockactioncontext.getClickedPos();
        World world = blockactioncontext.getLevel();
        int i = getDistance(world, blockposition);

        return (IBlockData) ((IBlockData) ((IBlockData) this.defaultBlockState().setValue(BlockScaffolding.WATERLOGGED, world.getFluidState(blockposition).getType() == FluidTypes.WATER)).setValue(BlockScaffolding.DISTANCE, i)).setValue(BlockScaffolding.BOTTOM, this.isBottom(world, blockposition, i));
    }

    @Override
    protected void onPlace(IBlockData iblockdata, World world, BlockPosition blockposition, IBlockData iblockdata1, boolean flag) {
        if (!world.isClientSide) {
            world.scheduleTick(blockposition, (Block) this, 1);
        }

    }

    @Override
    protected IBlockData updateShape(IBlockData iblockdata, IWorldReader iworldreader, ScheduledTickAccess scheduledtickaccess, BlockPosition blockposition, EnumDirection enumdirection, BlockPosition blockposition1, IBlockData iblockdata1, RandomSource randomsource) {
        if ((Boolean) iblockdata.getValue(BlockScaffolding.WATERLOGGED)) {
            scheduledtickaccess.scheduleTick(blockposition, (FluidType) FluidTypes.WATER, FluidTypes.WATER.getTickDelay(iworldreader));
        }

        if (!iworldreader.isClientSide()) {
            scheduledtickaccess.scheduleTick(blockposition, (Block) this, 1);
        }

        return iblockdata;
    }

    @Override
    protected void tick(IBlockData iblockdata, WorldServer worldserver, BlockPosition blockposition, RandomSource randomsource) {
        int i = getDistance(worldserver, blockposition);
        IBlockData iblockdata1 = (IBlockData) ((IBlockData) iblockdata.setValue(BlockScaffolding.DISTANCE, i)).setValue(BlockScaffolding.BOTTOM, this.isBottom(worldserver, blockposition, i));

        if ((Integer) iblockdata1.getValue(BlockScaffolding.DISTANCE) == 7) {
            if ((Integer) iblockdata.getValue(BlockScaffolding.DISTANCE) == 7) {
                EntityFallingBlock.fall(worldserver, blockposition, iblockdata1);
            } else {
                worldserver.destroyBlock(blockposition, true);
            }
        } else if (iblockdata != iblockdata1) {
            worldserver.setBlock(blockposition, iblockdata1, 3);
        }

    }

    @Override
    protected boolean canSurvive(IBlockData iblockdata, IWorldReader iworldreader, BlockPosition blockposition) {
        return getDistance(iworldreader, blockposition) < 7;
    }

    @Override
    protected VoxelShape getCollisionShape(IBlockData iblockdata, IBlockAccess iblockaccess, BlockPosition blockposition, VoxelShapeCollision voxelshapecollision) {
        return voxelshapecollision.isAbove(VoxelShapes.block(), blockposition, true) && !voxelshapecollision.isDescending() ? BlockScaffolding.STABLE_SHAPE : ((Integer) iblockdata.getValue(BlockScaffolding.DISTANCE) != 0 && (Boolean) iblockdata.getValue(BlockScaffolding.BOTTOM) && voxelshapecollision.isAbove(BlockScaffolding.BELOW_BLOCK, blockposition, true) ? BlockScaffolding.UNSTABLE_SHAPE_BOTTOM : VoxelShapes.empty());
    }

    @Override
    protected Fluid getFluidState(IBlockData iblockdata) {
        return (Boolean) iblockdata.getValue(BlockScaffolding.WATERLOGGED) ? FluidTypes.WATER.getSource(false) : super.getFluidState(iblockdata);
    }

    private boolean isBottom(IBlockAccess iblockaccess, BlockPosition blockposition, int i) {
        return i > 0 && !iblockaccess.getBlockState(blockposition.below()).is((Block) this);
    }

    public static int getDistance(IBlockAccess iblockaccess, BlockPosition blockposition) {
        BlockPosition.MutableBlockPosition blockposition_mutableblockposition = blockposition.mutable().move(EnumDirection.DOWN);
        IBlockData iblockdata = iblockaccess.getBlockState(blockposition_mutableblockposition);
        int i = 7;

        if (iblockdata.is(Blocks.SCAFFOLDING)) {
            i = (Integer) iblockdata.getValue(BlockScaffolding.DISTANCE);
        } else if (iblockdata.isFaceSturdy(iblockaccess, blockposition_mutableblockposition, EnumDirection.UP)) {
            return 0;
        }

        Iterator iterator = EnumDirection.EnumDirectionLimit.HORIZONTAL.iterator();

        while (iterator.hasNext()) {
            EnumDirection enumdirection = (EnumDirection) iterator.next();
            IBlockData iblockdata1 = iblockaccess.getBlockState(blockposition_mutableblockposition.setWithOffset(blockposition, enumdirection));

            if (iblockdata1.is(Blocks.SCAFFOLDING)) {
                i = Math.min(i, (Integer) iblockdata1.getValue(BlockScaffolding.DISTANCE) + 1);
                if (i == 1) {
                    break;
                }
            }
        }

        return i;
    }

    static {
        VoxelShape voxelshape = Block.box(0.0D, 14.0D, 0.0D, 16.0D, 16.0D, 16.0D);
        VoxelShape voxelshape1 = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 16.0D, 2.0D);
        VoxelShape voxelshape2 = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 16.0D, 2.0D);
        VoxelShape voxelshape3 = Block.box(0.0D, 0.0D, 14.0D, 2.0D, 16.0D, 16.0D);
        VoxelShape voxelshape4 = Block.box(14.0D, 0.0D, 14.0D, 16.0D, 16.0D, 16.0D);

        STABLE_SHAPE = VoxelShapes.or(voxelshape, voxelshape1, voxelshape2, voxelshape3, voxelshape4);
        VoxelShape voxelshape5 = Block.box(0.0D, 0.0D, 0.0D, 2.0D, 2.0D, 16.0D);
        VoxelShape voxelshape6 = Block.box(14.0D, 0.0D, 0.0D, 16.0D, 2.0D, 16.0D);
        VoxelShape voxelshape7 = Block.box(0.0D, 0.0D, 14.0D, 16.0D, 2.0D, 16.0D);
        VoxelShape voxelshape8 = Block.box(0.0D, 0.0D, 0.0D, 16.0D, 2.0D, 2.0D);

        UNSTABLE_SHAPE = VoxelShapes.or(BlockScaffolding.UNSTABLE_SHAPE_BOTTOM, BlockScaffolding.STABLE_SHAPE, voxelshape6, voxelshape5, voxelshape8, voxelshape7);
    }
}