package com.github.litermc.vschunkloader;

import com.github.litermc.vschunkloader.attachment.ForceLoadAttachment;

import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.ServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class VSCApi {
	private static final VSCApi INSTANCE = new VSCApi();

	private VSCApi() {}

	public static VSCApi get() {
		return INSTANCE;
	}

	/**
	 * update a ship's force load token
	 *
	 * @param server The {@link MinecraftServer} instance
	 * @param id     The ship's ID
	 * @param token  Force load token, same token must be used when invoking {@link stopForceLoadShip}
	 * @param load   {@code true} if force loading ship, {@code false} if unloading
	 * @return {@code true} if the token is updated, or {@code false} if ship is not found
	 *
	 * @see stopForceLoadShip
	 */
	public static boolean forceLoad(final MinecraftServer server, final long id, final String token, final boolean load) {
		final ServerShip ship = getShip(server, id);
		if (ship == null) {
			return false;
		}
		final ForceLoadAttachment attachment = ForceLoadAttachment.get(ship);
		if (load) {
			attachment.addForceLoad(token);
		} else {
			attachment.removeForceLoad(token);
		}
		return true;
	}

	/**
	 * remove all force load token on a ship
	 *
	 * @param server The {@link MinecraftServer} instance
	 * @param id     The ship's ID
	 * @return {@code true} if all the tokens are removed, or {@code false} if ship is not found
	 *
	 * @see stopForceLoadShip
	 */
	public static boolean stopAllForceLoading(final MinecraftServer server, final long id) {
		final ServerShip ship = getShip(server, id);
		if (ship == null) {
			return false;
		}
		final ForceLoadAttachment attachment = ForceLoadAttachment.get(ship);
		attachment.removeAllForceLoadTokens();
		return true;
	}

	/**
	 * check if a ship is force loaded
	 *
	 * @param server The {@link MinecraftServer} instance
	 * @param id     The ship's ID
	 * @return whether or not the ship is force loaded
	 */
	public static boolean isForceLoaded(final MinecraftServer server, final long id) {
		final ServerShip ship = getShip(server, id);
		if (ship == null) {
			return false;
		}
		return ForceLoadAttachment.get(ship).isForceLoaded();
	}

	/**
	 * check if a ship is force loaded
	 *
	 * @param server The {@link MinecraftServer} instance
	 * @param id     The ship's ID
	 * @param token  Force load token, should be already used in {@link forceLoadShip}
	 * @return whether or not the ship is force loaded by the token
	 */
	public static boolean isForceLoadedBy(final MinecraftServer server, final long id, final String token) {
		final ServerShip ship = getShip(server, id);
		if (ship == null) {
			return false;
		}
		return ForceLoadAttachment.get(ship).isForceLoadedBy(token);
	}

	private static ServerShip getShip(final MinecraftServer server, final long id) {
		final ServerShipWorld shipWorld = VSGameUtilsKt.getShipObjectWorld(server);
		final ServerShip ship = shipWorld.getLoadedShips().getById(id);
		if (ship != null) {
			return ship;
		}
		return shipWorld.getAllShips().getById(id);
	}
}
