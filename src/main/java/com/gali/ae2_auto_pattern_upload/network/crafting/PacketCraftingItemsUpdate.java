package com.gali.ae2_auto_pattern_upload.network.crafting;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import com.gali.ae2_auto_pattern_upload.crafting.CraftingItemsCache;

import appeng.api.storage.data.IAEItemStack;
import appeng.util.item.AEItemStack;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 同步正在合成的物品到客户端
 */
public class PacketCraftingItemsUpdate implements IMessage {

    private Set<IAEItemStack> craftingItems = new HashSet<>();

    public PacketCraftingItemsUpdate() {}

    public PacketCraftingItemsUpdate(Set<IAEItemStack> items) {
        this.craftingItems = items;
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        int count = buf.readInt();
        craftingItems = new HashSet<>();
        for (int i = 0; i < count; i++) {
            try {
                IAEItemStack stack = AEItemStack.loadItemStackFromPacket(buf);
                if (stack != null) {
                    craftingItems.add(stack);
                }
            } catch (IOException e) {
                // ignore
            }
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(craftingItems.size());
        for (IAEItemStack stack : craftingItems) {
            try {
                stack.writeToPacket(buf);
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public Set<IAEItemStack> getCraftingItems() {
        return craftingItems;
    }

    /**
     * 客户端处理器
     */
    public static class ClientHandler implements IMessageHandler<PacketCraftingItemsUpdate, IMessage> {

        @Override
        public IMessage onMessage(PacketCraftingItemsUpdate message, MessageContext ctx) {
            // 直接在主线程更新缓存
            CraftingItemsCache.updateFromServer(message.getCraftingItems());
            return null;
        }
    }
}
