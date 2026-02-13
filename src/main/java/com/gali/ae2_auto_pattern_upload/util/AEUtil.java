package com.gali.ae2_auto_pattern_upload.util;

import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;
import net.p455w0rd.wirelesscraftingterminal.api.IWirelessCraftingTermHandler;
import net.p455w0rd.wirelesscraftingterminal.common.WCTGuiHandler;
import net.p455w0rd.wirelesscraftingterminal.common.container.ContainerWirelessCraftingTerminal;
import net.p455w0rd.wirelesscraftingterminal.helpers.WirelessTerminalGuiObject;
import net.p455w0rd.wirelesscraftingterminal.reference.Reference;

import com.gali.ae2_auto_pattern_upload.MyMod;
import com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor.AEBaseContainerAccessor;
import com.gali.ae2_auto_pattern_upload.mixin.ae2.accessor.ContainerWirelessCraftingTerminalAccessor;

import appeng.api.AEApi;
import appeng.api.config.SecurityPermissions;
import appeng.api.features.IWirelessTermHandler;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridNode;
import appeng.api.networking.security.IActionHost;
import appeng.api.networking.security.ISecurityGrid;
import appeng.api.networking.storage.IStorageGrid;
import appeng.api.storage.data.IAEItemStack;
import appeng.container.AEBaseContainer;
import appeng.container.implementations.ContainerCraftAmount;
import appeng.container.implementations.ContainerWirelessTerm;
import appeng.core.sync.GuiBridge;
import appeng.util.Platform;
import appeng.util.item.AEItemStack;

/**
 * AE2 相关的工具类
 */
public class AEUtil {

    /**
     * 检查无线终端是否在范围内
     */
    public static boolean checkWirelessRange(AEBaseContainer container) {
        try {
            // 检查是否是无线终端容器
            if (container instanceof ContainerWirelessTerm wirelessContainer) {
                return !wirelessContainer.getWirelessTerminalGUIObject()
                    .rangeCheck();
            }

            // 检查是否是无线合成终端容器
            if (container instanceof ContainerWirelessCraftingTerminal craftingTerminal) {
                // 使用 Mixin 调用 isInRange 方法
                ContainerWirelessCraftingTerminalAccessor accessor = (ContainerWirelessCraftingTerminalAccessor) craftingTerminal;
                return !accessor.invokeIsInRange();
            }
        } catch (Throwable e) {
            MyMod.LOG.debug("Failed to check wireless range", e);
        }
        return false;
    }

    /**
     * 获取 ActionHost，特殊处理无线终端
     */
    public static IActionHost getActionHost(AEBaseContainer container) {
        try {
            // 首先尝试从 target 获取
            Object target = container.getTarget();
            if (target instanceof IActionHost) {
                return (IActionHost) target;
            }

            // 特殊处理 AE2 无线终端容器
            if (container instanceof ContainerWirelessTerm wirelessContainer) {
                return wirelessContainer.getWirelessTerminalGUIObject();
            }

            // 特殊处理无线合成终端容器 (Wireless Crafting Terminal)
            if (container instanceof ContainerWirelessCraftingTerminal) {
                // 通过反射获取 obj 字段 (WirelessTerminalGuiObject 类型)
                java.lang.reflect.Field objField = container.getClass()
                    .getDeclaredField("obj");
                objField.setAccessible(true);
                Object obj = objField.get(container);
                if (obj instanceof IActionHost) {
                    return (IActionHost) obj;
                }
            }

            // 使用 Mixin 调用 getActionHost 方法
            AEBaseContainerAccessor accessor = (AEBaseContainerAccessor) container;
            return accessor.invokeGetActionHost();
        } catch (Throwable e) {
            MyMod.LOG.debug("Failed to get action host", e);
        }
        return null;
    }

    /**
     * 检查物品是否可合成
     */
    public static boolean isCraftable(IGrid grid, IAEItemStack stack) {
        try {
            IStorageGrid storage = grid.getCache(IStorageGrid.class);
            if (storage == null) {
                return false;
            }

            IAEItemStack found = storage.getItemInventory()
                .getStorageList()
                .findPrecise(stack);
            if (found != null) {
                return found.isCraftable();
            }

            return false;
        } catch (Throwable e) {
            return false;
        }
    }

