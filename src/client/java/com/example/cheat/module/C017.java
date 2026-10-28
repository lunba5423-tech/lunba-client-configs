package com.example.cheat.module;
import com.example.cheat.C001;
import java.util.*;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.Minecraft;
import net.minecraft.client.KeyMapping;
import com.mojang.blaze3d.platform.InputConstants;
import org.lwjgl.glfw.GLFW;
public abstract class C017 {
  public final String name; public final C012 category; public boolean enabled;
  public final KeyMapping toggleKey;
  public final List<C020> settings = new ArrayList<>();
  protected C017(String n, C012 c){
    name=n; category=c;
    String bindName = n.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", ".");
    toggleKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cheatclient.toggle." + bindName,
      InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, C001.CAT));
  }
  public void toggle(){ enabled=!enabled; }
  public void onTick(Minecraft mc){}
  /** Add geometry through Renderer.box / Renderer.tracer. td = partial tick. */
  public void onRender(Minecraft mc, float td){}
}
