package com.kqp.inventorytabs.tabs.provider;

import com.kqp.inventorytabs.tabs.tab.SimpleBlockTab;
import com.kqp.inventorytabs.tabs.tab.Tab;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShulkerBoxBlock;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ShulkerBoxBlockEntity;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.mob.ShulkerLidCollisions;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Provides tabs for shulker boxes.
 * Takes into account if it's blocked.
 */
public class ShulkerBoxTabProvider extends BlockTabProvider {
    @Override
    public void addAvailableTabs(ClientPlayerEntity player, List<Tab> tabs) {
        super.addAvailableTabs(player, tabs);

        Set<SimpleBlockTab> tabsToRemove = new HashSet();

        List<SimpleBlockTab> shulkerTabs = tabs.stream()
                .filter(tab -> tab instanceof SimpleBlockTab)
                .map(tab -> (SimpleBlockTab) tab)
                .filter(tab -> tab.block instanceof ShulkerBoxBlock)
                .collect(Collectors.toList());

        World world = player.world;

        // Add any chests that are blocked
        shulkerTabs.stream()
                .filter(tab -> {
                    BlockEntity blockEntity = player.world.getBlockEntity(tab.blockPos);

                    if (blockEntity instanceof ShulkerBoxBlockEntity) {
                        BlockState blockState = player.world.getBlockState(tab.blockPos);
                        Direction direction = blockState.get(ShulkerBoxBlock.FACING);

                        if (((ShulkerBoxBlockEntity) blockEntity).getAnimationStage() == ShulkerBoxBlockEntity.AnimationStage.CLOSED) {
                            if (!player.world.doesNotCollide(ShulkerLidCollisions.getLidCollisionBox(tab.blockPos, direction))) {
                                return true;
                            }
                        }
                    }

                    return false;
                })
                .forEach(tabsToRemove::add);

        tabs.removeAll(tabsToRemove);
    }

    @Override
    public boolean matches(World world, BlockPos pos) {
        return world.getBlockState(pos).getBlock() instanceof ShulkerBoxBlock;
    }

    @Override
    public Tab createTab(World world, BlockPos pos) {
        return new SimpleBlockTab(world.getBlockState(pos).getBlock(), pos);
    }
}
