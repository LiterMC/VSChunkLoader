package com.github.litermc.vschunkloader.block.ammo;

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

public final class AmmoManagerBlock extends Block implements EntityBlock {
	public AmmoManagerBlock(final BlockBehaviour.Properties props) {
		super(props);
	}

	@Override
	public AmmoManagerBlockEntity newBlockEntity(final BlockPos pos, final BlockState state) {
		return new AmmoManagerBlockEntity(pos, state);
	}

	@Override
	public <T extends BlockEntity> BlockEntityTicker<T> getTicker(final Level level, final BlockState state, final BlockEntityType<T> type) {
		if (type != VSCRegistry.BlockEntities.AMMO_MANAGER.get()) {
			return null;
		}
		return level.isClientSide ? null : (level2, pos, state2, entity) -> ((AmmoManagerBlockEntity) (entity)).serverTick();
	}
}
