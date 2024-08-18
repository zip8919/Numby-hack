package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.settings.StringListSetting;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.Text;

import java.util.List;

public class IgnoreDeaths extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<String>> names = sgGeneral.add(new StringListSetting.Builder()
            .name("player names")
            .description("要隐藏死亡消息的玩家名称。")
            .defaultValue(List.of())
            .build()
    );

    private final Setting<Boolean> mustContainWords = sgGeneral.add(new BoolSetting.Builder()
            .name("必须包含单词")
            .description("仅在消息包含指定单词和玩家名称时才会忽略该消息。")
            .defaultValue(false)
            .build()
    );

    private final Setting<List<String>> blockedWords = sgGeneral.add(new StringListSetting.Builder()
            .name("被阻止的单词")
            .description("将导致消息被阻止的单词列表。")
            .defaultValue(List.of())
            .visible(mustContainWords::get)
            .build()
    );

    public IgnoreDeaths() {
        super(NumbyHack.CATEGORY, "ignore-deaths", "从聊天中移除包含死亡玩家名称的消息。");
    }

    @EventHandler
    private void onMessageReceive(ReceiveMessageEvent event) {
        Text message = event.getMessage();

        if (message == null) return;

        message = Text.of(message.getString().toLowerCase());

        if (mustContainWords.get()) {
            for (String name : names.get()) {
                for (String word : blockedWords.get()) {
                    if (message.getString().contains(name) && message.getString().contains(word.toLowerCase())) {
                        event.cancel();
                    }
                }
            }
        } else {
            for (String name : names.get()) {
                if (message.getString().contains(name)) {
                    event.cancel();
                }
            }
        }

        event.setMessage(message);
    }
}
