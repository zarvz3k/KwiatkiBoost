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
import net.minecraft.core.Direction;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class FlowerBoostClient implements ClientModInitializer {

    private static final int WAIT_TICKS = 8; // czas na odpowiedź serwera po bone mealu
    private static final int MAX_PLACE = 30;

    private static int countdown = -1;
    private static BlockPos center = null;
    private static int originalSlot = 0;

    @Override
    public void onInitializeClient() {
        // komendy tylko u Ciebie: /fbc count 6, /fbc radius 1
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
                                        })))));

        // zapamiętaj kliknięcie bone mealem w trawę (tylko na cudzych serwerach)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            Minecraft mc = Minecraft.getInstance();
            if (!world.isClientSide()) return InteractionResult.PASS;
            if (mc.getSingleplayerServer() != null) return InteractionResult.PASS; // w singleplayerze robi to mod serwerowy
            if (!player.getItemInHand(hand).is(Items.BONE_MEAL)) return InteractionResult.PASS;
            BlockPos pos = hitResult.getBlockPos();
            if (!world.getBlockState(pos).is(Blocks.GRASS_BLOCK)) return InteractionResult.PASS;

            center = pos;
            countdown = WAIT_TICKS;
            originalSlot = mc.player.getInventory().selected;
            return InteractionResult.PASS;
        });

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            if (countdown < 0) return;
            if (--countdown > 0) return;
            countdown = -1;
            if (mc.player == null || mc.level == null || mc.gameMode == null || center == null) return;
            topUp(mc, mc.player, mc.level, center);
        });
    }

    private static void topUp(Minecraft mc, LocalPlayer player, Level level, BlockPos c) {
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

        Random rnd = new Random();
        for (BlockPos grass : grassBlocks) {
            if (missing <= 0) break;

            // wybierz losowy kwiatek z paska szybkiego dostępu
            List<Integer> slots = new ArrayList<>();
            for (int i = 0; i < 9; i++) {
                ItemStack s = player.getInventory().getItem(i);
                if (!s.isEmpty() && s.getItem() instanceof BlockItem bi
                        && bi.getBlock().defaultBlockState().is(BlockTags.SMALL_FLOWERS)) {
                    slots.add(i);
                }
            }
            if (slots.isEmpty()) {
                player.displayClientMessage(Component.literal("Brak kwiatków na pasku szybkiego dostępu!"), true);
                break;
            }

            player.getInventory().selected = slots.get(rnd.nextInt(slots.size()));
            BlockHitResult hit = new BlockHitResult(
                    Vec3.atCenterOf(grass).add(0, 0.5, 0), Direction.UP, grass, false);
            mc.gameMode.useItemOn(player, InteractionHand.MAIN_HAND, hit);
            missing--;
        }

        player.getInventory().selected = originalSlot; // wróć do bone meala
    }
}
