package com.github.litermc.vschunkloader;

import com.github.litermc.vschunkloader.block.BlockCapabilityProviders;

import net.fabricmc.api.ModInitializer;

public class VSChunkLoaderMod implements ModInitializer {
	@Override
	public void onInitialize() {
		VSCRegistry.register();
		BlockCapabilityProviders.register();
	}
}
