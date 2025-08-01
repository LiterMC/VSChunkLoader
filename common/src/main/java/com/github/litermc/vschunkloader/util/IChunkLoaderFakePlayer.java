package com.github.litermc.vschunkloader.util;

import com.github.litermc.vschunkloader.block.ChunkLoaderBlockEntity;

public interface IChunkLoaderFakePlayer {
	void bindChunkLoader(ChunkLoaderBlockEntity be);

	ChunkLoaderBlockEntity getChunkLoader();
}
