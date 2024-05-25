package mod.trasiter101.esc.common.menu;

import mod.trasiter101.esc.common.capability.ExtendedSlotCapacityHandler;
import mod.trasiter101.esc.common.slot.ExtendedSlot;
import mod.trasiter101.esc.common.slot.ExtendedSlotItemHandler;
import org.jetbrains.annotations.Contract;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.SlotItemHandler;

/**
 * This is a bare-bones {@link Slot} agnostic Menu for slots which can exceed {@value Container#LARGE_MAX_STACK_SIZE} items.
 * If you wish to use this Menu you need something like {@link ExtendedSlotCapacityHandler} in order to correctly store
 * and serialize the stack sizes as vanilla serialization caps out at {@value Byte#MAX_VALUE}
 * <p>
 * The container slots must support extended counts via {@link Slot#getMaxStackSize()} and {@link Slot#getMaxStackSize(ItemStack)}
 * We provide {@link ExtendedSlotItemHandler} and {@link ExtendedSlot} as the forge provided {@link SlotItemHandler} and vanilla {@link Slot}
 * implementation of {@link Slot#getMaxStackSize(ItemStack)} clamps the max stack size to the passed in stacks {@link ItemStack#getMaxStackSize()}
 *
 * @apiNote Vanillas syncing (anonymous {@link ContainerSynchronizer} class in {@link ServerPlayer}) syncs {@link ItemStack}s via
 * {@link FriendlyByteBuf#writeItemStack(ItemStack, boolean)} and {@link FriendlyByteBuf#readItem()} which serializes the stack count as a byte.
 * We provide {@link ExtendedSlotCapacitySynchronizer} for synchronizing. Children of this menu will automatically have
 * {@link ExtendedSlotCapacitySynchronizer} set as their synchronize.
 */
public abstract class ExtendedSlotCapacityMenu extends AbstractContainerMenu {

	/**
	 * The amount of slots this container has
	 */
	protected final int containerSlots;

	/**
	 * @param containerSlots The amount of slots this container has
	 */
	@Contract(pure = true)
	protected ExtendedSlotCapacityMenu(final MenuType<? extends ExtendedSlotCapacityMenu> menuType, final int windowId, final int containerSlots) {
		super(menuType, windowId);
		this.containerSlots = containerSlots;
	}

	/**
	 * Simple, static implementation of {@link AbstractContainerMenu#quickMoveStack(Player, int)}
	 *
	 * @param menu The {@link ExtendedSlotCapacityMenu}
	 * @param slotIndex The slot index
	 *
	 * @return Contents of slot or {@link ItemStack#EMPTY}
	 */
	@SuppressWarnings("unused")
	protected static ItemStack quickMoveStack(final ExtendedSlotCapacityMenu menu, final int slotIndex) {
		final Slot slot = menu.slots.get(slotIndex);

		if (slot.hasItem()) {
			final ItemStack slotStack = slot.getItem();

			if (slotIndex < menu.containerSlots) {
				if (!menu.moveItemStackTo(slotStack, menu.containerSlots, menu.slots.size(), true)) return ItemStack.EMPTY;
			} else if (!menu.moveItemStackTo(slotStack, 0, menu.containerSlots, false)) return ItemStack.EMPTY;

			if (slotStack.isEmpty()) {
				slot.setByPlayer(ItemStack.EMPTY);
			} else slot.setChanged();

			return slotStack;
		}

		return ItemStack.EMPTY;
	}

	/**
	 * {@inheritDoc}
	 *
	 * @see #quickMoveStack(ExtendedSlotCapacityMenu, int)
	 */
	@Override
	public abstract ItemStack quickMoveStack(final Player player, final int slotIndex);

