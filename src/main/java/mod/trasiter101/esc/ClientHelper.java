package mod.trasiter101.esc;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;

import net.minecraftforge.api.distmarker.*;

@OnlyIn(Dist.CLIENT)
public final class ClientHelper {

	public static Player getPlayer() {
		return Minecraft.getInstance().player;
	}
}