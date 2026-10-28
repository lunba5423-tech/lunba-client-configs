package com.example.cheat.module;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/**
 * Auto double hand: the moment you open your inventory, your hotbar (optionally also the offhand, see "Fill offhand")
 * switches to a totem slot. The offhand is left alone unless "Fill offhand" is switched on.
 * If no totem is in the hotbar (e.g. it just popped), it selects the slot a totem was last seen in, or your fallback slot.
 */
public class C008 extends C017 {
  public final C020.Bool fillOffhand = new C020.Bool("Fill offhand", false);
  public final C020.Bool selectSlot = new C020.Bool("Select totem slot", true);
  public final C020.Bool remember = new C020.Bool("Remember last slot", true);
  public final C020.Num fallback = new C020.Num("Fallback slot", 9, 1, 9);
  public final C020.Num delay = new C020.Num("Delay (ticks)", 0, 0, 5);
  private boolean wasOpen; private int wait = -1, lastSlot = -1;

  public C008(){ super("Auto Double Hand", C012.C014);
    settings.add(fillOffhand); settings.add(selectSlot); settings.add(remember); settings.add(fallback); settings.add(delay); }

  @Override public void onTick(Minecraft mc){
    Inventory inv = mc.player.getInventory();

    // remember where a hotbar totem was last seen, so the slot is still known after it pops
    if (remember.v) for (int i = 0; i < 9; i++) if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) { lastSlot = i; break; }

    boolean open = mc.screen instanceof InventoryScreen;
    if (open && !wasOpen) wait = (int) delay.v;                                 // inventory was just opened
    wasOpen = open;
    if (!open) { wait = -1; return; }
    if (wait < 0) return;
    if (wait > 0) { wait--; return; }
    wait = -1;
    if (mc.player.containerMenu != mc.player.inventoryMenu) return;

    // where are the totems?
    List<Integer> hotbar = new ArrayList<>(), main = new ArrayList<>();
    for (int i = 0; i < 9; i++) if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) hotbar.add(i);
    for (int i = 9; i < 36; i++) if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) main.add(i);

    int mainSlot = hotbar.isEmpty() ? -1 : hotbar.get(0);                       // totem we'll hold in the main hand

    if (fillOffhand.v && !mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) {
      int src = -1;
      if (!main.isEmpty()) src = main.get(0);                                   // prefer inventory totems, so the hotbar one stays for the main hand
      else if (hotbar.size() > 1) src = hotbar.get(hotbar.size() - 1);          // otherwise use a spare hotbar totem
      else if (hotbar.size() == 1) { src = hotbar.get(0); mainSlot = -1; }      // only one totem: the offhand wins
      if (src >= 0) {
        int slotId = src < 9 ? 36 + src : src;                                  // inventory-menu ids: hotbar 36-44, main 9-35
        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slotId, 40, ClickType.SWAP, mc.player); } }

    if (selectSlot.v) {
      int slot = mainSlot >= 0 ? mainSlot : (remember.v && lastSlot >= 0 ? lastSlot : (int) fallback.v - 1);
      inv.setSelectedSlot(slot); }
  }
}
