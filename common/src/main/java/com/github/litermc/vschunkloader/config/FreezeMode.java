package com.github.litermc.vschunkloader.config;

public enum FreezeMode {
	/**
	 * Freeze any ship that hitting a loading chunk
	 */
	ALL,
	/**
	 * Only freeze ammo ship which hitting a loading chunk
	 */
	AMMO,
	/**
	 * Do not freeze any ship
	 */
	NONE;
}
