package com.github.litermc.vschunkloader;

import com.github.litermc.vschunkloader.block.BlockCapabilityProviders;
import com.github.litermc.vschunkloader.command.VSCCommands;
import com.github.litermc.vschunkloader.config.ConfigSpec;
import com.github.litermc.vschunkloader.platform.ForgeConfigFile;

import com.electronwill.nightconfig.core.file.FileConfig;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.level.ChunkEvent;
import net.minecraftforge.event.level.LevelEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.config.ModConfig;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;

@Mod(Constants.MOD_ID)
@Mod.EventBusSubscriber
public class VSChunkLoaderMod {
	public VSChunkLoaderMod() {
		final FMLJavaModLoadingContext context = FMLJavaModLoadingContext.get();
		final IEventBus modBus = context.getModEventBus();

		VSCListeners.onModInit();
		BlockCapabilityProviders.register();

		context.registerConfig(ModConfig.Type.SERVER, ((ForgeConfigFile)(ConfigSpec.serverSpec)).spec());
		modBus.addListener(this::onCommonSetup);
		modBus.addListener(this::onConfigLoad);
		modBus.addListener(this::onConfigReload);
	}

	private void onCommonSetup(final FMLCommonSetupEvent event) {
		event.enqueueWork(VSCListeners::onModSetup);
	}

	@SubscribeEvent
	public static void onRegisterCommands(final RegisterCommandsEvent event) {
		VSCCommands.register(event.getDispatcher());
	}

	@SubscribeEvent
	public static void onLevelLoad(final LevelEvent.Load event) {
		if (event.getLevel() instanceof final ServerLevel level) {
			VSCListeners.onServerLevelLoad(level);
		}
	}

	@SubscribeEvent
	public static void onLevelUnload(final LevelEvent.Unload event) {
		if (event.getLevel() instanceof final ServerLevel level) {
			VSCListeners.onServerLevelUnload(level);
		}
	}

	@SubscribeEvent
	public static void onServerTick(final TickEvent.ServerTickEvent event) {
		switch (event.phase) {
		case START -> VSCListeners.preServerTick(event.getServer());
		case END -> VSCListeners.postServerTick(event.getServer());
		}
	}

	@SubscribeEvent
	public static void onChunkLoad(final ChunkEvent.Load event) {
		if (event.getLevel() instanceof final ServerLevel level && event.getChunk() instanceof final LevelChunk chunk) {
			VSCListeners.onServerChunkLoad(level, chunk);
		}
	}

	@SubscribeEvent
	public static void onChunkUnload(final ChunkEvent.Unload event) {
		if (event.getLevel() instanceof final ServerLevel level && event.getChunk() instanceof final LevelChunk chunk) {
			VSCListeners.onServerChunkUnload(level, chunk);
		}
	}

	// Following code comes from CC: Tweaked
	//
	// SPDX-FileCopyrightText: 2017 The CC: Tweaked Developers
	//
	// SPDX-License-Identifier: MPL-2.0

	private void onConfigLoad(final ModConfigEvent.Loading event) {
		this.syncConfig(event.getConfig());
	}

	private void onConfigReload(final ModConfigEvent.Reloading event) {
		this.syncConfig(event.getConfig());
	}

	private void syncConfig(final ModConfig config) {
		if (!config.getModId().equals(Constants.MOD_ID)) return;

		var path = config.getConfigData() instanceof final FileConfig fileConfig ? fileConfig.getNioPath() : null;

		if (config.getType() == ModConfig.Type.SERVER && ((ForgeConfigFile)(ConfigSpec.serverSpec)).spec().isLoaded()) {
			ConfigSpec.syncServer(path);
		} else if (config.getType() == ModConfig.Type.CLIENT) {
			ConfigSpec.syncClient(path);
		}
	}
}
