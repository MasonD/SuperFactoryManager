package ca.teamdman.sfm.common.item;

import ca.teamdman.sfm.common.label.SelectionTargets;
import ca.teamdman.sfm.common.util.SFMHandUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.UnknownNullability;

public interface ToolItem {
    boolean onItemScroll(EntityPlayerSP player, SFMHandUtils.ItemStackInHand itemInHand, int scrollDirection);

    int maxDigDepth(ItemStack stack);

    SelectionTargets getSelectedBlocksFromRaycast(EntityPlayer player, ItemStack stack, BlockPos raycastPos);

    /**
     * Return 0 for aim mode off, and any other number for on.
     * Changing the number will invalidate the cached SelectionTargets, e.g. for when the user holds a different modifier
     *
     */
    int getBlockSelectionModifierState(EntityPlayerSP player, EnumHand hand, ItemStack stack);
}
