package com.github.litermc.vschunkloader.attachment;

import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vtil.api.storage.IShipAdditionalData;
import com.github.litermc.vtil.api.storage.ShipDataStorage;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ships.ShipData;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public final class ForceLoadStorage implements IShipAdditionalData {
	private Set<ResourceLocation> forceLoadTokens = new HashSet<>();

	public ForceLoadStorage() {}

	public static ForceLoadStorage get(final ServerShip ship) {
		return ShipDataStorage.get(ship).getOrCreate(ForceLoadStorage.class);
	}

	@Override
	public void load(final CompoundTag data) {
		data.getList("forceLoadTokens", Tag.TAG_STRING)
			.forEach((tag) -> this.forceLoadTokens.add(new ResourceLocation(tag.getAsString())));
	}

	@Override
	public void save(final CompoundTag data) {
		final ListTag forceLoadTokens = new ListTag();
		for (final ResourceLocation token : this.forceLoadTokens) {
			forceLoadTokens.add(StringTag.valueOf(token.toString()));
		}
		data.put("forceLoadTokens", forceLoadTokens);
	}

	public boolean isForceLoaded() {
		final PlatformHelper platform = PlatformHelper.get();
		for (final ResourceLocation id : this.forceLoadTokens) {
			if (platform.isModLoaded(id.getNamespace())) {
				return true;
			}
		}
		return false;
	}

	public boolean isForceLoadedBy(final ResourceLocation token) {
		return this.forceLoadTokens.contains(token);
	}

	public void addForceLoad(final ResourceLocation token) {
		this.forceLoadTokens.add(token);
	}

	public void removeForceLoad(final ResourceLocation token) {
		this.forceLoadTokens.remove(token);
	}

	public void removeAllForceLoadTokens() {
		this.forceLoadTokens.clear();
	}

	public Set<ResourceLocation> getAllForceLoadTokens() {
		return Collections.unmodifiableSet(this.forceLoadTokens);
	}
}
