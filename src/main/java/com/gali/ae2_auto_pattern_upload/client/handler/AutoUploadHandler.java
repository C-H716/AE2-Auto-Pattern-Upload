package com.gali.ae2_auto_pattern_upload.client.handler;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.inventory.Container;
import net.minecraft.item.ItemStack;

import com.gali.ae2_auto_pattern_upload.config.AutoUploadTargetConfig;
import com.gali.ae2_auto_pattern_upload.network.AutoUploadPatternPacket;
import com.gali.ae2_auto_pattern_upload.network.ModNetwork;
import com.gali.ae2_auto_pattern_upload.util.RecipeNameUtil;
import com.glodblock.github.client.gui.GuiFluidPatternTerminal;
import com.glodblock.github.client.gui.GuiFluidPatternTerminalEx;
import com.glodblock.github.client.gui.container.base.FCContainerEncodeTerminal;

import appeng.client.gui.implementations.GuiPatternTerm;
import appeng.client.gui.implementations.GuiPatternTermEx;
import appeng.container.implementations.ContainerPatternTerm;
import appeng.container.implementations.ContainerPatternTermEx;
import appeng.container.slot.SlotRestrictedInput;
import cpw.mods.fml.common.FMLCommonHandler;
import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.TickEvent;

/**
 * 自动上传处理器 - 监视配方编写并自动上传
 */
public class AutoUploadHandler {

    private static final AutoUploadHandler INSTANCE = new AutoUploadHandler();

    // 记录上一个输出槽的状态，用于检测变化
    private ItemStack lastOutputStack = null;
    private int cooldownTicks = 0;
    private static final int COOLDOWN = 5; // 冷却时间，防止重复触发
    private boolean initialized = false; // 标记是否已完成初始化（防止打开界面时误判）

    public static void register() {
        FMLCommonHandler.instance()
            .bus()
            .register(INSTANCE);
    }

    @SubscribeEvent
    public void onClientTick(TickEvent.ClientTickEvent event) {
        if (event.phase != TickEvent.Phase.END) {
            return;
        }

        if (cooldownTicks > 0) {
            cooldownTicks--;
            return;
        }

        Minecraft mc = Minecraft.getMinecraft();
        if (mc == null || mc.thePlayer == null) {
            return;
        }

        GuiScreen currentScreen = mc.currentScreen;
        if (!(currentScreen instanceof GuiPatternTerm) && !(currentScreen instanceof GuiPatternTermEx)
            && !(currentScreen instanceof GuiFluidPatternTerminal)
            && !(currentScreen instanceof GuiFluidPatternTerminalEx)) {
            // 不在配方终端界面，重置状态
            lastOutputStack = null;
            initialized = false;
            return;
        }

        Container container = mc.thePlayer.openContainer;
        SlotRestrictedInput outputSlot = resolveOutputSlot(container);
        if (outputSlot == null) {
            return;
        }

        ItemStack currentOutput = outputSlot.getStack();

        // 首次进入界面时，只记录状态，不触发上传
        if (!initialized) {
            lastOutputStack = currentOutput;
            initialized = true;
            return;
        }

        // 检测是否刚刚完成编码（输出槽从空变为有物品）
        if ((lastOutputStack == null || lastOutputStack.stackSize == 0) && currentOutput != null
            && currentOutput.stackSize > 0) {
            // 有配方被编码，尝试自动上传
            tryAutoUpload();
        }

        lastOutputStack = currentOutput;
    }

    private void tryAutoUpload() {
        // Get last captured recipe name
        String recipeKey = RecipeNameUtil.getLastRecipeName();
        String rawRecipeId = RecipeNameUtil.getLastRawRecipeId();

        if (recipeKey == null || recipeKey.isEmpty()) {
            return;
        }

        // 在客户端查找映射
        String mappedName = findMappedName(recipeKey, rawRecipeId);

        if (mappedName == null || mappedName.isEmpty()) {
            return;
        }

        // Get target provider names from client config
        List<String> targetProviderNames = new ArrayList<>(AutoUploadTargetConfig.getTargetProviders());

        // Send auto upload request with mapped name and target provider names
        ModNetwork.CHANNEL.sendToServer(new AutoUploadPatternPacket(mappedName, targetProviderNames));

        // Set cooldown
        cooldownTicks = COOLDOWN;

        // Clear used recipe name
        // RecipeNameUtil.clearLastRecipeName();
    }

    /**
     * 在客户端查找映射名称
     */
    private String findMappedName(String recipeKey, String rawRecipeId) {
        Map<String, String> mappings = RecipeNameUtil.getMappingsView();

        // 首先尝试 rawRecipeId
        if (rawRecipeId != null && !rawRecipeId.isEmpty()) {
            String mapped = mappings.get(rawRecipeId);
            if (mapped != null && !mapped.isEmpty()) {
                return mapped;
            }
        }

        // 然后尝试 recipeKey
        if (recipeKey != null && !recipeKey.isEmpty()) {
            String mapped = mappings.get(recipeKey);
            if (mapped != null && !mapped.isEmpty()) {
                return mapped;
            }
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
            if (container instanceof FCContainerEncodeTerminal terminal) {
                Field field = FCContainerEncodeTerminal.class.getDeclaredField("patternSlotOUT");
                field.setAccessible(true);
                return (SlotRestrictedInput) field.get(terminal);
            }
        } catch (Exception ignored) {}
        return null;
    }
}
