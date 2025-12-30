package ca.teamdman.sfm.client.handler;

import ca.teamdman.sfm.common.util.Mth;
import net.minecraft.client.entity.EntityPlayerSP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.EnumHand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.RayTraceResult;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class AimModeTargetHandler {
    static int digDepth = 0;
    @Nullable
    private static BlockPos cachedLookTarget;
    @Nullable
    private static BlockPos cachedUnshiftedTarget;

    public static void resetDigDepth() {
        AimModeTargetHandler.digDepth = 0;
    }

    public static void shiftDigDepth(int amount, int max) {
        digDepth = Mth.clamp(amount + digDepth, -max, max);
    }

    @Nullable
    public static BlockPos getCurrentTarget() {
        return cachedLookTarget;
    }

    public static @Nullable BlockPos getUnshiftedTarget() {
        return cachedUnshiftedTarget;
    }

    public static boolean isShifted() {
        return cachedLookTarget != null && cachedUnshiftedTarget != cachedLookTarget;
    }

    @Nullable
    public static BlockPos tracePlayerLook(
            EntityPlayerSP player,
            ItemStack toolStack,
            EnumHand hand,
            float partialTicks
    ) {

        Vec3d eye = player.getPositionEyes(partialTicks);
        Vec3d look = player.getLook(partialTicks);

        return traceCustomProjection(player.getEntityWorld(), eye, look);
    }

    @Nullable
    public static BlockPos traceCustomProjection(World world, Vec3d eye, Vec3d look) {
        double remainingDistance = 100;
        Vec3d endVec;
        endVec = eye.add(look.scale(remainingDistance));
        RayTraceResult trace = world.rayTraceBlocks(eye, endVec, false, false, false);
        if (trace != null && trace.typeOfHit == RayTraceResult.Type.BLOCK) {
            cachedUnshiftedTarget =trace.getBlockPos();
            cachedLookTarget = digDepth != 0 ? cachedUnshiftedTarget.offset( trace.sideHit.getOpposite(), digDepth) : cachedUnshiftedTarget;
            return cachedLookTarget;
        }
        return null;
    }


}
