package com.github.litermc.vschunkloader.block.ammo;

import com.github.litermc.vschunkloader.VSCRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.DirectionalBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;

import java.util.List;

public class AmmoAssemblerBlock extends DirectionalBlock implements EntityBlock {
	public static final EnumProperty<AssembleResult.AssembleLED> LED = EnumProperty.create("led", AssembleResult.AssembleLED.class);

	public AmmoAssemblerBlock(final Properties properties) {
		super(properties);
		this.registerDefaultState(
			this.defaultBlockState()
				.setValue(DirectionalBlock.FACING, Direction.UP)
				.setValue(LED, AssembleResult.AssembleLED.GREEN)
		);
	}

	@Override
	public void createBlockStateDefinition(final StateDefinition.Builder<Block, BlockState> builder) {
		builder
			.add(DirectionalBlock.FACING)
			.add(LED);
	}

	@Override
	public BlockState getStateForPlacement(final BlockPlaceContext ctx) {
		Direction dir = ctx.getNearestLookingDirection().getOpposite();
		if (ctx.isSecondaryUseActive()) {
			dir = dir.getOpposite();
		}
		return this.defaultBlockState()
			.setValue(DirectionalBlock.FACING, dir)
			.setValue(LED, AssembleResult.AssembleLED.GREEN);
	}

	@Override
	public void neighborChanged(
		final BlockState state,
		final Level world,
		final BlockPos pos,
		final Block neighbor,
		final BlockPos neighborPos,
		final boolean moving
	) {
		super.neighborChanged(state, world, pos, neighbor, neighborPos, moving);
		final AmmoAssemblerBlockEntity be = (AmmoAssemblerBlockEntity) world.getBlockEntity(pos);
		be.neighborChanged(neighbor, neighborPos, moving);
	}

	@Override
	public AmmoAssemblerBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new AmmoAssemblerBlockEntity(pos, state);
	}

	@Override
	public <U extends BlockEntity> BlockEntityTicker<U> getTicker(final Level level, final BlockState state, final BlockEntityType<U> type) {
		if (type != VSCRegistry.BlockEntities.AMMO_ASSEMBLER.get()) {
			return null;
		}
		return level.isClientSide ? null : (level2, pos, state2, entity) -> ((AmmoAssemblerBlockEntity) (entity)).serverTick();
	}
}
