package com.github.litermc.vschunkloader.attachment;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.VSCApi;
import com.github.litermc.vschunkloader.config.Config;
import com.github.litermc.vschunkloader.util.TaskUtil;
import com.github.litermc.vtil.api.assemble.ShipAllocator;
import com.github.litermc.vtil.api.attachment.IServerTickListener;

import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ships.ShipData;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	setterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class AmmoShipAttachment implements IServerTickListener {
	private static final ResourceLocation AMMO_SHIP_TICKET = new ResourceLocation(Constants.MOD_ID, "ammo_ship");

	private int activatedTick;
	private boolean unmanaged = false;
	private boolean deactivated = false;

	public AmmoShipAttachment() {}

	public static AmmoShipAttachment create(final ServerShip ship) {
		final AmmoShipAttachment attachment = new AmmoShipAttachment();
		if (ship instanceof final LoadedServerShip loadedShip) {
			loadedShip.setAttachment(attachment);
			return attachment;
		} else if (ship instanceof final ShipData shipData) {
			final var attachmentHolder = shipData.getAttachmentHolder();
			attachmentHolder.setAttachment(attachment);
		} else {
			throw new IllegalArgumentException("ship is neither LoadedServerShip nor ShipData");
		}
		return attachment;
	}

	@JsonGetter("activatedTick")
	private int getActivatedTick() {
		return this.activatedTick;
	}

	@JsonSetter("activatedTick")
	private void setActivatedTick(final int activatedTick) {
		this.activatedTick = activatedTick;
	}

	@Override
	public void onServerTick(final ServerLevel level, final LoadedServerShip ship) {
		if (this.activatedTick == 0) {
			this.checkActivate(level, ship);
			return;
		}
		if (this.activatedTick > Config.ammoMaxActivateSeconds * 20) {
			if (!this.deactivated) {
				this.deactivated = true;
				this.deactivate(level, ship);
			}
			return;
		}
		this.activatedTick++;
	}

	public void managerTick() {
		this.unmanaged = false;
	}

	private void checkActivate(final ServerLevel level, final LoadedServerShip ship) {
		if (this.unmanaged) {
			this.activate(level, ship);
			return;
		}
		this.unmanaged = true;
	}

	private void activate(final ServerLevel level, final LoadedServerShip ship) {
		this.activatedTick = 1;
		VSCApi.forceLoad(level.getServer(), ship.getId(), AMMO_SHIP_TICKET, true);
	}

	private void deactivate(final ServerLevel level, final LoadedServerShip ship) {
		VSCApi.forceLoad(level.getServer(), ship.getId(), AMMO_SHIP_TICKET, false);
		if (!Config.removeAmmoAfterExpired) {
			return;
		}
		TaskUtil.queueTickStart(() -> ShipAllocator.get(level.getServer()).putShip(ship));
	}
}
