package ca.teamdman.sfm.client.handler;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.registry.SFMKeyMappings;
import ca.teamdman.sfm.client.screen.PointerSelectScreen;
import ca.teamdman.sfm.common.item.ToolItem;
import ca.teamdman.sfm.common.util.SFMHandUtils;
import net.minecraft.client.Minecraft;
import net.minecraftforge.client.event.MouseEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

@Mod.EventBusSubscriber(modid = SFM.MOD_ID, value = Side.CLIENT)
public class ToolItemScrollHandler {
    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void onScroll(MouseEvent event) {
        if (event.getDwheel() == 0) return;
        var player = Minecraft.getMinecraft().player;
        if (player == null) return;

        var itemInHand = SFMHandUtils.getItemAndHand(player, ToolItem.class);
        if (itemInHand == null) return;

        var handled = ((ToolItem) itemInHand.stack().getItem()).onItemScroll(
                player,
                itemInHand,
                event.getDwheel() < 0 ? -1 : 1
        );

        if (handled) {
            event.setCanceled(true);
            return;
        }

        if (SFMKeyMappings.isKeyDown(SFMKeyMappings.AIM_MODE_MODIFIER_KEY)) {
            AimModeTargetHandler.shiftDigDepth(
                    event.getDwheel() < 0 ? -1 : 1,
                    ((ToolItem) itemInHand.stack().getItem()).maxDigDepth(itemInHand.stack())
            );
            event.setCanceled(true);
        }
    }
}