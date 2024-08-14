package cqb13.NumbyHack.modules.hud;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.utils.entity.EntityUtils;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.entity.player.PlayerEntity;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

import static meteordevelopment.meteorclient.MeteorClient.mc;

public class TextRadarHud extends HudElement {
    public static final HudElementInfo<TextRadarHud> INFO = new HudElementInfo<>(NumbyHack.HUD_GROUP, "text-radar", "在你的视觉范围内显示玩家及其统计数据。", TextRadarHud::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgScale = settings.createGroup("缩放");
    private final SettingGroup sgBackground = settings.createGroup("背景");

    // 通用
    private final Setting<Integer> limit = sgGeneral.add(new IntSetting.Builder()
            .name("limit")
            .description("显示的最大玩家数量。")
            .defaultValue(10)
            .min(1)
            .sliderRange(1, 20)
            .build()
    );

    private final Setting<Boolean> health = sgGeneral.add(new BoolSetting.Builder()
            .name("health")
            .description("在玩家名称旁边显示其健康值。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> ping = sgGeneral.add(new BoolSetting.Builder()
            .name("ping")
            .description("在玩家名称旁边显示其ping值。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> distance = sgGeneral.add(new BoolSetting.Builder()
            .name("distance")
            .description("在玩家名称旁边显示其距离。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> friends = sgGeneral.add(new BoolSetting.Builder()
            .name("display-friends")
            .description("是否显示朋友。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> shadow = sgGeneral.add(new BoolSetting.Builder()
            .name("shadow")
            .description("在文本后面渲染阴影。")
            .defaultValue(true)
            .build()
    );

    private final Setting<SettingColor> primaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("primary-color")
            .description("主颜色。")
            .defaultValue(new SettingColor(146, 188, 98))
            .build()
    );

    private final Setting<SettingColor> secondaryColor = sgGeneral.add(new ColorSetting.Builder()
            .name("secondary-color")
            .description("辅助颜色。")
            .defaultValue(new SettingColor(175, 175, 175))
            .build()
    );

    private final Setting<Alignment> alignment = sgGeneral.add(new EnumSetting.Builder<Alignment>()
            .name("alignment")
            .description("水平对齐方式。")
            .defaultValue(Alignment.Auto)
            .build()
    );

    private final Setting<Integer> border = sgGeneral.add(new IntSetting.Builder()
            .name("border")
            .description("在元素周围添加的空间。")
            .defaultValue(0)
            .build()
    );

    // 缩放
    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
            .name("custom-scale")
            .description("应用自定义文本缩放而不是全局缩放。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
            .name("scale")
            .description("自定义缩放。")
            .visible(customScale::get)
            .defaultValue(1)
            .min(0.5)
            .sliderRange(0.5, 3)
            .build()
    );

    // 背景
    private final Setting<Boolean> background = sgBackground.add(new BoolSetting.Builder()
            .name("background")
            .description("显示背景。")
            .defaultValue(false)
            .build()
    );

    private final Setting<SettingColor> backgroundColor = sgBackground.add(new ColorSetting.Builder()
            .name("background-color")
            .description("用于背景的颜色。")
            .visible(background::get)
            .defaultValue(new SettingColor(25, 25, 25, 50))
            .build()
    );

    private final List<AbstractClientPlayerEntity> players = new ArrayList<>();

    public TextRadarHud() {
        super(INFO);
    }

    private final Color RED = new Color(255, 25, 25);
    private final Color AMBER = new Color(255, 105, 25);
    private final Color GREEN = new Color(25, 252, 25);
    private final Color BLUE = new Color(20, 170, 170);

    @Override
    public void setSize(double width, double height) {
        super.setSize(width + border.get() * 2, height + border.get() * 2);
    }

    @Override
    protected double alignX(double width, Alignment alignment) {
        return box.alignX(getWidth() - border.get() * 2, width, alignment);
    }

    @Override
    public void tick(HudRenderer renderer) {
        double width = renderer.textWidth("Players:", shadow.get(), getScale());
        double height = renderer.textHeight(shadow.get(), getScale());

        if (mc.world == null) {
            setSize(width, height);
            return;
        }

        for (PlayerEntity entity : getPlayers()) {
            if (entity.equals(mc.player)) continue;
            if (!friends.get() && Friends.get().isFriend(entity)) continue;

            String text = entity.getName().getString();
            if (health.get()) text += " " + (int) entity.getHealth() + entity.getAbsorptionAmount();
            if (ping.get()) text += String.format("(%sms)", EntityUtils.getPing(entity));
            if (distance.get()) text += String.format("(%sm)", Math.round(Objects.requireNonNull(mc.getCameraEntity()).distanceTo(entity)));

            width = Math.max(width, renderer.textWidth(text, shadow.get(), getScale()));
            height += renderer.textHeight(shadow.get(), getScale()) + 2;
        }

        setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        double y = this.y + border.get();

        if (background.get()) {
            renderer.quad(this.x, this.y, getWidth(), getHeight(), backgroundColor.get());
        }

        renderer.text("Players:", x + border.get() + alignX(renderer.textWidth("Players:", shadow.get(), getScale()), alignment.get()), y, secondaryColor.get(), shadow.get(), getScale());

        if (mc.world == null) return;

        for (PlayerEntity entity : getPlayers()) {
            if (entity.equals(mc.player)) continue;
            if (!friends.get() && Friends.get().isFriend(entity)) continue;

            String text = entity.getName().getString();
            Color color = PlayerUtils.getPlayerColor(entity, primaryColor.get());

            double width = renderer.textWidth(text, shadow.get(), getScale());
            double x = this.x + border.get() + alignX(width, alignment.get());
            y += renderer.textHeight(shadow.get(), getScale()) + 2;

            renderer.text(text, x, y, color, shadow.get(), getScale());

            if (health.get() || ping.get() || distance.get()) {
                x += renderer.textWidth(text + " ");

                text = "-";
                color = secondaryColor.get();

                renderer.text(text, x, y, color, shadow.get(), getScale());
            }

            if (health.get()) {
                x += renderer.textWidth(text + " ");

                text = String.format("%s", Math.round(entity.getHealth() + entity.getAbsorptionAmount()));
                double healthPercentage = Math.round(entity.getHealth() + entity.getAbsorptionAmount()) / (entity.getMaxHealth() + entity.getAbsorptionAmount());
                if (healthPercentage <= 0.333) color = RED;
                else if (healthPercentage <= 0.666) color = AMBER;
                else color = GREEN;

                renderer.text(text, x, y, color, shadow.get(), getScale());
            }

            if (ping.get()) {
                x += renderer.textWidth(text + " ");

                text = String.format("[%sms]", Math.round(EntityUtils.getPing(entity)));
                color = BLUE;

                renderer.text(text, x, y, color, shadow.get(), getScale());
            }

            if (distance.get()) {
                x += renderer.textWidth(text + " ");

                text = String.format("(%sm)", Math.round(Objects.requireNonNull(mc.getCameraEntity()).distanceTo(entity)));
                color = secondaryColor.get();

                renderer.text(text, x, y, color, shadow.get(), getScale());
            }
        }
    }

    private List<AbstractClientPlayerEntity> getPlayers() {
        assert mc.world != null;
        players.clear();
        players.addAll(mc.world.getPlayers());
        if (players.size() > limit.get()) players.subList(limit.get() - 1, players.size() - 1).clear();
        players.sort(Comparator.comparingDouble(e -> e.squaredDistanceTo(mc.getCameraEntity())));

        return players;
    }

    private double getScale() {
        return customScale.get() ? scale.get() : -1;
    }
}
