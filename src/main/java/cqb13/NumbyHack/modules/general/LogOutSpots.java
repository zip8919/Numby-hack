package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import cqb13.NumbyHack.utils.TimerUtils;
import meteordevelopment.meteorclient.events.entity.EntityAddedEvent;
import meteordevelopment.meteorclient.events.render.Render2DEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.Renderer2D;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.renderer.text.TextRenderer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.PlayerUtils;
import meteordevelopment.meteorclient.utils.render.NametagUtils;
import meteordevelopment.meteorclient.utils.render.WireframeEntityRenderer;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.Dimension;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import org.joml.Vector3d;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class LogOutSpots extends Module {

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    // 通用设置
    private final Setting<Boolean> nameRender = sgGeneral.add(new BoolSetting.Builder()
            .name("name")
            .description("显示玩家的名字。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> healthRender = sgGeneral.add(new BoolSetting.Builder()
            .name("health")
            .description("显示玩家的生命值。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Boolean> coordRender = sgGeneral.add(new BoolSetting.Builder()
            .name("coordinates")
            .description("显示玩家的坐标。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Boolean> armorCheck = sgGeneral.add(new BoolSetting.Builder()
            .name("armor-check")
            .description("检查玩家是否穿戴盔甲。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Double> scale = sgGeneral.add(new DoubleSetting.Builder()
            .name("scale")
            .description("文本的缩放比例。")
            .defaultValue(1)
            .min(0.2)
            .sliderRange(0.2, 2)
            .build()
    );

    private final Setting<Boolean> notification = sgGeneral.add(new BoolSetting.Builder()
            .name("notification")
            .description("当玩家下线时通知你。")
            .defaultValue(true)
            .build()
    );

    // 渲染设置
    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
            .name("shape-mode")
            .description("形状类型。")
            .defaultValue(ShapeMode.Both)
            .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
            .name("side-color")
            .description("侧面颜色。")
            .defaultValue(new SettingColor(146, 188, 98, 10))
            .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
            .name("line-color")
            .description("线条颜色。")
            .defaultValue(new SettingColor(146, 188, 98, 255))
            .build()
    );

    private final Setting<SettingColor> nameColor = sgRender.add(new ColorSetting.Builder()
            .name("name-color")
            .description("名字颜色。")
            .defaultValue(new SettingColor(255, 255, 255))
            .build()
    );

    private final Setting<SettingColor> nameBackgroundColor = sgRender.add(new ColorSetting.Builder()
            .name("name-background-color")
            .description("名字背景颜色。")
            .defaultValue(new SettingColor(0, 0, 0, 75))
            .build()
    );

    private final List<Entry> players = new ArrayList<>();

    private final List<PlayerListEntry> lastPlayerList = new ArrayList<>();
    private final List<PlayerEntity> lastPlayers = new ArrayList<>();

    private int timer;
    private Dimension lastDimension;

    public LogOutSpots() {
        super(NumbyHack.CATEGORY, "log-spots-+", "显示其他玩家下线时的位置框。");
        lineColor.onChanged();
    }

    @Override
    public void onActivate() {
        lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
        updateLastPlayers();

        timer = 10;
        lastDimension = PlayerUtils.getDimension();
    }

    @Override
    public void onDeactivate() {
        players.clear();
        lastPlayerList.clear();
    }

    private void updateLastPlayers() {
        lastPlayers.clear();
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof PlayerEntity) lastPlayers.add((PlayerEntity) entity);
        }
    }

    @EventHandler
    private void onEntityAdded(EntityAddedEvent event) {
        if (event.entity instanceof PlayerEntity) {
            int toRemove = -1;

            for (int i = 0; i < players.size(); i++) {
                if (players.get(i).uuid.equals(event.entity.getUuid())) {
                    toRemove = i;
                    break;
                }
            }

            if (toRemove != -1) {
                players.remove(toRemove);
            }
        }
    }

    @EventHandler
    private void onTick(TickEvent.Post event) {
        if (mc.getNetworkHandler().getPlayerList().size() != lastPlayerList.size()) {
            for (PlayerListEntry entry : lastPlayerList) {
                if (mc.getNetworkHandler().getPlayerList().stream().anyMatch(playerListEntry -> playerListEntry.getProfile().equals(entry.getProfile()))) continue;

                for (PlayerEntity player : lastPlayers) {
                    if (player.getUuid().equals(entry.getProfile().getId())) {
                        if (armorCheck.get()) {
                            for (int position = 3; position >= 0; position--) {
                                ItemStack itemStack = getItem(position, player);

                                if (itemStack.isEmpty()) return;
                            }
                        }
                        add(new Entry(player));
                    }
                }
            }

            lastPlayerList.clear();
            lastPlayerList.addAll(mc.getNetworkHandler().getPlayerList());
            updateLastPlayers();
        }

        if (timer <= 0) {
            updateLastPlayers();
            timer = 10;
        } else {
            timer--;
        }

        Dimension dimension = PlayerUtils.getDimension();
        if (dimension != lastDimension) players.clear();
        lastDimension = dimension;
    }

    private void add(Entry entry) {
        players.removeIf(player -> player.uuid.equals(entry.uuid));
        players.add(entry);
    }

    private ItemStack getItem(int i, PlayerEntity playerEntity) {
        if (playerEntity == null) return ItemStack.EMPTY;

        return switch (i) {
            case 4 -> playerEntity.getOffHandStack();
            case 5 -> playerEntity.getMainHandStack();
            default -> playerEntity.getInventory().getArmorStack(i);
        };
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        for (Entry player : players) player.render3D(event);
    }

    @EventHandler
    private void onRender2D(Render2DEvent event) {
        for (Entry player : players) player.render2D();
    }

    private static final Vector3d pos = new Vector3d();

    private class Entry {
        public final double x, y, z;
        public final double xWidth, zWidth, halfWidth, height;

        public final TimerUtils passed = new TimerUtils();

        public final UUID uuid;
        public final String name;
        public final int health, maxHealth;
        public final String healthText;
        PlayerEntity entity;

        public Entry(PlayerEntity entity) {
            passed.reset();
            halfWidth = entity.getWidth() / 2;
            x = entity.getX() - halfWidth;
            y = entity.getY();
            z = entity.getZ() - halfWidth;

            xWidth = entity.getBoundingBox().getLengthX();
            zWidth = entity.getBoundingBox().getLengthZ();
            height = entity.getBoundingBox().getLengthY();

            this.entity = entity;

            uuid = entity.getUuid();
            name = entity.getName().getString();
            health = Math.round(entity.getHealth() + entity.getAbsorptionAmount());
            maxHealth = Math.round(entity.getMaxHealth() + entity.getAbsorptionAmount());

            healthText = " " + health;
        }

        public void render3D(Render3DEvent event) {
            WireframeEntityRenderer.render(event, entity, scale.get(), sideColor.get(), lineColor.get(), shapeMode.get());
        }

        public void render2D() {
            if (PlayerUtils.distanceToCamera(x, y, z) > mc.options.getViewDistance().getValue() * 16) return;

            TextRenderer text = TextRenderer.get();
            double s = scale.get();
            pos.set(x + halfWidth, y + height + 0.5, z + halfWidth);

            if (!NametagUtils.to2D(pos, s)) return;

            NametagUtils.begin(pos);

            String content = "";
            if (nameRender.get()) content = content + name;
            if (healthRender.get()) content = content + " " + healthText + "HP";
            if (coordRender.get()) content = content + " (" + Math.round(entity.getX()) + " " + Math.round(entity.getY()) + " " + Math.round(entity.getZ()) + ")";

            // 渲染背景
            double i = text.getWidth(content) / 2;
            Renderer2D.COLOR.begin();
            Renderer2D.COLOR.quad(-i, 0, i * 2, text.getHeight(), nameBackgroundColor.get());
            Renderer2D.COLOR.render(null);

            // 渲染名字和生命值文本
            text.beginBig();
            if (nameRender.get()) text.render(content, -i, 0, nameColor.get());
            text.end();

            NametagUtils.end();
        }
    }
}
