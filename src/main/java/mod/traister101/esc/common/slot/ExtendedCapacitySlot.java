package mod.traister101.esc.common.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Same as {@link Slot} but with an overridden {@link #getMaxStackSize(ItemStack)}
 */
public class ExtendedCapacitySlot extends Slot {

	public ExtendedCapacitySlot(final Container container, final int slotIndex, final int xPosition, final int yPosition) {
		super(container, slotIndex, xPosition, yPosition);
	}

	@Override
	public int getMaxStackSize(final ItemStack itemStack) {
		return itemStack.isDamageableItem() ? itemStack.getMaxStackSize() : getMaxStackSize();
	}

	@Override
	public ItemStack safeInsert(final ItemStack insertStack, final int increment) {
		if (insertStack.isEmpty() || !mayPlace(insertStack)) return insertStack;

		final ItemStack currentStack = getItem();
		final int maxSize = getMaxStackSize(insertStack);
		final int insertAmount = Math.min(Math.min(increment, insertStack.getCount()), maxSize);
		if (currentStack.isEmpty()) {
			setByPlayer(insertStack.split(insertAmount));
			return insertStack;
		}

		if (ItemStack.isSameItemSameTags(currentStack, insertStack)) {
			insertStack.shrink(insertAmount);
			currentStack.grow(insertAmount);
			setByPlayer(currentStack);
			return insertStack;
		}
		return insertStack;
	}
}