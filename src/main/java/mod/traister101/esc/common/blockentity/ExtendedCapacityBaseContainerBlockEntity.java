package mod.traister101.esc.common.blockentity;

import mod.traister101.esc.common.menu.ExtendedSlotCapacityMenu;

import net.minecraft.core.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.*;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.BlockState;

/**
 * An extension of {@link BaseContainerBlockEntity} with a simple {@link Container} implementation allowing for Extended slot capacity.
 * For menus, it is recommended you use {@link ExtendedSlotCapacityMenu} otherwise you'll need to implement your own menu supporting extended slot
 * capacity (and if you do that why are you using the lib?)
 */
public abstract class ExtendedCapacityBaseContainerBlockEntity extends BaseContainerBlockEntity {

	public final int slotCapacity;
	protected final NonNullList<ItemStack> items;

	protected ExtendedCapacityBaseContainerBlockEntity(final BlockEntityType<? extends ExtendedCapacityBaseContainerBlockEntity> blockEntityType,
			final BlockPos blockPos, final BlockState blockState, final int containerSize, final int slotCapacity) {
		super(blockEntityType, blockPos, blockState);
		this.items = NonNullList.withSize(containerSize, ItemStack.EMPTY);
		this.slotCapacity = slotCapacity;
	}

	@Override
	public int getContainerSize() {
		return items.size();
	}

	@Override
	public ItemStack getItem(final int slotIndex) {
		return items.get(slotIndex);
	}

	@Override
	public ItemStack removeItem(final int slotIndex, final int count) {
		final ItemStack itemstack = ContainerHelper.removeItem(items, slotIndex, count);
		if (!itemstack.isEmpty()) setChanged();

		return itemstack;
	}

	@Override
	public ItemStack removeItemNoUpdate(final int slotIndex) {
		return ContainerHelper.takeItem(items, slotIndex);
	}

	@Override
	public void setItem(final int slotIndex, final ItemStack itemStack) {
		items.set(slotIndex, itemStack);
		final int maxSize = itemStack.isDamageableItem() ? itemStack.getMaxStackSize() : slotCapacity;
		if (itemStack.getCount() > maxSize) itemStack.setCount(maxSize);

		setChanged();
	}

	@Override
	public int getMaxStackSize() {
		return slotCapacity;
	}

	@Override
	public void load(final CompoundTag compoundTag) {
		super.load(compoundTag);
		ContainerHelper.loadAllItems(compoundTag, items);
	}

	@Override
	protected void saveAdditional(final CompoundTag compoundTag) {
		super.saveAdditional(compoundTag);
		ContainerHelper.saveAllItems(compoundTag, items);
	}

	@Override
	public void clearContent() {
		items.clear();
	}
}