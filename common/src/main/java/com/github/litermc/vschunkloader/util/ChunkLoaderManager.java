package com.github.litermc.vschunkloader.util;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.block.ChunkLoaderBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector3dc;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Stream;

public final class ChunkLoaderManager extends SavedData {
	private static final String DATA_NAME = Constants.MOD_ID + "_ChunkLoaders";
	private static final String POSITIONS_KEY = "Positions";

	private final ServerLevel level;
	private final Map<BlockPos, ChunkLoaderPlayerHolder> chunkLoaders = new ConcurrentHashMap<>();
	private final Map<Long, ChunkLoaderPlayerHolder> forcedShips = new ConcurrentHashMap<>();
	private final Map<ChunkPos, ChunkLoaderPlayerHolder> pingingRegions = new ConcurrentHashMap<>();

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
			manager.chunkLoaders.put(pos, manager.createChunkLoaderHolder(pos));
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

	public Stream<ChunkLoaderPlayerHolder> streamChunkLoaders() {
		return Stream.of(
			this.chunkLoaders.values().stream(),
			this.forcedShips.values().stream(),
			this.pingingRegions.values().stream()
		)
			.flatMap(Function.identity());
	}

	public void refreshChunkLoader(final BlockPos pos) {
		final ChunkLoaderPlayerHolder holder = this.chunkLoaders.compute(pos, (p, oldHolder) -> {
			final boolean noOld = oldHolder == null;
			if (noOld || oldHolder.isDiscarding()) {
				if (noOld) {
					this.setDirty();
				} else {
					oldHolder.setDiscardCallback(null);
				}
				oldHolder = this.createChunkLoaderHolder(p);
			}
			return oldHolder;
		});
		holder.refresh();
	}

	public void deactivateChunkLoader(final BlockPos pos) {
		final ChunkLoaderPlayerHolder holder = this.chunkLoaders.get(pos);
		if (holder != null) {
			holder.discard();
		}
	}

	private ChunkLoaderPlayerHolder createChunkLoaderHolder(final BlockPos pos) {
		final ChunkLoaderPlayerHolder holder = ChunkLoaderPlayerHolder.createForBlock(this.level, pos);
		holder.setDiscardCallback(() -> {
			if (this.chunkLoaders.remove(pos, holder)) {
				this.setDirty();
			}
		});
		return holder;
	}

	public void refreshForcedShip(final ServerShip ship) {
		final AABBic box = ship.getShipAABB();
		if (box == null) {
			return;
		}

		final Vec3 position = new Vec3((box.maxX() + box.minX()) / 2, (box.maxY() + box.minY()) / 2, (box.maxZ() + box.minZ()) / 2);
		final ChunkLoaderPlayerHolder holder = this.forcedShips.compute(ship.getId(), (id, oldHolder) -> {
			if (oldHolder != null) {
				if (!oldHolder.isDiscarding()) {
					oldHolder.setPosition(position);
					return oldHolder;
				}
				oldHolder.setDiscardCallback(null);
			}
			final ChunkLoaderPlayerHolder newHolder = ChunkLoaderPlayerHolder.createForShip(this.level, id, position);
			newHolder.setDiscardCallback(() -> this.forcedShips.remove(id, newHolder));
			return newHolder;
		});
		holder.refresh();
	}

	public void pingChunks(final int minX, final int maxX, final int minZ, final int maxZ) {
		final int
			minRX = this.chunkToRegionPos(minX), maxRX = this.chunkToRegionPos(maxX),
			minRZ = this.chunkToRegionPos(minZ), maxRZ = this.chunkToRegionPos(maxZ);
		for (int x = minRX; x <= maxRX; x++) {
			for (int z = minRZ; z <= maxRZ; z++) {
				this.pingRegion(x, z);
			}
		}
	}

	private int getRegionSize() {
		return this.level.getServer().getPlayerList().getSimulationDistance() * 2 - 1;
	}

	private int chunkToRegionPos(final int n) {
		return Math.floorDiv(n, this.getRegionSize());
	}

	private int regionToChunkPos(final int n) {
		final int size = this.getRegionSize();
		return n * size + size / 2;
	}

	private void pingRegion(final int x, final int z) {
		final ChunkPos pos0 = new ChunkPos(regionToChunkPos(x), regionToChunkPos(z));
		final ChunkLoaderPlayerHolder holder = this.pingingRegions.compute(pos0, (pos, oldHolder) -> {
			if (oldHolder != null) {
				if (!oldHolder.isDiscarding()) {
					return oldHolder;
				}
				oldHolder.setDiscardCallback(null);
			}
			final ChunkLoaderPlayerHolder newHolder = ChunkLoaderPlayerHolder.createFixed(this.level, new Vec3(pos.getMiddleBlockX(), 0, pos.getMiddleBlockZ()));
			newHolder.setDiscardCallback(() -> this.pingingRegions.remove(pos, newHolder));
			return newHolder;
		});
		holder.refresh();
	}
}
