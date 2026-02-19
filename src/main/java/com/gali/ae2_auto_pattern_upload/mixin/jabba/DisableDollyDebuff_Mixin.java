package com.gali.ae2_auto_pattern_upload.mixin.jabba;

import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import mcp.mobius.betterbarrels.common.items.dolly.ItemBarrelMover;

@Mixin(value = ItemBarrelMover.class)
public class DisableDollyDebuff_Mixin {

    @Inject(
        method = "onUpdate(Lnet/minecraft/item/ItemStack;Lnet/minecraft/world/World;Lnet/minecraft/entity/Entity;IZ)V",
        at = @At("HEAD"),
        cancellable = true)
    private void NHUtilities$disableDebuff(ItemStack stack, World world, Entity entity, int par4, boolean par5,
        CallbackInfo ci) {
        // 如果世界不是客户端，且物品有容器NBT标签，且实体是玩家，则取消整个onUpdate方法
        // 这样可以完全禁用debuff效果
        if (!world.isRemote && stack.hasTagCompound()
            && stack.getTagCompound()
                .hasKey("Container")
            && entity instanceof EntityPlayer) {
            ci.cancel();
        }
    }
}
