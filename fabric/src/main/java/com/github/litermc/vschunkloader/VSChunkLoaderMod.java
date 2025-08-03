package com.github.litermc.vschunkloader;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.block.BlockCapabilityProviders;
import com.github.litermc.vschunkloader.config.ConfigSpec;
import com.github.litermc.vschunkloader.platform.FabricConfigFile;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.world.level.storage.LevelResource;

public class VSChunkLoaderMod implements ModInitializer {
	private static final String SERVERCONFIG = "serverconfig";

	@Override
	public void onInitialize() {
		VSCRegistry.register();
		BlockCapabilityProviders.register();

		ServerLifecycleEvents.SERVER_STARTING.register((server) -> {
			((FabricConfigFile)(ConfigSpec.serverSpec))
				.load(server.getWorldPath(LevelResource.ROOT).resolve(SERVERCONFIG).resolve(Constants.MOD_ID + "-server.toml"));
		});
		ServerLifecycleEvents.SERVER_STOPPED.register((server) -> {
			((FabricConfigFile)(ConfigSpec.serverSpec)).unload();
		});
	}
}
