package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.block.ChunkLoaderBlockEntity;
import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vschunkloader.util.ChunkLoaderManager;

import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.apigame.world.IPlayer;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

import java.util.HashSet;
import java.util.Set;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ShipObjectServerWorld.class)
public abstract class MixinShipObjectServerWorld {
	@ModifyVariable(method = "setPlayers", at = @At("HEAD"), remap = false)
	public Set<? extends IPlayer> setPlayers(final Set<? extends IPlayer> players) {
		final HashSet<IPlayer> playerSet = new HashSet<>(players);
		for (final ServerLevel level : PlatformHelper.get().getCurrentServer().getAllLevels()) {
			ChunkLoaderManager.get(level).streamActiveChunkLoaders()
				.map(ChunkLoaderBlockEntity::getOrCreatePlayerData)
				.forEach(playerSet::add);
		}
		return playerSet;
	}
}
