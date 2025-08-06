package com.github.litermc.vschunkloader.attachment;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import org.valkyrienskies.core.api.ships.ServerShip;

import java.util.HashSet;
import java.util.Set;

@JsonAutoDetect(
	fieldVisibility = JsonAutoDetect.Visibility.ANY,
	getterVisibility = JsonAutoDetect.Visibility.NONE,
	isGetterVisibility = JsonAutoDetect.Visibility.NONE
)
public final class ForceLoadAttachment {
	private Set<String> forceLoadTokens = new HashSet<>();

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

	public boolean isForceLoadedBy(final String token) {
		return this.forceLoadTokens.contains(token);
	}

	public void addForceLoad(final String token) {
		this.forceLoadTokens.add(token);
	}

	public void removeForceLoad(final String token) {
		this.forceLoadTokens.remove(token);
	}

	public void removeAllForceLoadTokens() {
		this.forceLoadTokens.clear();
	}
}
