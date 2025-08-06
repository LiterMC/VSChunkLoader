package com.github.litermc.vschunkloader.config;

import com.github.litermc.vschunkloader.platform.PlatformHelper;

import java.nio.file.Path;

public final class ConfigSpec {
	public static final ConfigFile serverSpec;

	public static final ConfigFile.Value<Boolean> FORCE_LOAD_ALL_SHIPS;

	public static final ConfigFile.Value<Integer> CHUNK_LOADER_ENERGY_CONSUME_RATE;
	public static final ConfigFile.Value<Integer> WEAK_CHUNK_LOADER_ENERGY_CONSUME_RATE;
	public static final ConfigFile.Value<Integer> WEAK_CHUNK_LOADER_MAX_ACTIVATE_SECONDS;

	private ConfigSpec() {}

	static {
		final ConfigFile.Builder builder = PlatformHelper.get().createConfigBuilder();
		{
			builder
				.comment("General settings")
				.push("general");

			FORCE_LOAD_ALL_SHIPS = builder
				.comment("Should force load all ships on the server")
				.define("force_load_all_ships", Config.forceLoadAllShips);

			builder.pop();
		}
		{
			builder
				.comment("Chunk loader consumptions")
				.push("consumptions");

			CHUNK_LOADER_ENERGY_CONSUME_RATE = builder
				.comment("Energy comsume rate to active chunk loader, in FE/s")
				.defineInRange("chunk_loader_energy_consume_rate", Config.chunkLoaderEnergyConsumeRate, 0, Integer.MAX_VALUE / 16);
			WEAK_CHUNK_LOADER_ENERGY_CONSUME_RATE = builder
				.comment("Energy comsume rate to active weak chunk loader, in FE/s")
				.defineInRange("weak_chunk_loader_energy_consume_rate", Config.weakChunkLoaderEnergyConsumeRate, 0, Integer.MAX_VALUE / 16);
			WEAK_CHUNK_LOADER_MAX_ACTIVATE_SECONDS = builder
				.comment("Max seconds weak chunk loader can active before self-destroy")
				.defineInRange("weak_chunk_loader_max_activate_seconds", Config.weakChunkLoaderMaxActivateSeconds, 0, Integer.MAX_VALUE);

			builder.pop();
		}

		serverSpec = builder.build(ConfigSpec::syncServer);
	}

	public static void syncServer(Path path) {
		Config.forceLoadAllShips = FORCE_LOAD_ALL_SHIPS.get();
		Config.chunkLoaderEnergyConsumeRate = CHUNK_LOADER_ENERGY_CONSUME_RATE.get();
		Config.weakChunkLoaderEnergyConsumeRate = WEAK_CHUNK_LOADER_ENERGY_CONSUME_RATE.get();
		Config.weakChunkLoaderMaxActivateSeconds = WEAK_CHUNK_LOADER_MAX_ACTIVATE_SECONDS.get();
	}

	public static void syncClient(Path path) {
	}
}
