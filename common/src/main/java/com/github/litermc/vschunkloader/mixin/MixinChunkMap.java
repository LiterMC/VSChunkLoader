package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.accessor.ChunkMapAccessor;
import com.github.litermc.vschunkloader.util.ChunkSensor;

import net.minecraft.server.level.ChunkMap;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;


@Mixin(ChunkMap.class)
public abstract class MixinChunkMap implements ChunkMapAccessor {
	@Unique
	private final ChunkSensor chunkSensor = new ChunkSensor();

	@Override
	public ChunkSensor vsc$getChunkSensor() {
		return this.chunkSensor;
	}
}
