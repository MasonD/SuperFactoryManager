package ca.teamdman.sfm.common.item;

import ca.teamdman.sfm.client.ClientLabelGunWarningHelper;
import ca.teamdman.sfm.client.handler.LabelGunKeyMappingHandler;
import ca.teamdman.sfm.client.handler.ToolItemAimModeHandler;
import ca.teamdman.sfm.client.registry.SFMKeyMappings;
import ca.teamdman.sfm.client.screen.SFMScreenChangeHelpers;
import ca.teamdman.sfm.common.cablenetwork.CableNetwork;
import ca.teamdman.sfm.common.cablenetwork.CableNetworkManager;
import ca.teamdman.sfm.common.label.LabelPositionHolder;
import ca.teamdman.sfm.common.label.SelectionTargets;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.net.ServerboundLabelGunSetActiveLabelPacket;
import ca.teamdman.sfm.common.net.ServerboundLabelGunUsePacket;
import ca.teamdman.sfm.common.registry.SFMPackets;
import ca.teamdman.sfm.common.util.SFMHandUtils;
import ca.teamdman.sfm.common.util.SFMItemUtils;
import ca.teamdman.sfm.common.util.SFMStreamUtils;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.settings.GameSettings;
import net.minecraft.client.util.ITooltipFlag;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NBTTagInt;
import net.minecraft.nbt.NBTTagString;
import net.minecraft.util.ActionResult;
import net.minecraft.util.EnumActionResult;
import net.minecraft.util.EnumFacing;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.Style;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ca.teamdman.sfm.common.util.SFMStreamUtils.get3DNeighbours;
import static ca.teamdman.sfm.common.util.SFMStreamUtils.get3DNeighboursIncludingKittyCorner;

public class LabelGunItem extends Item implements ToolItem {
    public LabelGunItem() {
        super();
        setMaxStackSize(1);
    }

    public static void setActiveLabel(
            ItemStack stack,
            @Nullable String label
    ) {
        if (label == null || label.isEmpty()) {
            clearActiveLabel(stack);
        } else {
            LabelPositionHolder.from(stack).addReferencedLabel(label).save(stack);
            stack.setTagInfo("sfm:active_label", new NBTTagString(label));
        }
    }

    public static void clearActiveLabel(
            ItemStack gun
    ) {
        gun.removeSubCompound("sfm:active_label");
    }

    public static String getActiveLabel(ItemStack stack) {
        //noinspection DataFlowIssue
        return !stack.hasTagCompound() ? "" : stack.getTagCompound().getString("sfm:active_label");
    }

    public static String getNextLabel(
            ItemStack gun,
            int change
    ) {
        var labels = LabelPositionHolder
                .from(gun)
                .labels()
                .keySet()
                .stream()
                .sorted(Comparator.naturalOrder())
                .collect(Collectors.toList());
        if (labels.isEmpty()) return "";
        var currentLabel = getActiveLabel(gun);

        int currentLabelIndex = 0;
        for (int i = 0; i < labels.size(); i++) {
            if (labels.get(i).equals(currentLabel)) {
                currentLabelIndex = i;
                break;
            }
        }

        int nextLabelIndex = currentLabelIndex + change;
        // ensure going negative wraps around
        nextLabelIndex = ((nextLabelIndex % labels.size()) + labels.size()) % labels.size();

        return labels.get(nextLabelIndex);
    }

    /**
     * Returns the current enum mode for the label gun item.
     */
    public static LabelGunViewMode getViewMode(ItemStack stack) {
        int ordinal = stack.getTagCompound() != null ? stack.getTagCompound().getInteger("sfm:label_gun_view_mode") : 0;
        // fallback if out of bounds or missing
        if (ordinal < 0 || ordinal >= LabelGunViewMode.values().length) {
            return LabelGunViewMode.SHOW_ALL;
        }
        return LabelGunViewMode.values()[ordinal];
    }

    /**
     * Sets the view mode in NBT.
     */
    public static void setViewMode(
            ItemStack stack,
            LabelGunViewMode mode
    ) {
        stack.setTagInfo("sfm:label_gun_view_mode", new NBTTagInt(mode.ordinal()));
    }

    public static void cycleViewMode(ItemStack stack) {
        LabelGunViewMode current = getViewMode(stack);
        int nextOrdinal = (current.ordinal() + 1) % LabelGunViewMode.values().length;
        setViewMode(stack, LabelGunViewMode.values()[nextOrdinal]);
    }


