package com.github.litermc.vschunkloader.block.ammo;

import com.github.litermc.vschunkloader.Constants;
import com.github.litermc.vschunkloader.compat.CompatMods;

import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.capabilities.ICapabilityProvider;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.AttachCapabilitiesEvent;

import dan200.computercraft.shared.Capabilities;

public final class AmmoAssemblerBlockEntityCapabilityProvider implements ICapabilityProvider {
	public static final ResourceLocation CAPABILITY_ID = new ResourceLocation(Constants.MOD_ID, "ammo_assembler");

	private final AmmoAssemblerBlockEntity be;
	private final LazyOptional<IEnergyStorage> energyStorage;
	private final LazyOptional<Object> lazyPeripheral;

	private AmmoAssemblerBlockEntityCapabilityProvider(final AmmoAssemblerBlockEntity be) {
		this.be = be;
		this.energyStorage = LazyOptional.of(() -> new EnergyStorage(this.be));
		this.lazyPeripheral = LazyOptional.of(this.be::createPeripheral);
	}

	@Override
	public <T> LazyOptional<T> getCapability(final Capability<T> cap, final Direction side) {
		if (cap == ForgeCapabilities.ENERGY) {
			return this.energyStorage.cast();
		}
		if (CompatMods.COMPUTERCRAFT.isLoaded() && cap == Capabilities.CAPABILITY_PERIPHERAL) {
			return this.lazyPeripheral.cast();
		}
		return LazyOptional.empty();
	}

	private void invalidate() {
		this.energyStorage.invalidate();
		this.lazyPeripheral.invalidate();
	}

	public static void onGatherCapabilities(final AttachCapabilitiesEvent<AmmoAssemblerBlockEntity> event) {
		final AmmoAssemblerBlockEntityCapabilityProvider provider = new AmmoAssemblerBlockEntityCapabilityProvider(event.getObject());
		event.addCapability(CAPABILITY_ID, provider);
		event.addListener(provider::invalidate);
	}

	private static final class EnergyStorage implements IEnergyStorage {
		private final AmmoAssemblerBlockEntity be;

		private EnergyStorage(final AmmoAssemblerBlockEntity be) {
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
