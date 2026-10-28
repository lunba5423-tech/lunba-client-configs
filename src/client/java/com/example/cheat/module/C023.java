package com.example.cheat.module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
/** Attacks whatever your crosshair is on once the cooldown is ready. With Crits on, it waits until the hit would be a critical. */
public class C023 extends C017 {
  public final C020.Bool crits = new C020.Bool("Crits", true);
  public final C020.Num cooldown = new C020.Num("Min cooldown %", 95, 50, 100);
  public final C020.Num delay = new C020.Num("Random delay (ticks)", 2, 0, 6);
  public final C020.Bool playersOnly = new C020.Bool("Players only", true);
  private int cd;
  public C023(){ super("Trigger Bot", C012.C014); settings.add(crits); settings.add(cooldown); settings.add(delay); settings.add(playersOnly); }
  @Override public void onTick(Minecraft mc){
    if (cd > 0) { cd--; return; }
    if (mc.screen != null || C014.acted(mc)) return;
    LivingEntity t = C014.target(mc); if (t == null) return;
    if (playersOnly.v && !(t instanceof Player)) return;
    var p = mc.player;
    if (p.getAttackStrengthScale(0.5f) < cooldown.v / 100.0) return;
    if (crits.v && !(p.fallDistance > 0 && !p.onGround() && !p.onClimbable() && !p.isInWater() && !p.isSprinting())) return;   // wait for a falling hit
    C014.hitWith(mc, p.getInventory().getSelectedSlot(), t); cd = C014.rand((int) delay.v); }
}