    /**
     * 打开合成数量选择界面
     */
    public static void openCraftingAmountGui(EntityPlayerMP player, AEBaseContainer container, IAEItemStack stack) {
        try {
            if (container.getOpenContext() != null) {
                Platform.openGUI(
                    player,
                    container.getOpenContext()
                        .getTile(),
                    container.getOpenContext()
                        .getSide(),
                    GuiBridge.GUI_CRAFTING_AMOUNT);

                // 设置要合成的物品
                if (player.openContainer instanceof ContainerCraftAmount craftAmount) {
                    craftAmount.getCraftingItem()
                        .putStack(stack.getItemStack());
                    craftAmount.setItemToCraft(stack);
                    craftAmount.detectAndSendChanges();
                }
            }
        } catch (Throwable e) {
            MyMod.LOG.warn("Error opening crafting amount GUI", e);
        }
    }

    /**
     * 获取网格节点
     */
    public static IGridNode getGridNode(AEBaseContainer container) {
        IActionHost actionHost = getActionHost(container);
        if (actionHost == null) {
            return null;
        }
        return actionHost.getActionableNode();
    }

    /**
     * 获取网格
     */
    public static IGrid getGrid(AEBaseContainer container) {
        IGridNode node = getGridNode(container);
        if (node == null) {
            return null;
        }
        return node.getGrid();
    }

    /**
     * 搜索玩家背包中的无线终端并获取网格
     */
    public static IGrid getGridFromWirelessTerminal(EntityPlayer player) {
        if (player == null) {
            return null;
        }

        // 遍历玩家背包寻找无线终端
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack == null) {
                continue;
            }

            // 获取无线终端处理器
            IWirelessTermHandler handler = AEApi.instance()
                .registries()
                .wireless()
                .getWirelessTerminalHandler(stack);
            if (handler == null) {
                continue;
            }

            // 检查是否是有效的无线终端
            if (!handler.canHandle(stack)) {
                continue;
            }

