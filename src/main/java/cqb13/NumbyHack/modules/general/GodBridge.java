package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Direction;

import java.util.Arrays;

public class GodBridge extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    final Direction[] allowedSides = new Direction[] { Direction.NORTH, Direction.EAST, Direction.SOUTH, Direction.WEST };

    public GodBridge() {
        super(NumbyHack.CATEGORY, "god-bridge", "让你处于正确的位置以进行神桥建造。");
    }

    @Override
    public void onActivate() {
        // 模块激活时的逻辑
    }

    @Override
    public void onDeactivate() {
        // 模块停用时的逻辑
    }

    // TODO: 使其工作
    @EventHandler
    private void onTick(TickEvent.Pre event) {
        mc.player.setYaw(mc.player.getMovementDirection().asRotation());
        if(mc.player.getPitch() > 83 || mc.player.getPitch() < 81) {
            mc.player.setPitch(82.5f);
        }
        HitResult hr = mc.crosshairTarget;
        if (hr.getType() == HitResult.Type.BLOCK && hr instanceof BlockHitResult result) {
            if(Arrays.stream(allowedSides).anyMatch(direction -> direction == result.getSide())) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
    }
}