    @Override
    public EnumActionResult onItemUseFirst(
            EntityPlayer player,
            World world,
            BlockPos pos,
            EnumFacing side,
            float hitX,
            float hitY,
            float hitZ,
            EnumHand hand
    ) {
        if (world.isRemote && player != null) {
            sendLabelGunUsePacket(player, pos, hand);
            return EnumActionResult.SUCCESS;
        }
        return EnumActionResult.SUCCESS;
    }

    private static void sendLabelGunUsePacket(EntityPlayer player, BlockPos pos, EnumHand hand) {
        boolean pickBlock = SFMKeyMappings.isKeyDown(SFMKeyMappings.LABEL_GUN_PICK_BLOCK_MODIFIER_KEY);
        boolean contiguous = SFMKeyMappings.isKeyDown(SFMKeyMappings.LABEL_GUN_CONTIGUOUS_MODIFIER_KEY);
        boolean clear = SFMKeyMappings.isKeyDown(SFMKeyMappings.LABEL_GUN_CLEAR_MODIFIER_KEY);
        boolean pull = SFMKeyMappings.isKeyDown(SFMKeyMappings.LABEL_GUN_PULL_MODIFIER_KEY);
        boolean aimMode = SFMKeyMappings.isKeyDown(SFMKeyMappings.AIM_MODE_MODIFIER_KEY);

        ServerboundLabelGunUsePacket msg = new ServerboundLabelGunUsePacket(
                hand,
                pos,
                contiguous,
                pickBlock,
                clear,
                pull,
                aimMode,
                ToolItemAimModeHandler.getCurrentSelection()
        );
        ClientLabelGunWarningHelper.sendLabelGunUsePacketFromClientWithConfirmationIfNecessary(msg, player);
        // we don't want to toggle the overlay if we're using pick-block
        LabelGunKeyMappingHandler.setExternalDebounce();
    }

    @Override
    public ActionResult<ItemStack> onItemRightClick(
            World world,
            EntityPlayer player,
            EnumHand hand
    ) {
        ItemStack stack = player.getHeldItem(hand);

        if (world.isRemote) {
            if (SFMKeyMappings.isKeyDown(SFMKeyMappings.AIM_MODE_MODIFIER_KEY) && ToolItemAimModeHandler.getMainSelectedBlock() != null) {
                sendLabelGunUsePacket(player, ToolItemAimModeHandler.getMainSelectedBlock(), hand);
            } else {
                SFMScreenChangeHelpers.showLabelGunScreen(stack, hand);
            }
        }
        return ActionResult.newResult(EnumActionResult.SUCCESS, stack);
    }

    @SideOnly(Side.CLIENT)
    @Override
    public boolean onItemScroll(EntityPlayerSP player, SFMHandUtils.ItemStackInHand itemInHand, int scrollDirection) {
        var tool = itemInHand.stack();
        var hand = itemInHand.hand();

        if (SFMKeyMappings.isKeyDown(SFMKeyMappings.LABEL_GUN_SCROLL_MODIFIER_KEY)) {
            var next = LabelGunItem.getNextLabel(tool, scrollDirection);
            SFMPackets.sendToServer(new ServerboundLabelGunSetActiveLabelPacket(
                    next,
                    hand
            ));
            LabelGunKeyMappingHandler.setExternalDebounce();
            return true;
        }
        return false;
    }

