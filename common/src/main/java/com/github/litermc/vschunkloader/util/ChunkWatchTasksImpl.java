package com.github.litermc.vschunkloader.util;

import net.minecraft.world.level.ChunkPos;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.apigame.world.IPlayer;
import org.valkyrienskies.core.apigame.world.chunks.ChunkUnwatchTask;
import org.valkyrienskies.core.apigame.world.chunks.ChunkWatchTask;
import org.valkyrienskies.core.apigame.world.chunks.ChunkWatchTasks;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Collections;

public final class ChunkWatchTasksImpl implements ChunkWatchTasks {
	private final SortedSet<ChunkWatchTask> watchTasks;
	private final SortedSet<ChunkUnwatchTask> unwatchTasks;

	public ChunkWatchTasksImpl(final SortedSet<ChunkWatchTask> watchTasks, final SortedSet<ChunkUnwatchTask> unwatchTasks) {
		this.watchTasks = watchTasks == null ? Collections.emptySortedSet() : watchTasks;
		this.unwatchTasks = unwatchTasks == null ? Collections.emptySortedSet() : unwatchTasks;
	}

	public static ChunkWatchTasks merge(final ChunkWatchTasks a, final ChunkWatchTasks b) {
		return new ChunkWatchTasksImpl(mergeWatchSet(a.getWatchTasks(), b.getWatchTasks()), mergeUnwatchSet(a.getUnwatchTasks(), b.getUnwatchTasks()));
	}

	@Override
	public SortedSet<ChunkWatchTask> getWatchTasks() {
		return this.watchTasks;
	}

	@Override
	public SortedSet<ChunkUnwatchTask> getUnwatchTasks() {
		return this.unwatchTasks;
	}

	public static SortedSet<ChunkWatchTask> mergeWatchSet(final SortedSet<ChunkWatchTask> a, final SortedSet<ChunkWatchTask> b) {
		if (a.isEmpty()) {
			return b;
		}
		if (b.isEmpty()) {
			return a;
		}
		final SortedSet<ChunkWatchTask> set = new TreeSet<>((x, y) -> Long.compare(x.getChunkPos(), y.getChunkPos()));
		set.addAll(a);
		set.addAll(b);
		return set;
	}

	public static SortedSet<ChunkUnwatchTask> mergeUnwatchSet(final SortedSet<ChunkUnwatchTask> a, final SortedSet<ChunkUnwatchTask> b) {
		if (a.isEmpty()) {
			return b;
		}
		if (b.isEmpty()) {
			return a;
		}
		final SortedSet<ChunkUnwatchTask> set = new TreeSet<>((x, y) -> Long.compare(x.getChunkPos(), y.getChunkPos()));
		set.addAll(a);
		set.addAll(b);
		return set;
	}

	public static final class ChunkUnwatchTaskImpl implements ChunkUnwatchTask {
		private final ChunkPos chunkPos;
		private final String dimension;
		private final Iterable<IPlayer> players;
		private final boolean shouldUnload;
		private final ServerShip ship;

		public ChunkUnwatchTaskImpl(final ChunkPos chunkPos, final String dimension, final Iterable<IPlayer> players, final boolean shouldUnload, final ServerShip ship) {
			this.chunkPos = chunkPos;
			this.dimension = dimension;
			this.players = players;
			this.shouldUnload = shouldUnload;
			this.ship = ship;
		}

		@Override
		public long getChunkPos() {
			return this.chunkPos.toLong();
		}

		@Override
		public String getDimensionId() {
			return this.dimension;
		}

		@Override
		public Iterable<IPlayer> getPlayersNeedUnwatching() {
			return this.players;
		}

		@Override
		public boolean getShouldUnload() {
			return this.shouldUnload;
		}

		@Override
		public ServerShip getShip() {
			return this.ship;
		}

		@Override
		public int getChunkX() {
			return this.chunkPos.x;
		}

		@Override
		public int getChunkZ() {
			return this.chunkPos.z;
		}

		@Override
		public int compareTo(final ChunkUnwatchTask task) {
			return 0;
		}
	}
}
