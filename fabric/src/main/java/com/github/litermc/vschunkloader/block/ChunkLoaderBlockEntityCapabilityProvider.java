package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.VSCRegistry;

import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.minecraft.core.Direction;
import team.reborn.energy.api.EnergyStorage;

public class ChunkLoaderBlockEntityCapabilityProvider {
	public static void register() {
		EnergyStorage.SIDED.registerForBlockEntity(ChunkLoaderBlockEntityCapabilityProvider::energyStorageGetter, VSCRegistry.BlockEntities.CHUNK_LOADER.get());
		EnergyStorage.SIDED.registerForBlockEntity(ChunkLoaderBlockEntityCapabilityProvider::energyStorageGetter, VSCRegistry.BlockEntities.CHUNK_LOADER_WEAK.get());
	}

	private static EnergyStorage energyStorageGetter(final ChunkLoaderBlockEntity be, final Direction side) {
		if (be.energyStorage == null) {
			be.energyStorage = new EnergyStorageImpl(be);
		}
		return (EnergyStorage)(be.energyStorage);
	}

	private static final class EnergyStorageImpl implements EnergyStorage {
		private final ChunkLoaderBlockEntity be;

		private EnergyStorageImpl(final ChunkLoaderBlockEntity be) {
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
