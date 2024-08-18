package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.misc.Pool;
import meteordevelopment.meteorclient.utils.player.FindItemResult;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.FallingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 基于 Tanuki
 */
public class SafetyNet extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("渲染");

    // 安全网
    private final Setting<SafetyNet.PlaceMode> safetyMode = sgGeneral.add(new EnumSetting.Builder<SafetyNet.PlaceMode>()
            .name("安全模式")
            .description("使用哪种安全模式。")
            .defaultValue(PlaceMode.SafetyNet)
            .build()
    );

    private final Setting<Integer> yLock = sgGeneral.add(new IntSetting.Builder()
            .name("安全网高度")
            .description("安全网的Y级别。")
            .min(1)
            .max(255)
            .sliderMin(1)
            .sliderMax(255)
            .defaultValue(1)
            .build()
    );

    private final Setting<Integer> safetyNetWindow = sgGeneral.add(new IntSetting.Builder()
            .name("安全网窗口")
            .description("从Y级别锁定开始的激活窗口+Y。")
            .min(1)
            .max(32)
            .sliderMin(1)
            .sliderMax(32)
            .defaultValue(10)
            .build()
    );

    private final Setting<Double> safetyNetMultiplier = sgGeneral.add(new DoubleSetting.Builder()
            .name("Y减速乘数")
            .description("Y速度减速乘数。")
            .min(0.1)
            .max(0.99)
            .sliderMin(0.1)
            .sliderMax(0.99)
            .defaultValue(0.7)
            .build()
    );

    // 通用
    private final Setting<Boolean> autoSwitch = sgGeneral.add(new BoolSetting.Builder()
            .name("自动切换")
            .description("在放置之前自动切换到方块。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> renderSwing = sgGeneral.add(new BoolSetting.Builder()
            .name("摆动")
            .description("渲染客户端摆动。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
            .name("旋转")
            .description("旋转到正在放置的方块。")
            .defaultValue(true)
            .build()
    );

    private final Setting<List<Block>> blocks = sgGeneral.add(new BlockListSetting.Builder()
            .name("方块")
            .description("选定的方块。")
            .build()
    );

    private final Setting<SafetyNet.ListMode> blocksFilter = sgGeneral.add(new EnumSetting.Builder<SafetyNet.ListMode>()
            .name("方块过滤器")
            .description("如何使用方块列表设置")
            .defaultValue(SafetyNet.ListMode.Blacklist)
            .build()
    );

    // 渲染
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("形状模式")
            .description("形状的渲染方式。")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("侧面颜色")
            .description("目标方块渲染的侧面颜色。")
            .defaultValue(new SettingColor(146,188,98, 75))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("线条颜色")
            .description("目标方块渲染的线条颜色。")
            .defaultValue(new SettingColor(146,188,98, 255))
            .build()
    );

    private final Pool<SafetyNet.RenderBlock> renderBlockPool = new Pool<>(SafetyNet.RenderBlock::new);
    private final List<SafetyNet.RenderBlock> renderBlocks = new ArrayList<>();

    private final BlockPos.Mutable bp = new BlockPos.Mutable();
    private final BlockPos.Mutable prevBp = new BlockPos.Mutable();

    private boolean lastWasSneaking;
    private double lastSneakingY;
    private double placeRange = 4;

    public SafetyNet() {
        super(NumbyHack.CATEGORY, "safety-net", "在设定的 Y 水平线上，在你的下方放置一个木块。");
    }

    @Override
    public void onActivate() {
        lastWasSneaking = mc.options.sneakKey.isPressed();
        if (lastWasSneaking) lastSneakingY = mc.player.getY();

        for (SafetyNet.RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @Override
    public void onDeactivate() {
        for (SafetyNet.RenderBlock renderBlock : renderBlocks) renderBlockPool.free(renderBlock);
        renderBlocks.clear();
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // Ticking fade animation
        renderBlocks.forEach(SafetyNet.RenderBlock::tick);
        renderBlocks.removeIf(renderBlock -> renderBlock.ticks <= 0);

        if (safetyMode.get() == PlaceMode.AirPlace) {
            Vec3d vec = mc.player.getPos().add(mc.player.getVelocity()).add(0, -0.5f, 0);
            bp.set(vec.getX(), vec.getY(), vec.getZ());

        } else if (safetyMode.get() == PlaceMode.SafetyNet) {
            if (mc.player.getY() < (yLock.get() + safetyNetWindow.get()) - 1) {
                if (mc.world.getBlockState(mc.player.getBlockPos().down()).isAir()) {
                    mc.player.setVelocity(mc.player.getVelocity().getX(), mc.player.getVelocity().getY() * safetyNetMultiplier.get(), mc.player.getVelocity().getZ());
                    Vec3d vec = mc.player.getPos().add(mc.player.getVelocity()).add(0, -0.5f, 0);
                    bp.set(vec.getX(), yLock.get(), vec.getZ());
                } else {
                    info ("Safetynet landed on block | disabling");
                    toggle();
                }
            }

        } else {
            if (BlockUtils.getPlaceSide(mc.player.getBlockPos().down()) != null) {
                bp.set(mc.player.getBlockPos().down());

            } else {
                Vec3d pos = mc.player.getPos();
                pos = pos.add(0, -0.98f, 0);
                pos.add(mc.player.getVelocity());

                if (PlayerUtils.distanceTo(prevBp) > placeRange) {
                    List<BlockPos> blockPosArray = new ArrayList<>();

                    for (int x = (int) (mc.player.getX() - placeRange); x < mc.player.getX() + placeRange; x++) {
                        for (int z = (int) (mc.player.getZ() - placeRange); z < mc.player.getZ() + placeRange; z++) {
                            for (int y = (int) Math.max(mc.world.getBottomY(), mc.player.getY() - placeRange); y < Math.min(mc.world.getTopY(), mc.player.getY() + placeRange); y++) {
                                bp.set(x, y, z);
                                if (!mc.world.getBlockState(bp).isAir()) blockPosArray.add(new BlockPos(bp));
                            }
                        }
                    }
                    if (blockPosArray.size() == 0) {
                        return;
                    }

                    blockPosArray.sort(Comparator.comparingDouble(PlayerUtils::distanceTo));

                    prevBp.set(blockPosArray.get(0));
                }

                Vec3d vecPrevBP = new Vec3d((double) prevBp.getX() + 0.5f,
                        (double) prevBp.getY() + 0.5f,
                        (double) prevBp.getZ() + 0.5f);

                Vec3d sub = pos.subtract(vecPrevBP);
                Direction facing;
                if (sub.getY() < -0.5f) {
                    facing = Direction.DOWN;
                } else if (sub.getY() > 0.5f) {
                    facing = Direction.UP;
                } else facing = Direction.getFacing(sub.getX(), 0, sub.getZ());

                bp.set(prevBp.offset(facing));
            }
        }

        FindItemResult item = InvUtils.findInHotbar(itemStack -> validItem(itemStack, bp));
        if (!item.found()) return;


        if (item.getHand() == null && !autoSwitch.get()) return;

        // Move down if shifting
        if (mc.options.sneakKey.isPressed() && !mc.options.jumpKey.isPressed()) {
            if (lastSneakingY - mc.player.getY() < 0.1) {
                lastWasSneaking = false;
                return;
            }
        } else {
            lastWasSneaking = false;
        }
        if (!lastWasSneaking) lastSneakingY = mc.player.getY();

        if (mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed()) {
            mc.player.setVelocity(0, 0.42f, 0);
        }

        if (BlockUtils.place(bp, item, rotate.get(), 50, renderSwing.get(), true)) {
            // Render block if was placed
            renderBlocks.add(renderBlockPool.get().set(bp));

            // Move player down so they are on top of the placed block ready to jump again
            if (mc.options.jumpKey.isPressed() && !mc.options.sneakKey.isPressed() && !mc.player.isOnGround() && !mc.world.getBlockState(bp).isAir()) {
                mc.player.setVelocity(0, -0.28f, 0);
            }
        }

        if (!mc.world.getBlockState(bp).isAir()) {
            prevBp.set(bp);
        }
    }

    private boolean validItem(ItemStack itemStack, BlockPos pos) {
        if (!(itemStack.getItem() instanceof BlockItem)) return false;

        Block block = ((BlockItem) itemStack.getItem()).getBlock();

        if (blocksFilter.get() == SafetyNet.ListMode.Blacklist && blocks.get().contains(block)) return false;
        else if (blocksFilter.get() == SafetyNet.ListMode.Whitelist && !blocks.get().contains(block)) return false;

        if (!Block.isShapeFullCube(block.getDefaultState().getCollisionShape(mc.world, pos))) return false;
        return !(block instanceof FallingBlock) || !FallingBlock.canFallThrough(mc.world.getBlockState(pos));
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        renderBlocks.sort(Comparator.comparingInt(o -> -o.ticks));
        renderBlocks.forEach(renderBlock -> renderBlock.render(event, sideColor.get(), lineColor.get(), shapeMode.get()));
    }

    // Rendering

    public enum ListMode {
        Whitelist,
        Blacklist
    }

    public enum PlaceMode {
        SafetyNet,
        AirPlace
    }

    public static class RenderBlock {
        public BlockPos.Mutable pos = new BlockPos.Mutable();
        public int ticks;

        public RenderBlock set(BlockPos blockPos) {
            pos.set(blockPos);
            ticks = 8;

            return this;
        }

        public void tick() {
            ticks--;
        }

        public void render(Render3DEvent event, Color sides, Color lines, ShapeMode shapeMode) {
            int preSideA = sides.a;
            int preLineA = lines.a;

            sides.a *= (double) ticks / 8;
            lines.a *= (double) ticks / 8;

            event.renderer.box(pos, sides, lines, shapeMode, 0);

            sides.a = preSideA;
            lines.a = preLineA;
        }
    }
}

