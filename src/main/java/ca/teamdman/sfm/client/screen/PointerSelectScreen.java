package ca.teamdman.sfm.client.screen;

import ca.teamdman.sfm.client.ClientLabelGunWarningHelper;
import ca.teamdman.sfm.client.handler.AimModeTargetHandler;
import ca.teamdman.sfm.client.registry.SFMKeyMappings;
import ca.teamdman.sfm.common.label.SelectionTargets;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.net.ServerboundLabelGunUsePacket;
import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.input.Mouse;

import java.io.IOException;
import java.util.*;

public class PointerSelectScreen extends GuiScreen {
    EntityPlayerSP player;
    EnumHand hand;

    @Nullable
    public Block targetBlock = null;

    @Nullable
    BlockPos clickStart = null;

    @Nullable
    public BlockPos selectionStart = null;


    @Nullable
    protected BlockPos cachedSelectionEnd = null;
    @Nullable
    protected Set<BlockPos> cachedSelectionSet = null;


    public PointerSelectScreen(EntityPlayerSP player, EnumHand hand) {
        this.player = player;
        this.hand = hand;
    }

    @Override
    public void drawDefaultBackground() {
        // no background

    }

    @Override
    public void drawScreen(int mouseX, int mouseY, float partialTicks) {
        super.drawScreen(mouseX, mouseY, partialTicks);

        var font = this.fontRenderer;

        var reminder = LocalizationKeys.POINTER_SELECT_SCREEN_TITLE.getComponent();
        int titleWidth = font.getStringWidth(reminder.getUnformattedText());
        int x = width / 2 - titleWidth / 2;
        int y = 20;
        SFMFontUtils.draw(
                font,
                reminder,
                x,
                y,
                0xFFACD0FF,
                true
        );


        if (targetBlock != null) {
            int textWidth = font.getStringWidth("Selected block:");
            int itemSize = 14;
            int tooltipTextWidth = textWidth + 2 + 2 + itemSize;
            int backgroundColor = 0xF0100010;
            int borderColorStart = 0x505000FF;
            int borderColorEnd = (borderColorStart & 0xFEFEFE) >> 1 | borderColorStart & 0xFF000000;
            int tooltipX = 8;
            int tooltipY = 40;
            int tooltipHeight = 14;

            drawTooltipBox(
                    tooltipX,
                    tooltipY,
                    tooltipTextWidth,
                    backgroundColor,
                    tooltipHeight,
                    borderColorStart,
                    borderColorEnd
            );


            font.drawString("Selected block:", tooltipX, tooltipY + 2, -1);
            this.itemRender.renderItemAndEffectIntoGUI(
                    this.player,
                    new ItemStack(Item.getItemFromBlock(targetBlock)),
                    tooltipX + textWidth + 4,
                    tooltipY
            );
        }
    }

    private void drawTooltipBox(
            int tooltipX,
            int tooltipY,
            int tooltipTextWidth,
            int backgroundColor,
            int tooltipHeight,
            int borderColorStart,
            int borderColorEnd
    ) {
        drawGradientRect(
                tooltipX - 3,
                tooltipY - 4,
                tooltipX + tooltipTextWidth + 3,
                tooltipY - 3,
                backgroundColor,
                backgroundColor
        );
        drawGradientRect(
                tooltipX - 3,
                tooltipY + tooltipHeight + 3,
                tooltipX + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 4,
                backgroundColor,
                backgroundColor
        );
        drawGradientRect(
                tooltipX - 3,
                tooltipY - 3,
                tooltipX + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3,
                backgroundColor,
                backgroundColor
        );
        drawGradientRect(
                tooltipX - 4,
                tooltipY - 3,
                tooltipX - 3,
                tooltipY + tooltipHeight + 3,
                backgroundColor,
                backgroundColor
        );
        drawGradientRect(
                tooltipX + tooltipTextWidth + 3,
                tooltipY - 3,
                tooltipX + tooltipTextWidth + 4,
                tooltipY + tooltipHeight + 3,
                backgroundColor,
                backgroundColor
        );
        drawGradientRect(
                tooltipX - 3,
                tooltipY - 3 + 1,
                tooltipX - 3 + 1,
                tooltipY + tooltipHeight + 3 - 1,
                borderColorStart,
                borderColorEnd
        );
        drawGradientRect(
                tooltipX + tooltipTextWidth + 2,
                tooltipY - 3 + 1,
                tooltipX + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3 - 1,
                borderColorStart,
                borderColorEnd
        );
        drawGradientRect(
                tooltipX - 3,
                tooltipY - 3,
                tooltipX + tooltipTextWidth + 3,
                tooltipY - 3 + 1,
                borderColorStart,
                borderColorStart
        );
        drawGradientRect(
                tooltipX - 3,
                tooltipY + tooltipHeight + 2,
                tooltipX + tooltipTextWidth + 3,
                tooltipY + tooltipHeight + 3,
                borderColorEnd,
                borderColorEnd
        );
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) throws IOException {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            if (clickStart == null && AimModeTargetHandler.getCurrentTarget() != null) {
                clickStart = AimModeTargetHandler.getCurrentTarget();
            }
            if (targetBlock != null && clickStart != null) {
                startSelection(clickStart);
            }
        }
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();

