package com.github.litermc.vschunkloader.util;

import com.github.litermc.vschunkloader.platform.PlatformHelper;

import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.mod.common.VSGameUtilsKt;

import java.util.HashMap;

public final class Utils {
	private Utils() {}

	private static final HashMap<String, ServerLevel> ID_TO_LEVEL_CACHE = new HashMap<>();

	public static void onServerLevelLoad(final ServerLevel level) {
		ID_TO_LEVEL_CACHE.put(VSGameUtilsKt.getDimensionId(level), level);
	}

	public static void onServerLevelUnload(final ServerLevel level) {
		ID_TO_LEVEL_CACHE.remove(VSGameUtilsKt.getDimensionId(level), level);
	}

	public static ServerLevel getLevel(final String dimId) {
		return ID_TO_LEVEL_CACHE.get(dimId);
	}
}
