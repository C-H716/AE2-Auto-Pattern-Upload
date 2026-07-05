package com.gali.ae2_auto_pattern_upload.mixin.vp;

import com.sinthoras.visualprospecting.database.veintypes.VeinType;
import com.sinthoras.visualprospecting.database.veintypes.VeinTypeCaching;
import cpw.mods.fml.common.Loader;
import net.minecraft.util.EnumChatFormatting;
import net.moecraft.nechar.NecharUtils;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Mixin(value = VeinTypeCaching.class, remap = false)
public abstract class VeinTypeCachingMixin {

    @Inject(method = "recalculateSearch", at = @At("HEAD"), cancellable = true)
    private static void ae2_auto_pattern_upload$recalculateSearchWithNechar(Pattern filterPattern, CallbackInfo ci) {
        for (VeinType veinType : VeinTypeCaching.getVeinTypes()) {
            if (veinType == VeinType.NO_VEIN) continue;
            if (filterPattern != null) {
                List<String> searchableStrings = new ArrayList<String>(veinType.getOreMaterialNames());
                searchableStrings.add(veinType.getVeinName());
                final String query = filterPattern.pattern().replace("\\Q", "").replace("\\E", "");
                final boolean match = searchableStrings.stream().map(EnumChatFormatting::getTextWithoutFormattingCodes)
                    .filter(searchableString -> searchableString != null && !searchableString.isEmpty())
                    .anyMatch(searchableString -> ae2_auto_pattern_upload$matchesSearch(searchableString, query, filterPattern));

                veinType.setNEISearchHighlight(match);
            } else {
                veinType.setNEISearchHighlight(true);
            }
        }
        ci.cancel();
    }

    private static boolean ae2_auto_pattern_upload$matchesSearch(String name, String query, Pattern filterPattern) {
        if (filterPattern.matcher(name.toLowerCase()).find()) {
            return true;
        }
        if (Loader.isModLoaded("nechar")) {
            try {
                return NecharUtils.contain(name, query, true);
            } catch (Throwable ignored) {}
        }
        return false;
    }
}
