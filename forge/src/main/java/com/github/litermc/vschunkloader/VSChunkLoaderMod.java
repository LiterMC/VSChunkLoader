package com.github.litermc.vschunkloader;

import com.github.litermc.vschunkloader.block.BlockCapabilityProviders;

import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;

@Mod(Constants.MOD_ID)
public class VSChunkLoaderMod {
	public VSChunkLoaderMod() {
		// IEventBus modBus = FMLJavaModLoadingContext.get().getModEventBus();

		BlockCapabilityProviders.register();
		VSCRegistry.register();
	}
}
