package com.gali.ae2_auto_pattern_upload.network;

import static com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil.normalizeKey;

import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.inventory.Container;
import net.minecraft.inventory.IInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.tileentity.TileEntity;
import net.minecraft.util.ChatComponentText;
import net.minecraft.util.StatCollector;

import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminal;
import com.glodblock.github.client.gui.container.ContainerFluidPatternTerminalEx;
import com.glodblock.github.client.gui.container.base.FCContainerEncodeTerminal;
import com.glodblock.github.common.item.ItemFluidEncodedPattern;
import com.glodblock.github.inventory.item.IItemPatternTerminal;

import appeng.api.AEApi;
import appeng.api.networking.IGrid;
import appeng.api.networking.IGridHost;
import appeng.api.networking.IGridNode;
import appeng.api.networking.IMachineSet;
import appeng.api.networking.crafting.ICraftingProvider;
import appeng.api.networking.security.IActionHost;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.container.slot.SlotRestrictedInput;
import appeng.helpers.IInterfaceHost;
import appeng.parts.AEBasePart;
import cpw.mods.fml.common.network.simpleimpl.IMessage;
import cpw.mods.fml.common.network.simpleimpl.IMessageHandler;
import cpw.mods.fml.common.network.simpleimpl.MessageContext;
import io.netty.buffer.ByteBuf;

/**
 * 自动上传样板数据包 - 根据配方名称映射自动匹配供应器
 * 客户端发送映射后的名称和目标供应器名称列表，服务器根据列表进行匹配
 */
public class AutoUploadPatternPacket implements IMessage {

    private String mappedName; // 客户端查找后的映射名称
    private List<String> targetProviderNames; // 客户端设置的目标供应器名称列表

    public AutoUploadPatternPacket() {
        this.targetProviderNames = new ArrayList<>();
    }

    public AutoUploadPatternPacket(String mappedName, List<String> targetProviderNames) {
        this.mappedName = mappedName;
        this.targetProviderNames = targetProviderNames != null ? targetProviderNames : new ArrayList<>();
    }

    @Override
    public void fromBytes(ByteBuf buf) {
        // 读取映射后的名称
        int len1 = buf.readShort();
        byte[] bytes1 = new byte[len1];
        buf.readBytes(bytes1);
        this.mappedName = new String(bytes1, StandardCharsets.UTF_8);

        // 读取目标供应器名称列表
        int targetCount = buf.readShort();
        this.targetProviderNames = new ArrayList<>();
        for (int i = 0; i < targetCount; i++) {
            int nameLen = buf.readShort();
            byte[] nameBytes = new byte[nameLen];
            buf.readBytes(nameBytes);
            this.targetProviderNames.add(new String(nameBytes, StandardCharsets.UTF_8));
        }
    }

    @Override
    public void toBytes(ByteBuf buf) {
        // 写入映射后的名称
        byte[] bytes1 = mappedName.getBytes(StandardCharsets.UTF_8);
        buf.writeShort(bytes1.length);
        buf.writeBytes(bytes1);

        // 写入目标供应器名称列表
        buf.writeShort(targetProviderNames.size());
        for (String name : targetProviderNames) {
            byte[] nameBytes = name.getBytes(StandardCharsets.UTF_8);
            buf.writeShort(nameBytes.length);
            buf.writeBytes(nameBytes);
        }
    }

    public static class Handler implements IMessageHandler<AutoUploadPatternPacket, IMessage> {

        @Override
        public IMessage onMessage(AutoUploadPatternPacket message, MessageContext ctx) {
            EntityPlayerMP player = ctx.getServerHandler().playerEntity;
            if (player == null) {
                return null;
            }

            Container container = player.openContainer;
            IActionHost terminal = resolveTerminal(container);
            if (terminal == null) {
                return null;
            }

            SlotRestrictedInput outputSlot = resolveOutputSlot(container);
            if (outputSlot == null) {
                return null;
            }

            ItemStack encodedPattern = outputSlot.getStack();
            if (encodedPattern == null || encodedPattern.stackSize <= 0) {
                return null;
            }

            if (!isSupportedPattern(encodedPattern)) {
                return null;
            }

            try {
                IGridNode node = terminal.getActionableNode();
                if (node == null) {
                    return null;
                }
                IGrid grid = node.getGrid();
                if (grid == null) {
                    return null;
                }

                // 直接使用客户端发送的映射名称
                String mappedName = message.mappedName;
                if (mappedName == null || mappedName.isEmpty()) {
                    return null;
                }

                // Find matching provider (using target list from client)
                ProviderMatchResult matchResult = findMatchingProvider(grid, mappedName, message.targetProviderNames);

                // Process based on match result
                if (matchResult.matchType == MatchType.EXACT_MATCH) {
                    // Exact match, execute upload
                    boolean placedInProvider = insertPatternIntoProvider(matchResult.provider, encodedPattern.copy());

                    if (placedInProvider) {
                        outputSlot.putStack(null);
                        if (terminal instanceof AEBasePart part) {
                            part.saveChanges();
                        }
                        // Send success message
                        sendSuccessMessage(player, mappedName);
                    }
                }
                // PARTIAL_MATCH and NO_MATCH do nothing
            } catch (Throwable ignored) {}

            return null;
        }

