package com.github.litermc.vschunkloader.block.ammo;

import com.github.litermc.vschunkloader.VSCRegistry;
import com.github.litermc.vschunkloader.attachment.AmmoShipAttachment;
import com.github.litermc.vschunkloader.config.Config;

import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import org.joml.primitives.AABBd;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.apigame.world.ServerShipWorldCore;
import org.valkyrienskies.mod.common.VSGameUtilsKt;

public final class AmmoManagerBlockEntity extends BlockEntity {
	private final AABBd manageArea;

	public AmmoManagerBlockEntity(final BlockPos pos, final BlockState state) {
		super(VSCRegistry.BlockEntities.AMMO_MANAGER.get(), pos, state);
		final double r = Config.ammoManagerAnchoringRange;
		this.manageArea = new AABBd(pos.getX() - r, pos.getY() - r, pos.getZ() - r, pos.getX() + r + 1, pos.getY() + r + 1, pos.getZ() + r + 1);
	}

	public AABBd getManageArea() {
		return this.manageArea;
	}

	public void serverTick() {
		final ServerShipWorldCore world = VSGameUtilsKt.getShipObjectWorld((ServerLevel) (this.getLevel()));
		for (final LoadedServerShip ship : world.getLoadedShips().getIntersecting(this.getManageArea())) {
			final AmmoShipAttachment attachment = ship.getAttachment(AmmoShipAttachment.class);
			if (attachment != null) {
				attachment.managerTick();
			}
		}
	}
}