    @Override
    @SideOnly(Side.CLIENT)
    public void addInformation(
            ItemStack stack,
            @Nullable World level,
            List<String> lines,
            ITooltipFlag detail
    ) {
        if (SFMItemUtils.isClientAndMoreInfoKeyPressed()) {
            GameSettings options = Minecraft.getMinecraft().gameSettings;
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_TOGGLE_LABEL_REMINDER.get(
                            SFMKeyMappings.getKeyDisplay(options.keyBindUseItem)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_CLEAR_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.LABEL_GUN_PULL_MODIFIER_KEY),
                            SFMKeyMappings.getKeyDisplay(options.keyBindUseItem)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_PULL_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.LABEL_GUN_PULL_MODIFIER_KEY),
                            SFMKeyMappings.getKeyDisplay(options.keyBindUseItem)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_PUSH_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(options.keyBindUseItem)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_AIM_MODE_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.AIM_MODE_MODIFIER_KEY),
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.AIM_MODE_MODIFIER_KEY)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_CONTIGUOUS_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.LABEL_GUN_CONTIGUOUS_MODIFIER_KEY)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_PICK_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.LABEL_GUN_PICK_BLOCK_MODIFIER_KEY),
                            SFMKeyMappings.getKeyDisplay(options.keyBindUseItem)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_NEXT_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.LABEL_GUN_NEXT_LABEL_KEY)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_PREVIOUS_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.LABEL_GUN_PREVIOUS_LABEL_KEY)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_SCROLL_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.LABEL_GUN_SCROLL_MODIFIER_KEY)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_CYCLE_VIEW_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(SFMKeyMappings.CYCLE_LABEL_VIEW_KEY)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
            lines.add(
                    LocalizationKeys.LABEL_GUN_ITEM_TOOLTIP_GUI_REMINDER.getComponent(
                            SFMKeyMappings.getKeyDisplay(options.keyBindUseItem)
                    ).setStyle(new Style().setColor(TextFormatting.GRAY)).getFormattedText()
            );
        } else {
            SFMItemUtils.appendMoreInfoKeyReminderTextIfOnClient(lines);
            lines.addAll(LabelPositionHolder.from(stack).asHoverText());
        }
    }

    @Override
    public String getItemStackDisplayName(ItemStack stack) {
        var name = getActiveLabel(stack);
        if (name.isEmpty()) return super.getItemStackDisplayName(stack);
        return LocalizationKeys.LABEL_GUN_ITEM_NAME_WITH_LABEL
                .getComponent(new TextComponentString(name).setStyle(new Style().setBold(true)))
                .setStyle(new Style().setColor(TextFormatting.AQUA)).getFormattedText();
    }

    public static void clearAll(ItemStack stack) {
        LabelPositionHolder.clear(stack);
        LabelGunItem.setActiveLabel(stack, null);
    }

    @Override
    public int maxDigDepth(ItemStack stack) {
        return 5;
    }

    @Override
    public int getBlockSelectionModifierState(EntityPlayerSP player, EnumHand hand, ItemStack stack) {
        if (!SFMKeyMappings.isKeyDown(SFMKeyMappings.AIM_MODE_MODIFIER_KEY)) {
            return 0;
        }
        if (SFMKeyMappings.isKeyDown(SFMKeyMappings.LABEL_GUN_CONTIGUOUS_MODIFIER_KEY)) {
            return 2;
        }
        return 1;
    }

    @Override
    public SelectionTargets getSelectedBlocksFromRaycast(
            EntityPlayer player,
            ItemStack stack,
            BlockPos raycastPos
    ) {
        var level = player.getEntityWorld();
        boolean contiguous = SFMKeyMappings.isKeyDown(SFMKeyMappings.LABEL_GUN_CONTIGUOUS_MODIFIER_KEY);


        // get the block type of the target position
        Block targetBlock = level.getBlockState(raycastPos).getBlock();

        if (!contiguous) {
            return new SelectionTargets(new HashSet<>(Arrays.asList(raycastPos)), Collections.emptySet());
        }
        Set<BlockPos> targets;

        // find all cable positions so that we only include blocks adjacent to a cable
        Set<BlockPos> cablePositions;
        if (level.isRemote) {
            // There are no cable networks on the client, so we need to discover the cable positions
            // We need to know this to determine how large the change is and if we need to ask the client for confirmation
            cablePositions = get3DNeighbours(raycastPos)
                    .filter(pos -> CableNetwork.isCable(level, pos))
                    .flatMap(cablePos -> CableNetwork.discoverCables(level, cablePos))
                    .collect(Collectors.toSet());
        } else {
            cablePositions = get3DNeighbours(raycastPos)
                    .map(suspected_cable_pos -> CableNetworkManager.getOrRegisterNetworkFromCablePosition(
                            level,
                            suspected_cable_pos
                    ))
                    .filter(Optional::isPresent)
                    .map(Optional::get)
                    .flatMap(CableNetwork::getCablePositions)
                    .collect(Collectors.toSet());
        }

        Set<BlockPos> warnBecauseNoCableNeighbour = new HashSet<>();
        Predicate<BlockPos> isAdjacentToCable = p -> {
            boolean isAdjacent = get3DNeighbours(p).anyMatch(cablePositions::contains);
            if (!isAdjacent) {
                warnBecauseNoCableNeighbour.add(p);
            }
            return isAdjacent;
        };
        targets = SFMStreamUtils.<BlockPos, BlockPos>getRecursiveStream(
                        (current, nextQueue, results) -> {
                            results.accept(current);
                            get3DNeighboursIncludingKittyCorner(current)
                                    .filter(p -> level.getBlockState(p).getBlock() == targetBlock)
                                    .filter(isAdjacentToCable)
                                    .forEach(nextQueue);
                        }, raycastPos
                )
                .collect(Collectors.toSet());
        return new SelectionTargets(targets, warnBecauseNoCableNeighbour);
    }


    public enum LabelGunViewMode {
        SHOW_ALL,
        SHOW_ONLY_ACTIVE_LABEL_AND_TARGETED_BLOCK,
        SHOW_ONLY_TARGETED_BLOCK,
    }
}
