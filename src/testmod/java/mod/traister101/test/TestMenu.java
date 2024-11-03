package mod.traister101.test;

import mod.trasiter101.esc.common.capability.ExtendedSlotCapacityHandler;
import mod.trasiter101.esc.common.menu.ExtendedSlotCapacityMenu;
import mod.trasiter101.esc.common.slot.ExtendedSlotItemHandler;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.items.IItemHandler;

public class TestMenu extends ExtendedSlotCapacityMenu {

	protected TestMenu(final int windowId, final Inventory inventory, final IItemHandler handler) {
		super(TestMod.TEST_MENU.get(), windowId, handler.getSlots());

		this.addContainerSlots(handler);
		this.addPlayerInventorySlots(inventory);
	}

	public static TestMenu fromNetwork(int i, Inventory inventory, FriendlyByteBuf friendlyByteBuf) {
		return new TestMenu(i, inventory, new ExtendedSlotCapacityHandler(friendlyByteBuf.readVarInt(), friendlyByteBuf.readVarInt()));
	}

	@Override
	public ItemStack quickMoveStack(final Player player, final int slotIndex) {
		return ExtendedSlotCapacityMenu.quickMoveStack(this, slotIndex);
	}

	@Override
	public boolean stillValid(final Player player) {
		return true;
	}

	/**
	 * Adds the slots for this container
	 */
	protected void addContainerSlots(final IItemHandler handler) {
		switch (containerSlots) {
			case 1 -> addSlots(handler, 1, 1, 80, 32);
			case 4 -> addSlots(handler, 2, 2, 71, 23);
			case 8 -> addSlots(handler, 2, 4, 53, 23);
			case 18 -> addSlots(handler, 2, 9, 8, 23);
			default -> {
				// We want to round up, integer math rounds down
				final int rows = Math.round((float) containerSlots / 9);
				final int columns = containerSlots / rows;
				addSlots(handler, rows, columns);
			}
		}
	}

	/**
	 * Dynamically adds slots to the container depending on the amount of rows and columns.
	 *
	 * @param rows How many rows of slots
	 * @param columns How many columns of slots
	 * @param startX The X starting position
	 * @param startY The Y starting position
	 */
	private void addSlots(final IItemHandler handler, final int rows, final int columns, final int startX, final int startY) {
		assert rows != 0 : "Cannot have zero rows of slots";
		assert columns != 0 : "Cannot have zero columns of slots";

		for (int row = 0; row < rows; row++) {
			for (int column = 0; column < columns; column++) {
				final int yPosition = startY + row * 18;
				final int xPosition = startX + column * 18;
				final int index = column + row * columns;
				addSlot(new ExtendedSlotItemHandler(handler, index, xPosition, yPosition));
			}
		}
	}

	/**
	 * Dynamically adds slots to the container depending on the amount of rows and columns. Will start from the top left
	 *
	 * @param rows How many rows of slots
	 * @param columns How many columns of slots
	 */
	private void addSlots(final IItemHandler handler, final int rows, final int columns) {
		if (rows > 1) {
			addSlots(handler, rows - 1, 9, 8, 18);
		}

		for (int column = 0; column < columns; column++) {
			final int yPosition = 18 * (rows - 1) + 18;
			final int xPosition = 8 + column * 18;
			final int index = column + (rows - 1) * columns;
			addSlot(new ExtendedSlotItemHandler(handler, index, xPosition, yPosition));
		}
	}

	/**
	 * Adds the player inventory slots to the container.
	 */
	protected final void addPlayerInventorySlots(final Inventory inventory) {
		// Main Inventory. Indexes [0, 27)
		for (int i = 0; i < 3; i++) {
			for (int j = 0; j < 9; j++) {
				addSlot(new Slot(inventory, j + i * 9 + 9, 8 + j * 18, 84 + i * 18));
			}
		}

		// Hotbar. Indexes [27, 36)
		for (int k = 0; k < 9; k++) {
			addSlot(new Slot(inventory, k, 8 + k * 18, 142));
		}
	}
}