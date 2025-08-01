package com.github.litermc.vschunkloader.client;

import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.VSCRegistry;

@Mod.EventBusSubscriber(modid = Constants.MOD_ID, value = Dist.CLIENT, bus = Mod.EventBusSubscriber.Bus.MOD)
public final class ClientRegistry {
	@SubscribeEvent
	public static void clientSetup(final FMLClientSetupEvent event) {
		event.enqueueWork(() -> {
			ItemBlockRenderTypes.setRenderLayer(VSCRegistry.Blocks.CHUNK_LOADER.get(), RenderType.cutout());
			ItemBlockRenderTypes.setRenderLayer(VSCRegistry.Blocks.CHUNK_LOADER_WEAK.get(), RenderType.cutout());
		});
	}
}
