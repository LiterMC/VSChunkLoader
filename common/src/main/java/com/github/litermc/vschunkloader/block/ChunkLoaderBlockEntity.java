package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.VSCRegistry;
import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vschunkloader.util.ChunkLoaderManager;
import com.github.litermc.vschunkloader.util.IChunkLoaderFakePlayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;

import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.MinecraftPlayer;

import java.util.UUID;
import java.nio.charset.StandardCharsets;

public class ChunkLoaderBlockEntity extends BlockEntity {
	private int activating = 0;
	private boolean wasActivated = false;
	private int energyStored = 0;
	private boolean deactivating = false;

	protected final GameProfile fakeGameProfile;
	private ServerPlayer fakePlayer = null;
	private MinecraftPlayer playerData = null;

	protected ChunkLoaderBlockEntity(final BlockEntityType<? extends ChunkLoaderBlockEntity> type, final BlockPos pos, final BlockState state) {
		super(type, pos, state);
		final String name = "ChunkLoader:" + pos.asLong();
		final UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
		this.fakeGameProfile = new GameProfile(uuid, name);
	}

	public ChunkLoaderBlockEntity(final BlockPos pos, final BlockState state) {
		this(VSCRegistry.BlockEntities.CHUNK_LOADER.get(), pos, state);
	}

	public boolean isRunning() {
		return this.activating > 0;
	}

	public boolean isDeactivating() {
		return this.deactivating;
	}

	public boolean shouldTrack() {
		return this.isRunning() || this.playerData != null;
	}

	public MinecraftPlayer getOrCreatePlayerData() {
		if (this.playerData == null) {
			final ServerLevel level = ((ServerLevel)(this.getLevel()));
			final BlockPos blockPos = this.getBlockPos();

			this.fakePlayer = PlatformHelper.get().createFakePlayer(level, this.fakeGameProfile);
			((IChunkLoaderFakePlayer)(this.fakePlayer)).bindChunkLoader(this);
			this.playerData = new MinecraftPlayer(this.fakePlayer);
			final Vec3 pos = VSGameUtilsKt.toWorldCoordinates(level, blockPos.getCenter());
			this.fakePlayer.moveTo(pos.x, pos.y, pos.z);
			level.addNewPlayer(this.fakePlayer);
		}
		return this.playerData;
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
			if (this.fakePlayer != null) {
				this.fakePlayer.discard();
				this.fakePlayer = null;
				this.playerData = null;
			}
			this.setRemoved();
			return;
		}
		if (this.isDeactivating()) {
			return;
		}
		if (this.activating <= 1) {
			final int newEnergy = this.energyStored - this.getEnergyConsumeRate();
			if (newEnergy >= 0) {
				this.activating = 21;
				this.energyStored = newEnergy;
				this.setChanged();
			}
		}
		if (!this.isRunning()) {
			if (this.wasActivated) {
				this.wasActivated = false;
				this.queueDeactivate();
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
		this.fakePlayer.setOldPosAndRot();
		this.fakePlayer.setPos(pos.x, pos.y, pos.z);
		level.getChunkSource().move(this.fakePlayer);
	}

	public void onActivated() {
		final ServerLevel level = (ServerLevel)(this.getLevel());
		ChunkLoaderManager.get(level).activateChunkLoader(this.getBlockPos());
	}

	public void queueDeactivate() {
		final ServerLevel level = (ServerLevel)(this.getLevel());
		final BlockPos blockPos = this.getBlockPos();
		PlatformHelper.get().queueTask(20, () -> {
			ChunkLoaderManager.get(level).deactivateChunkLoader(blockPos);
			if (this.playerData != null) {
				level.removePlayerImmediately(this.fakePlayer, Entity.RemovalReason.UNLOADED_WITH_PLAYER);
				this.fakePlayer = null;
				this.playerData = null;
			}
		});
	}
}
