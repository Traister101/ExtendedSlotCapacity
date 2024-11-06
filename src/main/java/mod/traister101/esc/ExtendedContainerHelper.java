package mod.traister101.esc;

import net.minecraft.core.NonNullList;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

public final class ExtendedContainerHelper {

	public static CompoundTag saveAllItems(final CompoundTag compoundTag, final NonNullList<ItemStack> itemStacks, final int slotStackLimit) {
		return saveAllItems(compoundTag, itemStacks, slotStackLimit, true);
	}

	public static CompoundTag saveAllItems(final CompoundTag compoundTag, final NonNullList<ItemStack> itemStacks, final int slotStackLimit,
			final boolean saveEmpty) {
		final ListTag nbtTagList = new ListTag();
		for (int slotIndex = 0; slotIndex < itemStacks.size(); slotIndex++) {
			final ItemStack slotStack = itemStacks.get(slotIndex);
			if (slotStack.isEmpty()) continue;

			final int realCount = Math.min(slotStackLimit, slotStack.getCount());
			final CompoundTag itemTag = new CompoundTag();
			itemTag.putInt("Slot", slotIndex);
			slotStack.save(itemTag);
			itemTag.putInt("ExtendedCount", realCount);
			nbtTagList.add(itemTag);
		}


		if (!nbtTagList.isEmpty() || saveEmpty) compoundTag.put("Items", nbtTagList);

		return compoundTag;
	}

	public static void loadAllItems(final CompoundTag compoundTag, final NonNullList<ItemStack> itemStacks) {
		final ListTag tagList = compoundTag.getList("Items", Tag.TAG_COMPOUND);
		for (int i = 0; i < tagList.size(); i++) {
			final CompoundTag itemTag = tagList.getCompound(i);

			final int slotIndex = itemTag.getInt("Slot");

			if (0 > slotIndex || itemStacks.size() <= slotIndex) continue;

			final ItemStack itemStack = ItemStack.of(itemTag);
			itemStack.setCount(itemTag.getInt("ExtendedCount"));
			itemStacks.set(slotIndex, itemStack);
		}
	}
}