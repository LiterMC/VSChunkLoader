package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.VSCRegistry;
import com.github.litermc.vschunkloader.config.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;

public final class ChunkLoaderWeakBlockEntity extends ChunkLoaderBlockEntity {
	private int secondsUsed = 0;

	public ChunkLoaderWeakBlockEntity(final BlockPos pos, final BlockState state) {
		super(VSCRegistry.BlockEntities.CHUNK_LOADER_WEAK.get(), pos, state);
	}

	@Override
	public boolean isRunning() {
		return super.isRunning() && !this.isOutOfTime();
	}

	@Override
	public int getEnergyConsumeRate() {
		return Config.weakChunkLoaderEnergyConsumeRate;
	}

	public boolean isOutOfTime() {
		return this.secondsUsed >= this.getMaxActiveSeconds();
	}

	public int getMaxActiveSeconds() {
		return Config.weakChunkLoaderMaxActivateSeconds;
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.secondsUsed = data.getInt("SecondsUsed");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putInt("SecondsUsed", this.secondsUsed);
	}

	@Override
	public void serverTick() {
		super.serverTick();
		if (this.isRemoved()) {
			return;
		}
		if (this.isOutOfTime()) {
			this.getLevel().destroyBlock(this.getBlockPos(), false);
			this.setRemoved();
		}
	}

	@Override
	public void onRefresh() {
		super.onRefresh();
		if (!this.isOutOfTime()) {
			this.secondsUsed++;
			this.setChanged();
		}
	}
}