	@Override
	public void clicked(final int slotIndex, final int mouseButton, final ClickType clickType, final Player player) {
		// Not a slot
		if (0 > slotIndex) {
			if (slotIndex != SLOT_CLICKED_OUTSIDE) return;
			if (clickType != ClickType.PICKUP && clickType != ClickType.QUICK_MOVE) return;
			if (mouseButton != 0 && mouseButton != 1) return;
			if (getCarried().isEmpty()) return;

			final ClickAction clickAction = mouseButton == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;
			if (clickAction != ClickAction.PRIMARY) {
				player.drop(getCarried().split(1), true);
				return;
			}

			player.drop(getCarried(), true);
			setCarried(ItemStack.EMPTY);
			return;
		}

		final Inventory inventory = player.getInventory();
		if (mouseButton == 0 || mouseButton == 1) {
			if (clickType == ClickType.PICKUP) {
				clickPickup(slotIndex, mouseButton, player);
				return;
			}
			if (clickType == ClickType.QUICK_MOVE) {
				clickQuickMove(slotIndex, player);
				return;
			}
		}

		if (clickType == ClickType.SWAP) {
			clickSwap(slotIndex, mouseButton, player, inventory);
			return;
		}

		if (clickType == ClickType.CLONE && player.getAbilities().instabuild && getCarried().isEmpty()) {
			final Slot slot = slots.get(slotIndex);
			if (slot.hasItem()) {
				final ItemStack slotStack = slot.getItem();
				setCarried(slotStack.copyWithCount(slotStack.getMaxStackSize()));
			}
			return;
		}

		if (clickType == ClickType.THROW && getCarried().isEmpty()) {
			final Slot slot = slots.get(slotIndex);
			final int stackCount = mouseButton == 0 ? 1 : slot.getItem().getMaxStackSize();
			final ItemStack dropStack = slot.safeTake(stackCount, Integer.MAX_VALUE, player);
			player.drop(dropStack, true);
			return;
		}

		if (clickType == ClickType.PICKUP_ALL) {
			clickPickupAll(slotIndex, mouseButton, player);
		}
	}

	@Override
	public abstract boolean stillValid(final Player player);

	@Override
	protected boolean moveItemStackTo(final ItemStack movedStack, final int startIndex, final int endIndex, final boolean reverseDirection) {
		boolean haveMovedStack = false;
		// Start iterating from the end if we merge in reverse order
		int slotIndex = reverseDirection ? endIndex - 1 : startIndex;

		if (movedStack.isStackable()) {
			while (!movedStack.isEmpty()) {
				if (reverseDirection) {
					if (slotIndex < startIndex) break;
				} else if (slotIndex >= endIndex) break;

				final Slot slot = slots.get(slotIndex);
				final ItemStack slotStack = slot.getItem();

				// Can't merge these stacks
				if (!ItemStack.isSameItemSameTags(movedStack, slotStack)) {
					slotIndex += (reverseDirection) ? -1 : 1;
					continue;
				}

				final int total = slotStack.getCount() + movedStack.getCount();

				final int maxSize;
				// If it's our container slots use the slot limit to determine the max stack size
				if (slotIndex < containerSlots) {
					maxSize = slot.getMaxStackSize();
				} else maxSize = Math.min(slot.getMaxStackSize(), movedStack.getMaxStackSize());

				// Can fully consume the merge stack
				if (maxSize >= total) {
					movedStack.setCount(0);
					slotStack.setCount(total);
					slot.setChanged();
					haveMovedStack = true;
					slotIndex += (reverseDirection) ? -1 : 1;
					continue;
				}

				// Can only partially consume the stack
				if (maxSize > slotStack.getCount()) {
					movedStack.shrink(maxSize - slotStack.getCount());
					slotStack.grow(maxSize - slotStack.getCount());
					slot.setChanged();
					haveMovedStack = true;
					slotIndex += (reverseDirection) ? -1 : 1;
					continue;
				}
				slotIndex += (reverseDirection) ? -1 : 1;
			}
		}

		// Try and fill empty slots now
		if (!movedStack.isEmpty()) {
			if (reverseDirection) {
				slotIndex = endIndex - 1;
			} else slotIndex = startIndex;

			while (true) {
				if (reverseDirection) {
					if (slotIndex < startIndex) break;
				} else if (slotIndex >= endIndex) break;

				final Slot slot = slots.get(slotIndex);

				// Continue early if we can't put anything in this slot
				if (slot.hasItem() || !slot.mayPlace(movedStack)) {
					slotIndex += (reverseDirection) ? -1 : 1;
					continue;
				}

				// If it's our container slots use the slots stack cap
				if (slotIndex < containerSlots) {
					slot.setByPlayer(movedStack.split(slot.getMaxStackSize()));
					haveMovedStack = true;
					slotIndex += (reverseDirection) ? -1 : 1;
					continue;
				}

				{
					final int splitSize = Math.min(slot.getMaxStackSize(movedStack), movedStack.getMaxStackSize());
					// Can merge
					if (movedStack.getCount() > splitSize) {
						slot.setByPlayer(movedStack.split(splitSize));
						haveMovedStack = true;
						slotIndex += (reverseDirection) ? -1 : 1;
						continue;
					}
				}

				// Put the whole stack in the slot
				slot.setByPlayer(movedStack.split(movedStack.getCount()));
				haveMovedStack = true;
				slotIndex += (reverseDirection) ? -1 : 1;
			}
		}
		return haveMovedStack;
	}

