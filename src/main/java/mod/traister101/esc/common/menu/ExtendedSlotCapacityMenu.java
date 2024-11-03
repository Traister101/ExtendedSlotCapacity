package mod.traister101.esc.common.menu;

import mod.traister101.esc.common.capability.ExtendedSlotCapacityHandler;
import mod.traister101.esc.common.slot.*;
import mod.traister101.esc.mixin.common.accessor.AbstractContainerMenuAccessor;

import net.minecraft.*;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.ByIdMap;
import net.minecraft.util.ByIdMap.OutOfBoundsStrategy;
import net.minecraft.world.Container;
import net.minecraft.world.entity.SlotAccess;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.flag.FeatureFlagSet;
import net.minecraft.world.inventory.*;
import net.minecraft.world.item.ItemStack;

import net.minecraftforge.common.ForgeHooks;
import net.minecraftforge.items.SlotItemHandler;
import net.minecraftforge.registries.ForgeRegistries;

import lombok.Getter;
import org.jetbrains.annotations.*;
import java.util.*;
import java.util.function.IntFunction;

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
@Getter
public abstract class ExtendedSlotCapacityMenu extends AbstractContainerMenu {

	/**
	 * The amount of slots this container has, not including player inventory.
	 */
	public final int containerSlots;

	/**
	 * @param containerSlots The amount of slots this container has
	 */
	@Contract(pure = true)
	protected ExtendedSlotCapacityMenu(final @Nullable MenuType<? extends ExtendedSlotCapacityMenu> menuType, final int windowId,
			final int containerSlots) {
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

	/**
	 * You should probably not override this.
	 * See {{@link #dragStart(int, Player, DragType)}} {@link #dragContinue(int, Player, DragType)} {@link #dragEnd(int, Player, DragType)}
	 * {@link #clickPickup(int, int, Player)} {@link #clickQuickMove(int, Player)} {@link #clickSwap(int, int, Player, Inventory)} and
	 * {@link #clickPickupAll(int, int, Player)} instead
	 */
	@Override
	public void clicked(final int slotIndex, final int mouseButton, final ClickType clickType, final Player player) {
		try {
			doClick(slotIndex, mouseButton, clickType, player);
		} catch (final Exception exception) {
			final var crashreport = CrashReport.forThrowable(exception, "Container click");
			final var crashreportcategory = crashreport.addCategory("Click info");
			crashreportcategory.setDetail("Menu Type",
					() -> Objects.requireNonNullElse(ForgeRegistries.MENU_TYPES.getKey(((AbstractContainerMenuAccessor) this).getMenuType()),
							"<no type>").toString());
			crashreportcategory.setDetail("Menu Class", () -> getClass().getCanonicalName());
			crashreportcategory.setDetail("Slot Count", slots.size());
			crashreportcategory.setDetail("Slot", slotIndex);
			crashreportcategory.setDetail("Mouse Button", mouseButton);
			crashreportcategory.setDetail("Click Type", clickType);
			throw new ReportedException(crashreport);
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
	 * Start a slot drag or "quick craft" going by vanilla terms
	 *
	 * @param slotIndex The slot index
	 * @param player The player
	 * @param dragType The dragging type
	 */
	protected void dragStart(@SuppressWarnings("unused") final int slotIndex, final Player player, final DragType dragType) {
		if (!isValidQuickcraftType(dragType.quickCraftTypeId, player)) {
			resetQuickCraft();
			return;
		}
		setDragStatus(DragStatus.CONTINUE);
		getDragSlots().clear();
	}

	/**
	 * Continue dragging though slots or a "quick craft" going by vanilla terms
	 *
	 * @param slotIndex The slot index
	 * @param player The player
	 * @param dragType The dragging type
	 */
	protected void dragContinue(final int slotIndex, @SuppressWarnings("unused") final Player player, final DragType dragType) {
		final Slot slot = slots.get(slotIndex);
		final ItemStack carriedStack = getCarried();
		final var dragSlots = getDragSlots();
		if (canItemQuickReplace(slot, carriedStack, true) && slot.mayPlace(
				carriedStack) && (dragType == DragType.CLONE || carriedStack.getCount() > dragSlots.size()) && canDragTo(slot)) {
			dragSlots.add(slot);
		}
	}

	/**
	 * End the slot drag or a "quick craft" going by vanilla terms
	 *
	 * @param slotIndex The slot index
	 * @param player The player
	 * @param dragType The dragging type
	 */
	protected void dragEnd(@SuppressWarnings("unused") final int slotIndex, final Player player, final DragType dragType) {
		final var dragSlots = getDragSlots();
		if (!dragSlots.isEmpty()) {
			if (dragSlots.size() == 1) {
				final int index = (dragSlots.iterator().next()).index;
				resetQuickCraft();
				doClick(index, dragType.quickCraftTypeId, ClickType.PICKUP, player);
				return;
			}

			final ItemStack originalCarriedStack = getCarried().copy();
			if (originalCarriedStack.isEmpty()) {
				resetQuickCraft();
				return;
			}

			int carriedCount = originalCarriedStack.getCount();

			for (final var slot : dragSlots) {
				final ItemStack carriedStack = getCarried();
				if (slot != null && canItemQuickReplace(slot, carriedStack, true) && slot.mayPlace(
						carriedStack) && (dragType == DragType.CLONE || carriedStack.getCount() >= dragSlots.size()) && canDragTo(slot)) {
					final int j = slot.hasItem() ? slot.getItem().getCount() : 0;
					final int k = Math.min(originalCarriedStack.getMaxStackSize(), slot.getMaxStackSize(originalCarriedStack));
					final int l = Math.min(getQuickCraftPlaceCount(dragSlots, dragType.quickCraftTypeId, originalCarriedStack) + j, k);
					carriedCount -= l - j;
					slot.setByPlayer(originalCarriedStack.copyWithCount(l));
				}
			}

			originalCarriedStack.setCount(carriedCount);
			setCarried(originalCarriedStack);
		}

		resetQuickCraft();
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

	private void doClick(final int slotIndex, final int mouseButton, final ClickType clickType, final Player player) {
		if (clickType == ClickType.QUICK_CRAFT) {
			final DragStatus oldDragStatus = getDragStatus();
			final DragStatus newDragStatus = DragStatus.getDragStatus(mouseButton);
			setDragStatus(newDragStatus);
			if ((oldDragStatus != DragStatus.CONTINUE || newDragStatus != DragStatus.END) && oldDragStatus != newDragStatus) {
				resetQuickCraft();
				return;
			}

			if (getCarried().isEmpty()) {
				resetQuickCraft();
				return;
			}

			switch (newDragStatus) {
				case START -> {
					final DragType dragType = DragType.getDragType(mouseButton);
					setDragType(dragType);
					dragStart(slotIndex, player, dragType);
				}
				case CONTINUE -> dragContinue(slotIndex, player, getDragType());
				case END -> dragEnd(slotIndex, player, getDragType());
			}

			return;
		}

		if (getDragStatus() != DragStatus.START) {
			resetQuickCraft();
			return;
		}

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

	/**
	 * @return The current {@link DragType}
	 */
	protected final DragType getDragType() {
		return DragType.byQuickCraftTypeId(((AbstractContainerMenuAccessor) this).getQuickcraftType());
	}

	/**
	 * @param dragType The new {@link DragType} to set
	 */
	protected final void setDragType(final DragType dragType) {
		((AbstractContainerMenuAccessor) this).setQuickcraftType(dragType.quickCraftTypeId);
	}

	/**
	 * @return The current {@link DragStatus}
	 */
	protected final DragStatus getDragStatus() {
		return DragStatus.byHeaderId(((AbstractContainerMenuAccessor) this).getQuickcraftStatus());
	}

	/**
	 * @param dragStatus The new {@link DragStatus} to set
	 */
	protected final void setDragStatus(final DragStatus dragStatus) {
		((AbstractContainerMenuAccessor) this).setQuickcraftStatus(dragStatus.headerId);
	}

	/**
	 * @return The drag slots
	 */
	protected final Set<Slot> getDragSlots() {
		return ((AbstractContainerMenuAccessor) this).getQuickcraftSlots();
	}

	/**
	 * An enum for {@link AbstractContainerMenu#QUICKCRAFT_TYPE_CHARITABLE} {@link AbstractContainerMenu#QUICKCRAFT_TYPE_GREEDY}
	 * {@link AbstractContainerMenu#QUICKCRAFT_TYPE_CLONE}
	 */
	@Getter
	protected enum DragType {
		CHARITABLE(QUICKCRAFT_TYPE_CHARITABLE),
		GREEDY(QUICKCRAFT_TYPE_GREEDY),
		CLONE(QUICKCRAFT_TYPE_CLONE);

		private static final IntFunction<DragType> BY_ID = ByIdMap.continuous(DragType::getQuickCraftTypeId, values(), OutOfBoundsStrategy.ZERO);

		public final int quickCraftTypeId;

		DragType(final int quickCraftTypeId) {
			this.quickCraftTypeId = quickCraftTypeId;
		}

		public static DragType byQuickCraftTypeId(final int dragTypeId) {
			return BY_ID.apply(dragTypeId);
		}

		private static DragType getDragType(final int mouseButton) {
			return byQuickCraftTypeId(mouseButton >> 2 & 3);
		}
	}

	/**
	 * An enum for {@link AbstractContainerMenu#QUICKCRAFT_HEADER_START} {@link AbstractContainerMenu#QUICKCRAFT_HEADER_CONTINUE}
	 * {@link AbstractContainerMenu#QUICKCRAFT_HEADER_END}
	 */
	@Getter
	protected enum DragStatus {
		START(QUICKCRAFT_HEADER_START),
		CONTINUE(QUICKCRAFT_HEADER_CONTINUE),
		END(QUICKCRAFT_HEADER_END);

		private static final IntFunction<DragStatus> BY_ID = ByIdMap.continuous(DragStatus::getHeaderId, values(), OutOfBoundsStrategy.ZERO);

		public final int headerId;

		DragStatus(final int headerId) {
			this.headerId = headerId;
		}

		public static DragStatus byHeaderId(final int dragStatusId) {
			return BY_ID.apply(dragStatusId);
		}

		private static DragStatus getDragStatus(final int mouseButton) {
			return byHeaderId(mouseButton & 3);
		}
	}
}