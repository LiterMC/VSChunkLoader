package com.github.litermc.vschunkloader.util;

import net.minecraft.world.level.ChunkPos;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.internal.world.VsiPlayer;
import org.valkyrienskies.core.internal.world.chunks.VsiChunkUnwatchTask;
import org.valkyrienskies.core.internal.world.chunks.VsiChunkWatchTask;
import org.valkyrienskies.core.internal.world.chunks.VsiChunkWatchTasks;

import java.util.SortedSet;
import java.util.TreeSet;
import java.util.Collections;

public final class ChunkWatchTasksImpl implements VsiChunkWatchTasks {
	private final SortedSet<VsiChunkWatchTask> watchTasks;
	private final SortedSet<VsiChunkUnwatchTask> unwatchTasks;

	public ChunkWatchTasksImpl(final SortedSet<VsiChunkWatchTask> watchTasks, final SortedSet<VsiChunkUnwatchTask> unwatchTasks) {
		this.watchTasks = watchTasks == null ? Collections.emptySortedSet() : watchTasks;
		this.unwatchTasks = unwatchTasks == null ? Collections.emptySortedSet() : unwatchTasks;
	}

	public static VsiChunkWatchTasks merge(final VsiChunkWatchTasks a, final VsiChunkWatchTasks b) {
		return new ChunkWatchTasksImpl(mergeWatchSet(a.getWatchTasks(), b.getWatchTasks()), mergeUnwatchSet(a.getUnwatchTasks(), b.getUnwatchTasks()));
	}

	@Override
	public SortedSet<VsiChunkWatchTask> getWatchTasks() {
		return this.watchTasks;
	}

	@Override
	public SortedSet<VsiChunkUnwatchTask> getUnwatchTasks() {
		return this.unwatchTasks;
	}

	public static SortedSet<VsiChunkWatchTask> mergeWatchSet(final SortedSet<VsiChunkWatchTask> a, final SortedSet<VsiChunkWatchTask> b) {
		if (a.isEmpty()) {
			return b;
		}
		if (b.isEmpty()) {
			return a;
		}
		final SortedSet<VsiChunkWatchTask> set = new TreeSet<>((x, y) -> Long.compare(x.getChunkPos(), y.getChunkPos()));
		set.addAll(a);
		set.addAll(b);
		return set;
	}

	public static SortedSet<VsiChunkUnwatchTask> mergeUnwatchSet(final SortedSet<VsiChunkUnwatchTask> a, final SortedSet<VsiChunkUnwatchTask> b) {
		if (a.isEmpty()) {
			return b;
		}
		if (b.isEmpty()) {
			return a;
		}
		final SortedSet<VsiChunkUnwatchTask> set = new TreeSet<>((x, y) -> Long.compare(x.getChunkPos(), y.getChunkPos()));
		set.addAll(a);
		set.addAll(b);
		return set;
	}

	public static final class ChunkUnwatchTaskImpl implements VsiChunkUnwatchTask {
		private final ChunkPos chunkPos;
		private final String dimension;
		private final Iterable<VsiPlayer> players;
		private final boolean shouldUnload;
		private final ServerShip ship;

		public ChunkUnwatchTaskImpl(final ChunkPos chunkPos, final String dimension, final Iterable<VsiPlayer> players, final boolean shouldUnload, final ServerShip ship) {
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
		public Iterable<VsiPlayer> getPlayersNeedUnwatching() {
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
		public int compareTo(final VsiChunkUnwatchTask task) {
			return 0;
		}
	}
}
