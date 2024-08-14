package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.color.Color;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.passive.AbstractHorseEntity;
import net.minecraft.entity.passive.LlamaEntity;
import net.minecraft.util.math.MathHelper;
import org.joml.Vector3d;

import java.util.Objects;

// https://github.com/Declipsonator/Meteor-Tweaks/blob/main/src/main/java/me/declipsonator/meteortweaks/modules/RideStats.java

public class RideStats extends Module {
    private final Vector3d pos = new Vector3d();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgEntities = settings.createGroup("Entities");

    // 实体设置

    private final Setting<Boolean> horse = sgEntities.add(new BoolSetting.Builder()
            .name("horses")
            .description("在马的头上显示统计信息。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> donkey = sgEntities.add(new BoolSetting.Builder()
            .name("donkeys")
            .description("在驴的头上显示统计信息。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> mule = sgEntities.add(new BoolSetting.Builder()
            .name("mules")
            .description("在骡子的头上显示统计信息。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> llama = sgEntities.add(new BoolSetting.Builder()
            .name("llamas")
            .description("在羊驼的头上显示统计信息。")
            .defaultValue(true)
            .build()
    );

    // 一般设置

    private final Setting<Boolean> displaySpeed = sgGeneral.add(new BoolSetting.Builder()
            .name("display-max-speed")
            .description("显示实体的最大速度。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayJumpHeight = sgGeneral.add(new BoolSetting.Builder()
            .name("display-max-jump-height")
            .description("显示实体的最大跳跃高度。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayHealth = sgGeneral.add(new BoolSetting.Builder()
            .name("display-max-health")
            .description("显示实体的最大生命值。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> displayInventorySlots = sgGeneral.add(new BoolSetting.Builder()
            .name("display-llama-slots")
            .description("显示羊驼的库存槽位。")
            .defaultValue(true)
            .visible(llama::get)
            .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("名牌的缩放比例。")
            .defaultValue(1.5)
            .min(0.1)
            .build()
    );

    private final Setting<Double> height = sgGeneral.add(new DoubleSetting.Builder()
            .name("height")
            .description("名牌显示在实体头上方的高度。")
            .defaultValue(1)
            .sliderMax(3)
            .build()
    );

    private final Setting<SettingColor> entityNameColor = sgGeneral.add(new ColorSetting.Builder()
            .name("name-color")
            .description("实体名称的颜色。")
            .defaultValue(new SettingColor())
            .build()
    );

    private final Setting<SettingColor> backgroundColor = sgGeneral.add(new ColorSetting.Builder()
            .name("background-color")
            .description("名牌背景的颜色。")
            .defaultValue(new SettingColor(0, 0, 0, 75))
            .build()
    );

    public RideStats() {
        super(NumbyHack.CATEGORY, "ride-stats", "在可骑乘实体的头上显示信息。");
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entity entity: Objects.requireNonNull(mc.world).getEntities()) {
            boolean horse = entity.getType() == EntityType.HORSE && this.horse.get();
            boolean mule = entity.getType() == EntityType.MULE && this.mule.get();
            boolean donkey = entity.getType() == EntityType.DONKEY && this.donkey.get();
            boolean llama = entity.getType() == EntityType.LLAMA && this.llama.get();
            if (horse || mule || donkey || llama) {
                pos.set(new double[]{
                        MathHelper.lerp(event.tickDelta, entity.lastRenderX, entity.getX()),
                        MathHelper.lerp(event.tickDelta, entity.lastRenderY, entity.getY()),
                        MathHelper.lerp(event.tickDelta, entity.lastRenderZ, entity.getZ())
                });
                pos.add(0, entity.getEyeHeight(entity.getPose()) + 0.75, 0);
                pos.add(0, -1 + height.get(), 0);
                if (NametagUtils.to2D(pos, scale.get())) renderHorseNametag((AbstractHorseEntity) entity, entity);
            }
        }
    }

    private void renderHorseNametag(AbstractHorseEntity horseEntity, Entity entity) {
        boolean llama = entity.getType() == EntityType.LLAMA;
        TextRenderer text = TextRenderer.get();
        NametagUtils.begin(pos);
        text.beginBig();

        // 名称
        String name;
        name = horseEntity.getType().getName().getString();

        // 生命值
        double health = horseEntity.getMaxHealth();
        String healthText = " " + String.format("%.1f", health).replace(".", ",");

        // 速度
        double speed = genericSpeedToBlockPerSecond(horseEntity.getAttributes().getBaseValue(EntityAttributes.GENERIC_MOVEMENT_SPEED));
        String speedText = " " + String.format("%.1f", speed).replace(".", ",") + " bps";

        // 跳跃
        double maxJump = jumpStrengthToJumpHeight(horseEntity.getAttributes().getBaseValue(EntityAttributes.GENERIC_JUMP_STRENGTH));
        String maxJumpText = " " + String.format("%.1f", maxJump).replace(".", ",") + "m";

        // 库存槽位
        int invSlots = 0;
        if (llama) invSlots = ((LlamaEntity) entity).getInventoryColumns() * 3;
        String invSlotsText = " " + invSlots + " slots";

        // 宽度计算
        double nameWidth = text.getWidth(name, true);
        double healthWidth = text.getWidth(healthText, true);
        double speedWidth = text.getWidth(speedText, true);
        double jumpWidth = text.getWidth(maxJumpText, true);
        double invSlotsWidth = text.getWidth(invSlotsText, true);
        double width = nameWidth;

        if (displayHealth.get()) width += healthWidth;
        if (displaySpeed.get()) width += speedWidth;
        if (displayJumpHeight.get()) width += jumpWidth;
        if (displayInventorySlots.get() && llama) width += invSlotsWidth;

        double widthHalf = width / 2;
        double heightDown = text.getHeight(true);

        drawBg(-widthHalf, -heightDown, width, heightDown);

        // 渲染文本
        double hX = -widthHalf;
        double hY = -heightDown;

        hX = text.render(name, hX, hY, entityNameColor.get(), true);

        if (displayHealth.get()) hX = text.render(healthText, hX, hY, Color.GREEN, true);
        if (displaySpeed.get()) hX = text.render(speedText, hX, hY, Color.BLUE, true);
        if (displayJumpHeight.get() && !llama) text.render(maxJumpText, hX, hY, Color.GRAY, true);
        else if (displayJumpHeight.get() && llama) hX = text.render(maxJumpText, hX, hY, Color.GRAY, true);
        if (displayInventorySlots.get() && llama) text.render(invSlotsText, hX, hY, Color.YELLOW, true);

        text.end();
        NametagUtils.end();
    }

    public static double jumpStrengthToJumpHeight(double strength) {
        return - 0.1817584952 * Math.pow(strength, 3) + 3.689713992 * Math.pow(strength, 2) + 2.128599134 * strength - 0.343930367;
    }

    public static double genericSpeedToBlockPerSecond(double speed) {
        return 0.132 * speed * speed + 42.119 * speed;
    }

    private void drawBg(double x, double y, double width, double height) {
        Renderer2D.COLOR.begin();
        Renderer2D.COLOR.quad(x - 1, y - 1, width + 2, height + 2, backgroundColor.get());
        Renderer2D.COLOR.render(null);
    }
}
