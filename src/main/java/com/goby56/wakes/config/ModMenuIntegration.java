package com.goby56.wakes.config;

import com.goby56.wakes.WakesClient;
import com.goby56.wakes.utils.WakesUtils;
import com.terraformersmc.modmenu.api.ConfigScreenFactory;
import com.terraformersmc.modmenu.api.ModMenuApi;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConfirmScreen;
import net.minecraft.screen.ScreenTexts;
import net.minecraft.text.Text;
import net.minecraft.util.Util;

import java.net.URI;

public class ModMenuIntegration implements ModMenuApi {
    @Override
    public ConfigScreenFactory<?> getModConfigScreenFactory() {
        // Great idea taken from Benjamin Norton: https://github.com/Benjamin-Norton
        // Do a barrel roll is a sick mod. Go download: https://modrinth.com/mod/do-a-barrel-roll
        if (WakesClient.isYACLLoaded()) {
            return YACLIntegration::createScreen;
        } else {
            return parent -> new ConfirmScreen(result -> {
                if (result) {
                    Util.getOperatingSystem().open(URI.create("https://modrinth.com/mod/yacl/versions"));
                }
                MinecraftClient.getInstance().setScreen(parent);
            }, Text.translatable(String.format("%s.%s", WakesClient.MOD_ID, "yacl_missing")),
                    Text.translatable(String.format("%s.%s", WakesClient.MOD_ID, "yacl_missing.message")),
                    ScreenTexts.YES,
                    ScreenTexts.NO);
        }
        // https://github.com/enjarai/do-a-barrel-roll/blob/1.20/dev/src/main/java/nl/enjarai/doabarrelroll/compat/modmenu/ModMenuIntegration.java#L18
    }
}
