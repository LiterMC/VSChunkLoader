package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vschunkloader.util.AdvancedBitSet;
import com.github.litermc.vschunkloader.util.ChunkLoaderManager;
import com.github.litermc.vschunkloader.util.ChunkLoaderPlayerHolder;
import com.github.litermc.vschunkloader.util.ChunkSensor;
import com.github.litermc.vschunkloader.util.ChunkWatchTasksImpl;
import com.github.litermc.vschunkloader.util.TaskUtil;
import com.github.litermc.vtil.util.LevelUtil;

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
import org.valkyrienskies.core.internal.ShipTeleportData;
import org.valkyrienskies.core.internal.world.VsiPlayer;
import org.valkyrienskies.core.internal.world.VsiServerShipWorld;
import org.valkyrienskies.core.internal.world.chunks.VsiChunkUnwatchTask;
import org.valkyrienskies.core.internal.world.chunks.VsiChunkWatchTasks;
import org.valkyrienskies.mod.common.VSGameUtilsKt;
import org.valkyrienskies.mod.common.util.MinecraftPlayer;
import org.valkyrienskies.mod.mixin.accessors.server.level.ChunkMapAccessor;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
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

@Mixin(org.valkyrienskies.core.impl.shadow.Et.class)
public abstract class MixinShipObjectServerWorld implements VsiServerShipWorld {
	@Unique
	private static final String M_clearNewUpdatedDeletedShipObjectsAndVoxelUpdates = "k";

	@Shadow(remap = false)
	@Final
	private ArrayList<org.valkyrienskies.core.impl.shadow.Et.b> k;
	@Unique
	private final Set<VsiPlayer> disconnectedPlayers = new HashSet<>();
	@Unique
	private final Map<Long, String> teleportedShips = new HashMap<>();
	@Unique
	private final SortedSet<VsiChunkUnwatchTask> pendingUnwatchTasks = new TreeSet<>((a, b) -> Long.compare(a.getChunkPos(), b.getChunkPos()));
	@Unique
	private final Map<ChunkPos, AdvancedBitSet> loadedChunks = new HashMap<>();

	@Unique
	public ArrayList<org.valkyrienskies.core.impl.shadow.Et.b> getVoxelShapeUpdatesList() {
		return this.k;
	}

	@ModifyVariable(method = "setPlayers", at = @At("HEAD"), remap = false)
	public Set<? extends VsiPlayer> setPlayers$head(final Set<? extends VsiPlayer> players) {
		final HashSet<VsiPlayer> playerSet = new HashSet<>(players);
		for (final ServerLevel level : PlatformHelper.get().getCurrentServer().getAllLevels()) {
			ChunkLoaderManager.get(level).streamChunkLoaders()
				.map(ChunkLoaderPlayerHolder::getPlayerData)
				.forEach(playerSet::add);
		}
		return Collections.unmodifiableSet(playerSet);
	}

	@Inject(method = "onDisconnect", at = @At("RETURN"), remap = false)
	public void onDisconnect(final VsiPlayer player, final CallbackInfo ci) {
		if (!(((Object) (player)) instanceof VsiPlayer)) {
			throw new RuntimeException("VsiPlayer verify failed on " + player);
		}
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
			final HashSet<VsiPlayer> players = new HashSet<>();
			this.getIPlayersWatchingShipChunk(x, z, shipDim).forEachRemaining(players::add);
			if (!players.isEmpty()) {
				this.pendingUnwatchTasks.add(new ChunkWatchTasksImpl.ChunkUnwatchTaskImpl(new ChunkPos(x, z), shipDim, players, true, ship));
			}
		});
		final ServerLevel level = LevelUtil.getLevel(dimId);
		if (level == null) {
			return;
		}
		final Vector3dc pos = teleportData.getNewPos();
		ChunkLoaderPlayerHolder.createFixed(level, new Vec3(pos.x(), pos.y(), pos.z()));
	}

	@Inject(method = "getChunkWatchTasks", at = @At("RETURN"), remap = false, cancellable = true)
	public void getChunkWatchTasks(final CallbackInfoReturnable<VsiChunkWatchTasks> cir) {
		if (this.disconnectedPlayers.isEmpty() && this.teleportedShips.isEmpty() && this.pendingUnwatchTasks.isEmpty()) {
			return;
		}
		final VsiChunkWatchTasks oldWatchTasks = cir.getReturnValue();
		final SortedSet<VsiChunkUnwatchTask> unwatchTasks = new TreeSet<>((a, b) -> Long.compare(a.getChunkPos(), b.getChunkPos()));
		unwatchTasks.addAll(this.pendingUnwatchTasks);
		this.pendingUnwatchTasks.clear();
		if (!this.disconnectedPlayers.isEmpty()) {
			for (final LoadedServerShip ship : this.getLoadedShips()) {
				final String dim = ship.getChunkClaimDimension();
				ship.getActiveChunksSet().forEach((x, z) -> {
					final HashSet<VsiPlayer> players = new HashSet<>(this.disconnectedPlayers);
					final HashSet<VsiPlayer> oldPlayers = new HashSet<>();
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
		final VsiChunkWatchTasks newWatchTasks = ChunkWatchTasksImpl.merge(oldWatchTasks, new ChunkWatchTasksImpl(null, unwatchTasks));
		cir.setReturnValue(newWatchTasks);
	}

	@Inject(method = M_clearNewUpdatedDeletedShipObjectsAndVoxelUpdates + "()V", at = @At("HEAD"), remap = false)
	public void clearNewUpdatedDeletedShipObjectsAndVoxelUpdates(final CallbackInfo ci) {
		for (final org.valkyrienskies.core.impl.shadow.Et.b updates : this.getVoxelShapeUpdatesList()) {
			final ServerLevel level = LevelUtil.getLevel(updates.a());
			if (level == null) {
				continue;
			}
			final ChunkSensor sensor = ChunkSensor.get(level);
			final int maxSectionCount = level.getSectionsCount();
			for (final org.valkyrienskies.core.impl.shadow.Ip update : updates.b()) {
				final int x = update.a(), z = update.c();
				if (VSGameUtilsKt.isChunkInShipyard(level, x, z)) {
					continue;
				}
				final int y = level.getSectionIndexFromSectionY(update.b());
				final ChunkPos pos = new ChunkPos(x, z);
				final boolean isload = update.d() != org.valkyrienskies.core.impl.shadow.It.DELETE;
				if (isload) {
					final AdvancedBitSet sections = this.loadedChunks.computeIfAbsent(pos, (pos0) -> new AdvancedBitSet(maxSectionCount));
					if (sections.set(y) && sections.count() == maxSectionCount) {
						TaskUtil.queueTickStart(() -> {
							sensor.onChunkLoaded(pos);
						});
					}
					continue;
				}
				final AdvancedBitSet sections = this.loadedChunks.get(pos);
				if (sections == null) {
					// should not happen
					continue;
				}
				if (sections.count() == maxSectionCount) {
					TaskUtil.queueTickStart(() -> {
						sensor.onChunkUnload(pos);
					});
				}
				sections.clear(y);
				if (sections.isEmpty()) {
					this.loadedChunks.remove(pos);
				}
			}
		}
	}
}
