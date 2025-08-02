package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.VSCRegistry;
import com.github.litermc.vschunkloader.util.ChunkLoaderManager;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class ChunkLoaderBlockEntity extends BlockEntity {
	private int activating = 0;
	private boolean wasActivated = true;
	private int energyStored = 0;

	Object energyStorage = null;

	protected ChunkLoaderBlockEntity(final BlockEntityType<? extends ChunkLoaderBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
	}

	public ChunkLoaderBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSCRegistry.BlockEntities.CHUNK_LOADER.get(), pos, state);
	}

	public boolean isRunning() {
		return this.activating > 0;
	}

	public int receiveEnergy(final int maxReceive, final boolean simulate) {
		final int avaliable = this.getMaxEnergyStored() - this.energyStored;
		if (avaliable <= 0) {
			return 0;
		}
		final int received = Math.min(avaliable, maxReceive);
		if (!simulate) {
			this.energyStored += received;
		}
		return received;
	}

	public int getEnergyStored() {
		return this.energyStored;
	}

	/**
	 * @return energy usage in FE/s
	 */
	public int getEnergyConsumeRate() {
		return 4096 * 20;
	}

	public int getMaxEnergyStored() {
		return this.getEnergyConsumeRate() * 4;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.activating = data.getInt("Activating");
		this.energyStored = data.getInt("EnergyStored");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putInt("Activating", this.activating);
		data.putInt("EnergyStored", this.energyStored);
	}

	public void serverTick() {
		final ServerLevel level = (ServerLevel)(this.getLevel());
		if (level.getBlockEntity(this.getBlockPos()) != this) {
			this.setRemoved();
			return;
		}
		if (this.activating <= 1) {
			final int newEnergy = this.energyStored - this.getEnergyConsumeRate();
			if (newEnergy >= 0) {
				this.activating += 20;
				this.energyStored = newEnergy;
				this.setChanged();
			}
		}
		if (!this.isRunning()) {
			if (this.wasActivated) {
				this.wasActivated = false;
				this.onDeactivate();
				this.setChanged();
			}
			return;
		}
		this.activating--;
		this.setChanged();
		this.wasActivated = true;
		ChunkLoaderManager.get(level).refreshChunkLoader(this.getBlockPos());
	}

	public void onDeactivate() {
		final ServerLevel level = (ServerLevel)(this.getLevel());
		ChunkLoaderManager.get(level).deactivateChunkLoader(this.getBlockPos());
	}
}
