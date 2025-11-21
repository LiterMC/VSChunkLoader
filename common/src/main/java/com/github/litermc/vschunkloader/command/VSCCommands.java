package com.github.litermc.vschunkloader.command;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.VSCApi;

import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;

import org.valkyrienskies.core.api.ships.ServerShip;
import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.ShipArgument;
import org.valkyrienskies.mod.mixinducks.feature.command.VSCommandSource;

import java.util.Set;

public final class VSCCommands {
	public static final String ROOT_LITERAL = "vschunkloader";
	public static final ResourceLocation FORCELOAD_TOKEN = new ResourceLocation(Constants.MOD_ID, "command");

	private VSCCommands() {}

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(ROOT_LITERAL)
			.requires((source) -> source.hasPermission(2))
			.then(Commands.literal("forceload")
				.then(Commands.argument("ships", ShipArgument.Companion.ships())
					.executes(VSCCommands::forceLoad)
				)
			)
			.then(Commands.literal("unforceload")
				.then(Commands.argument("ships", ShipArgument.Companion.ships())
					.executes(VSCCommands::unforceLoad)
				)
			)
			.then(Commands.literal("unforceload-all")
				.then(Commands.argument("ships", ShipArgument.Companion.ships())
					.executes(VSCCommands::unforceLoadAll)
				)
			)
			.then(Commands.literal("is-forceloaded")
				.then(Commands.argument("ships", ShipArgument.Companion.ships())
					.executes(VSCCommands::isForceLoaded)
				)
			)
			.then(Commands.literal("query-forceload-tokens")
				.then(Commands.argument("ships", ShipArgument.Companion.ships())
					.executes(VSCCommands::queryForceLoadTokens)
				)
			)
		);
	}

	private static int forceLoad(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final Set<Ship> ships = ShipArgument.Companion.getShips((CommandContext<VSCommandSource>)((CommandContext<?>)(context)), "ships");
		int successCount = 0;
		for (final Ship ship : ships) {
			if (VSCApi.forceLoad(server, ship.getId(), FORCELOAD_TOKEN, true)) {
				successCount++;
			}
		}
		final int finalSuccessCount = successCount;
		source.sendSuccess(() ->
			Component.translatable("command." + Constants.MOD_ID + ".forceload", finalSuccessCount),
			true
		);
		return finalSuccessCount;
	}

	private static int unforceLoad(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final Set<Ship> ships = ShipArgument.Companion.getShips((CommandContext<VSCommandSource>)((CommandContext<?>)(context)), "ships");
		int successCount = 0;
		for (final Ship ship : ships) {
			if (VSCApi.forceLoad(server, ship.getId(), FORCELOAD_TOKEN, false)) {
				successCount++;
			}
		}
		final int finalSuccessCount = successCount;
		source.sendSuccess(() ->
			Component.translatable("command." + Constants.MOD_ID + ".unforceload", finalSuccessCount),
			true
		);
		return finalSuccessCount;
	}

	private static int unforceLoadAll(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final Set<Ship> ships = ShipArgument.Companion.getShips((CommandContext<VSCommandSource>)((CommandContext<?>)(context)), "ships");
		int successCount = 0;
		for (final Ship ship : ships) {
			if (VSCApi.clearForceLoadTokens(server, ship.getId())) {
				successCount++;
			}
		}
		final int finalSuccessCount = successCount;
		source.sendSuccess(() ->
			Component.translatable("command." + Constants.MOD_ID + ".unforceload", finalSuccessCount),
			true
		);
		return finalSuccessCount;
	}

	private static int isForceLoaded(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final Set<Ship> ships = ShipArgument.Companion.getShips((CommandContext<VSCommandSource>)((CommandContext<?>)(context)), "ships");
		int loadedCount = 0;
		for (final Ship ship : ships) {
			if (VSCApi.isForceLoaded(server, ship.getId())) {
				loadedCount++;
			}
		}
		final int finalLoadedCount = loadedCount;
		source.sendSuccess(() ->
			finalLoadedCount == 0
				? Component.translatable("command." + Constants.MOD_ID + ".is_forceloaded.none")
				: Component.translatable("command." + Constants.MOD_ID + ".is_forceloaded", finalLoadedCount),
			false
		);
		return finalLoadedCount;
	}

	private static int queryForceLoadTokens(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final Set<Ship> ships = ShipArgument.Companion.getShips((CommandContext<VSCommandSource>)((CommandContext<?>)(context)), "ships");
		if (ships.isEmpty()) {
			source.sendFailure(Component.translatable("argument.valkyrienskies.ship.no_found"));
			return 0;
		}
		if (ships.size() > 1) {
			source.sendFailure(Component.translatable("argument.valkyrienskies.ship.multiple_found"));
			return 0;
		}
		final Ship ship = ships.iterator().next();
		final Set<ResourceLocation> tokens = VSCApi.getForceLoadTokens(server, ship.getId());
		if (tokens == null) {
			source.sendFailure(Component.translatable("argument.valkyrienskies.ship.no_found"));
			return 0;
		}
		final int count = tokens.size();
		if (count == 0) {
			source.sendSuccess(() -> Component.translatable("command." + Constants.MOD_ID + ".query_forceload.none"), false);
			return 1;
		}
		source.sendSuccess(() -> {
			final MutableComponent component = Component.translatable("command." + Constants.MOD_ID + ".query_forceload.title", count);
			tokens.stream()
				.map(ResourceLocation::toString)
				.sorted()
				.map((token) -> Component.literal("\n- ").append(token))
				.forEach(component::append);
			return component;
		}, false);
		return count + 1;
	}
}
