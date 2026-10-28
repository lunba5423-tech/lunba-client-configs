package com.example.cheat.module;
import net.minecraft.client.Minecraft;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
/** If the player you're aiming at is blocking with a shield, hit them with an axe from your hotbar, then switch back. */
public class C021 extends C017 {
  public final C020.Num delay = new C020.Num("Random delay (ticks)", 1, 0, 6); private int cd;
  public C021(){ super("Shield Breaker", C012.C014); settings.add(delay); }
  @Override public void onTick(Minecraft mc){
    if (cd > 0) { cd--; return; }
    if (mc.screen != null || C014.acted(mc)) return;
    LivingEntity t = C014.target(mc);
    if (!(t instanceof Player p) || !p.isBlocking()) return;
    int axe = C014.find(mc.player.getInventory(), s -> s.is(ItemTags.AXES)); if (axe < 0) return;
    C014.hitWith(mc, axe, t); cd = C014.rand((int) delay.v); }
}
