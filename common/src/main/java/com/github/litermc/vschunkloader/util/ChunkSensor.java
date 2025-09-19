package com.github.litermc.vschunkloader.util;

import com.github.litermc.vschunkloader.accessor.ServerLevelAccessor;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public final class ChunkSensor {
	private final Set<ChunkPos> loadedChunks = ConcurrentHashMap.newKeySet();

	/**
	 * module-private
	 * DO NOT use. Use {@link get} instead.
	 */
	public ChunkSensor() {}

	public static ChunkSensor get(final ServerLevel level) {
		return ((ServerLevelAccessor) (level)).vsc$getChunkSensor();
	}

	public boolean isChunkLoaded(final ChunkPos pos) {
		return this.loadedChunks.contains(pos);
	}

	public boolean isChunkLoaded(final int x, final int z) {
		return this.isChunkLoaded(new ChunkPos(x, z));
	}

	/**
	 * module-private
	 * @param pos
	 */
	public void onChunkLoaded(final ChunkPos pos) {
		this.loadedChunks.add(pos);
	}

	/**
	 * module-private
	 * @param pos
	 */
	public void onChunkUnload(final ChunkPos pos) {
		this.loadedChunks.remove(pos);
	}
}
