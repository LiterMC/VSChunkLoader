/**
 * Copyright (C) 2025  the authors of ValkyrienSkies mod
 * Modified by zyxkad  2025
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.github.litermc.vschunkloader.mixin.client;

import com.github.litermc.vschunkloader.accessor.ClientChunkCacheAccessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.ViewArea;
import net.minecraft.world.level.ChunkPos;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * This mixin allows {@link LevelRenderer} to render ship chunks.
 */
@Mixin(LevelRenderer.class)
public abstract class MixinLevelRenderer {
	@Shadow
	private ClientLevel level;
	@Shadow
	private ViewArea viewArea;

	/**
	 * Prevents ships from disappearing on f3+a
	 */
	@Inject(
		method = "allChanged",
		at = @At(
			value = "INVOKE",
			target = "Lnet/minecraft/client/renderer/ViewArea;repositionCamera(DD)V"
		)
	)
	private void afterRefresh(final CallbackInfo ci) {
		((ClientChunkCacheAccessor) this.level.getChunkSource()).vsc$getShipChunks().keySet().forEach((pos) -> {
			final int x = ChunkPos.getX(pos);
			final int z = ChunkPos.getZ(pos);
			for (int y = this.level.getMinSection(); y < this.level.getMaxSection(); y++) {
				this.viewArea.setDirty(x, y, z, true);
			}
		});
	}
}
