package com.simibubi.create.content.logistics.block.funnel;

import com.simibubi.create.AllBlocks;
import com.simibubi.create.AllShapes;
import com.simibubi.create.content.contraptions.relays.belt.BeltBlock;
import com.simibubi.create.content.contraptions.relays.belt.BeltTileEntity;
import com.simibubi.create.content.logistics.block.belts.tunnel.BeltTunnelBlock;
import com.simibubi.create.content.logistics.block.depot.DepotBlock;
import com.simibubi.create.foundation.tileEntity.TileEntityBehaviour;
import com.simibubi.create.foundation.tileEntity.behaviour.filtering.FilteringBehaviour;
import com.simibubi.create.foundation.utility.Lang;
import com.simibubi.create.foundation.utility.VoxelShaper;
import com.tterrag.registrate.util.entry.BlockEntry;

import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.item.BlockItemUseContext;
import net.minecraft.state.EnumProperty;
import net.minecraft.state.IProperty;
import net.minecraft.state.StateContainer.Builder;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.Direction;
import net.minecraft.util.IStringSerializable;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.shapes.ISelectionContext;
import net.minecraft.util.math.shapes.VoxelShape;
import net.minecraft.world.IBlockReader;
import net.minecraft.world.IWorld;
import net.minecraft.world.IWorldReader;
import net.minecraft.world.World;

public abstract class BeltFunnelBlock extends HorizontalInteractionFunnelBlock {

	public static final IProperty<Shape> SHAPE = EnumProperty.create("shape", Shape.class);

	public enum Shape implements IStringSerializable {
		RETRACTED(AllShapes.BELT_FUNNEL_RETRACTED), EXTENDED(AllShapes.BELT_FUNNEL_EXTENDED);

		VoxelShaper shaper;

		private Shape(VoxelShaper shaper) {
			this.shaper = shaper;
		}

		@Override
		public String getName() {
			return Lang.asId(name());
		}
	}

	public BeltFunnelBlock(BlockEntry<? extends FunnelBlock> parent, Properties p_i48377_1_) {
		super(parent, p_i48377_1_);
		setDefaultState(getDefaultState().with(SHAPE, Shape.RETRACTED));
	}

	@Override
	public BlockState getStateForPlacement(BlockItemUseContext ctx) {
		BlockState state = super.getStateForPlacement(ctx);
		return getStateForPosition(ctx.getWorld(), ctx.getPos(), state, ctx.getFace());
	}

	public BlockState getStateForPosition(World world, BlockPos pos, BlockState defaultState, Direction facing) {
		BlockState state = defaultState.with(HORIZONTAL_FACING, facing);
		BlockPos posBelow = pos.down();
		BlockState stateBelow = world.getBlockState(posBelow);
		if (!AllBlocks.BELT.has(stateBelow))
			return state;
		TileEntity teBelow = world.getTileEntity(posBelow);
		if (teBelow == null || !(teBelow instanceof BeltTileEntity))
			return state;
		BeltTileEntity beltTileEntity = (BeltTileEntity) teBelow;
		if (beltTileEntity.getSpeed() == 0)
			return state;
		Direction movementFacing = beltTileEntity.getMovementFacing();
		return state.with(PUSHING, movementFacing == facing);
	}

	@Override
	protected void fillStateContainer(Builder<Block, BlockState> p_206840_1_) {
		super.fillStateContainer(p_206840_1_.add(SHAPE));
	}

	@Override
	public VoxelShape getShape(BlockState state, IBlockReader p_220053_2_, BlockPos p_220053_3_,
		ISelectionContext p_220053_4_) {
		return state.get(SHAPE).shaper.get(state.get(HORIZONTAL_FACING));
	}

	@Override
	public BlockState updatePostPlacement(BlockState state, Direction direction, BlockState neighbour, IWorld world,
		BlockPos pos, BlockPos p_196271_6_) {
		if (direction == state.get(HORIZONTAL_FACING))
			return updateShape(state, world, pos);
		else
			return super.updatePostPlacement(state, direction, neighbour, world, pos, p_196271_6_);
	}

	public static boolean isOnValidBelt(BlockState state, IWorldReader world, BlockPos pos) {
		BlockState stateBelow = world.getBlockState(pos.down());
		if (stateBelow.getBlock() instanceof DepotBlock)
			return true;
		if (!(stateBelow.getBlock() instanceof BeltBlock))
			return false;
		if (!BeltBlock.canTransportObjects(stateBelow))
			return false;
		return true;
	}

	public static BlockState updateShape(BlockState state, IBlockReader world, BlockPos pos) {
		state = state.with(SHAPE, Shape.RETRACTED);
		Direction horizontalFacing = state.get(HORIZONTAL_FACING);

		BlockState below = world.getBlockState(pos.down());
		if (below.getBlock() instanceof BeltBlock && below.get(BeltBlock.HORIZONTAL_FACING)
			.getAxis() != horizontalFacing.getAxis())
			return state;

		BlockState neighbour = world.getBlockState(pos.offset(horizontalFacing));
		if (canConnectTo(state, neighbour))
			return state.with(SHAPE, Shape.EXTENDED);
		return state;
	}

	private static boolean canConnectTo(BlockState state, BlockState neighbour) {
		if (neighbour.getBlock() instanceof BeltTunnelBlock)
			return true;
		if (neighbour.getBlock() instanceof BeltFunnelBlock
			&& neighbour.get(HORIZONTAL_FACING) == state.get(HORIZONTAL_FACING)
				.getOpposite())
			return true;
		return false;
	}

	@Override
	public void onReplaced(BlockState p_196243_1_, World p_196243_2_, BlockPos p_196243_3_, BlockState p_196243_4_,
		boolean p_196243_5_) {
		if (p_196243_1_.hasTileEntity()
			&& (p_196243_1_.getBlock() != p_196243_4_.getBlock() && !FunnelBlock.isFunnel(p_196243_4_)
				|| !p_196243_4_.hasTileEntity())) {
			TileEntityBehaviour.destroy(p_196243_2_, p_196243_3_, FilteringBehaviour.TYPE);
			p_196243_2_.removeTileEntity(p_196243_3_);
		}
	}

	@Override
	protected boolean canStillInteract(BlockState state, IWorldReader world, BlockPos pos) {
		return isOnValidBelt(state, world, pos);
	}

}
