package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.platform.PlatformHelper;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.players.PlayerList;
import net.minecraft.stats.ServerStatsCounter;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.storage.LevelResource;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.io.File;

@Mixin(PlayerList.class)
public class MixinPlayerList {
	@Shadow
	@Final
	private MinecraftServer server;

	@Inject(method = "getPlayerStats", at = @At("HEAD"), cancellable = true)
	public void getPlayerStats(final Player player, final CallbackInfoReturnable<ServerStatsCounter> cir) {
		if (!(player instanceof final ServerPlayer serverPlayer)) {
			return;
		}
		if (!PlatformHelper.get().isSpecialFakePlayer(serverPlayer)) {
			return;
		}
		cir.setReturnValue(new ServerStatsCounter(
			this.server,
			new File(
				this.server.getWorldPath(LevelResource.PLAYER_STATS_DIR).toFile(),
				player.getUUID() + ".json"
			)
		));
	}
}
