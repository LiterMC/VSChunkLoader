package com.github.litermc.vschunkloader.mixin;

import com.github.litermc.vschunkloader.util.ShipAllocator;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.ShipSelector;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(ShipSelector.class)
public class MixinShipSelector {
	@ModifyArg(
		method = "select",
		at = @At(
			value = "INVOKE",
			target = "Lkotlin/collections/CollectionsKt;asSequence(Ljava/lang/Iterable;)Lkotlin/sequences/Sequence;"
		),
		remap = false
	)
	private Iterable<Ship> selectIterable(Iterable<Ship> ships) {
		return new ShipAllocator.SafeShipIterable(ships);
	}
}
