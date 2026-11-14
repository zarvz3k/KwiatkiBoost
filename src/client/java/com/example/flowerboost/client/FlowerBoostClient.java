package com.example.flowerboost.client;

import com.example.flowerboost.FlowerConfig;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FlowerBoostClient implements ClientModInitializer {

    private static final int MAX_PLACE = 30;

    private static final Random RND = new Random();

    private static int countdown = -1;
    private static BlockPos center = null;
    private static int originalSlot = 0;

    // faza stawiania
    private static boolean placing = false;
    private static int stepWait = 0;
    private static final List<BlockPos> queue = new ArrayList<>();

    @Override
    public void onInitializeClient() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) ->
                dispatcher.register(ClientCommandManager.literal("fbc")
                        .then(ClientCommandManager.literal("count")
                                .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(0, 50))
                                        .executes(ctx -> {
                                            FlowerConfig.flowerCount = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendFeedback(Component.literal("Kwiatków: " + FlowerConfig.flowerCount));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("radius")
                                .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(1, 4))
                                        .executes(ctx -> {
                                            FlowerConfig.radius = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendFeedback(Component.literal("Zasięg: " + FlowerConfig.radius));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("wait")
                                .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> {
                                            FlowerConfig.waitTicks = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendFeedback(Component.literal("Wait: " + FlowerConfig.waitTicks + " tick"));
                                            return 1;
                                        })))
                        .then(ClientCommandManager.literal("step")
                                .then(ClientCommandManager.argument("n", IntegerArgumentType.integer(1, 100))
                                        .executes(ctx -> {
                                            FlowerConfig.stepTicks = IntegerArgumentType.getInteger(ctx, "n");
                                            FlowerConfig.save();
                                            ctx.getSource().sendFeedback(Component.literal("Step: " + FlowerConfig.stepTicks + " tick"));
                                            return 1;
                                        })))));

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!world.isClientSide()) return InteractionResult.PASS;
            if (mc.getSingleplayerServer() != null) return InteractionResult.PASS;
            if (placing) return InteractionResult.PASS;
            if (!player.getItemInHand(hand).is(Items.BONE_MEAL)) return InteractionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();
            if (!world.getBlockState(pos).is(Blocks.GRASS_BLOCK)) return InteractionResult.PASS;

            center = pos;
            countdown = FlowerConfig.waitTicks;
            originalSlot = mc.player.getInventory().selected;
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (mc.player == null || mc.level == null || mc.gameMode == null) {
                placing = false;
                countdown = -1;
                return;
            }

            if (placing) {
                stepPlace(mc, mc.player, mc.level);
                return;
            }

            if (countdown < 0) return;
            if (--countdown > 0) return;
            countdown = -1;
            if (center == null) return;
            prepare(mc.player, mc.level, center);
        });
    }

    // policz brakujące kwiatki i przygotuj kolejkę
    private static void prepare(LocalPlayer player, Level level, BlockPos c) {
        int r = FlowerConfig.radius;

        int current = 0;
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                for (int dy = -1; dy <= 2; dy++)
                    if (level.getBlockState(c.offset(dx, dy, dz)).is(BlockTags.SMALL_FLOWERS)) current++;

        int missing = Math.min(FlowerConfig.flowerCount - current, MAX_PLACE);
        if (missing <= 0) return;

        List<BlockPos> grassBlocks = new ArrayList<>();
        for (int dx = -r; dx <= r; dx++)
            for (int dz = -r; dz <= r; dz++)
                for (int dy = -1; dy <= 1; dy++) {
                    BlockPos grass = c.offset(dx, dy, dz);
                    BlockState above = level.getBlockState(grass.above());
                    if (level.getBlockState(grass).is(Blocks.GRASS_BLOCK)
                            && (above.isAir() || above.is(Blocks.SHORT_GRASS))) {
                        grassBlocks.add(grass);
                    }
                }
        Collections.shuffle(grassBlocks);

        queue.clear();
        for (BlockPos g : grassBlocks) {
            if (queue.size() >= missing) break;
            queue.add(g);
        }
        if (queue.isEmpty()) return;

        int slot = pickSlot(player);
        if (slot < 0) {
            player.displayClientMessage(Component.literal("Brak kwiatków na pasku szybkiego dostępu!"), true);
            queue.clear();
            return;
        }
        player.getInventory().selected = slot;
        stepWait = FlowerConfig.stepTicks - 1;
        placing = true;
    }

    // jeden krok: postaw kwiatek (slot był wybrany w poprzednim ticku), potem wybierz slot na następny
    private static void stepPlace(Minecraft mc, LocalPlayer player, Level level) {
        if (stepWait > 0) {
            stepWait--;
            return;
        }

        if (!queue.isEmpty()) {
            BlockPos grass = queue.remove(0);
            ItemStack held = player.getInventory().getItem(player.getInventory().selected);
            boolean heldIsFlower = !held.isEmpty() && held.getItem() instanceof BlockItem bi
                    && bi.getBlock().defaultBlockState().is(BlockTags.SMALL_FLOWERS);
            BlockState above = level.getBlockState(grass.above());

            if (heldIsFlower && level.getBlockState(grass).is(Blocks.GRASS_BLOCK)
                    && (above.isAir() || above.is(Blocks.SHORT_GRASS))) {
                BlockHitResult hit = new BlockHitResult(
                        Vec3.atCenterOf(grass).add(0, 0.5, 0), Direction.UP, grass, false);
                mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            }
        }

        if (queue.isEmpty()) {
            player.getInventory().selected = originalSlot; // wróć do bone meala
            placing = false;
            return;
        }

        int slot = pickSlot(player);
        if (slot < 0) {
            player.displayClientMessage(Component.literal("Brak kwiatków na pasku szybkiego dostępu!"), true);
            queue.clear();
            player.getInventory().selected = originalSlot;
            placing = false;
            return;
        }
        player.getInventory().selected = slot;
        stepWait = FlowerConfig.stepTicks - 1;
    }

    private static int pickSlot(LocalPlayer player) {
        List<Integer> slots = new ArrayList<>();
        for (int i = 0; i < 9; i++) {
            ItemStack s = player.getInventory().getItem(i);
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi
                    && bi.getBlock().defaultBlockState().is(BlockTags.SMALL_FLOWERS)) {
                slots.add(i);
            }
        }
        if (slots.isEmpty()) return -1;
        return slots.get(RND.nextInt(slots.size()));
    }
}
