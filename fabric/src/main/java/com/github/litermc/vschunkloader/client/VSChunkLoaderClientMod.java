package com.github.litermc.vschunkloader.client;

import com.github.litermc.vschunkloader.VSCRegistry;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.minecraft.client.renderer.RenderType;

public class VSChunkLoaderClientMod implements ClientModInitializer {
	@Override
	public void onInitializeClient() {
		BlockRenderLayerMap.INSTANCE.putBlock(VSCRegistry.Blocks.CHUNK_LOADER.get(), RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(VSCRegistry.Blocks.CHUNK_LOADER_WEAK.get(), RenderType.cutout());
	}
}
