package com.example.cheat.module;
import com.example.cheat.C024;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
public class C018 extends C017 {
  public final C020.Color color = new C020.Color("Colour", 0xFF5555);
  public C018(){ super("Player ESP", C012.RENDER); settings.add(color); }
  @Override public void onRender(Minecraft mc, float td){
    for (AbstractClientPlayer p : mc.level.players()) {          // sneaking makes no difference
      if (p == mc.player || p.isSpectator()) continue;
      C024.box(C024.lerpBox(p, td), color.rgb, true); } }
}
