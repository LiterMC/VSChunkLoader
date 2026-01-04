package com.github.litermc.vschunkloader.attachment;

import com.github.litermc.vschunkloader.platform.PlatformHelper;

import net.minecraft.resources.ResourceLocation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;
import org.valkyrienskies.core.api.ships.LoadedServerShip;
import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.impl.game.ships.ShipData;

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
public final class ForceLoadAttachment {
	private Set<ResourceLocation> forceLoadTokens = new HashSet<>();

	public ForceLoadAttachment() {}

	public static ForceLoadAttachment get(final ServerShip ship) {
		if (ship instanceof final LoadedServerShip loadedShip) {
			ForceLoadAttachment attachment = loadedShip.getAttachment(ForceLoadAttachment.class);
			if (attachment == null) {
				attachment = new ForceLoadAttachment();
				loadedShip.setAttachment(attachment);
			}
			return attachment;
		}
		if (ship instanceof final ShipData shipData) {
			final var attachmentHolder = shipData.getAttachmentHolder();
			return attachmentHolder.getOrPutAttachment(ForceLoadAttachment.class, ForceLoadAttachment::new);
		}
		throw new IllegalArgumentException("ship is neither LoadedServerShip nor ShipData");
	}

	@JsonGetter("forceLoadTokens")
	private Collection<String> getForceLoadTokens() {
		return this.forceLoadTokens.stream().map(ResourceLocation::toString).toList();
	}

	@JsonSetter("forceLoadTokens")
	private void setForceLoadTokens(final Collection<String> tokens) {
		this.forceLoadTokens.clear();
		tokens.stream().map(ResourceLocation::new).forEach(this.forceLoadTokens::add);
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
