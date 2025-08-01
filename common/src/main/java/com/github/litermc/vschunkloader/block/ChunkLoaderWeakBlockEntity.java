package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.VSCRegistry;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.MinecraftPlayer;

public class ChunkLoaderWeakBlockEntity extends ChunkLoaderBlockEntity {
	private int tickUsed = 0;

	public ChunkLoaderWeakBlockEntity(final BlockPos pos, final BlockState state) {
		super(VSCRegistry.BlockEntities.CHUNK_LOADER_WEAK.get(), pos, state);
	}

	public boolean isRunning() {
		return super.isRunning() && this.tickUsed < this.getMaxUseTime();
	}

	public int getMaxUseTime() {
		return 20 * 60; // 1 min
	}

	@Override
	public void load(final CompoundTag data) {
		super.load(data);
		this.tickUsed = data.getInt("TickUsed");
	}

	@Override
	protected void saveAdditional(final CompoundTag data) {
		super.saveAdditional(data);
		data.putInt("TickUsed", this.tickUsed);
	}

	@Override
	public void serverTick() {
		final boolean isDeactivating = this.isDeactivating();
		super.serverTick();
		if (isDeactivating || this.isRemoved()) {
			return;
		}
		if (this.tickUsed >= this.getMaxUseTime()) {
			this.getLevel().destroyBlock(this.getBlockPos(), false);
			return;
		}
		if (this.isRunning()) {
			this.tickUsed++;
			this.setChanged();
		}
	}
}
