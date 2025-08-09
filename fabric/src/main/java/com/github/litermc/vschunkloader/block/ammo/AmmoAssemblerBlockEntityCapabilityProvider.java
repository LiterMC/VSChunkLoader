package com.github.litermc.vschunkloader.block.ammo;

import com.github.litermc.vschunkloader.VSCRegistry;
import com.github.litermc.vschunkloader.compat.CompatMods;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import team.reborn.energy.api.EnergyStorage;

import dan200.computercraft.api.peripheral.IPeripheral;
import dan200.computercraft.api.peripheral.PeripheralLookup;

import java.util.function.BiFunction;

public class AmmoAssemblerBlockEntityCapabilityProvider {
	public static void register() {
		EnergyStorage.SIDED.registerForBlockEntity(AmmoAssemblerBlockEntityCapabilityProvider::energyStorageGetter, VSCRegistry.BlockEntities.AMMO_ASSEMBLER.get());
		if (CompatMods.COMPUTERCRAFT.isLoaded()) {
			PeripheralLookup.get().registerForBlockEntity(AmmoAssemblerBlockEntityCapabilityProvider::peripheralGetter, VSCRegistry.BlockEntities.AMMO_ASSEMBLER.get());
		}
	}

	private static EnergyStorage energyStorageGetter(final AmmoAssemblerBlockEntity be, final Direction side) {
		if (be.energyStorage == null) {
			be.energyStorage = new EnergyStorageImpl(be);
		}
		return (EnergyStorage)(be.energyStorage);
	}

	private static <T> T peripheralGetter(final AmmoAssemblerBlockEntity be, final Direction side) {
		if (be.peripheral == null) {
			be.peripheral = be.createPeripheral();
		}
		return (T)(be.peripheral);
	}

	private static final class EnergyStorageImpl implements EnergyStorage {
		private final AmmoAssemblerBlockEntity be;

		private EnergyStorageImpl(final AmmoAssemblerBlockEntity be) {
			this.be = be;
		}

		@Override
		public boolean supportsInsertion() {
			return true;
		}

		@Override
		public long insert(final long maxAmount, final TransactionContext transaction) {
			final int maxReceive = maxAmount > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int)(maxAmount);
			final int recieved = this.be.receiveEnergy(maxReceive, true);
			if (recieved > 0) {
				transaction.addCloseCallback((transactionCtx, result) -> {
					if (result.wasCommitted()) {
						this.be.receiveEnergy(maxReceive, false);
					}
				});
			}
			return recieved;
		}

		@Override
		public boolean supportsExtraction() {
			return false;
		}

		@Override
		public long extract(final long maxAmount, final TransactionContext transaction) {
			return 0;
		}

		@Override
		public long getAmount() {
			return this.be.getEnergyStored();
		}

		@Override
		public long getCapacity() {
			return this.be.getMaxEnergyStored();
		}
	}
}
