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
import net.minecraft.server.MinecraftServer;

import org.valkyrienskies.core.api.ships.Ship;
import org.valkyrienskies.mod.common.command.ShipArgument;
import org.valkyrienskies.mod.mixinducks.feature.command.VSCommandSource;

import java.util.Set;

public final class VSCCommands {
	public static final String ROOT_LITERAL = "vschunkloader";

	private VSCCommands() {}

	public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
		dispatcher.register(Commands.literal(ROOT_LITERAL)
			.requires((source) -> source.hasPermission(2))
			.then(Commands.literal("forceload")
				.then(Commands.argument("ships", ShipArgument.Companion.ships())
					.then(Commands.argument("forceload", BoolArgumentType.bool())
						.executes(VSCCommands::setForceLoad)
					)
				)
			)
			.then(Commands.literal("is-forceloaded")
				.then(Commands.argument("ships", ShipArgument.Companion.ships())
					.executes(VSCCommands::isForceLoaded)
				)
			)
		);
	}

	private static int setForceLoad(final CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		final CommandSourceStack source = context.getSource();
		final MinecraftServer server = source.getServer();
		final Set<Ship> ships = ShipArgument.Companion.getShips((CommandContext<VSCommandSource>)((CommandContext<?>)(context)), "ships");
		final boolean forceload = BoolArgumentType.getBool(context, "forceload");
		int successCount = 0;
		for (final Ship ship : ships) {
			if (VSCApi.forceLoad(server, ship.getId(), Constants.MOD_ID, forceload)) {
				successCount++;
			}
		}
		final int finalSuccessCount = successCount;
		source.sendSuccess(() ->
			Component.translatable("command." + Constants.MOD_ID + ".forceload." + (forceload ? "load" : "unload"), finalSuccessCount),
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
}
