package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.VSCRegistry;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class ChunkLoaderWeakBlock extends Block implements EntityBlock {
	public ChunkLoaderWeakBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public ChunkLoaderWeakBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new ChunkLoaderWeakBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
		if (type != VSCRegistry.BlockEntities.CHUNK_LOADER_WEAK.get()) {
			return null;
		}
		return level.isClientSide ? null : (level2, pos, state2, entity) -> ((ChunkLoaderWeakBlockEntity) (entity)).serverTick();
	}
}
