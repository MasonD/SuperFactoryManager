package ca.teamdman.sfm.client.handler;

import ca.teamdman.sfm.SFM;
import ca.teamdman.sfm.client.render.HighlightRenderList;
import ca.teamdman.sfm.client.screen.PointerSelectScreen;
import ca.teamdman.sfm.common.item.LabelGunItem;
import ca.teamdman.sfm.common.item.NetworkToolItem;
import ca.teamdman.sfm.common.item.ToolItem;
import ca.teamdman.sfm.common.label.LabelPositionHolder;
import com.bbscn.Tools;
import com.google.common.collect.HashMultimap;

import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.client.gui.FontRenderer;
import net.minecraft.client.renderer.*;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraftforge.client.event.RenderWorldLastEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import org.lwjgl.input.Mouse;
import org.lwjgl.opengl.GL11;
import org.lwjgl.util.glu.GLU;

import java.awt.*;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = SFM.MOD_ID, value = Side.CLIENT)
/*
 * This class uses code from tasgon's "observable" mod, also using MPLv2
 * https://github.com/tasgon/observable/blob/master/common/src/main/kotlin/observable/client/Overlay.kt
 * https://github.com/tasgon/observable/blob/c3c5a0d0385e0b2c758729bdd935f103122f0f85/common/src/main/kotlin/observable/client/Overlay.kt
 */
public class ItemWorldRenderer {

    private static final int capabilityColor = Tools.toARGB(64, 100, 0, 255);
    private static final int capabilityColorLimitedView = Tools.toARGB(64, 100, 255, 255);
    private static final int cableColor = Tools.toARGB(64, 100, 255, 0);
    private static final int warningColor = Tools.toARGB(120, 255, 50, 50);
    private static final HighlightRenderListCache renderCache = new HighlightRenderListCache();

    @SubscribeEvent
    public static void renderOverlays(RenderWorldLastEvent event) {
        boolean rendered = false;

        Minecraft mc = Minecraft.getMinecraft();
        EntityPlayerSP player = mc.player;
        if (player == null) return;


        if (mc.currentScreen instanceof PointerSelectScreen screen) {
            projectCursor(screen, player, mc, event.getPartialTicks());
        } else {
            ItemStack heldMain = player.getHeldItemMainhand();
            ItemStack heldOff = player.getHeldItemOffhand();

            if (heldMain.getItem() instanceof ToolItem) {
                handleToolItem(player, heldMain, EnumHand.MAIN_HAND, event.getPartialTicks());
                rendered = true;
            } else if (heldOff.getItem() instanceof ToolItem) {
                handleToolItem(player, heldOff, EnumHand.OFF_HAND, event.getPartialTicks());
                rendered = true;
            } else {
                ToolItemAimModeHandler.clear();
            }
        }


        ItemStack held;
        if ((held = getHeldItemOfType(player, NetworkToolItem.class)) != null) {
            handleNetworkTool(player, held, event.getPartialTicks());
            rendered = true;
        }
        if ((held = getHeldItemOfType(player, LabelGunItem.class)) != null) {
            handleLabelGun(player, held, event.getPartialTicks());
            rendered = true;
        }
        if (!rendered) {
            renderCache.clear();
        }
    }

    /**
     * Gets the equivalent look direction of the cursor's current position on screen
     * and sends that to AimModeTargetHandler in order to raycast the *cursor* onto blocks
     */
    private static void projectCursor(
            PointerSelectScreen screen,
            EntityPlayerSP player,
            Minecraft mc,
            float partialTicks
    ) {
        int mouseX = Mouse.getX();
        int mouseY = Mouse.getY();

        FloatBuffer modelView = BufferUtils.createFloatBuffer(16);
        FloatBuffer projection = BufferUtils.createFloatBuffer(16);
        IntBuffer viewport = BufferUtils.createIntBuffer(16);

        GL11.glGetFloat(GL11.GL_MODELVIEW_MATRIX, modelView);
        GL11.glGetFloat(GL11.GL_PROJECTION_MATRIX, projection);
        GL11.glGetInteger(GL11.GL_VIEWPORT, viewport);

        FloatBuffer result = BufferUtils.createFloatBuffer(4);

        // Cursor's position flush with the screen
        GLU.gluUnProject(
                mouseX,
                mouseY,
                0f,
                modelView,
                projection,
                viewport,
                result
        );
        Vec3d near = new Vec3d(result.get(0), result.get(1), result.get(2));

        result.clear();

        // Cursor's position projected 1f into the screen
        GLU.gluUnProject(
                mouseX,
                mouseY,
                1f,
                modelView,
                projection,
                viewport,
                result
        );
        Vec3d far = new Vec3d(result.get(0), result.get(1), result.get(2));

        Vec3d dir = far.subtract(near).normalize();

        Entity view = mc.getRenderViewEntity();
        Vec3d camPos = view.getPositionEyes(partialTicks);

        var target = AimModeTargetHandler.traceCustomProjection(mc.world, camPos, dir);

        if (target != null) {
            if (AimModeTargetHandler.isShifted()) {
                renderShiftLine();
            }
            if (screen.selectionStart != null) {
                renderSelectionBoundaries(screen.selectionStart, target);
                drawHighlights(
                        VBOKind.CURSOR,
                        screen.getSelectedBlocks(target),
                        capabilityColor,
                        mc.player,
                        0.5F
                );
            } else {
                if (screen.targetBlock != null) {
                    renderSelectionBoundaries(target, target);

                } else {
                    drawHighlights(
                            VBOKind.CURSOR,
                            Collections.singleton(target),
                            cableColor,
                            mc.player,
                            1
                    );
                }
            }
        } else {
            drawHighlights(VBOKind.NETWORK_TOOL_CABLES, Collections.emptySet(), cableColor, mc.player, 1);

        }
    }

