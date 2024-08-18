package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import cqb13.NumbyHack.utils.CHMainUtils;
import cqb13.NumbyHack.events.SendRawMessageEvent;
import meteordevelopment.meteorclient.events.game.ReceiveMessageEvent;
import meteordevelopment.meteorclient.events.game.SendMessageEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.Text;
import net.minecraft.text.TextColor;

public class ChatEncryption extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<String> key = sgGeneral.add(new StringSetting.Builder()
            .name("密钥")
            .description("用于加密和解密消息的密钥。")
            .defaultValue("81")
            .build()
    );

    private final Setting<String> secretKey = sgGeneral.add(new StringSetting.Builder()
            .name("秘密密钥")
            .description("确保安全的辅助密钥。")
            .defaultValue("🔑")
            .build()
    );

    private final Setting<String> encryptionPrefix = sgGeneral.add(new StringSetting.Builder()
            .name("加密前缀")
            .description("用于加密消息的前缀。")
            .defaultValue("!enc:")
            .build()
    );

    private final Setting<String> decryptionPrefix = sgGeneral.add(new StringSetting.Builder()
            .name("解密前缀")
            .description("用于解密消息的前缀。")
            .defaultValue("!dnc:")
            .build()
    );

    private final Setting<Boolean> alwaysEncrypt = sgGeneral.add(new BoolSetting.Builder()
            .name("始终加密")
            .description("自动始终加密您的消息。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Integer> wrapLength = sgGeneral.add(new IntSetting.Builder()
            .name("换行长度")
            .description("每行可以容纳多少个字符。")
            .defaultValue(50)
            .sliderMin(40)
            .sliderMax(75)
            .min(1)
            .max(256)
            .noSlider()
            .build()
    );

    public final Setting<SettingColor> feedbackColor = sgGeneral.add(new ColorSetting.Builder()
            .name("反馈颜色")
            .description("反馈文本的颜色。")
            .defaultValue(new SettingColor(146,188,98))
            .build());

    public ChatEncryption() {
        super(NumbyHack.CATEGORY, "chat-encryption", "加密您的聊天消息，使其对其他人不可读。");
    }

    @EventHandler
    private void onSendMessage(SendMessageEvent event) {
        event.message = encrypt(alwaysEncrypt.get() ? encryptionPrefix.get() + event.message + ";" : event.message);
    }

    @EventHandler
    private void onSendRawMessage(SendRawMessageEvent event) {
        event.message = encrypt(event.message);
    }

    private String encrypt(String string) {
        if (string.contains(encryptionPrefix.get()) && string.contains(";") && string.indexOf(encryptionPrefix.get()) < string.indexOf(";", string.indexOf(encryptionPrefix.get()))) {
            try {
                int index;

                while ((index = string.indexOf(encryptionPrefix.get())) != -1) {
                    String toEncrypt = string.substring(index + encryptionPrefix.get().length(), string.indexOf(";", index));
                    String encrypted;

                    try {
                        encrypted = CHMainUtils.encrypt(toEncrypt.concat(secretKey.get()), key.get());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        warning("加密失败: " + exception + " 异常。");

                        return string;
                    }

                    string = string.substring(0, index).concat(string.substring(index).replaceFirst(encryptionPrefix.get() + toEncrypt + ";", decryptionPrefix.get() + encrypted + ";"));
                }

                if (string.length() > 256) {
                    warning("加密后的字符串超过256个字符。无法加密。");

                    return string;
                }

                return string;
            } catch (Exception exception) {
                exception.printStackTrace();
                warning("加密失败: 无效格式导致的 " + exception + " 异常。");

                return string;
            }
        }

        return string;
    }

    @EventHandler
    public void onReceiveMessage(ReceiveMessageEvent event) {
        StringBuilder builder = new StringBuilder();

        event.getMessage().asOrderedText().accept((i, style, codePoint) -> {
            builder.append(new String(Character.toChars(codePoint)));
            return true;
        });

        String string = builder.toString();
        String original = builder.toString();

        if (string.contains(decryptionPrefix.get()) && string.contains(";") && string.indexOf(decryptionPrefix.get()) < string.indexOf(";", string.indexOf(decryptionPrefix.get()))) {
            try {
                int index;

                while ((index = string.indexOf(decryptionPrefix.get())) != -1) {
                    String toDecrypt = string.substring(string.indexOf(decryptionPrefix.get(), index) + decryptionPrefix.get().length(), string.indexOf(";", index));
                    String decrypted;

                    try {
                        decrypted = CHMainUtils.decrypt(toDecrypt, key.get());

                        if (!decrypted.endsWith(secretKey.get())) return;

                        decrypted = decrypted.substring(0, decrypted.length() - secretKey.get().length());
                    } catch (Exception exception) {
                        exception.printStackTrace();
                        warning("解密失败: " + exception + " 异常。");

                        return;
                    }

                    string = string.substring(0, index).concat(string.substring(index).replace(decryptionPrefix.get() + toDecrypt + ";", decrypted));
                }

                StringBuilder wrap = new StringBuilder();

                int i = 0;

                if (!original.isEmpty()) {
                    for (char c : original.toCharArray()) {
                        i++;

                        if (i > wrapLength.get()) {
                            wrap.append("\\n");
                            i = 0;
                        }

                        wrap.append(c);
                    }
                }

                event.setMessage(Text.literal(string + " ").append(Text.literal("[加密]")
                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(feedbackColor.get().getPacked()))
                                .withHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT, Text.literal(wrap.toString())
                                        .setStyle(Style.EMPTY.withColor(TextColor.fromRgb(feedbackColor.get().getPacked())))
                                ))
                        )
                ));
            } catch (Exception exception) {
                exception.printStackTrace();
                warning("解密失败: 无效格式导致的 " + exception + " 异常");
            }
        }
    }
}
