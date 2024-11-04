package mod.traister101.esc.mixin.common.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

import net.minecraft.world.inventory.Slot;

@Mixin(Slot.class)
public interface SlotAccessor {

	@Invoker
	void invokeOnSwapCraft(int pNumItemsCrafted);
}