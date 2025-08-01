package com.github.litermc.vschunkloader.util;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.block.ChunkLoaderBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Stream;

public final class ChunkLoaderManager extends SavedData {
	private static final String DATA_NAME = Constants.MOD_ID + "_ChunkLoaders";
	private static final String POSITIONS_KEY = "Positions";

	private final ServerLevel level;
	private final Set<BlockPos> chunkLoaders = new HashSet<>();

	private ChunkLoaderManager(final ServerLevel level) {
		this.level = level;
	}

	public static ChunkLoaderManager get(final ServerLevel level) {
		return level.getDataStorage().computeIfAbsent((data) -> ChunkLoaderManager.load(level, data), () -> new ChunkLoaderManager(level), DATA_NAME);
	}

	private static ChunkLoaderManager load(final ServerLevel level, final CompoundTag data) {
		final ChunkLoaderManager manager = new ChunkLoaderManager(level);
		for (final long posLong : data.getLongArray(POSITIONS_KEY)) {
			manager.chunkLoaders.add(BlockPos.of(posLong));
		}
		return manager;
	}

	@Override
	public CompoundTag save(final CompoundTag data) {
		data.putLongArray(POSITIONS_KEY, this.chunkLoaders.stream()
			.filter((pos) -> this.level.getBlockEntity(pos) instanceof ChunkLoaderBlockEntity chunkLoader && chunkLoader.isRunning())
			.mapToLong(BlockPos::asLong)
			.toArray());
		return data;
	}

	public void activateChunkLoader(final BlockPos pos) {
		this.chunkLoaders.add(pos);
		this.setDirty();
	}

	public void deactivateChunkLoader(final BlockPos pos) {
		System.out.println("removing: " + pos);
		this.chunkLoaders.remove(pos);
		this.setDirty();
	}

	public Stream<ChunkLoaderBlockEntity> streamActiveChunkLoaders() {
		return this.chunkLoaders.stream()
			.map(this.level::getBlockEntity)
			.filter(ChunkLoaderBlockEntity.class::isInstance)
			.map(ChunkLoaderBlockEntity.class::cast)
			.filter(ChunkLoaderBlockEntity::isRunning);
	}
}
