package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vschunkloader.util.ChunkLoaderManager;
import com.github.litermc.vschunkloader.util.ChunkLoaderPlayerHolder;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.world.IPlayer;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@Mixin(ShipObjectServerWorld.class)
public abstract class MixinShipObjectServerWorld {
	@ModifyVariable(method = "setPlayers", at = @At("HEAD"), remap = false)
	public Set<? extends IPlayer> setPlayers(final Set<? extends IPlayer> players) {
		final HashSet<IPlayer> playerSet = new HashSet<>(players);
		for (final ServerLevel level : PlatformHelper.get().getCurrentServer().getAllLevels()) {
			ChunkLoaderManager.get(level).streamActiveChunkLoaders()
				.map(ChunkLoaderPlayerHolder::getPlayerData)
				.forEach(playerSet::add);
		}
		return Collections.unmodifiableSet(playerSet);
	}

	@Inject(method = "teleportShip", at = @At("HEAD"), remap = false)
	public void teleportShip(final ServerShip ship, final ShipTeleportData teleportData, final CallbackInfo ci) {
		final String dimId = teleportData.getNewDimension();
		if (dimId == null) {
			return;
		}
		final String[] parts = dimId.split(":");
		if (parts.length != 4) {
			return;
		}
		final ResourceKey<Level> levelId = ResourceKey.create(
			ResourceKey.createRegistryKey(new ResourceLocation(parts[0], parts[1])),
			new ResourceLocation(parts[2], parts[3])
		);
		final ServerLevel level = PlatformHelper.get().getCurrentServer().getLevel(levelId);
		if (level == null) {
			return;
		}
		final Vector3dc pos = teleportData.getNewPos();
		ChunkLoaderPlayerHolder.createFixed(level, new Vec3(pos.x(), pos.y(), pos.z()));
	}
}
