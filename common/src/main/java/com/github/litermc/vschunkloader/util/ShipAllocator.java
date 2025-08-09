package com.github.litermc.vschunkloader.util;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.VSCApi;
import com.github.litermc.vschunkloader.config.Config;

import it.unimi.dsi.fastutil.longs.LongIterator;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.saveddata.SavedData;

import com.google.common.collect.MutableClassToInstanceMap;
import org.joml.Quaterniond;
import org.joml.Vector3d;
import org.joml.Vector3i;
import org.joml.primitives.AABBic;
import org.valkyrienskies.core.api.ships.QueryableShipData;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.core.apigame.ShipTeleportData;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.core.impl.game.ShipTeleportDataImpl;
import org.valkyrienskies.core.impl.game.ships.ShipData;
import org.valkyrienskies.core.impl.game.ships.ShipObjectServer;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

public final class ShipAllocator extends SavedData {
	private static final String DATA_NAME = Constants.MOD_ID + "_AllocatedShips";
	private static final String CACHED_SHIPS_TAG = "CachedShips";
	private static final Vector3d UNREACHABLE_POS = new Vector3d(1e8, -1e8, 1e8);
	private static final Vector3d ZERO3D = new Vector3d();

	private final ServerLevel level;
	private final ServerShipWorldCore shipWorld;
	private final String dimId;
	private final LongOpenHashSet avaliableShips = new LongOpenHashSet();

	private ShipAllocator(final ServerLevel level) {
		this.level = level;
		this.shipWorld = VSGameUtilsKt.getShipObjectWorld(level);
		this.dimId = VSGameUtilsKt.getDimensionId(level);
	}

	public static ShipAllocator get(final ServerLevel level) {
		return level.getDataStorage().computeIfAbsent((data) -> ShipAllocator.load(level, data), () -> new ShipAllocator(level), DATA_NAME);
	}

	public static ShipAllocator load(final ServerLevel level, final CompoundTag data) {
		final ShipAllocator allocator = new ShipAllocator(level);
		final QueryableShipData<ServerShip> shipStorage = allocator.shipWorld.getAllShips();
		final long[] ids = data.getLongArray(CACHED_SHIPS_TAG);
		for (final long id : ids) {
			final ServerShip ship = shipStorage.getById(id);
			if (ship != null && allocator.dimId.equals(ship.getChunkClaimDimension())) {
				allocator.avaliableShips.add(id);
			}
		}
		return allocator;
	}

	@Override
	public CompoundTag save(final CompoundTag data) {
		data.putLongArray(CACHED_SHIPS_TAG, this.avaliableShips.longStream().filter(this.shipWorld.getAllShips()::contains).toArray());
		return data;
	}

	public boolean putShip(final ServerShip ship) {
		if (!this.dimId.equals(ship.getChunkClaimDimension())) {
			this.shipWorld.deleteShip(ship);
			return false;
		}
		ship.setSlug(VSCApi.REUSABLE_SHIP_SLUG_PREFIX + ship.getId());
		ship.setStatic(true);
		clearShip(this.level, ship);

		if (!Config.reuseShipChunks) {
			this.shipWorld.deleteShip(ship);
			return false;
		}

		final Vector3i center = ship.getChunkClaim().getCenterBlockCoordinates(VSGameUtilsKt.getYRange(this.level), new Vector3i());
		level.setBlock(new BlockPos(center.x, center.y, center.z), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_NONE);

		final ShipTeleportData teleportData = new ShipTeleportDataImpl(UNREACHABLE_POS, new Quaterniond(), ZERO3D, ZERO3D, this.dimId, 1e-6);
		this.shipWorld.teleportShip(ship, teleportData);

		Constants.LOG.debug("ShipAllocator: Caching ship {} in {}", ship.getId(), ship.getChunkClaimDimension());
		this.avaliableShips.add(ship.getId());
		this.setDirty();
		return true;
	}

	private ServerShip getShip(final long shipId) {
		final ServerShip ship = this.shipWorld.getLoadedShips().getById(shipId);
		if (ship != null) {
			return ship;
		}
		return this.shipWorld.getAllShips().getById(shipId);
	}

	private ServerShip pollCachedShip() {
		if (!Config.reuseShipChunks) {
			return null;
		}
		final LongIterator iterator = this.avaliableShips.longIterator();
		while (iterator.hasNext()) {
			this.setDirty();
			final long shipId = iterator.nextLong();
			iterator.remove();
			final ServerShip ship = this.getShip(shipId);
			if (ship != null) {
				Constants.LOG.debug("ShipAllocator: Reusing ship {}", shipId);
				clearShip(this.level, ship);
				return ship;
			}
			Constants.LOG.debug("ShipAllocator: Ignored not existing ship {}", shipId);
		}
		return null;
	}

	public ServerShip allocShip(final Vector3i pos) {
		return allocShip(pos, 1.0);
	}

	public ServerShip allocShip(final Vector3i pos, final double scale) {
		final ServerShip ship = this.pollCachedShip();
		if (ship != null) {
			final ShipTeleportData teleportData = new ShipTeleportDataImpl(new Vector3d(pos).add(0.5, 0.5, 0.5), new Quaterniond(), ZERO3D, ZERO3D, this.dimId, scale);
			this.shipWorld.teleportShip(ship, teleportData);

			ship.setStatic(false);
			ship.setSlug(null);

			return ship;
		}
		final ServerShip newShip = this.shipWorld.createNewShipAtBlock(pos, false, scale, this.dimId);
		return newShip;
	}

	private static void clearShip(final ServerLevel level, final ServerShip ship) {
		MutableClassToInstanceMap<Object> attachments = null;
		if (ship instanceof final ShipData shipData) {
			attachments = shipData.getPersistentAttachedData();
		} else if (ship instanceof final ShipObjectServer shipObject) {
			attachments = shipObject.getShipData().getPersistentAttachedData();
		}
		if (attachments != null) {
			for (final Class<?> clazz : List.copyOf(attachments.keySet())) {
				ship.saveAttachment(clazz, null);
			}
		}
		final AABBic box = ship.getShipAABB();
		if (box == null) {
			return;
		}
		for (final BlockPos pos : BlockPos.betweenClosed(box.minX(), box.minY(), box.minZ(), box.maxX(), box.maxY(), box.maxZ())) {
			level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_KNOWN_SHAPE);
		}
	}

	public static class SafeShipIterable implements Iterable<Ship> {
		private final Iterable<? extends Ship> ships;

		public SafeShipIterable(Iterable<? extends Ship> ships) {
			this.ships = ships;
		}

		@Override
		public Iterator<Ship> iterator() {
			return new SafeShipIterator(this.ships.iterator());
		}
	}

	public static class SafeShipIterator implements Iterator<Ship> {
		private final Iterator<? extends Ship> ships;
		private Ship nextShip = null;

		public SafeShipIterator(Iterator<? extends Ship> ships) {
			this.ships = ships;
		}

		@Override
		public boolean hasNext() {
			if (this.nextShip != null) {
				return true;
			}
			while (this.ships.hasNext()) {
				final Ship ship = this.ships.next();
				final String slug = ship.getSlug();
				if (slug == null || !slug.startsWith(VSCApi.REUSABLE_SHIP_SLUG_PREFIX)) {
					this.nextShip = ship;
					return true;
				}
			}
			return false;
		}

		@Override
		public Ship next() {
			if (this.nextShip == null) {
				throw new NoSuchElementException();
			}
			final Ship ship = this.nextShip;
			this.nextShip = null;
			return ship;
		}
	}
}
