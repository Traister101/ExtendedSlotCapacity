package mod.traister101.esc.common.slot;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.*;

/**
 * Same as Forges {@link SlotItemHandler} but with an overridden {@link Slot#getMaxStackSize(ItemStack)} which respects the extended slot count
 */
public class ExtendedSlotItemHandler extends SlotItemHandler {

	public final int slotIndex;

	public ExtendedSlotItemHandler(final IItemHandler itemHandler, final int slotIndex, final int xPosition, final int yPosition) {
		super(itemHandler, slotIndex, xPosition, yPosition);
		this.slotIndex = slotIndex;
	}

	@Override
	public int getMaxStackSize(final ItemStack itemStack) {
		final IItemHandler handler = getItemHandler();
		final int maxInput = handler.getSlotLimit(slotIndex);
		final ItemStack maxAdd = itemStack.copyWithCount(maxInput);

		final ItemStack currentStack = handler.getStackInSlot(slotIndex);

		if (handler instanceof final IItemHandlerModifiable handlerModifiable) {

			handlerModifiable.setStackInSlot(slotIndex, ItemStack.EMPTY);

			final ItemStack remainder = handlerModifiable.insertItem(slotIndex, maxAdd, true);

			handlerModifiable.setStackInSlot(slotIndex, currentStack);

			return maxInput - remainder.getCount();
		}

		final ItemStack remainder = handler.insertItem(slotIndex, maxAdd, true);

		final int current = currentStack.getCount();
		final int added = maxInput - remainder.getCount();
		return current + added;
	}
}