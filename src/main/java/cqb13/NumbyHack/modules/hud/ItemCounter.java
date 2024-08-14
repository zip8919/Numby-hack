package cqb13.NumbyHack.modules.hud;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.hud.Alignment;
import meteordevelopment.meteorclient.systems.hud.HudElement;
import meteordevelopment.meteorclient.systems.hud.HudElementInfo;
import meteordevelopment.meteorclient.systems.hud.HudRenderer;
import meteordevelopment.meteorclient.systems.hud.elements.TextHud;
import meteordevelopment.meteorclient.utils.Utils;
import meteordevelopment.meteorclient.utils.misc.Names;
import meteordevelopment.meteorclient.utils.player.InvUtils;
import net.minecraft.item.*;

import java.util.*;

public class ItemCounter extends HudElement {
    public static final HudElementInfo<ItemCounter> INFO = new HudElementInfo<>(NumbyHack.HUD_GROUP, "item-counter", "在文本中统计不同的物品。", ItemCounter::new);

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("渲染");
    private final SettingGroup sgScale = settings.createGroup("缩放");

    public enum SortMode {
        最长,
        最短
    }

    // 通用
    private final Setting<SortMode> sortMode = sgGeneral.add(new EnumSetting.Builder<SortMode>()
            .name("排序模式")
            .description("如何排序物品列表。")
            .defaultValue(SortMode.最短)
            .build()
    );

    private final Setting<List<Item>> items = sgGeneral.add(new ItemListSetting.Builder()
            .name("物品")
            .description("在计数器列表中显示哪些物品。")
            .defaultValue(new ArrayList<>(0))
            .build()
    );

    // 渲染
    private final Setting<Boolean> shadow = sgRender.add(new BoolSetting.Builder()
            .name("阴影")
            .description("在文本后面渲染阴影。")
            .defaultValue(true)
            .build()
    );

    private final Setting<Alignment> alignment = sgRender.add(new EnumSetting.Builder<Alignment>()
            .name("对齐")
            .description("水平对齐。")
            .defaultValue(Alignment.Auto)
            .build()
    );

    // 缩放
    private final Setting<Boolean> customScale = sgScale.add(new BoolSetting.Builder()
            .name("自定义缩放")
            .description("应用自定义文本缩放而不是全局缩放。")
            .defaultValue(false)
            .build()
    );

    private final Setting<Double> scale = sgScale.add(new DoubleSetting.Builder()
            .name("缩放")
            .description("自定义缩放。")
            .visible(customScale::get)
            .defaultValue(1)
            .min(0.5)
            .sliderRange(0.5, 3)
            .build()
    );

    public ItemCounter() {
        super(INFO);
    }

    private final ArrayList<String> itemCounter = new ArrayList<>();

    @Override
    public void tick(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;

        updateCounter();

        double width = 0;
        double height = 0;
        int i = 0;

        if (itemCounter.isEmpty()) {
            String t = "物品计数器";
            width = Math.max(width, renderer.textWidth(t));
            height += renderer.textHeight();
        } else {
            for (String counter : itemCounter) {
                width = Math.max(width, renderer.textWidth(counter));
                height += renderer.textHeight();
                if (i > 0) height += 2;
                i++;
            }
        }
        box.setSize(width, height);
    }

    @Override
    public void render(HudRenderer renderer) {
        if (!Utils.canUpdate()) return;

        updateCounter();

        double x = this.x;
        double y = this.y;
        int i = 0;

        if (itemCounter.isEmpty()) {
            String text = "物品计数器:";
            renderer.text(text, x + alignX(renderer.textWidth(text, shadow.get(), getScale()), alignment.get()), y, TextHud.getSectionColor(0), shadow.get(), getScale());
        } else {
            for (String counter : itemCounter) {
                renderer.text(counter, x + alignX(renderer.textWidth(counter, shadow.get(), getScale()), alignment.get()), y, TextHud.getSectionColor(0), shadow.get(), getScale());
                y += renderer.textHeight();
                if (i > 0) y += 2;
                i++;
            }
        }
    }

    private double getScale() {
        return customScale.get() ? scale.get() : -1;
    }

    private void updateCounter() {
        items.get().sort(Comparator.comparingDouble(value -> getName(value).length()));

        itemCounter.clear();
        for (Item item : items.get()) itemCounter.add(getName(item) + ": " + InvUtils.find(item).count());

        if (sortMode.get().equals(SortMode.最短)) {
            itemCounter.sort(Comparator.comparing(String::length));
        } else {
            itemCounter.sort(Comparator.comparing(String::length).reversed());
        }
    }

    public static String getName(Item item) {
        if (item instanceof BedItem) return "床";
        if (item instanceof ExperienceBottleItem) return "经验瓶";
        if (item instanceof EndCrystalItem) return "末影水晶";
        if (item instanceof EnderPearlItem) return "末影珍珠";
        if (item == Items.ENCHANTED_GOLDEN_APPLE) return "附魔金苹果";
        if (item == Items.TOTEM_OF_UNDYING) return "不死图腾";
        if (item == Items.ENDER_CHEST) return "末地箱子";
        if (item == Items.OBSIDIAN) return "黑曜石";
        return Names.get(item);
    }
}
