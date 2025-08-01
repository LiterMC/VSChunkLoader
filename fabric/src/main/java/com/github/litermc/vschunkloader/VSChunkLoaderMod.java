package com.github.litermc.vschunkloader;

import net.fabricmc.api.ModInitializer;

public class VSChunkLoaderMod implements ModInitializer {
	@Override
	public void onInitialize() {
		VSCRegistry.register();
	}
}
