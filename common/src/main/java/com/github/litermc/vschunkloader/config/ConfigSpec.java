package com.github.litermc.vschunkloader.config;

import com.github.litermc.vschunkloader.platform.PlatformHelper;

import net.minecraft.resources.ResourceLocation;

import java.nio.file.Path;
import java.util.List;
import java.util.Set;

public final class ConfigSpec {
	public static final ConfigFile serverSpec;

	public static final ConfigFile.Value<Boolean> FORCE_LOAD_ALL_SHIPS;

	public static final ConfigFile.Value<Integer> CHUNK_LOADER_ENERGY_CONSUME_RATE;
	public static final ConfigFile.Value<Integer> WEAK_CHUNK_LOADER_ENERGY_CONSUME_RATE;
	public static final ConfigFile.Value<Integer> WEAK_CHUNK_LOADER_MAX_ACTIVATE_SECONDS;

	public static final ConfigFile.Value<Integer> AMMO_ASSEMBLE_ENERGY;
	public static final ConfigFile.Value<Boolean> REUSE_SHIP_CHUNKS;
	public static final ConfigFile.Value<List<? extends String>> AMMO_ASSEMBLE_BLACKLIST;
	public static final ConfigFile.Value<Integer> AMMO_MAX_BLOCKS;
	public static final ConfigFile.Value<Integer> AMMO_MAX_ACTIVATE_SECONDS;
	public static final ConfigFile.Value<Boolean> REMOVE_AMMO_AFTER_EXPIRED;
	public static final ConfigFile.Value<Integer> AMMO_MANAGER_ANCHORING_RANGE;

	public static final ConfigFile.Value<FreezeMode> SHIP_FREEZING;

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
		{
			builder
				.comment("Ammo Ship relative configs")
				.push("ammos");

			AMMO_ASSEMBLE_ENERGY = builder
				.comment("Energy required to assemble an Ammo")
				.defineInRange("ammo_assemble_energy", Config.ammoAssembleEnergy, 0, Integer.MAX_VALUE / 4);
			REUSE_SHIP_CHUNKS = builder
				.comment("Reuse deleted ship's chunks to assemble new ammo when possible")
				.define("reuse_ship_chunks", Config.reuseShipChunks);
			AMMO_ASSEMBLE_BLACKLIST = builder
				.comment("Blocks will prevent to assemble an Ammo")
				.defineList(
					"ammo_assemble_blacklist",
					Config.ammoAssembleBlacklist.stream().map(ResourceLocation::toString).sorted().toList(),
					(o) -> o instanceof String value && value.length() > 0
				);
			AMMO_MAX_BLOCKS = builder
				.comment("Max blocks can be assembled with Ammo Assembler")
				.defineInRange("ammo_max_blocks", Config.ammoMaxBlocks, 0, Integer.MAX_VALUE);
			AMMO_MAX_ACTIVATE_SECONDS = builder
				.comment("Ammo max activate seconds after leaves Ammo Manager")
				.defineInRange("ammo_max_activate_seconds", Config.ammoMaxActivateSeconds, 0, Integer.MAX_VALUE);
			REMOVE_AMMO_AFTER_EXPIRED = builder
				.comment("Should remove Ammo after it is expired")
				.define("remove_ammo_after_expired", Config.removeAmmoAfterExpired);
			AMMO_MANAGER_ANCHORING_RANGE = builder
				.comment("Ammo will keep fresh (keep unactivated) within the range of Ammo Manager")
				.defineInRange("ammo_manager_anchoring_range", Config.ammoManagerAnchoringRange, 1, 16 * 32);

			builder.pop();
		}
		{
			builder
				.comment("Experimental settings. UNSTABLE. May get changed / removed in the future")
				.push("experimental");

			SHIP_FREEZING = builder
				.comment(
					"Freeze ship for chunk loading. Avoid velocity reset when moving at high speed\n" +
					"ALL: Freeze any ship that hitting a loading chunk\n" +
					"AMMO: Only freeze ammo ship which hitting a loading chunk\n" +
					"NONE: Do not freeze any ship"
				)
				.defineEnum("ship_freezing", Config.shipFreezing);
			builder.pop();
		}

		serverSpec = builder.build(ConfigSpec::syncServer);
	}

	public static void syncServer(Path path) {
		Config.forceLoadAllShips = FORCE_LOAD_ALL_SHIPS.get();
		Config.chunkLoaderEnergyConsumeRate = CHUNK_LOADER_ENERGY_CONSUME_RATE.get();
		Config.weakChunkLoaderEnergyConsumeRate = WEAK_CHUNK_LOADER_ENERGY_CONSUME_RATE.get();
		Config.weakChunkLoaderMaxActivateSeconds = WEAK_CHUNK_LOADER_MAX_ACTIVATE_SECONDS.get();
		Config.ammoAssembleEnergy = AMMO_ASSEMBLE_ENERGY.get();
		Config.reuseShipChunks = REUSE_SHIP_CHUNKS.get();
		Config.ammoAssembleBlacklist = Set.copyOf(AMMO_ASSEMBLE_BLACKLIST.get().stream().map(ResourceLocation::new).toList());;
		Config.ammoMaxBlocks = AMMO_MAX_BLOCKS.get();
		Config.ammoMaxActivateSeconds = AMMO_MAX_ACTIVATE_SECONDS.get();
		Config.removeAmmoAfterExpired = REMOVE_AMMO_AFTER_EXPIRED.get();
		Config.ammoManagerAnchoringRange = AMMO_MANAGER_ANCHORING_RANGE.get();
		Config.shipFreezing = SHIP_FREEZING.get();
	}

	public static void syncClient(Path path) {
	}
}
