package ca.teamdman.sfm.common.label;

import ca.teamdman.sfm.common.cablenetwork.CableNetwork;
import ca.teamdman.sfm.common.cablenetwork.CableNetworkManager;
import ca.teamdman.sfm.common.net.FriendlyByteBuf;
import ca.teamdman.sfm.common.net.ServerboundLabelGunUsePacket;
import ca.teamdman.sfm.common.util.CompressedBlockPosSet;
import ca.teamdman.sfm.common.util.SFMStreamUtils;
import com.github.bsideup.jabel.Desugar;
import net.minecraft.block.Block;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import static ca.teamdman.sfm.common.util.SFMStreamUtils.get3DNeighbours;
import static ca.teamdman.sfm.common.util.SFMStreamUtils.get3DNeighboursIncludingKittyCorner;

@Desugar
public record SelectionTargets(
        Set<BlockPos> positions,
        Set<BlockPos> warningPositions
) {
    public static final SelectionTargets empty = new SelectionTargets(Collections.emptySet(), Collections.emptySet());

    public boolean isEmpty() {
        return this.positions.isEmpty();
    }

    public static SelectionTargets read(FriendlyByteBuf buf) {
        return new SelectionTargets(
                CompressedBlockPosSet.read(buf).into(),
                CompressedBlockPosSet.read(buf).into()
        );
    }

    public void write(FriendlyByteBuf buf) {
        CompressedBlockPosSet.from(this.positions).write(buf);
        CompressedBlockPosSet.from(this.warningPositions).write(buf);
    }
}
