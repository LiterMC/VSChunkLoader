package com.github.litermc.vschunkloader.block;

public final class BlockCapabilityProviders {
	private BlockCapabilityProviders() {}

	public static void register() {
		ChunkLoaderBlockEntityCapabilityProvider.register();
	}
}
