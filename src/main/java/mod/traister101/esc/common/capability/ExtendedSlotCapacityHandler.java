package mod.traister101.esc.common.capability;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
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
		return itemStack.isDamageableItem() ? itemStack.getMaxStackSize() : getSlotLimit(slotIndex);
	}

	@Override
	public CompoundTag serializeNBT() {
		final ListTag nbtTagList = new ListTag();
		for (int slotIndex = 0; slotIndex < stacks.size(); slotIndex++) {
			final ItemStack slotStack = stacks.get(slotIndex);
			if (slotStack.isEmpty()) continue;

			final int realCount = Math.min(slotStackLimit, slotStack.getCount());
			final CompoundTag itemTag = new CompoundTag();
			itemTag.putInt("Slot", slotIndex);
			slotStack.save(itemTag);
			itemTag.putInt("ExtendedCount", realCount);
			nbtTagList.add(itemTag);
		}

		final CompoundTag nbt = new CompoundTag();
		nbt.put("Items", nbtTagList);
		return nbt;
	}

	@Override
	public void deserializeNBT(final CompoundTag nbt) {
		final ListTag tagList = nbt.getList("Items", Tag.TAG_COMPOUND);
		for (int i = 0; i < tagList.size(); i++) {
			final CompoundTag itemTag = tagList.getCompound(i);

			final int slotIndex = itemTag.getInt("Slot");

			if (0 > slotIndex || stacks.size() <= slotIndex) continue;

			final ItemStack itemStack = ItemStack.of(itemTag);
			itemStack.setCount(itemTag.getInt("ExtendedCount"));
			stacks.set(slotIndex, itemStack);
		}
		onLoad();
	}
}