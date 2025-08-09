package com.github.litermc.vschunkloader.compat;

import com.github.litermc.vschunkloader.platform.PlatformHelper;

public enum CompatMods {
	COMPUTERCRAFT("computercraft"),
	CREATE("create");

	private final String modId;

	private CompatMods(final String modId) {
		this.modId = modId;
	}

	public String getId() {
		return this.modId;
	}

	public boolean isLoaded() {
		return PlatformHelper.get().isModLoaded(this.getId());
	}
}
