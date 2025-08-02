package com.github.litermc.vschunkloader.util;

import net.minecraft.world.phys.Vec3;

public interface IChunkLoaderFakePlayer {
	void bindPosition(Vec3 position);

	void refreshCountDown();

	void startDiscard();

	boolean isDiscarding();
}