        private void sendSuccessMessage(EntityPlayerMP player, String providerName) {
            if (player != null) {
                String msg = String.format(
                    StatCollector.translateToLocal("ae2_auto_pattern_upload.info.auto_upload_success"),
                    providerName);
                player.addChatMessage(new ChatComponentText(msg));
            }
        }

        private enum MatchType {
            NO_MATCH, // 没有匹配
            PARTIAL_MATCH, // 部分匹配（如"压印器-1"）
            EXACT_MATCH // 完全匹配
        }

        private static class ProviderMatchResult {

            final ICraftingProvider provider;
            final MatchType matchType;

            ProviderMatchResult(ICraftingProvider provider, MatchType matchType) {
                this.provider = provider;
                this.matchType = matchType;
            }
        }

        private ProviderMatchResult findMatchingProvider(IGrid grid, String mappedName,
            List<String> targetProviderNames) {
            String normalizedMappedName = normalizeKey(mappedName);

            // Collect all providers
            List<ProviderInfo> allProviders = new ArrayList<>();
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
                    if (!(machine instanceof ICraftingProvider provider)) {
                        continue;
                    }

                    String providerName = resolveProviderName(machine);
                    String normalizedProviderName = normalizeKey(providerName);
                    boolean hasEmpty = hasEmptySlot(provider);

                    ProviderInfo info = new ProviderInfo(provider, providerName, normalizedProviderName, hasEmpty);
                    allProviders.add(info);
                }
            }

            if (allProviders.isEmpty()) {
                return new ProviderMatchResult(null, MatchType.NO_MATCH);
            }

            // Step 1: Check for exact match with target providers
            List<ProviderInfo> exactMatches = new ArrayList<>();
            for (ProviderInfo info : allProviders) {
                if (info.normalizedName.equals(normalizedMappedName) && targetProviderNames.contains(info.name)) {
                    exactMatches.add(info);
                }
            }

            if (!exactMatches.isEmpty()) {
                // Check for variants (other providers starting with mapped name)
                for (ProviderInfo info : allProviders) {
                    if (!info.normalizedName.equals(normalizedMappedName)
                        && info.normalizedName.startsWith(normalizedMappedName)) {
                        // Variants exist, skip upload
                        return new ProviderMatchResult(null, MatchType.PARTIAL_MATCH);
                    }
                }

                // Select from exact matches with empty slots
                for (ProviderInfo info : exactMatches) {
                    if (info.hasEmptySlot) {
                        return new ProviderMatchResult(info.provider, MatchType.EXACT_MATCH);
                    }
                }

                // No exact match providers have empty slots
                return new ProviderMatchResult(null, MatchType.NO_MATCH);
            }

            // Step 2: Try fuzzy match (when no exact match)
            // Fuzzy match rule: provider name must start with mapped name
            List<ProviderInfo> fuzzyMatches = new ArrayList<>();
            for (ProviderInfo info : allProviders) {
                boolean isFuzzyMatch = info.normalizedName.startsWith(normalizedMappedName)
                    && targetProviderNames.contains(info.name);

                if (isFuzzyMatch) {
                    fuzzyMatches.add(info);
                }
            }

            // If more than 1 fuzzy match, variants exist, skip upload
            if (fuzzyMatches.size() > 1) {
                return new ProviderMatchResult(null, MatchType.PARTIAL_MATCH);
            }

            // Only one fuzzy match, check for empty slot
            if (fuzzyMatches.size() == 1) {
                ProviderInfo info = fuzzyMatches.get(0);
                if (info.hasEmptySlot) {
                    return new ProviderMatchResult(info.provider, MatchType.EXACT_MATCH);
                } else {
                    return new ProviderMatchResult(null, MatchType.NO_MATCH);
                }
            }