            try {
                // 获取加密密钥
                String encryptionKey = handler.getEncryptionKey(stack);
                if (encryptionKey == null || encryptionKey.isEmpty()) {
                    continue;
                }

                // 检查是否是 WCT 的无线终端（支持量子卡）
                if (handler instanceof IWirelessCraftingTermHandler) {
                    // 使用 WCT 的 WirelessTerminalGuiObject，因为它支持量子卡
                    WirelessTerminalGuiObject terminalObject = new WirelessTerminalGuiObject(
                        handler,
                        stack,
                        player,
                        player.worldObj,
                        i,
                        0,
                        0);

                    // 检查范围 - WCT 的 rangeCheck 会自动处理量子卡的情况
                    if (!terminalObject.rangeCheck()) {
                        continue;
                    }

                    // 获取网格节点
                    IGridNode node = terminalObject.getActionableNode();
                    if (node != null) {
                        IGrid grid = node.getGrid();
                        if (grid != null) {
                            return grid;
                        }
                    }
                } else {
                    // 使用 AE2 原版的 WirelessTerminalGuiObject
                    WirelessTerminalGuiObject terminalObject = new WirelessTerminalGuiObject(
                        handler,
                        stack,
                        player,
                        player.worldObj,
                        i,
                        0,
                        0);

                    // 检查范围
                    if (!terminalObject.rangeCheck()) {
                        continue;
                    }

                    // 获取网格节点
                    IGridNode node = terminalObject.getActionableNode();
                    if (node != null) {
                        IGrid grid = node.getGrid();
                        if (grid != null) {
                            return grid;
                        }
                    }
                }
            } catch (Throwable e) {
                MyMod.LOG.debug("Failed to get grid from wireless terminal in inventory slot " + i, e);
            }
        }

        return null;
    }

    /**
     * 检查容器是否为AE2相关的容器
     */
    public static boolean isAEContainer(Container container) {
        if (container == null) {
            return false;
        }

        String className = container.getClass()
            .getName();
        return className.startsWith("appeng.") || className.contains("WirelessCraftingTerminal");
    }

    /**
     * 从玩家背包中查找无线终端并获取ActionHost
     */
    public static IActionHost findWirelessActionHost(EntityPlayer player) {
        if (player == null) {
            return null;
        }

        // 遍历玩家背包寻找无线终端
        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack stack = player.inventory.mainInventory[i];
            if (stack == null) {
                continue;
            }

            // 获取无线终端处理器
            IWirelessTermHandler handler = AEApi.instance()
                .registries()
                .wireless()
                .getWirelessTerminalHandler(stack);
            if (handler == null) {
                continue;
            }

            // 检查是否是有效的无线终端
            if (!handler.canHandle(stack)) {
                continue;
            }

            try {
                // 检查是否是 WCT 的无线终端（支持量子卡）
                if (handler instanceof IWirelessCraftingTermHandler) {
                    // 使用 WCT 的 WirelessTerminalGuiObject，因为它支持量子卡
                    WirelessTerminalGuiObject terminalObject = new WirelessTerminalGuiObject(
                        handler,
                        stack,
                        player,
                        player.worldObj,
                        i,
                        0,
                        0);

                    // 检查范围 - WCT 的 rangeCheck 会自动处理量子卡的情况
                    if (!terminalObject.rangeCheck()) {
                        continue; // 如果不在范围内，则跳过
                    }

                    return terminalObject;
                } else {
                    // 使用 AE2 原版的 WirelessTerminalGuiObject
                    WirelessTerminalGuiObject terminalObject = new WirelessTerminalGuiObject(
                        handler,
                        stack,
                        player,
                        player.worldObj,
                        i,
                        0,
                        0);

                    // 检查范围
                    if (!terminalObject.rangeCheck()) {
                        continue; // 如果不在范围内，则跳过
                    }

                    return terminalObject;
                }
            } catch (Throwable e) {
                MyMod.LOG.debug("Failed to create wireless terminal object in inventory slot " + i, e);
            }
        }

        return null;
    }

    /**
     * 获取玩家的网格，优先从当前容器获取，否则从背包中的无线终端获取
     */
    public static IGrid getPlayerGrid(EntityPlayerMP player) {
        if (player == null) {
            return null;
        }

        Container container = player.openContainer;
        IGrid grid = null;

        // 尝试从AE容器获取网格
        if (container instanceof AEBaseContainer baseContainer) {
            // 获取能量源和存储
            if (baseContainer.getPowerSource() != null && baseContainer.getCellInventory() != null) {
                // 获取网格
                grid = getGrid(baseContainer);

                if (grid != null) {
                    // 检查无线终端范围
                    if (checkWirelessRange(baseContainer)) {
                        return null;
                    }
                }
            }
        }

        // 如果不在AE界面，尝试从背包中的无线终端获取网格
        if (grid == null) {
            grid = getGridFromWirelessTerminal(player);
        }

        return grid;
    }

    /**
     * 检查玩家是否有指定的网格权限
     */
    public static boolean hasGridPermission(EntityPlayer player, IGrid grid, SecurityPermissions permission) {
        if (grid == null || player == null) {
            return false;
        }

        ISecurityGrid security = grid.getCache(ISecurityGrid.class);
        return security == null || security.hasPermission(player, permission);
    }

    /**
     * 创建AE物品堆栈
     */
    public static IAEItemStack createAEStack(ItemStack itemStack) {
        if (itemStack == null) {
            return null;
        }
        return AEItemStack.create(itemStack);
    }

    /**
     * 打开合成数量选择界面（支持AE2界面和无线终端）
     */
    public static void openCraftingAmountGui(EntityPlayerMP player, IAEItemStack aeStack) {
        if (player == null || aeStack == null) {
            return;
        }

        Container container = player.openContainer;

        if (container instanceof AEBaseContainer baseContainer) {
            openCraftingAmountGui(player, baseContainer, aeStack);
        } else {
            // 不在AE界面，尝试通过无线终端打开GUI
            IActionHost actionHost = findWirelessActionHost(player);

            if (actionHost == null) {
                return;
            }

            // 检查是否是 WCT 的无线终端
            if (actionHost instanceof WirelessTerminalGuiObject) {
                // 使用 WCT 的方式打开合成界面
                int x = (int) player.posX;
                int y = (int) player.posY;
                int z = (int) player.posZ;
                WCTGuiHandler.launchGui(Reference.GUI_CRAFT_AMOUNT, player, player.worldObj, x, y, z);

                // 设置要合成的物品到当前容器
                if (player.openContainer instanceof ContainerCraftAmount craftAmount) {
                    craftAmount.getCraftingItem()
                        .putStack(aeStack.getItemStack());
                    craftAmount.setItemToCraft(aeStack);
                    craftAmount.detectAndSendChanges();
                }
            } else {
                // 使用 AE2 原版的方式打开合成界面
                Platform.openGUI(
                    player,
                    null,
                    net.minecraftforge.common.util.ForgeDirection.UNKNOWN,
                    GuiBridge.GUI_CRAFTING_AMOUNT);

                // 设置要合成的物品到当前容器
                if (player.openContainer instanceof ContainerCraftAmount craftAmount) {
                    craftAmount.getCraftingItem()
                        .putStack(aeStack.getItemStack());
                    craftAmount.setItemToCraft(aeStack);
                    craftAmount.detectAndSendChanges();
                }
            }
        }
    }
}
