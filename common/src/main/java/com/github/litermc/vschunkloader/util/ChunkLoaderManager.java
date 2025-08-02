package com.github.litermc.vschunkloader.util;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.block.ChunkLoaderBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class ChunkLoaderManager extends SavedData {
	private static final String DATA_NAME = Constants.MOD_ID + "_ChunkLoaders";
	private static final String POSITIONS_KEY = "Positions";

	private final ServerLevel level;
	private final Map<BlockPos, ChunkLoaderPlayerHolder> chunkLoaders = new HashMap<>();

	private ChunkLoaderManager(final ServerLevel level) {
		this.level = level;
	}

	public static ChunkLoaderManager get(final ServerLevel level) {
		return level.getDataStorage().computeIfAbsent((data) -> ChunkLoaderManager.load(level, data), () -> new ChunkLoaderManager(level), DATA_NAME);
	}

	private static ChunkLoaderManager load(final ServerLevel level, final CompoundTag data) {
		final ChunkLoaderManager manager = new ChunkLoaderManager(level);
		for (final long posLong : data.getLongArray(POSITIONS_KEY)) {
			final BlockPos pos = BlockPos.of(posLong);
			manager.chunkLoaders.put(pos, ChunkLoaderPlayerHolder.createForBlock(level, pos));
		}
		return manager;
	}

	@Override
	public CompoundTag save(final CompoundTag data) {
		data.putLongArray(POSITIONS_KEY, this.chunkLoaders.keySet().stream()
			.filter((pos) -> this.level.getBlockEntity(pos) instanceof ChunkLoaderBlockEntity chunkLoader && chunkLoader.isRunning())
			.mapToLong(BlockPos::asLong)
			.toArray());
		return data;
	}

	public void refreshChunkLoader(final BlockPos pos) {
		final ChunkLoaderPlayerHolder holder = this.chunkLoaders.compute(pos, (p, holde) -> {
			if (holde == null || holde.isDiscarding()) {
				this.setDirty();
				holde = ChunkLoaderPlayerHolder.createForBlock(this.level, p);
			}
			return holde;
		});
		holder.refresh();
	}

	public void deactivateChunkLoader(final BlockPos pos) {
		final ChunkLoaderPlayerHolder holder = this.chunkLoaders.remove(pos);
		if (holder != null) {
			holder.discard();
			this.setDirty();
		}
	}

	public Stream<ChunkLoaderPlayerHolder> streamActiveChunkLoaders() {
		return this.chunkLoaders.values().stream()
			.filter(Predicate.not(ChunkLoaderPlayerHolder::isDiscarding));
	}
}
