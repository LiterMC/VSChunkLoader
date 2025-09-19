package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.accessor.ChunkMapAccessor;
import com.github.litermc.vschunkloader.accessor.ServerLevelAccessor;
import com.github.litermc.vschunkloader.util.ChunkSensor;

import net.minecraft.server.level.ServerChunkCache;
import net.minecraft.server.level.ServerLevel;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;

@Mixin(ServerLevel.class)
public abstract class MixinServerLevel implements ServerLevelAccessor {
	@Shadow
	public abstract ServerChunkCache getChunkSource();

	@Override
	public ChunkSensor vsc$getChunkSensor() {
		return ((ChunkMapAccessor) (this.getChunkSource().chunkMap)).vsc$getChunkSensor();
	}
}
