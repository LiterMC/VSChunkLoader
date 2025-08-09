package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.block.ammo.AmmoAssemblerBlockEntityCapabilityProvider;

public final class BlockCapabilityProviders {
	private BlockCapabilityProviders() {}

	public static void register() {
		AmmoAssemblerBlockEntityCapabilityProvider.register();
		ChunkLoaderBlockEntityCapabilityProvider.register();
	}
}
