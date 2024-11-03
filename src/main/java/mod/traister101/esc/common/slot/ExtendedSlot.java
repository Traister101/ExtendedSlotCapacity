package mod.traister101.esc.common.slot;

import net.minecraft.world.Container;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Same as {@link Slot} but with an overridden {@link #getMaxStackSize(ItemStack)}
 */
public class ExtendedSlot extends Slot {

	public ExtendedSlot(final Container container, final int slotIndex, final int xPosition, final int yPosition) {
		super(container, slotIndex, xPosition, yPosition);
	}

	@Override
	public int getMaxStackSize(final ItemStack itemStack) {
		return getMaxStackSize();
	}
}