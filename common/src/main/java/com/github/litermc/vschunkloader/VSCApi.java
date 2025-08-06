package com.github.litermc.vschunkloader;

import com.github.litermc.vschunkloader.attachment.ForceLoadAttachment;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.world.ServerShipWorld;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Set;

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
	 * @param token  Force load token, same token must be used when invoking {@link stopForceLoadShip}.
	 *               The namespace of the token must be the mod's ID.
	 * @param load   {@code true} if force loading ship, {@code false} if unloading
	 * @return {@code true} if the token is updated, or {@code false} if ship is not found
	 *
	 * @see stopForceLoadShip
	 */
	public static boolean forceLoad(final MinecraftServer server, final long id, final ResourceLocation token, final boolean load) {
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
	public static boolean clearForceLoadTokens(final MinecraftServer server, final long id) {
		final ServerShip ship = getShip(server, id);
		if (ship == null) {
			return false;
		}
		final ForceLoadAttachment attachment = ForceLoadAttachment.get(ship);
		attachment.removeAllForceLoadTokens();
		return true;
	}

	/**
	 * gets a read-only view of the tokens of a ship.<br/>
	 * The set can be modified later by other methods in this API.
	 *
	 * @param server The {@link MinecraftServer} instance
	 * @param id     The ship's ID
	 * @return {@code null} if the ship is not found, or a unmodifiable set of current loading ticket.
	 */
	public static Set<ResourceLocation> getForceLoadTokens(final MinecraftServer server, final long id) {
		final ServerShip ship = getShip(server, id);
		if (ship == null) {
			return null;
		}
		final ForceLoadAttachment attachment = ForceLoadAttachment.get(ship);
		return attachment.getAllForceLoadTokens();
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
	public static boolean isForceLoadedBy(final MinecraftServer server, final long id, final ResourceLocation token) {
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
