package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;

/**
 * 由 cqb13 制作
 */
public class GameSettings extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> hudHidden = sgGeneral.add(new BoolSetting.Builder()
            .name("隐藏HUD")
            .description("隐藏你的HUD。")
            .defaultValue(mc.options.hudHidden)
            .onChanged(this::toggleHUD)
            .build()
    );

    private final Setting<Boolean> pauseOnLostFocus = sgGeneral.add(new BoolSetting.Builder()
            .name("失去焦点时暂停")
            .description("当游戏失去焦点时暂停。")
            .defaultValue(mc.options.pauseOnLostFocus)
            .onChanged(this::togglePauseOnLostFocus)
            .build()
    );

    private final Setting<Boolean> skipMultiplayerWarning = sgGeneral.add(new BoolSetting.Builder()
            .name("跳过多人游戏警告")
            .description("跳过多人游戏警告。")
            .defaultValue(mc.options.skipMultiplayerWarning)
            .onChanged(this::toggleSkipMultiplayerWarning)
            .build()
    );

    private final Setting<Boolean> smoothCameraEnabled = sgGeneral.add(new BoolSetting.Builder()
            .name("平滑摄像机")
            .description("平滑的摄像机移动。")
            .defaultValue(mc.options.smoothCameraEnabled)
            .onChanged(this::toggleSmoothCamera)
            .build()
    );

    private final Setting<Boolean> advancedTooltips = sgGeneral.add(new BoolSetting.Builder()
            .name("高级提示")
            .description("在你的物品栏中显示更多信息。")
            .defaultValue(mc.options.advancedItemTooltips)
            .onChanged(this::toggleAdvancedTooltips)
            .build()
    );

    private final Setting<Boolean> hideScore = sgGeneral.add(new BoolSetting.Builder()
            .name("隐藏分数")
            .description("死亡时隐藏分数。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> chatFeedback = sgGeneral.add(new BoolSetting.Builder()
            .name("聊天反馈")
            .description("在聊天中发送更新。")
            .defaultValue(true)
            .build()
    );

    public GameSettings() {
        super(NumbyHack.CATEGORY, "game-settings", "便于访问Minecraft的设置并添加一些调整。");
    }

    private void toggleHUD(Boolean b) {
        mc.options.hudHidden = b;
        sendChatInfo("HUD", b ? "隐藏" : "显示");
    }

    private void togglePauseOnLostFocus(Boolean b) {
        mc.options.pauseOnLostFocus = b;
        sendChatInfo("失去焦点时暂停", b ? "启用" : "禁用");
    }

    private void toggleSkipMultiplayerWarning(Boolean b) {
        mc.options.skipMultiplayerWarning = b;
        sendChatInfo("跳过多人游戏警告", b ? "启用" : "禁用");
    }

    private void toggleSmoothCamera(Boolean b) {
        mc.options.smoothCameraEnabled = b;
        sendChatInfo("平滑摄像机", b ? "启用" : "禁用");
    }

    private void toggleAdvancedTooltips(Boolean b) {
        mc.options.advancedItemTooltips = b;
        sendChatInfo("高级提示", b ? "启用" : "禁用");
    }

    public boolean toggleHideScore() {
        sendChatInfo("隐藏分数", hideScore.get() ? "启用" : "禁用");
        return isActive() && hideScore.get();
    }

    private void sendChatInfo(String setting, String value) {
        if (!chatFeedback.get()) return;
        ChatUtils.info("将 %s 设置为 %s。", setting, value);
    }
}
