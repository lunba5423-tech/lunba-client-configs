package com.example.cheat.module;
import java.util.function.Predicate;
import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.EntityHitResult;

/** Shared helpers for the attack modules, so two modules never attack in the same tick. */
public final class C014 {
  private static long lastTick = -1;
  private C014(){}
  public static boolean acted(Minecraft mc){ return mc.level.getGameTime() == lastTick; }

  public static LivingEntity target(Minecraft mc){
    return mc.hitResult instanceof EntityHitResult e && e.getEntity() instanceof LivingEntity l && l != mc.player && l.isAlive() ? l : null; }

  public static int find(Inventory inv, Predicate<ItemStack> p){
    for (int i = 0; i < 9; i++) if (p.test(inv.getItem(i))) return i;
    return -1; }

  public static int rand(int max){ return max <= 0 ? 0 : java.util.concurrent.ThreadLocalRandom.current().nextInt(max + 1); }

  /** Selects a hotbar slot, attacks, and puts the previously selected slot back. */
  public static void hitWith(Minecraft mc, int slot, Entity target){
    Inventory inv = mc.player.getInventory(); int prev = inv.getSelectedSlot();
    inv.setSelectedSlot(slot);
    mc.gameMode.attack(mc.player, target);
    mc.player.swing(InteractionHand.MAIN_HAND);
    inv.setSelectedSlot(prev);
    lastTick = mc.level.getGameTime(); }
}
