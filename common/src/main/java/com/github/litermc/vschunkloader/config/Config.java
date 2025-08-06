package com.github.litermc.vschunkloader.config;

public final class Config {
	/**
	 * Force load all ships
	 */
	public static boolean forceLoadAllShips = false;

	/**
	 * Normal Chunk Loader's energy consume rate in FE/t
	 */
	public static int chunkLoaderEnergyConsumeRate = 4096 * 20;

	/**
	 * Weak Chunk Loader's energy consume rate in FE/t
	 */
	public static int weakChunkLoaderEnergyConsumeRate = 4096 * 20;

	/**
	 * Weak Chunk Loader's max activate seconds
	 */
	public static int weakChunkLoaderMaxActivateSeconds = 60;

	private Config() {}
}
