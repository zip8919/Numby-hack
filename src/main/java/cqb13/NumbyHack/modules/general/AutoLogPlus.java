package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.systems.modules.Modules;
import meteordevelopment.meteorclient.systems.modules.misc.AutoReconnect;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.s2c.common.DisconnectS2CPacket;
import net.minecraft.text.Text;
import net.minecraft.client.network.PlayerListEntry;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * 由 cqb13 制作
 */
public class AutoLogPlus extends Module {
    private final SettingGroup sgTimeLog = settings.createGroup("时间登出");
    private final SettingGroup sgLocationLog = settings.createGroup("位置登出");
    private final SettingGroup sgPingLog = settings.createGroup("延迟登出");
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    // 时间登出
    private final Setting<Boolean> timeLog = sgTimeLog.add(new BoolSetting.Builder()
            .name("时间登出")
            .description("在一定时间后自动登出。")
            .defaultValue(false)
            .build()
    );

    private final Setting<String> logTime = sgTimeLog.add(new StringSetting.Builder()
            .name("时间")
            .description("登出的时间（使用24小时制）。")
            .defaultValue("12:00")
            .visible(timeLog::get)
            .build()
    );

    // 位置登出
    private final Setting<Boolean> locationLog = sgLocationLog.add(new BoolSetting.Builder()
            .name("位置登出")
            .description("到达设定坐标时断开连接。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> oneAxis = sgLocationLog.add(new BoolSetting.Builder()
            .name("单轴登出")
            .description("在特定轴上到达设定坐标时断开连接。")
            .defaultValue(false)
            .visible(locationLog::get)
            .build()
    );

    private final Setting<axisOptions> selectAxis = sgLocationLog.add(new EnumSetting.Builder<axisOptions>()
            .name("选择轴")
            .description("具有确切登出坐标的轴。")
            .defaultValue(axisOptions.X)
            .visible(oneAxis::get)
            .build()
    );

    private final Setting<Dimension> dimension = sgLocationLog.add(new EnumSetting.Builder<Dimension>()
            .name("维度")
            .description("坐标的维度。")
            .defaultValue(Dimension.Nether)
            .visible(locationLog::get)
            .build());

    private final Setting<Integer> xCoords = sgLocationLog.add(new IntSetting.Builder()
            .name("X坐标")
            .description("应该登出的X坐标。")
            .defaultValue(0)
            .range(-2147483648, 2147483647)
            .sliderRange(-2147483648, 2147483647)
            .visible(locationLog::get)
            .build());

    private final Setting<Integer> zCoords = sgLocationLog.add(new IntSetting.Builder()
            .name("Z坐标")
            .description("应该登出的Z坐标。")
            .defaultValue(-1000)
            .range(-2147483648, 2147483647)
            .sliderRange(-2147483648, 2147483647)
            .visible(locationLog::get)
            .build());

    private final Setting<Integer> radius = sgLocationLog.add(new IntSetting.Builder()
            .name("半径")
            .description("从确切坐标到登出坐标的半径。")
            .defaultValue(64)
            .min(0)
            .sliderRange(0, 256)
            .visible(locationLog::get)
            .build());

    // 延迟登出
    private final Setting<Boolean> pingLog = sgPingLog.add(new BoolSetting.Builder()
            .name("延迟登出")
            .description("当你的延迟超过一定值时断开连接。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> pingValue = sgPingLog.add(new IntSetting.Builder()
            .name("延迟值")
            .defaultValue(1000)
            .range(0, 10000)
            .sliderRange(0, 10000)
            .visible(pingLog::get)
            .build()
    );

    // 普通登出
    private final Setting<Boolean> onlyTrusted = sgGeneral.add(new BoolSetting.Builder()
            .name("敌人")
            .description("当渲染距离内出现不在好友列表中的玩家时断开连接。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> toggleAutoReconnect = sgGeneral.add(new BoolSetting.Builder()
            .name("切换自动重连")
            .description("断开连接时关闭自动重连。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> toggleOff = sgGeneral.add(new BoolSetting.Builder()
            .name("切换关闭")
            .description("使用后禁用时间登出。")
            .defaultValue(true)
            .build()
    );

    public AutoLogPlus() {
        super(NumbyHack.CATEGORY, "auto-log+", "当达到特定条件时断开连接。");
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc == null || mc.world == null || mc.player == null) return;

        playerLog();
        disconnectOnHighPing();
        timeLog();
        locationLog();
    }

    // 不良玩家登出
    private void playerLog() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity && entity.getUuid() != mc.player.getUuid()) {
                if (onlyTrusted.get() && entity != mc.player && !Friends.get().isFriend((PlayerEntity) entity)) {
                    disconnect(Text.of("[Auto Log+] 一名不受信任的玩家 [" + entity.getName().getString() + "] 已进入你的渲染距离。"));
                }
            }
        }
    }

    // 时间登出
    private void timeLog() {
        if (timeLog.get()) {
            DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm");
            LocalDateTime now = LocalDateTime.now();
            if (dtf.format(now).equals(logTime.get())) {
                disconnect(Text.of("[Auto Log+] 登出时间已到 " + logTime.get() + "。"));
            }
        }
    }

    // 位置登出
    private void locationLog() {
        if (locationLog.get() && PlayerUtils.getDimension() == dimension.get()) {
            if (xCoordsMatch() && zCoordsMatch()) {
                disconnect(Text.of("[Auto Log+] 你已到达目的地。"));
            } else if (oneAxis.get()) {
                if (selectAxis.get() == axisOptions.X && xCoordsMatch()) {
                    disconnect(Text.of("[Auto Log+] 你已到达目的地。"));
                } else if (selectAxis.get() == axisOptions.Z && zCoordsMatch()) {
                    disconnect(Text.of("[Auto Log+] 你已到达目的地。"));
                }
            }
        }
    }

    // 延迟登出
    private void disconnectOnHighPing() {
        if (!pingLog.get()) return;
        if (mc.getNetworkHandler() == null || mc.player == null) return;
        PlayerListEntry playerListEntry = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid());

        int ping = playerListEntry.getLatency();

        if (ping >= pingValue.get()) disconnect(Text.of("[Auto Log+] 高延迟 [" + ping + "]"));
    }

    private boolean xCoordsMatch() {
        return (mc.player.getX() <= xCoords.get() + radius.get() && mc.player.getX() >= xCoords.get() - radius.get());
    }

    private boolean zCoordsMatch() {
        return (mc.player.getZ() <= zCoords.get() + radius.get() && mc.player.getZ() >= zCoords.get() - radius.get());
    }

    private void disconnect(Text text) {
        if (mc.getNetworkHandler() == null) return;
        mc.player.networkHandler.onDisconnect(new DisconnectS2CPacket(text));

        if (toggleOff.get()) toggle();

        if (toggleAutoReconnect.get() && Modules.get().isActive(AutoReconnect.class))
            Modules.get().get(AutoReconnect.class).toggle();
    }

    public enum axisOptions {
        X,
        Z,
    }
}
