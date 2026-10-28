package com.example.cheat;
import com.example.cheat.gui.C004;
import com.example.cheat.module.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import java.util.function.Supplier;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.world.WorldRenderEvents;
import net.minecraft.client.KeyMapping;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
public class C001 implements ClientModInitializer {
  public static final String NAME = "Lunba Client";   // change this to rename the client in chat
  public static final List<C017> MODS = new ArrayList<>();
  private static final Logger LOG = LoggerFactory.getLogger("cheatclient");
  public static KeyMapping.Category CAT;      // key binding category, shared with modules that have their own keys
  private static KeyMapping guiKey;
  private static int saveTimer;

  /** Registers a module; if initialization fails, it is skipped without a user-facing error message. */
  private static void add(String name, Supplier<C017> s){
    try { MODS.add(s.get()); }
    catch (Throwable ignored) { /* Skip modules that fail to initialize without showing an error message. */ } }

  @Override public void onInitializeClient(){
    CAT = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("cheatclient", "main"));
    add("Crystal Macro", C015::new);
    add("Anchor Macro", C006::new);
    add("Safe Anchor", C019::new);
    add("Auto Totem", C010::new);
    add("Auto Double Hand", C008::new);
    add("Shield Breaker", C021::new);
    add("Mace Hit", C016::new);
    add("Auto Stun Slam", C009::new);
    add("Auto Cart", C007::new);
    add("Cobweb Placer", C013::new);
    add("Trigger Bot", C023::new);
    add("Player ESP", C018::new);
    add("Tracers", C022::new);
    add("Block ESP", C011::new);

    guiKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cheatclient.gui", InputConstants.Type.KEYSYM, InputConstants.KEY_RSHIFT, CAT));
    C002.loadCurrent();
    LOG.info("{} loaded with {} modules", NAME, MODS.size());

    ClientTickEvents.END_CLIENT_TICK.register(mc -> {
      while (guiKey.consumeClick()) mc.setScreen(new C004());
      if (++saveTimer >= 100) { C002.saveCurrent(); saveTimer = 0; }
      for (C017 m : MODS) if (m.toggleKey.consumeClick()) m.toggle();
      if (mc.player == null || mc.level == null) return;
      for (C017 m : MODS) if (m.enabled) m.onTick(mc); });
    WorldRenderEvents.BEFORE_TRANSLUCENT.register(C024::render);
  }
}
