package com.example.cheat.module;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;

/**
 * Stun slam: while falling onto a player who is blocking with a shield,
 * hit with an axe (disables the shield), then follow up with a mace smash.
 * Needs an axe and a mace in the hotbar. Turn Shield Breaker / Mace Hit off while using this.
 */
public class C009 extends C017 {
  public final C020.Num minFall = new C020.Num("Min fall (blocks)", 3, 2, 20);
  public final C020.Num gap = new C020.Num("Gap axe > mace (ticks)", 1, 0, 5);
  private int stage, wait, timeout;

  public C009(){ super("Auto Stun Slam", C012.C014); settings.add(minFall); settings.add(gap); }

  @Override public void onTick(Minecraft mc){
    if (mc.screen != null || C014.acted(mc)) return;
    var inv = mc.player.getInventory();

    if (stage == 0) {
      if (mc.player.onGround() || mc.player.fallDistance < minFall.v) return;
      LivingEntity t = C014.target(mc);
      if (!(t instanceof Player p) || !p.isBlocking()) return;
      int axe = C014.find(inv, s -> s.is(ItemTags.AXES)), mace = C014.find(inv, s -> s.is(Items.MACE));
      if (axe < 0 || mace < 0) return;
      C014.hitWith(mc, axe, t);                       // step 1: axe breaks the shield
      stage = 1; wait = (int) gap.v; timeout = 12; return; }

    // step 2: mace smash on the next valid tick
    if (--timeout <= 0 || mc.player.onGround()) { stage = 0; return; }
    if (wait > 0) { wait--; return; }
    LivingEntity t = C014.target(mc); if (t == null) return;
    int mace = C014.find(inv, s -> s.is(Items.MACE)); if (mace < 0) { stage = 0; return; }
    C014.hitWith(mc, mace, t); stage = 0;
  }
}
