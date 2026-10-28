package com.example.cheat.module;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

/** Keeps a Totem of Undying in the offhand: whenever the offhand isn't a totem, swap one in from the hotbar/inventory. */
public class C010 extends C017 {
  public final C020.Num delay = new C020.Num("Delay (ticks)", 0, 0, 10);
  public final C020.Bool searchInventory = new C020.Bool("Search inventory", true);   // off = hotbar only
  private int cd;

  public C010(){ super("Auto Totem", C012.C014); settings.add(delay); settings.add(searchInventory); }

  @Override public void onTick(Minecraft mc){
    if (cd > 0) { cd--; return; }
    if (mc.player.containerMenu != mc.player.inventoryMenu) return;          // another container (chest etc.) is open
    if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;       // already holding one

    Inventory inv = mc.player.getInventory();
    int found = -1;
    for (int i = 0; i < 9 && found < 0; i++) if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) found = i;                          // hotbar first
    if (found < 0 && searchInventory.v) for (int i = 9; i < 36 && found < 0; i++) if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) found = i;
    if (found < 0) return;

    int slotId = found < 9 ? 36 + found : found;     // inventory-menu slot ids: hotbar 36-44, main inventory 9-35
    // SWAP with button 40 exchanges that slot with the offhand
    mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slotId, 40, ClickType.SWAP, mc.player);
    cd = (int) delay.v;
  }
}
