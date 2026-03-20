package com.gali.ae2_auto_pattern_upload.network.provider;

import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;

import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminal;
import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminalEx;
import com.glodblock.github.inventory.item.IItemPatternTerminal;

import appeng.api.config.Upgrades;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.helpers.IInterfaceHost;
import appeng.parts.AEBasePart;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

public class RequestProvidersListPacket implements IMessage {

    @Override
    public void fromBytes(ByteBuf buf) {}

    @Override
    public void toBytes(ByteBuf buf) {}

    public static class Handler implements IMessageHandler<RequestProvidersListPacket, IMessage> {

        @Override
        public IMessage onMessage(RequestProvidersListPacket message, MessageContext ctx) {
            final EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            Container container = player.openContainer;
            if (!(container instanceof ContainerPatternTerm) && !(container instanceof ContainerPatternTermEx)
                && !(container instanceof ContainerFluidPatternTerminal)
                && !(container instanceof ContainerFluidPatternTerminalEx)) {
                return null;
            }

            try {
                IActionHost terminal = resolveTerminal(container);
                if (terminal == null) {
                    return null;
                }

                IGridNode node = terminal.getActionableNode();
                if (node == null) {
                    return null;
                }

                IGrid grid = node.getGrid();
                if (grid == null) {
                    return null;
                }

                List<Long> ids = new ArrayList<Long>();
                List<String> names = new ArrayList<String>();
                List<Integer> emptySlots = new ArrayList<Integer>();
                List<Boolean> canInstallCard = new ArrayList<Boolean>();

                for (Class<? extends IGridHost> hostClass : grid.getMachinesClasses()) {
                    if (!ICraftingProvider.class.isAssignableFrom(hostClass)) {
                        continue;
                    }

                    IMachineSet machines = grid.getMachines(hostClass);
                    if (machines == null) {
                        continue;
                    }

                    for (IGridNode machineNode : machines) {
                        if (machineNode == null) {
                            continue;
                        }

                        Object machine = machineNode.getMachine();
                        if (!(machine instanceof ICraftingProvider)) {
                            continue;
                        }

                        ICraftingProvider provider = (ICraftingProvider) machine;
                        long id = System.identityHashCode(provider);
                        String name = resolveProviderName(machine);

                        // 获取空槽位数量和是否可以装卡
                        SlotInfo slotInfo = getSlotInfo(provider);

                        // 显示所有 ICraftingProvider，包括：
                        // 1. 如果有空槽位（可以上传），正常显示
                        // 2. 如果没有空槽位，但可以装卡，显示（带装卡按钮）
                        // 3. 如果没有空槽位且不能装卡，也显示（禁用状态）
                        ids.add(id);
                        names.add(name);
                        emptySlots.add(slotInfo.emptySlots);
                        canInstallCard.add(slotInfo.canInstallCard);
                    }
                }

                ModNetwork.CHANNEL.sendTo(new ProvidersListS2CPacket(ids, names, emptySlots, canInstallCard), player);
            } catch (Throwable t) {
                t.printStackTrace();
            }

            return null;
        }

        private IActionHost resolveTerminal(Container container) {
            if (container instanceof ContainerPatternTerm term) {
                return term.getPatternTerminal();
            }
            if (container instanceof ContainerPatternTermEx termEx) {
                return termEx.getPatternTerminal();
            }
            if (container instanceof ContainerFluidPatternTerminal fluidTerm) {
                return fromPatternTerminal(fluidTerm.getPatternTerminal());
            }
            if (container instanceof ContainerFluidPatternTerminalEx fluidTermEx) {
                return fromPatternTerminal(fluidTermEx.getPatternTerminal());
            }
            return null;
        }

        private IActionHost fromPatternTerminal(IItemPatternTerminal terminal) {
            if (terminal instanceof IActionHost actionHost) {
                return actionHost;
            }
            return null;
        }

        /**
         * 槽位信息
         */
        private static class SlotInfo {

            int emptySlots; // 空槽位数量
            boolean canInstallCard; // 是否可以安装样板容量卡

            SlotInfo(int emptySlots, boolean canInstallCard) {
                this.emptySlots = emptySlots;
                this.canInstallCard = canInstallCard;
            }
        }

        /**
         * 获取接口的槽位信息
         */
        private SlotInfo getSlotInfo(ICraftingProvider provider) {
            if (provider instanceof IInterfaceHost host) {
                IInventory patterns = host.getPatterns();
                if (patterns != null) {
                    // 计算实际可用的槽位数量（基于升级卡）
                    int availableSlots = host.rows() * host.rowSize();
                    int limit = Math.min(availableSlots, patterns.getSizeInventory());
                    int empty = 0;
                    for (int i = 0; i < limit; i++) {
                        ItemStack slot = patterns.getStackInSlot(i);
                        if (slot == null || slot.stackSize <= 0) {
                            empty++;
                        }
                    }

                    // 检查是否可以安装更多样板容量卡
                    boolean canInstallCard = canInstallCapacityCard(host);

                    return new SlotInfo(empty, canInstallCard);
                }
            }
            if (provider instanceof IInventory inv) {
                int empty = 0;
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack slot = inv.getStackInSlot(i);
                    if (slot == null || slot.stackSize <= 0) {
                        empty++;
                    }
                }
                // 对于非IInterfaceHost，默认不能装卡
                return new SlotInfo(empty, false);
            }
            return new SlotInfo(0, false);
        }

        /**
         * 检查是否可以安装更多样板容量卡
         */
        private boolean canInstallCapacityCard(IInterfaceHost host) {
            try {
                // 获取当前已安装的样板容量卡数量
                int currentCards = host.getInstalledUpgrades(Upgrades.PATTERN_CAPACITY);

                // 获取升级槽位
                IInventory upgrades = host.getInterfaceDuality()
                    .getInventoryByName("upgrades");
                if (upgrades == null) {
                    return false;
                }

                // 检查是否还有空的升级槽位
                boolean hasEmptySlot = false;
                for (int i = 0; i < upgrades.getSizeInventory(); i++) {
                    ItemStack slot = upgrades.getStackInSlot(i);
                    if (slot == null || slot.stackSize <= 0) {
                        hasEmptySlot = true;
                        break;
                    }
                }

                if (!hasEmptySlot) {
                    return false;
                }

                // 检查是否达到最大数量
                int maxCards = 3; // 默认最大3个
                if (upgrades instanceof appeng.parts.automation.UpgradeInventory) {
                    maxCards = ((appeng.parts.automation.UpgradeInventory) upgrades)
                        .getMaxInstalled(Upgrades.PATTERN_CAPACITY);
                }

                return currentCards < maxCards;
            } catch (Throwable t) {
                return false;
            }
        }

        private String resolveProviderName(Object machine) {
            String name = null;
            if (machine instanceof TileEntity tile) {
                try {
                    if (tile.getBlockType() != null) {
                        name = tile.getBlockType()
                            .getLocalizedName();
                    }
                } catch (Throwable ignored) {}

                if (machine instanceof IInventory inv) {
                    try {
                        if (inv.hasCustomInventoryName()) {
                            name = inv.getInventoryName();
                        }
                    } catch (Throwable ignored) {}
                }
            }
            if (machine instanceof AEBasePart part) {
                try {
                    String customName = part.getCustomName();
                    if (customName != null && !customName.isEmpty()) {
                        name = customName;
                    }
                } catch (Throwable ignored) {}
            }
            // 如果没有获取到名字，使用默认的接口名字
            if (name == null || name.isEmpty()) {
                name = "ME Interface";
            }
            return name;
        }
    }
}
