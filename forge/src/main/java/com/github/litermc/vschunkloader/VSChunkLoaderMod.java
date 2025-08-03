package com.github.litermc.vschunkloader;

import com.github.litermc.vschunkloader.block.BlockCapabilityProviders;
import com.github.litermc.vschunkloader.config.ConfigSpec;
import com.github.litermc.vschunkloader.platform.ForgeConfigFile;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;

@Mod(Constants.MOD_ID)
@Mod.EventBusSubscriber(modid = Constants.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD)
public class VSChunkLoaderMod {
	public VSChunkLoaderMod() {
		VSCRegistry.register();
		BlockCapabilityProviders.register();

		ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, ((ForgeConfigFile)(ConfigSpec.serverSpec)).spec());
	}

	// Following code comes from CC: Tweaked
	//
	// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
	//
	// SPDX-License-Identifier: MPL-2.0

	@SubscribeEvent
	public static void sync(final ModConfigEvent.Loading event) {
		syncConfig(event.getConfig());
	}

	@SubscribeEvent
	public static void sync(final ModConfigEvent.Reloading event) {
		syncConfig(event.getConfig());
	}

	private static void syncConfig(final ModConfig config) {
		if (!config.getModId().equals(Constants.MOD_ID)) return;

		var path = config.getConfigData() instanceof FileConfig fileConfig ? fileConfig.getNioPath() : null;

		if (config.getType() == ModConfig.Type.SERVER && ((ForgeConfigFile)(ConfigSpec.serverSpec)).spec().isLoaded()) {
			ConfigSpec.syncServer(path);
		} else if (config.getType() == ModConfig.Type.CLIENT) {
			ConfigSpec.syncClient(path);
		}
	}
}
