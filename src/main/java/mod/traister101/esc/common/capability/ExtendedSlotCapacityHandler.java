package mod.traister101.esc.common.capability;

import mod.traister101.esc.ExtendedContainerHelper;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.*;

/**
 * {@link IItemHandler} for extended stack sizes.
 * Limited to {@link Integer#MAX_VALUE} as that's what {@link ItemStack} uses to store the count internally.
 */
public class ExtendedSlotCapacityHandler extends ItemStackHandler {

	/**
	 * The maximum item count a slot can hold
	 */
	public final int slotStackLimit;

	/**
	 * @param slotStackLimit The maximum item count a slot can hold
	 */
	@SuppressWarnings("unused")
	public ExtendedSlotCapacityHandler(final int slotCount, final int slotStackLimit) {
		super(slotCount);
		this.slotStackLimit = slotStackLimit;
	}

	/**
	 * @param slotStackLimit The maximum item count a slot can hold
	 */
	@SuppressWarnings("unused")
	public ExtendedSlotCapacityHandler(final NonNullList<ItemStack> stacks, final int slotStackLimit) {
		super(stacks);
		this.slotStackLimit = slotStackLimit;
	}

	@Override
	public int getSlotLimit(final int slotIndex) {
		return slotStackLimit;
	}

	@Override
	public int getStackLimit(final int slotIndex, final ItemStack itemStack) {
		if (itemStack.getMaxStackSize() == 1) return 1;
		return itemStack.isDamageableItem() ? itemStack.getMaxStackSize() : getSlotLimit(slotIndex);
	}

	@Override
	public CompoundTag serializeNBT() {
		return ExtendedContainerHelper.saveAllItems(new CompoundTag(), stacks, slotStackLimit);
	}

	@Override
	public void deserializeNBT(final CompoundTag nbt) {
		ExtendedContainerHelper.loadAllItems(nbt, stacks);
		onLoad();
	}
}