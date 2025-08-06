package com.github.litermc.vschunkloader.util;

import com.github.litermc.vschunkloader.platform.PlatformHelper;

import com.mojang.authlib.GameProfile;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

import org.valkyrienskies.core.apigame.world.IPlayer;
import org.valkyrienskies.mod.common.util.MinecraftPlayer;

import java.util.UUID;
import java.nio.charset.StandardCharsets;

public final class ChunkLoaderPlayerHolder {
	private final ServerLevel level;
	private final GameProfile fakeGameProfile;
	private Vec3 position;
	private IChunkLoaderFakePlayer fakePlayer;
	private IPlayer playerData;

	private ChunkLoaderPlayerHolder(final ServerLevel level, final Vec3 position, final String name) {
		this.level = level;
		this.position = position;
		final UUID uuid = UUID.nameUUIDFromBytes(name.getBytes(StandardCharsets.UTF_8));
		this.fakeGameProfile = new GameProfile(uuid, name);
		final ServerPlayer fakePlayer = PlatformHelper.get().createFakePlayer(this.level, this.fakeGameProfile);
		this.fakePlayer = ((IChunkLoaderFakePlayer)(fakePlayer));
		this.playerData = new MinecraftPlayer(fakePlayer);

		this.fakePlayer.bindPosition(this.position);
		fakePlayer.moveTo(this.position);
		this.level.addNewPlayer(fakePlayer);
	}

	public static ChunkLoaderPlayerHolder createForBlock(final ServerLevel level, final BlockPos blockPos) {
		final String name = "ChunkLoader:" + level.dimension().location().toString() + "#" + blockPos.toString();
		return new ChunkLoaderPlayerHolder(level, blockPos.getCenter(), name);
	}

	public static ChunkLoaderPlayerHolder createFixed(final ServerLevel level, final Vec3 position) {
		final String name = "ChunkLoaderFixed:" + level.dimension().location().toString() + "#" + position.toString();
		return new ChunkLoaderPlayerHolder(level, position, name);
	}

	public static ChunkLoaderPlayerHolder createForShip(final ServerLevel level, final long shipId, final Vec3 position) {
		final String name = "ChunkLoaderShip:" + level.dimension().location().toString() + "#" + shipId;
		return new ChunkLoaderPlayerHolder(level, position, name);
	}

	public IPlayer getPlayerData() {
		return this.playerData;
	}

	public void setDiscardCallback(final Runnable callback) {
		this.fakePlayer.setDiscardCallback(callback);
	}

	public void setPosition(final Vec3 position) {
		this.position = position;
		this.fakePlayer.bindPosition(position);
	}

	public void refresh() {
		this.fakePlayer.refreshCountDown();
	}

	public void discard() {
		this.fakePlayer.startDiscard();
	}

	public boolean isDiscarding() {
		return this.fakePlayer.isDiscarding();
	}
}
