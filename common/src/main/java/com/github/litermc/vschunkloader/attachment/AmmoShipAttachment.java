package com.github.litermc.vschunkloader.attachment;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.VSCApi;
import com.github.litermc.vschunkloader.config.Config;
import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vschunkloader.util.ShipAllocator;
import com.github.litermc.vschunkloader.util.Utils;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.ServerTickListener;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class AmmoShipAttachment implements ServerTickListener {
	private static final ResourceLocation AMMO_SHIP_TICKET = new ResourceLocation(Constants.MOD_ID, "ammo_ship");

	private final MinecraftServer server;
	private final ServerShipWorldCore world;
	private long shipId;
	private int activatedTick;
	private int unmanagedTick = 0;
	private boolean deactivated = false;

	public AmmoShipAttachment() {
		this(-1);
	}

	public AmmoShipAttachment(final long shipId) {
		this.server = PlatformHelper.get().getCurrentServer();
		this.world = VSGameUtilsKt.getShipObjectWorld(this.server);
		this.shipId = shipId;
	}

	public static AmmoShipAttachment create(final ServerShip ship) {
		final AmmoShipAttachment attachment = new AmmoShipAttachment(ship.getId());
		ship.saveAttachment(AmmoShipAttachment.class, attachment);
		return attachment;
	}

	@JsonGetter("shipId")
	private long getShipId() {
		return this.shipId;
	}

	@JsonSetter("shipId")
	private void setShipId(final long shipId) {
		this.shipId = shipId;
	}

	@JsonGetter("activatedTick")
	private long getActivatedTick() {
		return this.activatedTick;
	}

	@JsonSetter("activatedTick")
	private void setActivatedTick(final int activatedTick) {
		this.activatedTick = activatedTick;
	}

	@Override
	public void onServerTick() {
		if (this.activatedTick == 0) {
			this.checkActivate();
			return;
		}
		if (this.activatedTick > Config.ammoMaxActivateSeconds * 20) {
			if (!this.deactivated) {
				this.deactivated = true;
				this.deactivate();
			}
			return;
		}
		this.activatedTick++;
	}

	public void managerTick() {
		this.unmanagedTick = 0;
	}

	private ServerShip getShip() {
		final ServerShip ship = this.world.getLoadedShips().getById(this.shipId);
		if (ship != null) {
			return ship;
		}
		return this.world.getAllShips().getById(this.shipId);
	}

	private void checkActivate() {
		if (this.unmanagedTick > 1) {
			this.activate();
			return;
		}
		this.unmanagedTick++;
	}

	private void activate() {
		this.activatedTick = 1;
		VSCApi.forceLoad(this.server, this.shipId, AMMO_SHIP_TICKET, true);
	}

	private void deactivate() {
		VSCApi.forceLoad(this.server, this.shipId, AMMO_SHIP_TICKET, false);
		if (!Config.removeAmmoAfterExpired) {
			return;
		}
		final ServerShip ship = this.getShip();
		if (ship == null) {
			return;
		}
		final ServerLevel level = Utils.getLevel(ship.getChunkClaimDimension());
		if (level == null) {
			this.world.deleteShip(ship);
			return;
		}
		PlatformHelper.get().queueTask(() -> ShipAllocator.get(level).putShip(ship));
	}
}
