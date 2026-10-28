package com.example.cheat.module;
import com.example.cheat.C024;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.AbstractClientPlayer;
public class C022 extends C017 {
  public final C020.Color color = new C020.Color("Colour", 0xFFAA00);
  public final C020.Num width = new C020.Num("Width", 5, 1, 15);
  public final C020.Num opacity = new C020.Num("Opacity %", 100, 10, 100);
  public C022(){ super("Tracers", C012.RENDER); settings.add(color); settings.add(width); settings.add(opacity); }
  @Override public void onRender(Minecraft mc, float td){
    for (AbstractClientPlayer p : mc.level.players()) {
      if (p == mc.player || p.isSpectator()) continue;
      C024.tracer(mc, td, C024.lerpBox(p, td).getCenter(), color.rgb, width.v, (float) (opacity.v / 100.0)); } }
}