    private static void renderShiftLine() {
        GlStateManager.pushMatrix();
        var renderManager = Minecraft.getMinecraft().getRenderManager();
        GlStateManager.translate(
                -renderManager.viewerPosX,
                -renderManager.viewerPosY,
                -renderManager.viewerPosZ
        );
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.disableDepth();

        GlStateManager.color(255, 255, 255, 255);

        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(GL11.GL_LINES, DefaultVertexFormats.POSITION);

        var start = AimModeTargetHandler.getCurrentTarget();

        var end = AimModeTargetHandler.getUnshiftedTarget();
        wr.pos(start.getX() + 0.5, start.getY() + 0.5, start.getZ() + 0.5).endVertex();
        wr.pos(end.getX() + 0.5, end.getY() + 0.5, end.getZ() + 0.5).endVertex();

        Tessellator.getInstance().draw();

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void renderSelectionBoundaries(BlockPos start, BlockPos end) {
        GlStateManager.pushMatrix();
        var renderManager = Minecraft.getMinecraft().getRenderManager();
        GlStateManager.translate(
                -renderManager.viewerPosX,
                -renderManager.viewerPosY,
                -renderManager.viewerPosZ
        );
        GlStateManager.disableTexture2D();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
        GlStateManager.disableCull();
        GlStateManager.disableDepth();


        BufferBuilder wr = Tessellator.getInstance().getBuffer();
        wr.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_COLOR);

        double minX = Math.min(start.getX(), end.getX());
        double minY = Math.min(start.getY(), end.getY());
        double minZ = Math.min(start.getZ(), end.getZ());

        double maxX = Math.max(start.getX(), end.getX()) + 1;
        double maxY = Math.max(start.getY(), end.getY()) + 1;
        double maxZ = Math.max(start.getZ(), end.getZ()) + 1;

        wr.pos(minX, minY, minZ).color(255, 255, 255, 50).endVertex();
        wr.pos(minX, maxY, minZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, maxY, minZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, minY, minZ).color(255, 255, 255, 50).endVertex();

        wr.pos(maxX, minY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, maxY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(minX, maxY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(minX, minY, maxZ).color(255, 255, 255, 50).endVertex();

        wr.pos(minX, minY, minZ).color(255, 255, 255, 50).endVertex();
        wr.pos(minX, minY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(minX, maxY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(minX, maxY, minZ).color(255, 255, 255, 50).endVertex();

        wr.pos(maxX, maxY, minZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, maxY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, minY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, minY, minZ).color(255, 255, 255, 50).endVertex();

        wr.pos(minX, minY, minZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, minY, minZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, minY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(minX, minY, maxZ).color(255, 255, 255, 50).endVertex();

        wr.pos(minX, maxY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, maxY, maxZ).color(255, 255, 255, 50).endVertex();
        wr.pos(maxX, maxY, minZ).color(255, 255, 255, 50).endVertex();
        wr.pos(minX, maxY, minZ).color(255, 255, 255, 50).endVertex();

        Tessellator.getInstance().draw();

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static void handleToolItem(
            EntityPlayerSP player,
            ItemStack tool,
            EnumHand hand,
            float partialTicks
    ) {
        var selectionBlocks = ToolItemAimModeHandler.updateSelection(player, tool, hand, partialTicks);
        if (selectionBlocks.isEmpty()) return;
        drawHighlights(VBOKind.TOOL_ITEM_SELECTED_BLOCKS, selectionBlocks.positions(), capabilityColor, player, 1);
        drawHighlights(VBOKind.TOOL_ITEM_WARNING_BLOCKS, selectionBlocks.warningPositions(), warningColor, player, 1);

        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        var renderManager = Minecraft.getMinecraft().getRenderManager();
        GlStateManager.translate(
                -renderManager.viewerPosX,
                -renderManager.viewerPosY,
                -renderManager.viewerPosZ
        );
        if (ToolItemAimModeHandler.getMainSelectedBlock() != null) {
            drawLabel(
                    ToolItemAimModeHandler.getMainSelectedBlock(),
                    Collections.singleton(LabelGunItem.getActiveLabel(tool)),
                    player
            );
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }

    private static BlockPos lookingAt() {
        Minecraft mc = Minecraft.getMinecraft();
        if (mc.objectMouseOver != null) {
            return mc.objectMouseOver.getBlockPos();
        }
        return null;
    }

    private static @Nullable ItemStack getHeldItemOfType(
            EntityPlayerSP player,
            Class<?> itemClass
    ) {
        ItemStack mainHandItem = player.getHeldItemMainhand();
        if (itemClass.isInstance(mainHandItem.getItem())) {
            return mainHandItem;
        }

        ItemStack offhandItem = player.getHeldItemOffhand();
        if (itemClass.isInstance(offhandItem.getItem())) {
            return offhandItem;
        }

        return null;
    }

    private static void handleNetworkTool(
            EntityPlayerSP player,
            ItemStack networkTool,
            float partialTicks
    ) {
        if (!NetworkToolItem.getOverlayEnabled(networkTool)) return;
        Set<BlockPos> cablePositions = NetworkToolItem.getCablePositions(networkTool);
        Set<BlockPos> capabilityPositions = NetworkToolItem.getCapabilityProviderPositions(networkTool);

        var selectedPos = NetworkToolItem.getSelectedNetworkBlockPos(networkTool);
        if (cablePositions.isEmpty() && selectedPos != null) {
            drawHighlights(
                    VBOKind.NETWORK_TOOL_CABLES,
                    Stream.of(selectedPos).collect(Collectors.toCollection(HashSet::new)),
                    warningColor,
                    player,
                    1
            );
        } else {
            drawHighlights(VBOKind.NETWORK_TOOL_CABLES, cablePositions, cableColor, player, 1);
            drawHighlights(VBOKind.NETWORK_TOOL_CAPABILITIES, capabilityPositions, capabilityColor, player, 0.9F);
        }
    }


    private static void drawHighlights(
            VBOKind vboKind,
            Set<BlockPos> positions,
            int color,
            EntityPlayerSP player,
            float highlightFraction
    ) {
        var colorRGB = new Color(color, true);

        HighlightRenderList list = renderCache.getList(
                vboKind,
                positions,
                player,
                colorRGB.getRed(),
                colorRGB.getGreen(),
                colorRGB.getBlue(),
                colorRGB.getAlpha(),
                highlightFraction
        );

        if (list != null) {
            var renderManager = Minecraft.getMinecraft().getRenderManager();
            GlStateManager.pushMatrix();
            GlStateManager.translate(
                    -renderManager.viewerPosX,
                    -renderManager.viewerPosY,
                    -renderManager.viewerPosZ
            );
            GlStateManager.disableTexture2D();
            GlStateManager.enableBlend();
            GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GlStateManager.disableCull();
            GlStateManager.disableDepth();


            list.render();

            GlStateManager.enableDepth();
            GlStateManager.disableBlend();
            GlStateManager.enableCull();
            GlStateManager.enableTexture2D();
            GlStateManager.popMatrix();
        }
    }

    private static void handleLabelGun(EntityPlayerSP player, ItemStack labelGun, float partialTicks) {
        LabelGunItem.LabelGunViewMode viewMode = LabelGunItem.getViewMode(labelGun);
        LabelPositionHolder labelPositionHolder = LabelPositionHolder.from(labelGun);

        HashMultimap<BlockPos, String> labelsByPosition = HashMultimap.create();
        String activeLabel = LabelGunItem.getActiveLabel(labelGun);
        BlockPos lookingAtPos = lookingAt();


        switch (viewMode) {
            case SHOW_ALL -> //noinspection RedundantLabeledSwitchRuleCodeBlock
            {
                // Just add all labels
                labelPositionHolder.forEach((label, pos) -> labelsByPosition.put(pos, label));
            }
            case SHOW_ONLY_ACTIVE_LABEL_AND_TARGETED_BLOCK -> {
                // 1) Show the active label for all positions
                if (!activeLabel.isEmpty()) {
                    labelPositionHolder.forEach((label, pos) -> {
                        if (label.equals(activeLabel)) {
                            labelsByPosition.put(pos, label);
                        }
                    });
                }
                // 2) Also show *any* labels for the block the player is looking at
                if (lookingAtPos != null) {
                    labelsByPosition.putAll(lookingAtPos, labelPositionHolder.getLabels(lookingAtPos));
                }
            }
            case SHOW_ONLY_TARGETED_BLOCK -> {
                if (lookingAtPos != null) {
                    labelsByPosition.putAll(lookingAtPos, labelPositionHolder.getLabels(lookingAtPos));
                }
                break;
            }
        }


        drawHighlights(
                VBOKind.LABEL_GUN_CAPABILITIES,
                labelsByPosition.keySet(),
                viewMode != LabelGunItem.LabelGunViewMode.SHOW_ALL ? capabilityColorLimitedView : capabilityColor,
                player,
                0.9F
        );

        GlStateManager.pushMatrix();
        GlStateManager.disableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.disableDepth();
        GlStateManager.blendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);

        var renderManager = Minecraft.getMinecraft().getRenderManager();
        GlStateManager.translate(
                -renderManager.viewerPosX,
                -renderManager.viewerPosY,
                -renderManager.viewerPosZ
        );
        for (Map.Entry<BlockPos, Collection<String>> entry : labelsByPosition.asMap().entrySet()) {
            drawLabel(entry.getKey(), entry.getValue(), player);
        }

        GlStateManager.enableDepth();
        GlStateManager.disableBlend();
        GlStateManager.enableCull();
        GlStateManager.enableTexture2D();
        GlStateManager.popMatrix();
    }


    private static void drawLabel(BlockPos pos, Collection<String> labels, EntityPlayer player) {
        double x = pos.getX() + 0.5;
        double y = pos.getY() + 0.5;
        double z = pos.getZ() + 0.5;

        FontRenderer font = Minecraft.getMinecraft().fontRenderer;
        for (String label : labels) {
            GlStateManager.pushMatrix();
            GlStateManager.translate(x, y, z);
            GlStateManager.rotate(-player.rotationYaw, 0.0F, 1.0F, 0.0F);
            GlStateManager.rotate(player.rotationPitch, 1.0F, 0.0F, 0.0F);
            GlStateManager.scale(-0.025F, -0.025F, 0.025F);
            font.drawString(label, (int) (-font.getStringWidth(label) / 2f), 0, 0xFFFFFF);
            GlStateManager.popMatrix();
            y += 0.25;
        }
    }


    private enum VBOKind {
        LABEL_GUN_CAPABILITIES,
        NETWORK_TOOL_CAPABILITIES,
        NETWORK_TOOL_CABLES,
        TOOL_ITEM_SELECTED_BLOCKS,
        TOOL_ITEM_WARNING_BLOCKS,
        CURSOR,
    }


    private static class HighlightRenderListCache {
        private final EnumMap<VBOKind, HighlightRenderList> cache = new EnumMap<>(VBOKind.class);
        private int lastCachedTick = -1;

        public @Nullable HighlightRenderList getList(
                VBOKind kind,
                Set<BlockPos> positions,
                EntityPlayerSP player,
                int r,
                int g,
                int b,
                int a,
                float highlightFraction
        ) {
            if (positions.isEmpty()) {
                return null;
            }
            @Nullable HighlightRenderList entry = cache.get(kind);

            boolean shouldRebuild = entry == null;

            if (entry == null
                    || player.ticksExisted != lastCachedTick
                    || !entry.positions.equals(positions)
                    || entry.r != r
                    || entry.g != g
                    || entry.b != b
                    || entry.a != a
            ) {
                lastCachedTick = player.ticksExisted;
                shouldRebuild = true;
            }

            if (shouldRebuild) {
                // Dispose of the old VBO if it exists
                if (entry != null) {
                    entry.destroy();
                }

                entry = new HighlightRenderList(new HashSet<>(positions), r, g, b, a, highlightFraction);
                cache.put(kind, entry);
            }
            return entry;
        }

        public void clear() {
            // Dispose of all cached VBOs
            for (var entry : cache.values()) {
                entry.destroy();
            }
            cache.clear();
        }
    }


}
