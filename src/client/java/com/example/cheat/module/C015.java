package com.example.cheat.module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.phys.EntityHitResult;
public class C015 extends C017 {
  public final C020.Num delay = new C020.Num("Delay (ticks)", 1, 0, 10); private int cd;
  public C015(){ super("Crystal Macro", C012.C014); settings.add(delay); }
  /** Hold RMB: vanilla places crystals, this breaks the one you're looking at. */
  @Override public void onTick(Minecraft mc){
    if (cd > 0) { cd--; return; }
    if (!mc.options.keyUse.isDown() || mc.screen != null) return;
    if (mc.hitResult instanceof EntityHitResult e && e.getEntity() instanceof EndCrystal c) {
      mc.gameMode.attack(mc.player, c); mc.player.swing(InteractionHand.MAIN_HAND); cd = (int) delay.v; } }
}
