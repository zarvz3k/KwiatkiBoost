package com.example.flowerboost;

import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class FlowerBoost implements ModInitializer {

    private record Pending(ServerPlayer player, ServerLevel level, BlockPos pos) {}

    private static final List<Pending> PENDING = new ArrayList<>();

    @Override
    public void onInitialize() {
        FlowerConfig.load();

        // komendy
        CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) ->
                dispatcher.register(Commands.literal("flowerboost")
                        .requires(src -> src.hasPermission(2))
                        .then(Commands.literal("count")
                                .then(Commands.argument("n", IntegerArgumentType.integer(0, 50))
                                        .executes(ctx -> {
                                            FlowerConfig.flowerCount = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Kwiatków na bone meal: " + FlowerConfig.flowerCount), true);
                                            return 1;
                                        })))
                        .then(Commands.literal("radius")
                                .then(Commands.argument("n", IntegerArgumentType.integer(1, 10))
                                        .executes(ctx -> {
                                            FlowerConfig.radius = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendSuccess(
                                                    () -> Component.literal("Zasięg: " + FlowerConfig.radius), true);
                                            return 1;
                                        })))));

        // zapamiętaj kliknięcie bone mealem w trawę
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            if (world.isClientSide()) return InteractionResult.PASS;
            if (!(player instanceof ServerPlayer sp)) return InteractionResult.PASS;
            if (!(world instanceof ServerLevel level)) return InteractionResult.PASS;
            if (!player.getItemInHand(hand).is(Items.BONE_MEAL)) return InteractionResult.PASS;

            BlockPos pos = hitResult.getBlockPos();
            if (!level.getBlockState(pos).is(Blocks.GRASS_BLOCK)) return InteractionResult.PASS;

            PENDING.add(new Pending(sp, level, pos));
            return InteractionResult.PASS;
        });

        // pod koniec ticka dosadź brakujące kwiatki
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (PENDING.isEmpty()) return;
            List<Pending> work = new ArrayList<>(PENDING);
            PENDING.clear();
            for (Pending p : work) {
                topUp(p.player(), p.level(), p.pos());
            }
        });
    }

    private static void topUp(ServerPlayer player, ServerLevel level, BlockPos center) {
        int r = FlowerConfig.radius;

        int current = 0;
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 2; dy++) {
                    if (level.getBlockState(center.offset(dx, dy, dz)).is(BlockTags.SMALL_FLOWERS)) {
                        current++;
                    }
                }
            }
        }

        int missing = FlowerConfig.flowerCount - current;
        if (missing <= 0) return;

        List<BlockPos> spots = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++) {
            for (int dz = -r; dz <= r; dz++) {
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos above = center.offset(dx, dy + 1, dz);
                    BlockState atSpot = level.getBlockState(above);
                    if ((atSpot.isAir() || atSpot.is(Blocks.SHORT_GRASS))
                            && level.getBlockState(above.below()).is(Blocks.GRASS_BLOCK)) {
                        spots.add(above);
                    }
                }
            }
        }
        Collections.shuffle(spots);

        boolean creative = player.getAbilities().instabuild;

        for (BlockPos spot : spots) {
            if (missing <= 0) break;

            ItemStack stack = findFlower(player);
            if (stack == null) {
                player.displayClientMessage(
                        Component.literal("Brak kwiatków w ekwipunku!"), true);
                break;
            }

            Block block = ((BlockItem) stack.getItem()).getBlock();
            BlockState state = block.defaultBlockState();
            if (state.canSurvive(level, spot)) {
                level.setBlock(spot, state, Block.UPDATE_ALL);
                if (!creative) stack.shrink(1);
                missing--;
            }
        }
    }

    private static ItemStack findFlower(ServerPlayer player) {
        List<ItemStack> flowers = new ArrayList<>();
        int size = player.getInventory().getContainerSize();
        for (int i = 0; i < size; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi
                    && bi.getBlock().defaultBlockState().is(BlockTags.SMALL_FLOWERS)) {
                flowers.add(s);
            }
        }
        if (flowers.isEmpty()) return null;
        return flowers.get(new java.util.Random().nextInt(flowers.size()));
    }
}
