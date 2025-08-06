package com.github.litermc.vschunkloader.attachment;

import  com.github.litermc.vschunkloader.util.ResourceLocationCollectionSerializer;

import net.minecraft.resources.ResourceLocation;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.ANY,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class ForceLoadAttachment {
	@JsonSerialize(using = ResourceLocationCollectionSerializer.class)
	private Set<ResourceLocation> forceLoadTokens = new HashSet<>();

	public ForceLoadAttachment() {}

	public static ForceLoadAttachment get(final ServerShip ship) {
		ForceLoadAttachment attachment = ship.getAttachment(ForceLoadAttachment.class);
		if (attachment == null) {
			attachment = new ForceLoadAttachment();
			ship.saveAttachment(ForceLoadAttachment.class, attachment);
		}
		return attachment;
	}

	public boolean isForceLoaded() {
		return !this.forceLoadTokens.isEmpty();
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
