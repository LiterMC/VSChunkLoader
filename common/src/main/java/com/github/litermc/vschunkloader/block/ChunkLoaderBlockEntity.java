package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.VSCRegistry;
import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vschunkloader.util.ChunkLoaderManager;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.MinecraftPlayer;

import java.lang.ref.WeakReference;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

public class ChunkLoaderBlockEntity extends BlockEntity {
	private int activating = 0;
	private boolean wasActivated = false;
	private int energyStored = 0;

	private ServerPlayer fakePlayer = null;
	private MinecraftPlayer playerData = null;

	public ChunkLoaderBlockEntity(final BlockPos pos, final BlockState state) {
		super(VSCRegistry.BlockEntities.CHUNK_LOADER.get(), pos, state);
	}

	public boolean isRunning() {
		return this.activating > 0;
	}

	public MinecraftPlayer getOrCreatePlayerData() {
		if (this.playerData == null) {
			final ServerLevel level = ((ServerLevel)(this.getLevel()));

			final UUID uuid = UUID.nameUUIDFromBytes(("ChunkLoader:" + this.getBlockPos().asLong()).getBytes(StandardCharsets.UTF_8));
			this.fakePlayer = PlatformHelper.get().createFakePlayer(
				level,
				new GameProfile(uuid, "[ChunkLoader:" + this.getBlockPos() + "]")
			);
			this.playerData = new MinecraftPlayer(this.fakePlayer);
			final Vec3 pos = VSGameUtilsKt.toWorldCoordinates(level, this.getBlockPos().getCenter());
			this.fakePlayer.absMoveTo(pos.x, pos.y, pos.z);
			level.addNewPlayer(this.fakePlayer);
		}
		return this.playerData;
	}

	public int receiveEnergy(final int maxReceive, final boolean simulate) {
		final int avaliable = this.getMaxEnergyStored() - this.energyStored;
		if (avaliable <= 0) {
			return 0;
		}
		if (!simulate) {
			this.energyStored += Math.min(avaliable, maxReceive);
		}
		return Math.max(maxReceive - avaliable, 0);
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
		if (this.activating <= 0) {
			final int newEnergy = this.energyStored - this.getEnergyConsumeRate();
			if (newEnergy >= 0) {
				this.activating = 20;
				this.energyStored = newEnergy;
			}
		}
		if (!this.isRunning()) {
			if (this.wasActivated) {
				this.wasActivated = false;
				this.onDeactivated();
				this.setChanged();
			}
			return;
		}
		this.activating--;
		this.setChanged();
		if (!this.wasActivated) {
			this.wasActivated = true;
			this.onActivated();
		}
		this.getOrCreatePlayerData();
		final Vec3 pos = this.getBlockPos().getCenter();
		this.fakePlayer.absMoveTo(pos.x, pos.y, pos.z);
		level.getChunkSource().move(this.fakePlayer);
	}

	public void onActivated() {
		final ServerLevel level = (ServerLevel)(this.getLevel());
		ChunkLoaderManager.get(level).activateChunkLoader(this.getBlockPos());
	}

	public void onDeactivated() {
		final ServerLevel level = (ServerLevel)(this.getLevel());
		ChunkLoaderManager.get(level).deactivateChunkLoader(this.getBlockPos());
		if (this.playerData != null) {
			this.fakePlayer.discard();
			this.fakePlayer = null;
			this.playerData = null;
		}
	}
}
