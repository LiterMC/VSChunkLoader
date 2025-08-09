// SPDX-FileCopyrightText: 2019 The CC: Tweaked Developers
//
// SPDX-License-Identifier: MPL-2.0

package com.github.litermc.vschunkloader;

import com.github.litermc.vschunkloader.block.ChunkLoaderBlock;
import com.github.litermc.vschunkloader.block.ChunkLoaderBlockEntity;
import com.github.litermc.vschunkloader.block.ChunkLoaderWeakBlock;
import com.github.litermc.vschunkloader.block.ChunkLoaderWeakBlockEntity;
import com.github.litermc.vschunkloader.block.ammo.AmmoAssemblerBlock;
import com.github.litermc.vschunkloader.block.ammo.AmmoAssemblerBlockEntity;
import com.github.litermc.vschunkloader.block.ammo.AmmoManagerBlock;
import com.github.litermc.vschunkloader.block.ammo.AmmoManagerBlockEntity;
import com.github.litermc.vschunkloader.platform.PlatformHelper;
import com.github.litermc.vschunkloader.platform.RegistrationHelper;
import com.github.litermc.vschunkloader.platform.RegistryEntry;

import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.PushReaction;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;

public final class VSCRegistry {
	private VSCRegistry() {}

	public static void register() {
		Blocks.REGISTRY.register();
		BlockEntities.REGISTRY.register();
		Items.REGISTRY.register();
		CreativeTabs.REGISTRY.register();
	}

	public static final class Blocks {
		private static final RegistrationHelper<Block> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.BLOCK);

		public static final RegistryEntry<AmmoAssemblerBlock> AMMO_ASSEMBLER =
			REGISTRY.register("ammo_assembler", () -> new AmmoAssemblerBlock(
				BlockBehaviour.Properties.of()
					.strength(5f)
					.sound(SoundType.ANVIL)
					.pushReaction(PushReaction.IGNORE)
					.isRedstoneConductor((state, level, pos) -> false)
					.requiresCorrectToolForDrops()));

		public static final RegistryEntry<AmmoManagerBlock> AMMO_MANAGER =
			REGISTRY.register("ammo_manager", () -> new AmmoManagerBlock(
				BlockBehaviour.Properties.of()
					.strength(10f)
					.sound(SoundType.COPPER)
					.pushReaction(PushReaction.IGNORE)
					.isRedstoneConductor((state, level, pos) -> false)
					.requiresCorrectToolForDrops()));

		public static final RegistryEntry<ChunkLoaderBlock> CHUNK_LOADER =
			REGISTRY.register("chunk_loader", () -> new ChunkLoaderBlock(
				BlockBehaviour.Properties.of()
					.strength(5f)
					.sound(SoundType.METAL)
					.pushReaction(PushReaction.IGNORE)
					.noOcclusion()
					.isRedstoneConductor((state, level, pos) -> false)
					.requiresCorrectToolForDrops()));

		public static final RegistryEntry<ChunkLoaderWeakBlock> CHUNK_LOADER_WEAK =
			REGISTRY.register("chunk_loader_weak", () -> new ChunkLoaderWeakBlock(
				BlockBehaviour.Properties.of()
					.strength(3f)
					.sound(SoundType.STONE)
					.pushReaction(PushReaction.IGNORE)
					.noOcclusion()
					.isRedstoneConductor((state, level, pos) -> false)
					.requiresCorrectToolForDrops()));

		private Blocks() {}
	}

	public static final class BlockEntities {
		private static final RegistrationHelper<BlockEntityType<?>> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.BLOCK_ENTITY_TYPE);

		private static <T extends BlockEntity> RegistryEntry<BlockEntityType<T>> ofBlock(final RegistryEntry<? extends Block> block, final BiFunction<BlockPos, BlockState, T> factory) {
			return REGISTRY.register(block.id().getPath(), () -> PlatformHelper.get().createBlockEntityType(factory, block.get()));
		}

		public static final RegistryEntry<BlockEntityType<AmmoAssemblerBlockEntity>> AMMO_ASSEMBLER =
			ofBlock(Blocks.AMMO_ASSEMBLER, AmmoAssemblerBlockEntity::new);

		public static final RegistryEntry<BlockEntityType<AmmoManagerBlockEntity>> AMMO_MANAGER =
			ofBlock(Blocks.AMMO_MANAGER, AmmoManagerBlockEntity::new);

		public static final RegistryEntry<BlockEntityType<ChunkLoaderBlockEntity>> CHUNK_LOADER =
			ofBlock(Blocks.CHUNK_LOADER, ChunkLoaderBlockEntity::new);

		public static final RegistryEntry<BlockEntityType<ChunkLoaderWeakBlockEntity>> CHUNK_LOADER_WEAK =
			ofBlock(Blocks.CHUNK_LOADER_WEAK, ChunkLoaderWeakBlockEntity::new);

		private BlockEntities() {}
	}

	public static final class Items {
		private static final RegistrationHelper<Item> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.ITEM);
		private static final List<RegistryEntry<? extends Item>> TAB_ITEMS = new ArrayList<>();

		private static Item.Properties properties() {
			return new Item.Properties();
		}

		private static <B extends Block, I extends Item> RegistryEntry<I> ofBlock(RegistryEntry<B> block, BiFunction<B, Item.Properties, I> supplier) {
			final RegistryEntry<I> entry = REGISTRY.register(block.id().getPath(), () -> supplier.apply(block.get(), properties()));
			TAB_ITEMS.add(entry);
			return entry;
		}

		public static final RegistryEntry<BlockItem> AMMO_ASSEMBLER = ofBlock(
			Blocks.AMMO_ASSEMBLER,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(64))
		);

		public static final RegistryEntry<BlockItem> AMMO_MANAGER = ofBlock(
			Blocks.AMMO_MANAGER,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.RARE).stacksTo(1))
		);

		public static final RegistryEntry<BlockItem> CHUNK_LOADER = ofBlock(
			Blocks.CHUNK_LOADER,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.EPIC).stacksTo(1))
		);

		public static final RegistryEntry<BlockItem> CHUNK_LOADER_WEAK = ofBlock(
			Blocks.CHUNK_LOADER_WEAK,
			(block, props) -> new BlockItem(block, props.rarity(Rarity.UNCOMMON).stacksTo(16))
		);

		private Items() {}
	}

	static class CreativeTabs {
		static final RegistrationHelper<CreativeModeTab> REGISTRY = PlatformHelper.get().createRegistrationHelper(Registries.CREATIVE_MODE_TAB);

		private static final RegistryEntry<CreativeModeTab> TAB = REGISTRY.register(
			"tab",
			() -> PlatformHelper.get().newCreativeModeTab()
				.icon(() -> new ItemStack(Items.CHUNK_LOADER.get()))
				.title(Component.translatable("itemGroup.vschunkloader"))
				.displayItems((context, out) -> {
					Items.TAB_ITEMS.stream().map(RegistryEntry::get).forEach(out::accept);
				})
				.build()
		);
	}
}
