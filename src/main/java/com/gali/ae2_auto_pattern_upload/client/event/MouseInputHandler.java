package com.gali.ae2_auto_pattern_upload.client.event;

import net.minecraft.block.Block;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.item.ItemStack;
import net.minecraft.util.MovingObjectPosition;
import net.minecraftforge.client.event.MouseEvent;

import com.gali.ae2_auto_pattern_upload.MyMod;
import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.network.PacketMiddleClickExtract;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * 处理游戏中的鼠标输入事件
 * 主要用于检测玩家中键点击方块/物品
 */
@SideOnly(Side.CLIENT)
public class MouseInputHandler {

    private static final int MOUSE_MIDDLE = 2;
    private static boolean wasMiddleDown = false;

    public static void register() {
        net.minecraftforge.common.MinecraftForge.EVENT_BUS.register(new MouseInputHandler());
    }

    @SubscribeEvent
    public void onMouseEvent(MouseEvent event) {
        // 检查是否是中键点击
        if (event.button != MOUSE_MIDDLE) {
            return;
        }

        // 只在按键按下时触发，不处理松开
        if (!event.buttonstate) {
            return;
        }

        // 获取玩家
        Minecraft mc = Minecraft.getMinecraft();
        EntityClientPlayerMP player = mc.thePlayer;
        if (player == null) {
            return;
        }

        // 获取准星指向的对象
        MovingObjectPosition target = mc.objectMouseOver;
        if (target == null) {
            return;
        }

        // 处理不同类型的目标
        ItemStack targetStack = null;

        if (target.typeOfHit == MovingObjectPosition.MovingObjectType.BLOCK) {
            // 指向方块，获取方块的物品形式
            Block block = mc.theWorld.getBlock(target.blockX, target.blockY, target.blockZ);
            int meta = mc.theWorld.getBlockMetadata(target.blockX, target.blockY, target.blockZ);
            if (block != null) {
                // 尝试多种方式获取方块的物品形式

                // 方式1: 尝试获取TE的item形式（适用于AE Part类方块）
                try {
                    net.minecraft.tileentity.TileEntity te = mc.theWorld
                        .getTileEntity(target.blockX, target.blockY, target.blockZ);
                    if (te != null) {
                        // 尝试调用getDrops或类似方法获取物品
                        java.lang.reflect.Method getItem = te.getClass()
                            .getMethod("getItemFromTile");
                        targetStack = (ItemStack) getItem.invoke(te);
                    }
                } catch (Throwable ignored) {}

                // 方式2: 使用 Item.getItemFromBlock（最可靠的方式）
                if (targetStack == null || targetStack.getItem() == null) {
                    try {
                        net.minecraft.item.Item item = net.minecraft.item.Item.getItemFromBlock(block);
                        if (item != null) {
                            // 对于红石粉等特殊方块，需要使用 damageDropped 获取正确的 meta
                            int damage = block.damageDropped(meta);
                            targetStack = new ItemStack(item, 1, damage);
                        }
                    } catch (Throwable ignored) {}
                }

                // 方式3: 使用 getPickBlock（Forge添加的方法，最准确）
                if (targetStack == null || targetStack.getItem() == null) {
                    try {
                        java.lang.reflect.Method getPickBlock = Block.class.getMethod(
                            "getPickBlock",
                            MovingObjectPosition.class,
                            net.minecraft.world.World.class,
                            int.class,
                            int.class,
                            int.class);
                        targetStack = (ItemStack) getPickBlock
                            .invoke(block, target, mc.theWorld, target.blockX, target.blockY, target.blockZ);
                    } catch (Throwable ignored) {}
                }

                // 方式4: 使用 createStackedBlock（适用于某些特殊方块）
                if (targetStack == null || targetStack.getItem() == null) {
                    try {
                        java.lang.reflect.Method createStackedBlock = Block.class
                            .getDeclaredMethod("createStackedBlock", int.class);
                        createStackedBlock.setAccessible(true);
                        targetStack = (ItemStack) createStackedBlock.invoke(block, meta);
                    } catch (Throwable ignored) {}
                }

                // 方式5: 使用 getItemDropped（适用于大多数方块）
                if (targetStack == null || targetStack.getItem() == null) {
                    try {
                        net.minecraft.item.Item item = block.getItemDropped(meta, mc.theWorld.rand, 0);
                        if (item != null) {
                            targetStack = new ItemStack(item, 1, block.damageDropped(meta));
                        }
                    } catch (Throwable ignored) {}
                }

                // 方式6: 直接使用方块对应的物品（适用于普通方块）
                if (targetStack == null || targetStack.getItem() == null) {
                    try {
                        targetStack = new ItemStack(block, 1, meta);
                    } catch (Throwable ignored) {}
                }
            }
        }

        if (targetStack == null || targetStack.getItem() == null) {
            return;
        }

        // 检查玩家背包中是否已有该物品
        // 如果背包中有，让原版逻辑处理（不取消事件）
        // 如果背包中没有，发送到服务器从AE提取
        if (hasItemInInventory(player, targetStack)) {
            // 背包中有该物品，不取消事件，让原版处理
            return;
        }

        // 背包中没有该物品，取消事件并从AE提取
        event.setCanceled(true);
        ModNetwork.INSTANCE.sendToServer(new PacketMiddleClickExtract(targetStack));
    }

    /**
     * 检查玩家背包中是否有指定物品
     */
    private boolean hasItemInInventory(EntityClientPlayerMP player, ItemStack stack) {
        if (stack == null || stack.getItem() == null) {
            return false;
        }

        for (int i = 0; i < player.inventory.mainInventory.length; i++) {
            ItemStack invStack = player.inventory.mainInventory[i];
            if (invStack != null && invStack.getItem() == stack.getItem()) {
                // 检查metadata是否匹配（对于红石粉等需要精确匹配）
                if (invStack.getItemDamage() == stack.getItemDamage()) {
                    return true;
                }
            }
        }
        return false;
    }
}