        int dWheel = Mouse.getEventDWheel();
        if (dWheel != 0) {
            AimModeTargetHandler.shiftDigDepth(dWheel < 0 ? -1 : 1, 15);
        }
    }

    protected void startSelection(BlockPos clickStart) {
        this.selectionStart = clickStart;
    }

    protected void clearSelection() {
        this.selectionStart = null;
        this.targetBlock = null;
        this.cachedSelectionEnd = null;
        this.cachedSelectionSet = null;
        this.clickStart = null;
    }

    @Override
    protected void mouseReleased(int mouseX, int mouseY, int state) {
        super.mouseReleased(mouseX, mouseY, state);
        if (state == 0) {
            if (targetBlock == null) {
                if (
                        clickStart != null &&
                        AimModeTargetHandler.getCurrentTarget() != null
                            && clickStart.equals(AimModeTargetHandler.getCurrentTarget())
                ) {
                    targetBlock = player.getEntityWorld().getBlockState(clickStart).getBlock();
                }
            } else {
                // do the select
                Minecraft.getMinecraft().displayGuiScreen(null);
                if (this.cachedSelectionSet != null && selectionStart != null) {

                    ServerboundLabelGunUsePacket msg = new ServerboundLabelGunUsePacket(
                            hand,
                            selectionStart,
                            true,
                            false,
                            SFMKeyMappings.isKeyDown(SFMKeyMappings.LABEL_GUN_CLEAR_MODIFIER_KEY),
                            false,
                            true,
                            new SelectionTargets(this.cachedSelectionSet, Collections.emptySet())
                    );
                    ClientLabelGunWarningHelper.sendLabelGunUsePacketFromClientWithConfirmationIfNecessary(msg, player);
                }

                this.clearSelection();
            }

            this.clickStart = null;
        }
    }
    
    public Set<BlockPos> getSelectedBlocks(BlockPos blockPos) {
        if (selectionStart == null) {
            return Collections.emptySet();
        }
        if (cachedSelectionEnd == blockPos && cachedSelectionSet != null) {
            return cachedSelectionSet;
        }

        Set<BlockPos> blocks = new HashSet<>();
        int minX = Math.min(selectionStart.getX(), blockPos.getX());
        int minY = Math.min(selectionStart.getY(), blockPos.getY());
        int minZ = Math.min(selectionStart.getZ(), blockPos.getZ());

        int maxX = Math.max(selectionStart.getX(), blockPos.getX());
        int maxY = Math.max(selectionStart.getY(), blockPos.getY());
        int maxZ = Math.max(selectionStart.getZ(), blockPos.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    var pos = new BlockPos(x, y, z);
                    var blockState = player.getEntityWorld().getBlockState(pos);
                    if (blockState.getBlock() == targetBlock) {
                        blocks.add(pos);
                    }
                }
            }
        }
        cachedSelectionSet = blocks;
        cachedSelectionEnd = blockPos;
        return blocks;
    }

    @Override
    public boolean doesGuiPauseGame() {
        return false;
    }
}
