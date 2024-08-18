package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.combat.Quiver;
import meteordevelopment.meteorclient.systems.modules.player.EXPThrower;
import meteordevelopment.meteorclient.utils.player.Rotations;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;

// 来源于 venomhack

public class FloRida extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Double> speed = sgGeneral.add(new DoubleSetting.Builder()
            .name("旋转速度")
            .description("你旋转的速度。")
            .defaultValue(20)
            .sliderMin(0.0)
            .sliderMax(50.0)
            .build()
    );

    private int count = 0;

    public FloRida() {
        super(NumbyHack.CATEGORY, "flo-rida", "让你旋转起来。");
    }

    @EventHandler
    public void onTick(TickEvent.Post event) {
        Modules modules = Modules.get();
        // 检查是否没有激活 EXPThrower 和 Quiver 模块
        if (!modules.isActive(EXPThrower.class) && !modules.isActive(Quiver.class)) {
            count += speed.get(); // 根据设置的速度增加旋转角度
            if (count > 180) {
                count -= 360; // 保持角度在 -180 到 180 之间
            }

            Rotations.rotate(count, 0.0); // 进行旋转
        }
    }
}
