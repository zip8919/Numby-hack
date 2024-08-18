package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.entity.DamageEvent;
import meteordevelopment.meteorclient.events.game.GameLeftEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ModuleListSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.play.DeathMessageS2CPacket;

import java.util.List;

public class ConditionToggle extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> death = sgGeneral.add(new BoolSetting.Builder()
            .name("死亡切换")
            .description("当你死亡时切换模块。")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Module>> deathOnToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("死亡时启用的模块")
            .description("在死亡时激活的模块。")
            .visible(death::get)
            .build()
    );

    private final Setting<List<Module>> deathOffToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("死亡时禁用的模块")
            .description("在死亡时禁用的模块。")
            .visible(death::get)
            .build()
    );

    private final Setting<Boolean> logout = sgGeneral.add(new BoolSetting.Builder()
            .name("登出切换")
            .description("当你登出时切换模块。")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Module>> logoutOnToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("登出时启用的模块")
            .description("在登出时激活的模块。")
            .visible(logout::get)
            .build()
    );

    private final Setting<List<Module>> logoutOffToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("登出时禁用的模块")
            .description("在登出时禁用的模块。")
            .visible(logout::get)
            .build()
    );

    private final Setting<Boolean> damage = sgGeneral.add(new BoolSetting.Builder()
            .name("伤害切换")
            .description("当你受到伤害时切换模块。")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<Module>> damageOnToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("受到伤害时启用的模块")
            .description("在受到伤害时激活的模块。")
            .visible(damage::get)
            .build()
    );

    private final Setting<List<Module>> damageOffToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("受到伤害时禁用的模块")
            .description("在受到伤害时禁用的模块。")
            .visible(damage::get)
            .build()
    );

    private final Setting<Boolean> player = sgGeneral.add(new BoolSetting.Builder()
            .name("玩家切换")
            .description("当玩家进入你的渲染距离时切换模块。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> ignoreFriends = sgGeneral.add(new BoolSetting.Builder()
            .name("忽略朋友")
            .description("忽略进入你渲染距离的朋友。")
            .defaultValue(true)
            .visible(player::get)
            .build()
    );

    private final Setting<List<Module>> playerOnToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("玩家进入时启用的模块")
            .description("在玩家进入时激活的模块。")
            .visible(player::get)
            .build()
    );

    private final Setting<List<Module>> playerOffToggleModules = sgGeneral.add(new ModuleListSetting.Builder()
            .name("玩家进入时禁用的模块")
            .description("在玩家进入时禁用的模块。")
            .visible(player::get)
            .build()
    );

    public ConditionToggle() {
        super(NumbyHack.CATEGORY, "条件切换", "根据条件切换模块");
    }

    // 死亡切换
    @EventHandler
    private void onPacketReceive(PacketEvent.Receive event)  {
        if (event.packet instanceof DeathMessageS2CPacket packet) {
            Entity entity = mc.world.getEntityById(packet.playerId());
            if (entity == mc.player && death.get()) {
                toggleModules(deathOnToggleModules.get(), deathOffToggleModules.get());
            }
        }
    }

    // 伤害切换
    @EventHandler
    private void onDamage(DamageEvent event) {
        if (event.entity.getUuid() == null) return;
        if (!event.entity.getUuid().equals(mc.player.getUuid())) return;

        if (damage.get()) {
            toggleModules(damageOnToggleModules.get(), damageOffToggleModules.get());
        }
    }

    // 登出切换
    @EventHandler
    private void onGameLeft(GameLeftEvent event) {
        if (logout.get()) {
            toggleModules(logoutOffToggleModules.get(), logoutOnToggleModules.get());
        }
    }

    // 玩家切换
    @EventHandler
    private void onTick(TickEvent.Post event) {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) {
                if (entity.getUuid() != mc.player.getUuid()) {
                    if (!ignoreFriends.get() && entity != mc.player) {
                        if (player.get()) {
                            toggleModules(playerOnToggleModules.get(), playerOffToggleModules.get());
                        }
                    } else if (ignoreFriends.get() && !Friends.get().isFriend((PlayerEntity) entity)) {
                        if (player.get()) {
                            toggleModules(playerOnToggleModules.get(), playerOffToggleModules.get());
                        }
                    }
                }
            }
        }
    }

    private void toggleModules(List<Module> onModules, List<Module> offModules) {
        for (Module module : offModules) {
            if (module.isActive()) {
                module.toggle();
            }
        }
        for (Module module : onModules) {
            if (!module.isActive()) {
                module.toggle();
            }
        }
    }
}