            return new ProviderMatchResult(null, MatchType.NO_MATCH);
        }

        private static class ProviderInfo {

            final ICraftingProvider provider;
            final String name;
            final String normalizedName;
            final boolean hasEmptySlot;

            ProviderInfo(ICraftingProvider provider, String name, String normalizedName, boolean hasEmptySlot) {
                this.provider = provider;
                this.name = name;
                this.normalizedName = normalizedName;
                this.hasEmptySlot = hasEmptySlot;
            }
        }

        private boolean hasEmptySlot(ICraftingProvider provider) {
            if (provider instanceof IInterfaceHost host) {
                IInventory patterns = host.getPatterns();
                if (patterns != null) {
                    int availableSlots = host.rows() * host.rowSize();
                    int limit = Math.min(availableSlots, patterns.getSizeInventory());
                    for (int i = 0; i < limit; i++) {
                        ItemStack slot = patterns.getStackInSlot(i);
                        if (slot == null || slot.stackSize <= 0) {
                            return true;
                        }
                    }
                }
                return false;
            }
            if (provider instanceof IInventory inv) {
                for (int i = 0; i < inv.getSizeInventory(); i++) {
                    ItemStack slot = inv.getStackInSlot(i);
                    if (slot == null || slot.stackSize <= 0) {
                        return true;
                    }
                }
            }
            return false;
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
            if (name == null || name.isEmpty()) {
                name = "ME Interface";
            }
            return name;
        }

        private boolean isSupportedPattern(ItemStack stack) {
            if (stack == null) {
                return false;
            }
            if (AEApi.instance()
                .definitions()
                .items()
                .encodedPattern()
                .isSameAs(stack)) {
                return true;
            }
            return stack.getItem() instanceof ItemFluidEncodedPattern;
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

        private SlotRestrictedInput resolveOutputSlot(Container container) {
            try {
                if (container instanceof ContainerPatternTerm term) {
                    Field field = ContainerPatternTerm.class.getDeclaredField("patternSlotOUT");
                    field.setAccessible(true);
                    return (SlotRestrictedInput) field.get(term);
                }
                if (container instanceof ContainerPatternTermEx termEx) {
                    Field field = ContainerPatternTermEx.class.getDeclaredField("patternSlotOUT");
                    field.setAccessible(true);
                    return (SlotRestrictedInput) field.get(termEx);
                }
                if (container instanceof FCContainerEncodeTerminal fcContainer) {
                    Field field = FCContainerEncodeTerminal.class.getDeclaredField("patternSlotOUT");
                    field.setAccessible(true);
                    return (SlotRestrictedInput) field.get(fcContainer);
                }
            } catch (Exception ignored) {}
            return null;
        }

        private boolean insertPatternIntoProvider(ICraftingProvider provider, ItemStack pattern) {
            if (provider instanceof IInterfaceHost host) {
                IInventory patterns = host.getPatterns();
                if (patterns != null) {
                    int availableSlots = host.rows() * host.rowSize();
                    if (insertIntoPatternInventory(patterns, pattern, availableSlots)) {
                        host.saveChanges();
                        return true;
                    }
                }
                return false;
            }

            if (provider instanceof IInventory inventory) {
                return insertIntoInventory(inventory, pattern);
            }

            return false;
        }

        private boolean insertIntoPatternInventory(IInventory patterns, ItemStack pattern, int maxSlots) {
            if (patterns == null) {
                return false;
            }

            int limit = Math.min(maxSlots, patterns.getSizeInventory());
            for (int i = 0; i < limit; i++) {
                ItemStack slot = patterns.getStackInSlot(i);
                if (slot == null || slot.stackSize <= 0) {
                    if (patterns.isItemValidForSlot(i, pattern)) {
                        ItemStack copy = pattern.copy();
                        copy.stackSize = 1;
                        patterns.setInventorySlotContents(i, copy);
                        patterns.markDirty();
                        return true;
                    }
                }
            }

            return false;
        }

        private boolean insertIntoInventory(IInventory inventory, ItemStack pattern) {
            if (inventory == null) {
                return false;
            }

            for (int i = 0; i < inventory.getSizeInventory(); i++) {
                ItemStack slot = inventory.getStackInSlot(i);
                if (slot == null || slot.stackSize <= 0) {
                    ItemStack copy = pattern.copy();
                    copy.stackSize = 1;
                    inventory.setInventorySlotContents(i, copy);
                    inventory.markDirty();
                    return true;
                }
            }

            return false;
        }
    }
}
