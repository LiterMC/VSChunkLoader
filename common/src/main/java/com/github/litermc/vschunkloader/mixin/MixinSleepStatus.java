package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.platform.PlatformHelper;

import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.SleepStatus;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(SleepStatus.class)
public class MixinSleepStatus {
	@WrapOperation(
		method = "update",
		at = @At(value = "INVOKE", target = "Lnet/minecraft/server/level/ServerPlayer;isSpectator()Z")
	)
	public boolean update$isSpectator(final ServerPlayer player, final Operation<Boolean> operation) {
		return operation.call(player) || PlatformHelper.get().isFakePlayer(player);
	}
}
