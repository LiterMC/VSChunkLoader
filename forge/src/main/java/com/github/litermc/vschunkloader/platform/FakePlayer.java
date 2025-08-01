// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vschunkloader.platform;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

public final class FakePlayer extends net.minecraftforge.common.util.FakePlayer {
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
}
