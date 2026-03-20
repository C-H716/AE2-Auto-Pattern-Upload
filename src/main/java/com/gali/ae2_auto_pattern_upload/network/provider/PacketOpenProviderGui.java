package com.gali.ae2_auto_pattern_upload.network.provider;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.event.ClickEvent;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.world.World;
import net.minecraftforge.common.util.ForgeDirection;

import appeng.api.parts.IPart;
import appeng.api.parts.IPartHost;
import appeng.api.util.DimensionalCoord;
import appeng.core.sync.GuiBridge;
import appeng.helpers.IInterfaceHost;
import appeng.parts.misc.PartInterface;
import appeng.tile.misc.TileInterface;
import appeng.util.Platform;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 打开供应器(Interface)界面的数据包
 * 客户端发送供应器位置，服务器端打开对应的GUI
 */
public class PacketOpenProviderGui implements IMessage {

    private int x;
    private int y;
    private int z;
    private int dimension;
    private int sideOrdinal;

    public PacketOpenProviderGui() {}

    public PacketOpenProviderGui(DimensionalCoord coord, ForgeDirection side) {
        this.x = coord.x;
        this.y = coord.y;
        this.z = coord.z;
        this.dimension = coord.getDimension();
        this.sideOrdinal = side.ordinal();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        this.x = buf.readInt();
        this.y = buf.readInt();
        this.z = buf.readInt();
        this.dimension = buf.readInt();
        this.sideOrdinal = buf.readInt();
    }

    @Override
    public void toBytes(ByteBuf buf) {
        buf.writeInt(x);
        buf.writeInt(y);
        buf.writeInt(z);
        buf.writeInt(dimension);
        buf.writeInt(sideOrdinal);
    }

    public static class Handler implements IMessageHandler<PacketOpenProviderGui, IMessage> {

        @Override
        public IMessage onMessage(PacketOpenProviderGui message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            // 检查玩家所在维度
            if (player.worldObj.provider.dimensionId != message.dimension) {
                return null;
            }

            // 获取世界
            World world = player.worldObj;
            if (world == null) {
                return null;
            }

            // 获取位置
            int x = message.x;
            int y = message.y;
            int z = message.z;
            ForgeDirection side = ForgeDirection.getOrientation(message.sideOrdinal);

            // 检查区块是否加载
            if (!world.blockExists(x, y, z)) {
                return null;
            }

            // 获取 TileEntity
            TileEntity te = world.getTileEntity(x, y, z);
            if (te == null) {
                return null;
            }

            // 检查是否是接口方块 (TileInterface)
            if (te instanceof TileInterface tileInterface) {
                // 打开接口GUI
                Platform.openGUI(player, tileInterface, side, GuiBridge.GUI_INTERFACE);
                sendTeleportMessage(player, x, y, z);
                return null;
            }

            // 检查是否是 PartInterface (通过 IPartHost)
            if (te instanceof IPartHost partHost) {
                // 尝试所有方向查找 PartInterface
                for (ForgeDirection dir : ForgeDirection.VALID_DIRECTIONS) {
                    IPart part = partHost.getPart(dir);
                    if (part instanceof PartInterface partInterface) {
                        // 打开接口GUI - 使用 TileEntity 和方向
                        Platform.openGUI(player, te, dir, GuiBridge.GUI_INTERFACE);
                        sendTeleportMessage(player, x, y, z);
                        return null;
                    }
                }
            }

            // 检查是否实现了 IInterfaceHost 接口
            if (te instanceof IInterfaceHost interfaceHost) {
                Platform.openGUI(player, te, side, GuiBridge.GUI_INTERFACE);
                sendTeleportMessage(player, x, y, z);
                return null;
            }

            return null;
        }

        /**
         * 发送带点击传送功能的聊天消息给玩家
         */
        private void sendTeleportMessage(EntityPlayerMP player, int x, int y, int z) {
            net.minecraft.util.ChatComponentText message = new net.minecraft.util.ChatComponentText(
                "[" + x + ", " + y + ", " + z + "]");
            // 使用 /tp @p x y z 格式，确保传送到正确的位置，y+1 让玩家站在方块上方
            String tpCommand = "/tp @p " + x + " " + (y + 1) + " " + z;
            ChatStyle style = new ChatStyle().setColor(EnumChatFormatting.GREEN)
                .setChatClickEvent(new ClickEvent(ClickEvent.Action.RUN_COMMAND, tpCommand))
                .setUnderlined(true);
            message.setChatStyle(style);

            net.minecraft.util.ChatComponentTranslation prefix = new net.minecraft.util.ChatComponentTranslation(
                "ae2_auto_pattern_upload.info.provider_location");
            prefix.appendSibling(message);

            player.addChatMessage(prefix);
        }
    }
}
