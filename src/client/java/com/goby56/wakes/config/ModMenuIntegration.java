package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.utils.WakesUtils;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.util.Util;

import java.net.URI;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Concept taken from @enjarai
        // https://github.com/enjarai/do-a-barrel-roll/blob/1.20/dev/src/main/java/nl/enjarai/doabarrelroll/compat/modmenu/ModMenuIntegration.java#L18
        if (WakesClient.isYACLLoaded()) {
            return YACLIntegration::createScreen;
        } else {
            return parent -> new ConfirmScreen(result -> {
                if (result) {
                    Util.getOperatingSystem().open(URI.create("https://modrinth.com/mod/yacl/versions"));
                }
                MinecraftClient.getInstance().setScreen(parent);
            }, WakesUtils.translatable("config", "yacl_missing"),
                    WakesUtils.translatable("config", "yacl_missing.message"),
                    ScreenTexts.YES,
                    ScreenTexts.NO);
        }
    }
}
