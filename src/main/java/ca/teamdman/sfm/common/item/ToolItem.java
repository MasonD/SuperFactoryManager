package ca.teamdman.sfm.common.item;

import ca.teamdman.sfm.common.util.SFMHandUtils;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;

import java.util.Map;

public interface ToolItem {
    public boolean onItemScroll(EntityPlayerSP player, SFMHandUtils.ItemStackInHand itemInHand, int scrollDirection);

    public int maxDigDepth(ItemStack stack);

    public boolean isBlockSelectionOn(EntityPlayerSP player, EnumHand hand, ItemStack stack);

    public Map<BlockPos,String>getSelectedBlocksFromRaycast(EntityPlayerSP player, ItemStack stack, BlockPos raycastPos);
}
