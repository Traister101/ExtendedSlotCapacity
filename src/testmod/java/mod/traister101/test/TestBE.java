package mod.traister101.test;

import mod.traister101.esc.common.blockentity.ExtendedCapacityBaseContainerBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.*;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class TestBE extends ExtendedCapacityBaseContainerBlockEntity {

	public static final int CONTAINER_SIZE = 27;
	public static final int SLOT_CAPACITY = 128;

	protected TestBE(final BlockEntityType<? extends TestBE> blockEntityType, final BlockPos blockPos, final BlockState blockState,
			final int containerSize, final int slotCapacity) {
		super(blockEntityType, blockPos, blockState, containerSize, slotCapacity);
	}

	public TestBE(final BlockPos blockPos, final BlockState blockState) {
		this(TestMod.TEST_BE.get(), blockPos, blockState, CONTAINER_SIZE, SLOT_CAPACITY);
	}

	@Override
	protected Component getDefaultName() {
		return Component.literal("Ayyyooo");
	}

	@Override
	protected AbstractContainerMenu createMenu(final int pContainerId, final Inventory pInventory) {
		return new TestMenu(pContainerId, pInventory, this);
	}

	@Override
	public boolean isEmpty() {
		return items.stream().noneMatch(ItemStack::isEmpty);
	}

	@Override
	public boolean stillValid(final Player pPlayer) {
		return Container.stillValidBlockEntity(this, pPlayer);
	}
}