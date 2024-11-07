package mod.traister101.esc.common;

import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;

/**
 * An implementation of {@link Container} for extended slot capacity. You'd want to use this wherever you'd normally use {@link SimpleContainer}.
 */
public class ExtendedCapacitySimpleContainer extends SimpleContainer {

	private final int slotCapacity;

	public ExtendedCapacitySimpleContainer(final int size, final int slotCapacity) {
		super(size);
		this.slotCapacity = slotCapacity;
	}

	@SuppressWarnings("unused")
	public ExtendedCapacitySimpleContainer(final int slotCapacity, final ItemStack... items) {
		super(items);
		this.slotCapacity = slotCapacity;
	}

	@Override
	public int getMaxStackSize() {
		return slotCapacity;
	}
}