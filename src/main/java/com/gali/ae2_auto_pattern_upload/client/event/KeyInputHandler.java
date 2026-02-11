package com.gali.ae2_auto_pattern_upload.client.event;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import net.minecraft.client.gui.GuiTextField;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.item.ItemStack;

import org.lwjgl.input.Keyboard;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.PacketExtractItem;
import com.gali.ae2_auto_pattern_upload.network.PacketOpenCraftingAmount;

import codechicken.nei.ItemPanels;
import codechicken.nei.guihook.GuiContainerManager;
import codechicken.nei.guihook.IContainerInputHandler;

public class KeyInputHandler implements IContainerInputHandler {

    private static final int KEY_F = Keyboard.KEY_F;
    private static final int MOUSE_LEFT = 0;
    private static final int MOUSE_RIGHT = 1;
    private static final int MOUSE_MIDDLE = 2;

    public static void register() {
        GuiContainerManager.addInputHandler(new KeyInputHandler());
    }

    @Override
    public boolean keyTyped(GuiContainer gui, char keyChar, int keyCode) {
        if (keyCode != KEY_F) {
            return false;
        }

        if (!isAE2Gui(gui)) {
            return false;
        }

        // 查找搜索框
        Object searchField = findSearchField(gui);
        if (searchField == null) {
            return false;
        }

        // 检查搜索框是否已经获得焦点
        if (isSearchFieldFocused(searchField)) {
            return false;
        }

        // 获取鼠标下的物品
        ItemStack stackUnderMouse = GuiContainerManager.getStackMouseOver(gui);
        if (stackUnderMouse == null) {
            return false;
        }

        // 获取物品名称
        String itemName = stackUnderMouse.getDisplayName();
        if (itemName == null || itemName.isEmpty()) {
            return false;
        }

        // 写入搜索框
        return setSearchText(searchField, itemName);
    }

    @Override
    public void onKeyTyped(GuiContainer gui, char keyChar, int keyID) {}

    @Override
    public boolean lastKeyTyped(GuiContainer gui, char keyChar, int keyCode) {
        return false;
    }

    @Override
    public boolean mouseClicked(GuiContainer gui, int mousex, int mousey, int button) {
        // 检查鼠标是否在NEI面板（书签面板、物品面板、历史记录面板）上
        if (!isMouseOverNEIPanel(mousex, mousey)) {
            return false;
        }

        // 获取鼠标下的物品
        ItemStack stackUnderMouse = GuiContainerManager.getStackMouseOver(gui);
        if (stackUnderMouse == null) {
            return false;
        }

        // 检查是否为AE界面或非AE界面但需要处理的场景
        if (button == MOUSE_LEFT && isShiftKeyDown()) {
            return handleShiftLeftClick(gui, stackUnderMouse);
        }
        if (button == MOUSE_MIDDLE) {
            return handleMiddleClick(gui, stackUnderMouse);
        }

        return false;
    }

    @Override
    public void onMouseClicked(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public void onMouseUp(GuiContainer gui, int mousex, int mousey, int button) {}

    @Override
    public boolean mouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {
        return false;
    }

    @Override
    public void onMouseScrolled(GuiContainer gui, int mousex, int mousey, int scrolled) {}

    @Override
    public void onMouseDragged(GuiContainer gui, int mousex, int mousey, int button, long heldTime) {}

    /**
     * 检查鼠标是否在NEI面板（书签面板、物品面板、历史记录面板）上
     */
    private boolean isMouseOverNEIPanel(int mousex, int mousey) {
        try {
            // 检查书签面板（左侧收藏）
            if (ItemPanels.bookmarkPanel.contains(mousex, mousey)) {
                return true;
            }

            // 检查物品面板（右侧物品列表）
            if (ItemPanels.itemPanel.contains(mousex, mousey)) {
                return true;
            }

            // 检查历史记录面板
            if (ItemPanels.itemPanel.historyPanel.contains(mousex, mousey)) {
                return true;
            }

            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 处理 Shift + 左键点击
     * 1. 若目标物品有库存，执行拉取
     * 2. 若无库存但该物品可合成，自动打开下单界面
     * 3. 若玩家背包空间不足，执行拉取失败操作并静默失败
     */
    private boolean handleShiftLeftClick(GuiContainer gui, ItemStack stack) {
        if (stack == null) {
            return false;
        }

        try {
            // 发送数据包到服务器处理拉取/下单逻辑
            ModNetwork.INSTANCE.sendToServer(new PacketExtractItem(stack, true));
            return true;
        } catch (Throwable e) {
            // 静默失败
            return false;
        }
    }

    /**
     * 处理鼠标中键点击
     * 1. 检测该物品是否可合成
     * 2. 若可合成，自动打开下单界面
     */
    private boolean handleMiddleClick(GuiContainer gui, ItemStack stack) {
        if (stack == null) {
            return false;
        }

        try {
            // 发送数据包到服务器请求打开合成下单界面
            ModNetwork.INSTANCE.sendToServer(new PacketOpenCraftingAmount(stack));
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private boolean isAE2Gui(GuiContainer gui) {
        if (gui == null) {
            return false;
        }
        String className = gui.getClass()
            .getName();
        return className.startsWith("appeng.") || className.startsWith("com.glodblock.")
            || className.equals("net.p455w0rd.wirelesscraftingterminal.client.gui.GuiWirelessCraftingTerminal");
    }

    private boolean isShiftKeyDown() {
        return Keyboard.isKeyDown(Keyboard.KEY_LSHIFT) || Keyboard.isKeyDown(Keyboard.KEY_RSHIFT);
    }

    private Object findSearchField(GuiContainer gui) {
        Class<?> clazz = gui.getClass();

        while (clazz != null && clazz != Object.class) {
            for (Field field : clazz.getDeclaredFields()) {
                try {
                    String fieldName = field.getName()
                        .toLowerCase();
                    // 查找名称包含 search 且类型包含 TextField 的字段
                    if (fieldName.contains("search") && field.getType()
                        .getName()
                        .contains("TextField")) {
                        field.setAccessible(true);
                        Object value = field.get(gui);
                        if (value != null) {
                            return value;
                        }
                    }
                } catch (Throwable ignored) {}
            }
            clazz = clazz.getSuperclass();
        }

        return null;
    }

    private boolean isSearchFieldFocused(Object searchField) {
        try {
            if (searchField instanceof GuiTextField) {
                return ((GuiTextField) searchField).isFocused();
            }
            Method isFocused = searchField.getClass()
                .getMethod("isFocused");
            return (Boolean) isFocused.invoke(searchField);
        } catch (Throwable ignored) {}
        return false;
    }

    private boolean setSearchText(Object searchField, String text) {
        try {
            if (searchField instanceof GuiTextField) {
                ((GuiTextField) searchField).setText(text);
                return true;
            }
            Method setText = searchField.getClass()
                .getMethod("setText", String.class);
            setText.invoke(searchField, text);
            return true;
        } catch (Throwable ignored) {}
        return false;
    }
}
