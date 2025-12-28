package ca.teamdman.sfm.client.handler;

import ca.teamdman.sfm.common.item.ToolItem;
import ca.teamdman.sfm.common.label.SelectionTargets;
import ca.teamdman.sfm.common.util.Mth;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import org.jetbrains.annotations.Nullable;

public class BlockSelection {
    static int digDepth = 0;
    @Nullable
    private static BlockPos cachedBlockPos;
    private static int cachedModifierState;
    @Nullable
    private static ItemStack cachedItemStack;
    private static SelectionTargets cachedSelectedBlocks;

    public static void shiftDigDepth(int amount, int max) {
        digDepth = Mth.clamp(amount + digDepth, 0, max);
    }

    public static void clear() {
        digDepth = 0;
        cachedSelectedBlocks = SelectionTargets.empty;
    }

    public static void clearCache() {
        cachedBlockPos = null;
        cachedItemStack = null;
        cachedSelectedBlocks = SelectionTargets.empty;
    }

    public static SelectionTargets getCurrentSelection() {
        return cachedSelectedBlocks;
    }


    public static RayTraceResult traceLook(
            EntityPlayerSP player,
            ItemStack toolStack,
            EnumHand hand,
            float partialTicks
    ) {
        double remainingDistance = 100;

        Vec3d eye = player.getPositionEyes(partialTicks);
        Vec3d look = player.getLook(partialTicks);

        Vec3d endVec;
        endVec = eye.add(look.scale(remainingDistance));

        RayTraceResult trace = null;
        for (int i = 0; i <= BlockSelection.digDepth; i++) {
            trace = player.getEntityWorld().rayTraceBlocks(eye, endVec, false, false, false);

            if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK) {
                eye = trace.hitVec.add(look.x * 0.01, look.y * 0.01, look.z * 0.01);
            } else {
                break;
            }
        }

        return trace;
    }

    public static SelectionTargets updateSelection(
            EntityPlayerSP player,
            ItemStack tool,
            EnumHand hand,
            float partialTicks
    ) {
        if (!(tool.getItem() instanceof ToolItem toolItem)) {
            return SelectionTargets.empty;
        }

        int selectionState = toolItem.getBlockSelectionModifierState(player, hand, tool);
        if (selectionState != 0) {
            RayTraceResult trace = BlockSelection.traceLook(player, tool, hand, partialTicks);

            if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK) {
                if (
                        player.ticksExisted % 20 == 0
                                || tool != cachedItemStack || !trace.getBlockPos().equals(cachedBlockPos) || selectionState != cachedModifierState
                ) {
                    cachedModifierState = selectionState;
                    cachedBlockPos = trace.getBlockPos();
                    cachedItemStack = tool;
                    cachedSelectedBlocks = toolItem.getSelectedBlocksFromRaycast(
                            player,
                            cachedItemStack,
                            cachedBlockPos
                    );
                }
            } else {
                BlockSelection.clearCache();
            }
        } else {
            BlockSelection.clear();
        }
        return cachedSelectedBlocks;
    }

    public static boolean hasSelection() {
        return !cachedSelectedBlocks.isEmpty();
    }

    @Nullable
    public static BlockPos getMainSelectedBlock() {
        return cachedBlockPos;
    }
}
