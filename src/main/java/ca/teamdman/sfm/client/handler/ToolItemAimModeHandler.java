package ca.teamdman.sfm.client.handler;

import ca.teamdman.sfm.common.item.ToolItem;
import ca.teamdman.sfm.common.label.SelectionTargets;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;

public class ToolItemAimModeHandler {
    @Nullable
    private static BlockPos cachedLookTarget;
    private static int cachedModifierState;
    @Nullable
    private static ItemStack cachedItemStack;
    private static SelectionTargets cachedSelectedBlocks;

    public static void clear() {
        cachedSelectedBlocks = SelectionTargets.empty;
    }

    public static void clearCache() {
        cachedLookTarget = null;
        cachedItemStack = null;
        cachedSelectedBlocks = SelectionTargets.empty;
    }

    public static SelectionTargets getCurrentSelection() {
        return cachedSelectedBlocks;
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
            BlockPos trace = AimModeTargetHandler.tracePlayerLook(player, tool, hand, partialTicks);

            if (trace != null) {
                if (
                        player.ticksExisted % 20 == 0
                                || tool != cachedItemStack || !trace.equals(cachedLookTarget) || selectionState != cachedModifierState
                ) {
                    cachedLookTarget = trace;
                    cachedModifierState = selectionState;
                    cachedItemStack = tool;
                    cachedSelectedBlocks = toolItem.getSelectedBlocksFromRaycast(
                            player,
                            cachedItemStack,
                            cachedLookTarget
                    );
                }
            } else {
                clearCache();
            }
        } else {
            clear();
        }
        return cachedSelectedBlocks;
    }

    @Nullable
    public static BlockPos getMainSelectedBlock() {
        return cachedLookTarget;
    }
}
