package com.github.litermc.vschunkloader;

import com.github.litermc.vschunkloader.attachment.ChunkSensorAttachment;
import com.github.litermc.vschunkloader.config.Config;
import com.github.litermc.vschunkloader.util.ChunkLoaderManager;
import com.github.litermc.vschunkloader.util.ChunkSensor;
import com.github.litermc.vschunkloader.util.TaskUtil;
import com.github.litermc.vschunkloader.util.Utils;

import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.chunk.LevelChunk;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.ServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class VSCListeners {
	private VSCListeners() {}

	public static void onServerLevelLoad(final ServerLevel level) {
		Utils.onServerLevelLoad(level);
		ChunkLoaderManager.get(level);
	}

	public static void onServerLevelUnload(final ServerLevel level) {
		Utils.onServerLevelUnload(level);
	}

	public static void preServerTick(final MinecraftServer server) {
		TaskUtil.preServerTick();
		final ServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(server);
		for (final ServerShip ship : shipWorld.getAllShips()) {
			if (Config.forceLoadAllShips) {
				final String slug = ship.getSlug();
				if (slug != null && slug.startsWith(VSCApi.REUSABLE_SHIP_SLUG_PREFIX)) {
					continue;
				}
			} else if (!VSCApi.isForceLoaded(server, ship.getId())) {
				continue;
			}
			final ServerLevel level = Utils.getLevel(ship.getChunkClaimDimension());
			if (level == null) {
				continue;
			}
			ChunkLoaderManager.get(level).refreshForcedShip(ship);
		}

		for (final LoadedServerShip ship : shipWorld.getLoadedShips()) {
			ChunkSensorAttachment.get(ship).serverTick(ship);
		}
	}

	public static void postServerTick(final MinecraftServer server) {
		TaskUtil.postServerTick();
	}

	public static void onServerChunkLoad(final ServerLevel level, final LevelChunk chunk) {
		// final ChunkPos pos = chunk.getPos();
		// if (!VSGameUtilsKt.isChunkInShipyard(level, pos.x, pos.z)) {
		// 	ChunkSensor.get(level).onChunkLoaded(pos);
		// }
	}

	public static void onServerChunkUnload(final ServerLevel level, final LevelChunk chunk) {
		// final ChunkPos pos = chunk.getPos();
		// if (!VSGameUtilsKt.isChunkInShipyard(level, pos.x, pos.z)) {
		// 	ChunkSensor.get(level).onChunkUnload(pos);
		// }
	}
}
