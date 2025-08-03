// SPDX-FileCopyrightText: 2022 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vschunkloader.platform;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.util.IChunkLoaderFakePlayer;

import com.mojang.authlib.GameProfile;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;

public final class FakePlayer extends net.minecraftforge.common.util.FakePlayer implements IChunkLoaderFakePlayer {
	private static final int COUNTDOWN = 21;
	private static final int DISCARD_TIMEOUT = 30;
	private static final EntityDimensions DIMENSIONS = EntityDimensions.fixed(0, 0);

	private Vec3 position;
	private int countDown = COUNTDOWN;
	private int discarding = 0;
	private Runnable discardCallback = null;

	private FakePlayer(ServerLevel serverLevel, GameProfile gameProfile) {
		super(serverLevel, gameProfile);
		this.setInvulnerable(true);
		this.refreshDimensions();
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
	public boolean isAttackable() {
		return false;
	}

	@Override
	public boolean isInvulnerable() {
		return true;
	}

	@Override
	public boolean canBeSeenAsEnemy() {
		return false;
	}

	@Override
	public boolean canBeSeenByAnyone() {
		return false;
	}

	@Override
	public boolean isPickable() {
		return false;
	}

	@Override
	public boolean isPushable() {
		return false;
	}

	@Override
	public boolean isInvisible() {
		return false;
	}

	@Override
	public boolean isAffectedByPotions() {
		return false;
	}

	@Override
	public boolean attackable() {
		return false;
	}

	@Override
	public boolean canTakeItem(final ItemStack stack) {
		return false;
	}

	@Override
	public EntityDimensions getDimensions(final Pose pose) {
		return DIMENSIONS;
	}

	@Override
	public float getEyeHeight(final Pose pose) {
		return 0;
	}

	@Override
	public float getStandingEyeHeight(final Pose pose, final EntityDimensions dims) {
		return 0;
	}

	@Override
	public void bindPosition(final Vec3 position) {
		this.position = position;
		this.moveTo(position);
	}

	@Override
	public void setDiscardCallback(final Runnable callback) {
		this.discardCallback = callback;
	}

	@Override
	public void refreshCountDown() {
		this.countDown = COUNTDOWN;
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
			if (this.discarding > DISCARD_TIMEOUT) {
				this.discard();
				Constants.LOG.debug("FakePlayer: Discarded: {} {}", this, this.position);
				if (this.discardCallback != null) {
					this.discardCallback.run();
				}
			}
			return;
		}
		if (this.countDown <= 0) {
			Constants.LOG.debug("FakePlayer: Discarding due to out of time: {} {}", this, this.position);
			this.startDiscard();
			return;
		}
		Constants.LOG.debug("FakePlayer: ticking {} {} {}", this.countDown, this, this.position);
		this.setOldPosAndRot();
		this.setPos(this.position);
		this.countDown--;
		this.serverLevel().getChunkSource().move(this);
	}
}
