package com.github.litermc.vschunkloader.config;

import net.minecraft.resources.ResourceLocation;

import java.util.Set;

public final class Config {
	/**
	 * Force load all ships
	 */
	public static boolean forceLoadAllShips = false;

	/**
	 * Normal Chunk Loader's energy consume rate in FE/s
	 */
	public static int chunkLoaderEnergyConsumeRate = 4096 * 20;

	/**
	 * Weak Chunk Loader's energy consume rate in FE/s
	 */
	public static int weakChunkLoaderEnergyConsumeRate = 4096 * 20;

	/**
	 * Weak Chunk Loader's max activate seconds
	 */
	public static int weakChunkLoaderMaxActivateSeconds = 3 * 60;

	/**
	 * Energy required to assemble an Ammo
	 */
	public static int ammoAssembleEnergy = 4096 * 60;

	/**
	 * Reuse deleted ship's chunks to assemble new ammo when possible
	 */
	public static boolean reuseShipChunks = true;

	/**
	 * Blocks will prevent to assemble an Ammo
	 */
	public static Set<ResourceLocation> ammoAssembleBlacklist = Set.of(
		new ResourceLocation("minecraft", "barrier"),
		new ResourceLocation("minecraft", "bedrock")
	);

	/**
	 * Max blocks can be assembled with Ammo Assembler
	 */
	public static int ammoMaxBlocks = 16 * 16 * 16;

	/**
	 * Ammo max activate seconds after leaves Ammo Manager
	 */
	public static int ammoMaxActivateSeconds = 60;

	/**
	 * Should remove Ammo after it is expired
	 */
	public static boolean removeAmmoAfterExpired = true;

	/**
	 * Ammo will keep fresh (keep unactivated) within the range of Ammo Manager
	 */
	public static int ammoManagerAnchoringRange = 16 * 4;

	/**
	 * Freeze ship for chunk loading. Avoid velocity reset when moving at high speed
	 */
	public static FreezeMode shipFreezing = FreezeMode.AMMO;

	private Config() {}
}
