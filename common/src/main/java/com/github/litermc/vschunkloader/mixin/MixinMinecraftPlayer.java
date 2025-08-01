package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.util.IChunkLoaderFakePlayer;

import org.valkyrienskies.mod.common.util.MinecraftPlayer;

import net.minecraft.world.entity.player.Player;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MinecraftPlayer.class)
public abstract class MixinMinecraftPlayer {
	@Shadow(remap = false)
	public abstract Player getPlayer();

	@Inject(method = "getDimension", at = @At("HEAD"), remap = false, cancellable = true)
	public void getDimension(final CallbackInfoReturnable<String> cir) {
		if (this.getPlayer() instanceof IChunkLoaderFakePlayer fakePlayer) {
			if (!fakePlayer.getChunkLoader().isRunning()) {
				cir.setReturnValue("");
			}
		}
	}
}
