package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.block.ammo.AmmoAssemblerBlockEntity;
import com.github.litermc.vschunkloader.block.ammo.AmmoAssemblerBlockEntityCapabilityProvider;

import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.AttachCapabilitiesEvent;

public final class BlockCapabilityProviders {
	private BlockCapabilityProviders() {}

	public static void register() {
		MinecraftForge.EVENT_BUS.addGenericListener(BlockEntity.class, (AttachCapabilitiesEvent<? extends BlockEntity> event) -> {
			if (event.getObject() instanceof AmmoAssemblerBlockEntity) {
				AmmoAssemblerBlockEntityCapabilityProvider.onGatherCapabilities((AttachCapabilitiesEvent<AmmoAssemblerBlockEntity>) (event));
				return;
			}
			if (event.getObject() instanceof ChunkLoaderBlockEntity) {
				ChunkLoaderBlockEntityCapabilityProvider.onGatherCapabilities((AttachCapabilitiesEvent<ChunkLoaderBlockEntity>) (event));
				return;
			}
		});
	}
}
