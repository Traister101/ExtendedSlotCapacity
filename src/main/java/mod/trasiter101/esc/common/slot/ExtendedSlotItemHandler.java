package mod.trasiter101.esc.common.slot;

import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;
import net.minecraftforge.items.SlotItemHandler;

/**
 * Same as Forges {@link SlotItemHandler} but with an overridden {@link Slot#getMaxStackSize(ItemStack)} which respects the extended slot count
 */
public class ExtendedSlotItemHandler extends SlotItemHandler {

	public ExtendedSlotItemHandler(final IItemHandler itemHandler, final int slotIndex, final int xPosition, final int yPosition) {
		super(itemHandler, slotIndex, xPosition, yPosition);
	}

	@Override
	public int getMaxStackSize(final ItemStack itemStack) {
		final int maxInput = getMaxStackSize();
		final ItemStack maxAdd = itemStack.copyWithCount(maxInput);

		final IItemHandler handler = getItemHandler();
		final int slotIndex = getSlotIndex();
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