	/**
	 * Called to perform the pickup click action
	 */
	protected void clickPickup(final int slotIndex, final int mouseButton, final Player player) {
		final ClickAction clickAction = mouseButton == 0 ? ClickAction.PRIMARY : ClickAction.SECONDARY;

		final Slot slot = slots.get(slotIndex);
		final ItemStack slotStack = slot.getItem();
		final ItemStack carriedStack = getCarried();
		player.updateTutorialInventoryAction(carriedStack, slotStack, clickAction);
		if (tryItemClickBehaviourOverride(player, clickAction, slot, slotStack, carriedStack)) return;

		if (ForgeHooks.onItemStackedOn(slotStack, carriedStack, slot, clickAction, player, createCarriedSlotAccess())) return;

		if (slotStack.isEmpty()) {
			if (carriedStack.isEmpty()) return;

			final int insertCount = clickAction == ClickAction.PRIMARY ? carriedStack.getCount() : 1;
			setCarried(slot.safeInsert(carriedStack, insertCount));
			slot.setChanged();
			return;
		}

		if (!slot.mayPickup(player)) return;

		// Not holding anything
		if (carriedStack.isEmpty()) {
			// How much we should extract
			final int extractAmount;
			if (clickAction == ClickAction.PRIMARY) {
				extractAmount = slotStack.getCount();
			} else {
				extractAmount = (Math.min(slotStack.getCount(), slotStack.getMaxStackSize()) + 1) / 2;
			}
			slot.tryRemove(extractAmount, Integer.MAX_VALUE, player).ifPresent((stack) -> {
				setCarried(stack);
				slot.onTake(player, stack);
			});
			slot.setChanged();
			return;
		}

		if (slot.mayPlace(carriedStack)) {
			if (ItemStack.isSameItemSameTags(slotStack, carriedStack)) {
				final int insertAmount = clickAction == ClickAction.PRIMARY ? carriedStack.getCount() : 1;
				setCarried(slot.safeInsert(carriedStack, insertAmount));
				slot.setChanged();
				return;
			}

			if (carriedStack.getCount() <= slot.getMaxStackSize(carriedStack)) {
				setCarried(slotStack);
				slot.setByPlayer(carriedStack);
				slot.setChanged();
				return;
			}
		}

		if (ItemStack.isSameItemSameTags(slotStack, carriedStack)) {
			slot.tryRemove(slotStack.getCount(), carriedStack.getMaxStackSize() - carriedStack.getCount(), player).ifPresent((removedStack) -> {
				carriedStack.grow(removedStack.getCount());
				slot.onTake(player, removedStack);
			});
		}
		slot.setChanged();
	}

	/**
	 * Called to quick move stacks
	 */
	protected void clickQuickMove(final int slotIndex, final Player player) {
		final Slot slot = slots.get(slotIndex);
		if (!slot.mayPickup(player)) {
			return;
		}

		ItemStack moveStack = quickMoveStack(player, slotIndex);
		while (!moveStack.isEmpty() && ItemStack.isSameItem(slot.getItem(), moveStack)) {
			moveStack = quickMoveStack(player, slotIndex);
		}
	}

