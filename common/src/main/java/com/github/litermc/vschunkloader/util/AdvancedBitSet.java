package com.github.litermc.vschunkloader.util;

import java.util.BitSet;

public final class AdvancedBitSet {
	private final BitSet bitset;
	private int count;

	public AdvancedBitSet() {
		this.bitset = new BitSet();
	}

	public AdvancedBitSet(final int bsize) {
		this.bitset = new BitSet(bsize);
	}

	public int count() {
		return this.count;
	}

	public boolean isEmpty() {
		return this.count == 0;
	}

	public void clear() {
		this.bitset.clear();
		this.count = 0;
	}

	public boolean get(final int index) {
		return this.bitset.get(index);
	}

	public boolean set(final int index, final boolean value) {
		if (this.bitset.get(index) == value) {
			return false;
		}
		this.bitset.set(index, value);
		this.count += value ? 1 : -1;
		return true;
	}

	public boolean clear(final int index) {
		return this.set(index, false);
	}

	public boolean set(final int index) {
		return this.set(index, true);
	}
}
