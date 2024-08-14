package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.world.CollisionShapeEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.meteorclient.systems.modules.Module;
import net.minecraft.block.Blocks;
import net.minecraft.block.FireBlock;
import net.minecraft.block.SoulFireBlock;
import net.minecraft.util.shape.VoxelShapes;

/**
 * 由 walaryne 修改自 Tanuki 的原始版本
 */
public class SafeFire extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> fire = sgGeneral.add(new BoolSetting.Builder()
            .name("fire")
            .description("防止您踏入火焰。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> soulFire = sgGeneral.add(new BoolSetting.Builder()
            .name("soul-fire")
            .description("防止您踏入灵魂火焰。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> campfire = sgGeneral.add(new BoolSetting.Builder()
            .name("campfire")
            .description("防止您踏入普通篝火。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> soulCampfire = sgGeneral.add(new BoolSetting.Builder()
            .name("soul-campfire")
            .description("防止您踏入灵魂篝火。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> lava = sgGeneral.add(new BoolSetting.Builder()
            .name("lava")
            .description("防止您踏入熔岩。")
            .defaultValue(false)
            .build()
    );

    public SafeFire() {
        super(NumbyHack.CATEGORY, "safe-fire", "防止您踏入火焰。");
    }

    @EventHandler
    public void onCollisionShape(CollisionShapeEvent event) {
        if (event.state.getBlock() instanceof FireBlock && fire.get()) {
            event.shape = VoxelShapes.fullCube();
        }

        if (event.state.getBlock() instanceof SoulFireBlock && soulFire.get()) {
            event.shape = VoxelShapes.fullCube();
        }

        if (event.state.getBlock() == Blocks.CAMPFIRE && campfire.get()) {
            event.shape = VoxelShapes.fullCube();
        }

        if (event.state.getBlock() == Blocks.SOUL_CAMPFIRE && soulCampfire.get()) {
            event.shape = VoxelShapes.fullCube();
        }

        if (event.state.getBlock() == Blocks.LAVA && lava.get()) {
            event.shape = VoxelShapes.fullCube();
        }
    }
}