	/**
	 * Called to perform stack swap action
	 */
	protected void clickSwap(final int slotIndex, final int mouseButton, final Player player, final Inventory inventory) {
		final Slot slot = slots.get(slotIndex);
		final ItemStack itemStack = inventory.getItem(mouseButton);
		final ItemStack slotStack = slot.getItem();
		if (itemStack.isEmpty() && slotStack.isEmpty()) return;

		if (itemStack.isEmpty()) {
			if (!slot.mayPickup(player)) return;

			inventory.setItem(mouseButton, slotStack);
			// I think we don't have to worry about crafting...
			//slot.onSwapCraft(slotStack.getCount());
			slot.setByPlayer(ItemStack.EMPTY);
			slot.onTake(player, slotStack);
			return;
		}

		if (slotStack.isEmpty()) {
			if (!slot.mayPlace(itemStack)) return;

			final int maxStackSize = slot.getMaxStackSize(itemStack);

			if (maxStackSize >= itemStack.getCount()) {
				inventory.setItem(mouseButton, ItemStack.EMPTY);
				slot.setByPlayer(itemStack);
				return;
			}

			slot.setByPlayer(itemStack.split(maxStackSize));
			return;
		}
		if (slot.mayPickup(player) && slot.mayPlace(itemStack)) {
			final int maxStackSize = slot.getMaxStackSize(itemStack);
			if (itemStack.getCount() <= maxStackSize) {
				inventory.setItem(mouseButton, slotStack);
				slot.setByPlayer(itemStack);
				slot.onTake(player, slotStack);
				return;
			}

			slot.setByPlayer(itemStack.split(maxStackSize));
			slot.onTake(player, slotStack);
			if (!inventory.add(slotStack)) {
				player.drop(slotStack, true);
			}
		}
	}

	/**
	 * Called to perform pickup all
	 */
	protected void clickPickupAll(final int slotIndex, final int mouseButton, final Player player) {
		final Slot slot = slots.get(slotIndex);
		final ItemStack carriedStack = getCarried();
		if (!carriedStack.isEmpty() && (!slot.hasItem() || !slot.mayPickup(player))) {
			final int l1 = mouseButton == 0 ? 0 : slots.size() - 1;
			final int k2 = mouseButton == 0 ? 1 : -1;

			for (int l2 = 0; l2 < 2; ++l2) {
				for (int l3 = l1; l3 >= 0 && l3 < slots.size() && carriedStack.getCount() < carriedStack.getMaxStackSize(); l3 += k2) {
					final Slot loopSlot = slots.get(l3);

					if (!loopSlot.hasItem()) continue;
					if (!canItemQuickReplace(loopSlot, carriedStack, true)) continue;
					if (!loopSlot.mayPickup(player)) continue;
					if (!canTakeItemForPickAll(carriedStack, loopSlot)) continue;

					final ItemStack loopStack = loopSlot.getItem();
					if (l2 == 0 && loopStack.getCount() == loopStack.getMaxStackSize()) continue;
					final ItemStack resultStack = loopSlot.safeTake(loopStack.getCount(), carriedStack.getMaxStackSize() - carriedStack.getCount(),
							player);
					carriedStack.grow(resultStack.getCount());
				}
			}
		}
	}

	protected final boolean tryItemClickBehaviourOverride(final Player player, final ClickAction clickAction, final Slot slot,
			final ItemStack clickedStack, final ItemStack carriedStack) {
		final FeatureFlagSet featureflagset = player.level().enabledFeatures();
		if (carriedStack.isItemEnabled(featureflagset) && carriedStack.overrideStackedOnOther(slot, clickAction, player)) return true;

		return clickedStack.isItemEnabled(featureflagset) && clickedStack.overrideOtherStackedOnMe(carriedStack, slot, clickAction, player,
				createCarriedSlotAccess());
	}

	@Contract(value = "-> new", pure = true)
	protected final SlotAccess createCarriedSlotAccess() {
		return new SlotAccess() {
			public ItemStack get() {
				return ExtendedSlotCapacityMenu.this.getCarried();
			}

			public boolean set(final ItemStack itemStack) {
				ExtendedSlotCapacityMenu.this.setCarried(itemStack);
				return true;
			}
		};
	}
}