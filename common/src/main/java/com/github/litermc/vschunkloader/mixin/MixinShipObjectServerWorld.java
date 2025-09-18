package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vschunkloader.util.ChunkLoaderManager;
import com.github.litermc.vschunkloader.util.ChunkLoaderPlayerHolder;
import com.github.litermc.vschunkloader.util.ChunkWatchTasksImpl;
import com.github.litermc.vschunkloader.util.Pair;
import com.github.litermc.vschunkloader.util.TaskUtil;
import com.github.litermc.vschunkloader.util.Utils;

import net.minecraft.network.protocol.game.ClientboundLevelChunkWithLightPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.phys.Vec3;

import org.joml.Vector2i;
import org.joml.Vector3dc;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.properties.IShipActiveChunksSet;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.world.IPlayer;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.apigame.world.chunks.ChunkUnwatchTask;
import org.valkyrienskies.core.apigame.world.chunks.ChunkWatchTask;
import org.valkyrienskies.core.apigame.world.chunks.ChunkWatchTasks;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServerWorld;
import org.valkyrienskies.core.impl.networking.simple.SimplePackets;
import org.valkyrienskies.mod.common.networking.PacketRestartChunkUpdates;
import org.valkyrienskies.mod.common.util.MinecraftPlayer;
import org.valkyrienskies.mod.mixin.accessors.server.level.ChunkMapAccessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.Predicate;

@Mixin(ShipObjectServerWorld.class)
public abstract class MixinShipObjectServerWorld implements ServerShipWorldCore {
	@Unique
	private final Set<IPlayer> disconnectedPlayers = new HashSet<>();
	@Unique
	private final Map<Long, String> teleportedShips = new HashMap<>();
	@Unique
	private final SortedSet<ChunkUnwatchTask> pendingUnwatchTasks = new TreeSet<>((a, b) -> Long.compare(a.getChunkPos(), b.getChunkPos()));

	@ModifyVariable(method = "setPlayers", at = @At("HEAD"), remap = false)
	public Set<? extends IPlayer> setPlayers$head(final Set<? extends IPlayer> players) {
		final HashSet<IPlayer> playerSet = new HashSet<>(players);
		for (final ServerLevel level : PlatformHelper.get().getCurrentServer().getAllLevels()) {
			ChunkLoaderManager.get(level).streamChunkLoaders()
				.map(ChunkLoaderPlayerHolder::getPlayerData)
				.forEach(playerSet::add);
		}
		return Collections.unmodifiableSet(playerSet);
	}

	@Inject(method = "onDisconnect", at = @At("RETURN"), remap = false)
	public void onDisconnect(final IPlayer player, final CallbackInfo ci) {
		this.disconnectedPlayers.add(player);
	}

	@Inject(method = "teleportShip", at = @At("HEAD"), remap = false)
	public void teleportShip(final ServerShip ship, final ShipTeleportData teleportData, final CallbackInfo ci) {
		final long shipId = ship.getId();
		final String dimId = teleportData.getNewDimension();
		final String shipDim = ship.getChunkClaimDimension();
		if (dimId == null || dimId.equals(shipDim)) {
			return;
		}
		this.teleportedShips.put(ship.getId(), shipDim);
		ship.getActiveChunksSet().forEach((x, z) -> {
			final HashSet<IPlayer> players = new HashSet<>();
			this.getIPlayersWatchingShipChunk(x, z, shipDim).forEachRemaining(players::add);
			if (!players.isEmpty()) {
				this.pendingUnwatchTasks.add(new ChunkWatchTasksImpl.ChunkUnwatchTaskImpl(new ChunkPos(x, z), shipDim, players, true, ship));
			}
		});
		final ServerLevel level = Utils.getLevel(dimId);
		if (level == null) {
			return;
		}
		final Vector3dc pos = teleportData.getNewPos();
		ChunkLoaderPlayerHolder.createFixed(level, new Vec3(pos.x(), pos.y(), pos.z()));
	}

	@Inject(method = "getChunkWatchTasks", at = @At("RETURN"), remap = false, cancellable = true)
	public void getChunkWatchTasks(final CallbackInfoReturnable<ChunkWatchTasks> cir) {
		if (this.disconnectedPlayers.isEmpty() && this.teleportedShips.isEmpty() && this.pendingUnwatchTasks.isEmpty()) {
			return;
		}
		final ChunkWatchTasks oldWatchTasks = cir.getReturnValue();
		final SortedSet<ChunkUnwatchTask> unwatchTasks = new TreeSet<>((a, b) -> Long.compare(a.getChunkPos(), b.getChunkPos()));
		unwatchTasks.addAll(this.pendingUnwatchTasks);
		this.pendingUnwatchTasks.clear();
		if (!this.disconnectedPlayers.isEmpty()) {
			for (final LoadedServerShip ship : this.getLoadedShips()) {
				final String dim = ship.getChunkClaimDimension();
				ship.getActiveChunksSet().forEach((x, z) -> {
					final HashSet<IPlayer> players = new HashSet<>(this.disconnectedPlayers);
					final HashSet<IPlayer> oldPlayers = new HashSet<>();
					this.getIPlayersWatchingShipChunk(x, z, dim).forEachRemaining(oldPlayers::add);
					players.removeIf(Predicate.not(oldPlayers::contains));
					final boolean shouldUnload = players.size() == oldPlayers.size();
					unwatchTasks.add(new ChunkWatchTasksImpl.ChunkUnwatchTaskImpl(new ChunkPos(x, z), dim, players, shouldUnload, ship));
				});
			}
			this.disconnectedPlayers.clear();
		}
		if (!this.teleportedShips.isEmpty()) {
			final Set<ServerShip> teleportedShipSet = new HashSet<>(this.teleportedShips.size());
			this.teleportedShips.forEach((shipId, oldDim) -> {
				final ServerShip newShip = this.getAllShips().getById(shipId);
				if (newShip == null) {
					return;
				}
				final String newDim = newShip.getChunkClaimDimension();
				if (oldDim.equals(newDim)) {
					return;
				}
			});
			this.teleportedShips.clear();
			oldWatchTasks.getUnwatchTasks().removeIf((t) ->
				teleportedShipSet.contains(this.getAllShips().getByChunkPos(t.getChunkX(), t.getChunkZ(), t.getDimensionId()))
			);
		}
		final ChunkWatchTasks newWatchTasks = ChunkWatchTasksImpl.merge(oldWatchTasks, new ChunkWatchTasksImpl(null, unwatchTasks));
		cir.setReturnValue(newWatchTasks);
	}
}
