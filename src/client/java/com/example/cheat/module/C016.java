package com.example.cheat.module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Items;
/** While falling onto a target, switch to a hotbar mace, smash, and switch back. */
public class C016 extends C017 {
  public final C020.Num minFall = new C020.Num("Min fall (blocks)", 3, 2, 20);
  public final C020.Bool playersOnly = new C020.Bool("Players only", true);
  public final C020.Num delay = new C020.Num("Random delay (ticks)", 1, 0, 6); private int cd;
  public C016(){ super("Mace Hit", C012.C014); settings.add(minFall); settings.add(playersOnly); settings.add(delay); }
  @Override public void onTick(Minecraft mc){
    if (cd > 0) { cd--; return; }
    if (mc.screen != null || C014.acted(mc)) return;
    if (mc.player.onGround() || mc.player.fallDistance < minFall.v) return;
    LivingEntity t = C014.target(mc); if (t == null) return;
    if (playersOnly.v && !(t instanceof net.minecraft.world.entity.player.Player)) return;
    int mace = C014.find(mc.player.getInventory(), s -> s.is(Items.MACE)); if (mace < 0) return;
    C014.hitWith(mc, mace, t); cd = C014.rand((int) delay.v); }
}
