package com.gali.ae2_auto_pattern_upload.network;

import cpw.mods.fml.common.network.NetworkRegistry;
import cpw.mods.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import cpw.mods.fml.relauncher.Side;

public final class ModNetwork {

    public static final String CHANNEL_ID = "ae2apu";
    public static final SimpleNetworkWrapper INSTANCE = NetworkRegistry.INSTANCE.newSimpleChannel(CHANNEL_ID);
    public static final SimpleNetworkWrapper CHANNEL = INSTANCE;

    private static int discriminator = 0;

    private ModNetwork() {}

    public static void registerPackets() {
        INSTANCE.registerMessage(
            RequestProvidersListPacket.Handler.class,
            RequestProvidersListPacket.class,
            discriminator++,
            Side.SERVER);

        INSTANCE.registerMessage(
            ProvidersListS2CPacket.Handler.class,
            ProvidersListS2CPacket.class,
            discriminator++,
            Side.CLIENT);

        INSTANCE.registerMessage(
            UploadPatternPacket.Handler.class,
            UploadPatternPacket.class,
            discriminator++,
            Side.SERVER);

        // 注册物品拉取/下单数据包
        INSTANCE
            .registerMessage(PacketExtractItem.Handler.class, PacketExtractItem.class, discriminator++, Side.SERVER);

        // 注册合成下单界面打开数据包
        INSTANCE.registerMessage(
            PacketOpenCraftingAmount.Handler.class,
            PacketOpenCraftingAmount.class,
            discriminator++,
            Side.SERVER);

        // 注册配方材料提取数据包
        INSTANCE.registerMessage(
            PacketExtractIngredients.Handler.class,
            PacketExtractIngredients.class,
            discriminator++,
            Side.SERVER);

        // 注册滚轮存取物品数据包
        INSTANCE.registerMessage(
            PacketScrollTransfer.Handler.class,
            PacketScrollTransfer.class,
            discriminator++,
            Side.SERVER);

        // 注册请求正在合成物品数据包（客户端->服务器）
        INSTANCE.registerMessage(
            RequestCraftingItemsPacket.ServerHandler.class,
            RequestCraftingItemsPacket.class,
            discriminator++,
            Side.SERVER);

        // 注册正在合成物品更新数据包（服务器->客户端）
        INSTANCE.registerMessage(
            PacketCraftingItemsUpdate.ClientHandler.class,
            PacketCraftingItemsUpdate.class,
            discriminator++,
            Side.CLIENT);

        // 注册打开供应器界面数据包（客户端->服务器）
        INSTANCE.registerMessage(
            PacketOpenProviderGui.Handler.class,
            PacketOpenProviderGui.class,
            discriminator++,
            Side.SERVER);
    }
}
