// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vschunkloader.platform;

import com.github.litermc.vschunkloader.util.IChunkLoaderFakePlayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;

public final class FakePlayer extends net.fabricmc.fabric.api.entity.FakePlayer implements IChunkLoaderFakePlayer {
	private Vec3 position;
	private int countDown = 21;
	private int discarding = 0;

	private FakePlayer(ServerLevel serverLevel, GameProfile gameProfile) {
		super(serverLevel, gameProfile);
	}

	static FakePlayer create(ServerLevel serverLevel, GameProfile profile) {
		return new FakePlayer(serverLevel, profile);
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
	public void bindPosition(final Vec3 position) {
		this.position = position;
		this.moveTo(position);
	}

	@Override
	public void refreshCountDown() {
		this.countDown = 21;
	}

	@Override
	public void startDiscard() {
		this.discarding = 1;
	}

	@Override
	public boolean isDiscarding() {
		return this.discarding > 0;
	}

	@Override
	public void tick() {
		if (this.discarding > 0) {
			this.discarding++;
			if (this.discarding > 20) {
				this.discard();
			}
			return;
		}
		if (this.countDown <= 0) {
			this.startDiscard();
			return;
		}
		this.setOldPosAndRot();
		this.setPos(position);
		this.countDown--;
		this.serverLevel().getChunkSource().move(this);
	}
}
