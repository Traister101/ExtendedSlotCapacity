package mod.traister101.esc.mixin.common.accessor;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

import net.minecraft.world.inventory.*;

import java.util.Set;

@Mixin(AbstractContainerMenu.class)
public interface AbstractContainerMenuAccessor {

	@Accessor
	int getQuickcraftStatus();

	@Accessor
	void setQuickcraftStatus(int quickcraftStatus);

	@Accessor
	int getQuickcraftType();

	@Accessor
	void setQuickcraftType(int quickcraftType);

	@Accessor
	Set<Slot> getQuickcraftSlots();

	@Accessor
	MenuType<?> getMenuType();
}