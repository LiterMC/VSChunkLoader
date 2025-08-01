package com.github.litermc.vschunkloader.block;

import com.github.litermc.vschunkloader.Constants;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;

public final class ChunkLoaderBlockEntityCapabilityProvider implements ICapabilityProvider {
	public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Constants.MOD_ID, "chunk_loader");

	private final ChunkLoaderBlockEntity be;
	private final LazyOptional<IEnergyStorage> energyStorage;

	private ChunkLoaderBlockEntityCapabilityProvider(final ChunkLoaderBlockEntity be) {
		this.be = be;
		this.energyStorage = LazyOptional.of(() -> new EnergyStorage(this.be));
	}

	@Override
	public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
		if (cap == ForgeCapabilities.ENERGY) {
			return this.energyStorage.cast();
		}
		return LazyOptional.empty();
	}

	private void invalidate() {
		this.energyStorage.invalidate();
	}

	public static void onGatherCapabilities(final AttachCapabilitiesEvent<ChunkLoaderBlockEntity> event) {
		final ChunkLoaderBlockEntityCapabilityProvider provider = new ChunkLoaderBlockEntityCapabilityProvider(event.getObject());
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}

	private static final class EnergyStorage implements IEnergyStorage {
		private final ChunkLoaderBlockEntity be;

		private EnergyStorage(final ChunkLoaderBlockEntity be) {
			this.be = be;
		}

		public int receiveEnergy(final int maxReceive, final boolean simulate) {
			return this.be.receiveEnergy(maxReceive, simulate);
		}

		public int extractEnergy(final int maxExtract, final boolean simulate) {
			return 0;
		}

		public int getEnergyStored() {
			return this.be.getEnergyStored();
		}

		public int getMaxEnergyStored() {
			return this.be.getMaxEnergyStored();
		}

		public boolean canExtract() {
			return false;
		}

		public boolean canReceive() {
			return true;
		}
	}
}
