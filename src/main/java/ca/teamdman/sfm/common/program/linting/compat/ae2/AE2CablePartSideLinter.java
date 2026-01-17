package ca.teamdman.sfm.common.program.linting.compat.ae2;

import appeng.api.AEApi;
import appeng.api.parts.IPartHost;
import appeng.api.parts.PartItemStack;
import ca.teamdman.sfm.common.blockentity.ManagerBlockEntity;
import ca.teamdman.sfm.common.compat.SFMAE2Compat;
import ca.teamdman.sfm.common.compat.SFMModCompat;
import ca.teamdman.sfm.common.label.LabelPositionHolder;
import ca.teamdman.sfm.common.localization.LocalizationKeys;
import ca.teamdman.sfm.common.program.linting.IProgramLinter;
import ca.teamdman.sfm.common.program.linting.ProblemTracker;
import ca.teamdman.sfm.common.util.Pair;
import ca.teamdman.sfm.common.util.SFMStreamUtils;
import ca.teamdman.sfml.ast.*;
import net.minecraft.block.state.IBlockState;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import net.minecraftforge.registries.IForgeRegistryEntry;
import org.jetbrains.annotations.Nullable;

import java.util.stream.Collectors;
import java.util.stream.Stream;

import static ca.teamdman.sfm.common.localization.LocalizationKeys.PROGRAM_WARNING_AE2_CABLE_USED_WITH_NULL_DIRECTION;

public class AE2CablePartSideLinter extends IForgeRegistryEntry.Impl<IProgramLinter> implements IProgramLinter {
    @Override
    public void gatherWarnings(
            Program program,
            LabelPositionHolder labels,
            @Nullable ManagerBlockEntity manager,
            ProblemTracker tracker
    ) {
        if (manager == null) return;
        World level = manager.getLevel();
        if (level == null) return;

        Stream<IOStatement> ioStatements = program.getDescendantStatements()
                .filter(IOStatement.class::isInstance)
                .map(IOStatement.class::cast);
        for (IOStatement statement : SFMStreamUtils.iterate(ioStatements)) {
            if (gatherWarningsForIOStatement(statement, labels, level, tracker).isSaturated()) {
                break;
            }
        }
    }

    private static ProblemTracker.AddProblemResult gatherWarningsForIOStatement(
            IOStatement statement,
            LabelPositionHolder labelPositionHolder,
            World level,
            ProblemTracker warnings
    ) {
        var compat = SFMAE2Compat.instance();
        SideQualifier sides = statement.labelAccess().sides();
        Stream<Pair<Label, BlockPos>> ae2Blocks = statement
                .labelAccess()
                .getLabelledPositions(labelPositionHolder)
                .stream()
                .filter(pair -> level.isBlockLoaded(pair.getSecond()))
                .filter(pair -> compat.isAE2Cable(level, pair.getSecond()));

        if (sides.sides().contains(Side.NULL)) {
            for (Pair<Label, BlockPos> pair : SFMStreamUtils.iterate(ae2Blocks)) {
                Label label = pair.getFirst();
                if (
                        warnings.add(PROGRAM_WARNING_AE2_CABLE_USED_WITH_NULL_DIRECTION.get(
                                label,
                                statement.toStringPretty()
                        )).isSaturated()
                ) {
                    return ProblemTracker.AddProblemResult.TOO_MANY_PROBLEMS;
                }
            }
        } else {
            var partHelper = compat.partHelper;
            var resourceTypes = statement.getReferencedIOResourceIds().map(ResourceIdentifier::getResourceType).collect(
                    Collectors.toSet()
            );
            for (Pair<Label, BlockPos> pair : SFMStreamUtils.iterate(ae2Blocks)) {
                IBlockState blockState = level.getBlockState(pair.getSecond());

                IPartHost partHost = partHelper.getPartHost(level, pair.getSecond());
                if (partHost != null) {
                    for (var side : sides.sides()) {
                        var part = partHost.getPart(side.resolve(blockState));

                    }
                }
//                if (
//                        warnings.add(PROGRAM_WARNING_AE2_CABLE_USED_WITH_NULL_DIRECTION.get(
//                                label,
//                                statement.toStringPretty()
//                        )).isSaturated()
//                ) {
//                    return ProblemTracker.AddProblemResult.TOO_MANY_PROBLEMS;
//                }
            }
//            // Check side config
//            EnumSet<TransmissionType> referencedTransmissionTypes = SFMMekanismCompat
//                    .getReferencedTransmissionTypes(statement);
//
//            Predicate<DataType> dataTypePredicate;
//            if (ioStatement instanceof InputStatement) {
//                dataTypePredicate = dataType -> dataType.canOutput() || dataType == DataType.EXTRA;
//            } else if (ioStatement instanceof OutputStatement) {
//                dataTypePredicate = dataType -> dataType == DataType.INPUT
//                                                || dataType == DataType.INPUT_OUTPUT
//                                                || dataType == DataType.INPUT_1
//                                                || dataType == DataType.INPUT_2
//                                                || dataType == DataType.EXTRA;
//            } else {
//                throw new IllegalStateException("Unexpected value: " + ioStatement);
//            }
//
//            mekanismBlocks.forEach(pair -> {
//                BlockPos blockPos = pair.getSecond();
//                BlockEntity blockEntity = level.getBlockEntity(blockPos);
//                if (blockEntity instanceof ISideConfiguration mekBlockEntity) {
//                    TileComponentConfig config = mekBlockEntity.getConfig();
//                    BlockState blockState = blockEntity.getBlockState();
//                    for (TransmissionType transmissionType : referencedTransmissionTypes) {
//                        boolean anySuccess = false;
//                        ConfigInfo transmissionConfig = config.getConfig(transmissionType);
//                        if (transmissionConfig != null) {
//                            Set<Direction> activeSides = transmissionConfig.getSides(dataTypePredicate);
//                            for (Direction direction : sides.resolve(blockState)) {
//                                if (activeSides.contains(direction)) {
//                                    anySuccess = true;
//                                    break;
//                                }
//                            }
//                        }
//                        if (!anySuccess) {
//                            warnings.add(PROGRAM_WARNING_MEKANISM_BAD_SIDE_CONFIG.get(
//                                    blockPos,
//                                    pair.getFirst(),
//                                    statement.toStringPretty()
//                            ));
//                        }
//                    }
//                }
//            });
        }
        return ProblemTracker.AddProblemResult.SUCCESS;
    }

    @Override
    public void fixWarnings(
            Program program,
            LabelPositionHolder labels,
            ManagerBlockEntity manager,
            World level,
            ItemStack disk
    ) {

    }
}
