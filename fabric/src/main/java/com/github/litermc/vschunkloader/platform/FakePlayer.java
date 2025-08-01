// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vschunkloader.platform;

import com.github.litermc.vschunkloader.util.IChunkLoaderFakePlayer;
import com.github.litermc.vschunkloader.block.ChunkLoaderBlockEntity;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FakePlayer extends net.fabricmc.fabric.api.entity.FakePlayer implements IChunkLoaderFakePlayer {
	private ChunkLoaderBlockEntity be;

	private FakePlayer(ServerLevel serverLevel, GameProfile gameProfile) {
		super(serverLevel, gameProfile);
	}

	static FakePlayer create(ServerLevel serverLevel, GameProfile profile) {
		return new FakePlayer(serverLevel, profile);
	}

	@Override
	public void bindChunkLoader(ChunkLoaderBlockEntity be) {
		this.be = be;
	}

	@Override
	public ChunkLoaderBlockEntity getChunkLoader() {
		return this.be;
	}

	@Override
	protected int getPermissionLevel() {
		return 0;
	}

	@Override
	public boolean broadcastToPlayer(ServerPlayer player) {
		return false;
	}

	@Override
	public void tick() {
		if (this.be == null || this.be.isRemoved()) {
			this.discard();
		}
	}
}
