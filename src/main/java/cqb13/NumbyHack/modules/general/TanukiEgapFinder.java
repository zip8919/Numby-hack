package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.meteorclient.utils.math.BlockPos;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.Blocks;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.block.entity.ChestBlockEntity;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.Formatting;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * 从 Tanuki 模块改编
 */
// https://gitlab.com/Walaryne/tanuki/-/blob/master/src/main/java/minegame159/meteorclient/modules/misc/EgapFinder.java

public class TanukiEgapFinder extends Module {
    private final SettingGroup sgDefault = settings.getDefaultGroup();

    private final Setting<Boolean> coords = sgDefault.add(new BoolSetting.Builder()
            .name("coords")
            .description("在消息中发送坐标，以防你懒得查看你的 .minecraft 文件夹。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> debug = sgDefault.add(new BoolSetting.Builder()
            .name("debug")
            .description("无用。只是打印出它检测到的每个箱子信息，会在聊天中大量刷屏。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> playSound = sgDefault.add(new BoolSetting.Builder()
            .name("play-sound")
            .description("当你找到一个 egap 时播放声音。")
            .defaultValue(false)
            .build()
    );

    private boolean check;
    private boolean lock;
    private int stage = 0;
    private int checkDelay;
    private int comparatorHold = 0;
    private BlockPos chest;
    private BlockPos prevChest;

    public TanukiEgapFinder() {
        super(NumbyHack.CATEGORY, "egap-finder", "在 SP 世界中寻找 Egaps 并创建一个名为 \\"egap-coords.txt\\" 的文件。");
    }

    private static void writeToFile(String coords) {
        try (FileWriter fw = new FileWriter("egap-coords.txt", true);
             BufferedWriter bw = new BufferedWriter(fw);
             PrintWriter out = new PrintWriter(bw)) {
            out.println(coords);
        } catch (IOException exception) {
            exception.printStackTrace();
        }
    }

    @Override
    public void onActivate() {
        stage = 0;
        checkDelay = 0;
        lock = true;
        if (debug.get()) ChatUtils.info("开始");
    }

    @Override
    public void onDeactivate() {
        if (debug.get()) ChatUtils.info("停止");
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        check = false;
        for (BlockEntity blockEntity : Utils.blockEntities()) {
            if (blockEntity instanceof ChestBlockEntity) {
                if (blockEntity.isRemoved()) continue;
                chest = blockEntity.getPos();

                check = true;
                lock = false;
            }
        }

        if (!check) {
            checkDelay++;
        } else {
            checkDelay = 0;
        }
        if (checkDelay >= 2) {
            lock = true;
            checkDelay = 0;
            stage = 1;
        }

        if (!lock) {
            if (stage == 0) {
                stage = 1;
            }
            switch (stage) {
                case 1: {
                    int adjacent = chest.getX() - 1;
                    Block block = mc.world.getBlockState(chest.add(-1, 0, 0)).getBlock();
                    if (block != Blocks.COMPARATOR) {
                        ChatUtils.sendPlayerMsg("/setblock " + adjacent + " " + chest.getY() + " " + chest.getZ() + " minecraft:comparator[facing=east]");
                    }
                    stage++;
                    break;
                }
                case 2: {
                    int xAdjacent = chest.getX() - 1;
                    int yAdjacent = chest.getY() + 1;
                    Block block = mc.world.getBlockState(chest.add(-1, +1, 0)).getBlock();
                    if (block != Blocks.ACACIA_LEAVES) {
                        ChatUtils.sendPlayerMsg("/setblock " + xAdjacent + " " + yAdjacent + " " + chest.getZ() + " minecraft:acacia_leaves");
                    }
                    comparatorHold++;
                    if (comparatorHold == 3) {
                        stage++;
                        comparatorHold = 0;
                    }
                    break;
                }
                case 3: {
                    Block block = mc.world.getBlockState(chest).getBlock();
                    if (block == Blocks.CHEST) {
                        ChatUtils.sendPlayerMsg("/execute if data block " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " Items[{id:\\"minecraft:enchanted_golden_apple\\"}] as @p run setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " minecraft:diamond_block");
                    }
                    prevChest = chest;
                    stage++;
                    break;
                }
                case 4: {
                    Block diamondPos = mc.world.getBlockState(prevChest).getBlock();
                    if (diamondPos == Blocks.DIAMOND_BLOCK) {
                        if (playSound.get()) mc.player.playSound(SoundEvents.ENTITY_EXPERIENCE_ORB_PICKUP, 1.0f, 1.0f);
                        ChatUtils.info((!coords.get() ? Formatting.GREEN + "找到一个 egap！已将坐标写入文件。" : Formatting.GREEN + "找到一个 egap！已将坐标写入文件。 " + prevChest.getX() + " " + prevChest.getY() + " " + prevChest.getZ()));
                        writeToFile(prevChest.getX() + " " + prevChest.getY() + " " + prevChest.getZ());
                    } else
                        ChatUtils.sendPlayerMsg("/setblock " + chest.getX() + " " + chest.getY() + " " + chest.getZ() + " minecraft:air");
                    stage++;
                    break;
                }
            }
            if (stage == 5) {
                stage = 1;
            }
        }
    }
}
