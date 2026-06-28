package com.kazeshukufuku.centralvintage.content.pickling;

import com.simibubi.create.content.kinetics.belt.BeltBlockEntity;
import com.simibubi.create.content.kinetics.belt.behaviour.DirectBeltInputBehaviour;
import com.simibubi.create.content.logistics.funnel.FunnelBlock;
import com.simibubi.create.foundation.blockEntity.behaviour.BlockEntityBehaviour;
import net.createmod.catnip.data.Iterate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.ribs.vintagedelight.block.entity.FermentingJarBlockEntity;

import java.util.Collections;
import java.util.List;

public final class FermentingJarOutput {
    public static final DirectionProperty FACING = BlockStateProperties.FACING_HOPPER;
    public static final int OUTPUT_ANIMATION_TIME = 10;

    public static Direction updateOutputDirection(Level level, BlockPos jarPos, BlockState state) {
        return updateOutputDirection(level, jarPos, state, Collections.emptyList(), null);
    }

    public static Direction updateOutputDirection(
            Level level,
            BlockPos jarPos,
            BlockState state,
            List<Direction> disabledOutputs,
            Direction preferredOutput
    ) {
        if (!state.hasProperty(FACING)) {
            return Direction.DOWN;
        }

        Direction currentFacing = state.getValue(FACING);
        Direction newFacing = Direction.DOWN;
        for (Direction direction : Iterate.horizontalDirections) {
            if (canOutputTo(level, jarPos, direction) && !disabledOutputs.contains(direction)) {
                newFacing = direction;
            }
        }

        if (preferredOutput != null && preferredOutput != Direction.UP && canOutputTo(level, jarPos, preferredOutput)) {
            newFacing = preferredOutput;
        }

        if (newFacing != currentFacing) {
            level.setBlockAndUpdate(jarPos, state.setValue(FACING, newFacing));
        }
        return newFacing;
    }

    public static boolean canOutputTo(BlockGetter level, BlockPos jarPos, Direction direction) {
        BlockPos neighbour = jarPos.relative(direction);
        BlockPos output = neighbour.below();
        BlockState blockState = level.getBlockState(neighbour);

        if (FunnelBlock.isFunnel(blockState)) {
            if (FunnelBlock.getFunnelFacing(blockState) == direction) {
                return false;
            }
        } else if (!blockState.getCollisionShape(level, neighbour).isEmpty()) {
            return false;
        } else {
            BlockEntity blockEntity = level.getBlockEntity(output);
            if (blockEntity instanceof BeltBlockEntity belt) {
                return belt.getSpeed() == 0 || belt.getMovementFacing() != direction.getOpposite();
            }
        }

        DirectBeltInputBehaviour directBeltInputBehaviour =
                BlockEntityBehaviour.get(level, output, DirectBeltInputBehaviour.TYPE);
        return directBeltInputBehaviour != null && directBeltInputBehaviour.canInsertFromSide(direction);
    }

    public static void exportOutputs(FermentingJarBlockEntity fermentingJar, Level level, BlockPos jarPos, BlockState state) {
        Direction direction = fermentingJar instanceof FermentingJarOutputController outputController
                ? updateOutputDirection(
                level,
                jarPos,
                state,
                outputController.centralvintage$getDisabledOutputs(),
                outputController.centralvintage$getPreferredOutput()
        )
                : updateOutputDirection(level, jarPos, state);
        if (direction == Direction.DOWN) {
            return;
        }

        BlockPos output = jarPos.below().relative(direction);
        DirectBeltInputBehaviour directBeltInputBehaviour =
                BlockEntityBehaviour.get(level, output, DirectBeltInputBehaviour.TYPE);
        IItemHandler target = getTargetItemHandler(level, output, direction);
        IItemHandler source = fermentingJar.getCapability(ForgeCapabilities.ITEM_HANDLER, Direction.DOWN).orElse(null);
        if (source == null) {
            return;
        }

        for (int slot = 0; slot < source.getSlots(); slot++) {
            ItemStack extracted = source.extractItem(slot, 64, true);
            if (extracted.isEmpty()) {
                continue;
            }

            if (directBeltInputBehaviour != null && directBeltInputBehaviour.canInsertFromSide(direction)) {
                ItemStack remainder = directBeltInputBehaviour.handleInsertion(extracted.copy(), direction, true);
                int inserted = extracted.getCount() - remainder.getCount();
                if (inserted <= 0) {
                    continue;
                }

                ItemStack toInsert = source.extractItem(slot, inserted, false);
                ItemStack leftover = directBeltInputBehaviour.handleInsertion(toInsert, direction, false);
                visualizeOutput(fermentingJar, extracted, inserted);
                if (!leftover.isEmpty()) {
                    Block.popResource(level, jarPos, leftover);
                }
                continue;
            }

            if (target == null) {
                continue;
            }

            ItemStack remainder = ItemHandlerHelper.insertItemStacked(target, extracted, true);
            int inserted = extracted.getCount() - remainder.getCount();
            if (inserted <= 0) {
                continue;
            }

            ItemStack toInsert = source.extractItem(slot, inserted, false);
            ItemStack leftover = ItemHandlerHelper.insertItemStacked(target, toInsert, false);
            visualizeOutput(fermentingJar, extracted, inserted);
            if (!leftover.isEmpty()) {
                Block.popResource(level, jarPos, leftover);
            }
        }
    }

    private static IItemHandler getTargetItemHandler(Level level, BlockPos output, Direction direction) {
        BlockEntity blockEntity = level.getBlockEntity(output);
        if (blockEntity == null) {
            return null;
        }
        return blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).orElse(null);
    }

    private static void visualizeOutput(FermentingJarBlockEntity fermentingJar, ItemStack stack, int count) {
        if (!stack.isEmpty() && count > 0 && fermentingJar instanceof FermentingJarOutputVisuals visuals) {
            ItemStack visualizedStack = stack.copy();
            visualizedStack.setCount(count);
            visuals.centralvintage$visualizeOutput(visualizedStack);
        }
    }

    private FermentingJarOutput() {
    }
}
