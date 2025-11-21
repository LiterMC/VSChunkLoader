/**
 * Copyright (C) 2025  the authors of ValkyrienSkies mod
 * Modified by zyxkad  2025
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.litermc.vschunkloader.mixin.client;

import com.github.litermc.vschunkloader.accessor.ClientChunkCacheAccessor;

import io.netty.util.collection.LongObjectHashMap;
import io.netty.util.collection.LongObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectMaps;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientChunkCache;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.protocol.game.ClientboundLevelChunkPacketData.BlockEntityTagOutput;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.ChunkStatus;
import net.minecraft.world.level.chunk.LevelChunk;

import org.valkyrienskies.core.api.ships.ClientShip;
import org.valkyrienskies.core.api.ships.properties.ChunkClaim;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.compat.SodiumCompat;
import org.valkyrienskies.mod.compat.VSRenderer;
import org.valkyrienskies.mod.mixin.ValkyrienCommonMixinConfigPlugin;
import org.valkyrienskies.mod.mixin.accessors.client.multiplayer.ClientLevelAccessor;
import org.valkyrienskies.mod.mixin.accessors.client.render.LevelRendererAccessor;
import org.valkyrienskies.mod.mixinducks.client.render.IVSViewAreaMethods;
import org.valkyrienskies.mod.mixinducks.client.world.ClientChunkCacheDuck;
import org.valkyrienskies.mod.mixinducks.mod_compat.vanilla_renderer.LevelRendererDuck;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.function.Consumer;

/**
 * The purpose of this mixin is to allow {@link ClientChunkCache} to store ship chunks.
 */
@Mixin(value = ClientChunkCache.class, priority = 2000)
public abstract class MixinClientChunkCache implements ClientChunkCacheAccessor, ClientChunkCacheDuck {
	@Unique
	private static final LongObjectMap<LevelChunk> EMPTY_NETTY_MAP = new LongObjectHashMap<>();

	@Shadow
	@Final
	ClientLevel level;

	@Unique
	private final Long2ObjectMap<LevelChunk> shipChunks = Long2ObjectMaps.synchronize(new Long2ObjectOpenHashMap<>());

	@Override
	public LongObjectMap<LevelChunk> vs$getShipChunks() {
		return EMPTY_NETTY_MAP;
	}

	@Override
	public Long2ObjectMap<LevelChunk> vsc$getShipChunks() {
		return this.shipChunks;
	}

	@Inject(method = "replaceWithPacketData", at = @At("HEAD"), cancellable = true)
	private void preReplaceWithPacketData(
		final int x,
		final int z,
		final FriendlyByteBuf buf,
		final CompoundTag tag,
		final Consumer<BlockEntityTagOutput> consumer, 
		final CallbackInfoReturnable<LevelChunk> cir
	) {
		if (!VSGameUtilsKt.isChunkInShipyard(this.level, x, z)) {
			return;
		}
		if (Minecraft.getInstance().levelRenderer instanceof final LevelRendererDuck levelRenderer) {
			levelRenderer.vs$setNeedsFrustumUpdate();
		}
		final ChunkPos pos = new ChunkPos(x, z);
		final long chunkPosLong = pos.toLong();
		final LevelChunk oldChunk = this.shipChunks.get(chunkPosLong);
		final LevelChunk worldChunk;
		if (oldChunk != null) {
			worldChunk = oldChunk;
			worldChunk.replaceWithPacketData(buf, tag, consumer);
		} else {
			worldChunk = new LevelChunk(this.level, pos);
			worldChunk.replaceWithPacketData(buf, tag, consumer);
			this.shipChunks.put(chunkPosLong, worldChunk);
		}

		this.level.onChunkLoaded(pos);
		SodiumCompat.onChunkAdded(this.level, x, z);
		cir.setReturnValue(worldChunk);
	}

	@Override
	public void vs$removeShip(final ClientShip ship) {
		final ChunkClaim chunks = ship.getChunkClaim();
		for (int x = chunks.getXStart(); x <= chunks.getXEnd(); x++) {
			for (int z = chunks.getZStart(); z <= chunks.getZEnd(); z++) {
				this.removeShipChunk(x, z);
			}
		}
	}

	@Unique
	private void removeShipChunk(final int chunkX, final int chunkZ) {
		final LevelChunk chunk = this.shipChunks.remove(ChunkPos.asLong(chunkX, chunkZ));
		if (chunk == null) {
			return;
		}
		this.level.unload(chunk);
		if (ValkyrienCommonMixinConfigPlugin.getVSRenderer() != VSRenderer.SODIUM) {
			((IVSViewAreaMethods) ((LevelRendererAccessor) ((ClientLevelAccessor) this.level).getLevelRenderer()).getViewArea())
				.unloadChunk(chunkX, chunkZ);
		}
		SodiumCompat.onChunkRemoved(this.level, chunkX, chunkZ);
	}

	@Inject(
		method = "getChunk(IILnet/minecraft/world/level/chunk/ChunkStatus;Z)Lnet/minecraft/world/level/chunk/LevelChunk;",
		at = @At("HEAD"),
		cancellable = true
	)
	public void preGetChunk(
		final int chunkX,
		final int chunkZ,
		final ChunkStatus chunkStatus,
		final boolean bl,
		final CallbackInfoReturnable<LevelChunk> cir
	) {
		final LevelChunk shipChunk = this.shipChunks.get(ChunkPos.asLong(chunkX, chunkZ));
		if (shipChunk != null) {
			cir.setReturnValue(shipChunk);
		}
	}
}
