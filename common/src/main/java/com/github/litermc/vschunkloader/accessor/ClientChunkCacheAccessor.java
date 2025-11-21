package com.github.litermc.vschunkloader.accessor;

import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import net.minecraft.world.level.chunk.LevelChunk;

public interface ClientChunkCacheAccessor {
	Long2ObjectMap<LevelChunk> vsc$getShipChunks();
}
