package cqb13.NumbyHack.modules.general;

import cqb13.NumbyHack.NumbyHack;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.player.ChatUtils;
import meteordevelopment.orbit.EventHandler;

import java.util.Objects;

/**
 * 作者: cqb13
 */
public class Number81 extends Module {

    public Number81() {
        super(NumbyHack.CATEGORY, "81", "在聊天中计数到81。");
    }

    private int timer; // 定时器
    private int count; // 当前计数
    private boolean setTimer; // 是否设置定时器

    @Override
    public void onActivate() {
        count = 0; // 激活时将计数器重置为0
    }

    @Override
    public void onDeactivate() {
        assert mc.player != null;
        var name = mc.player.getName();
        // 如果玩家的名字是 "cqb13" 或 "Number81"，则不发送消息
        if (Objects.equals(name.toString(), "cqb13") || Objects.equals(name.toString(), "Number81")) {
            return;
        }
        // 如果计数没有到达81，发送一些消息
        if (count != 81) {
            assert mc.player != null;
            ChatUtils.sendPlayerMsg("我很懒，没有数到81！");
            ChatUtils.sendPlayerMsg("我很丢脸，应该受到惩罚！");
            ChatUtils.sendPlayerMsg("我真是个坏人！");
            ChatUtils.sendPlayerMsg("Number81是最棒的！");
        }
    }

    @EventHandler
    private void onTick(TickEvent.Pre event) {
        // 如果需要设置定时器
        if (setTimer) {
            timer = (int)(Math.random() * 70 + 40); // 随机生成一个40到110之间的数
            setTimer = false; // 重置设置标志
        }
        timer--; // 每次tick减少定时器
        // 如果定时器小于0，进行计数
        if (timer < 0) {
            count++; // 计数加1
            assert mc.player != null;
            ChatUtils.sendPlayerMsg(String.valueOf(count)); // 发送当前计数到聊天
            setTimer = true; // 设置定时器
        }
        // 如果计数达到81，发送消息并关闭模块
        if (count == 81) {
            assert mc.player != null;
            ChatUtils.sendPlayerMsg("Number81在最上面！");
            toggle(); // 切换模块状态
        }
    }
}
