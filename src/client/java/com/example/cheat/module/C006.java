package com.example.cheat.module;
import com.example.cheat.C001;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Two modes:
 *  - Automatic (default): charges and explodes an anchor while RMB is held on it, and right after you place one ("On place").
 *  - "Use on bind": nothing happens on its own. Press the bound key and it runs the full sequence:
 *    switch to anchor > place it on the block you're aiming at > switch to glowstone > charge (Charges times)
 *    > switch to the totem slot > explode. "Instant" does it all in one tick, otherwise one step every Delay ticks.
 *  The totem slot is the one a totem was last seen in ("Remember totem"), or the Fallback slot, so it still works after a pop.
 */
public class C006 extends C017 {
  public final C020.Num delay = new C020.Num("Delay (ticks)", 0, 0, 10);
  public final C020.Num charges = new C020.Num("Charges", 1, 1, 4);
  public final C020.Bool instant = new C020.Bool("Instant", true);
  public final C020.Bool onPlace = new C020.Bool("On place", true);
  public final C020.Bool rememberTotem = new C020.Bool("Remember totem", true);
  public final C020.Num explodeSlot = new C020.Num("Fallback slot", 2, 1, 9);   // used until a totem has been seen
  public final C020.Bool useBind = new C020.Bool("Use on bind", false);
  public final KeyMapping key;

  private int cd, placeTimer, totemSlot = -1;
  // bind-mode sequence state
  private int stage, wait, done, want; private BlockPos anchorPos; private BlockHitResult placeHit;

  public C006(){ super("Anchor Macro", C012.C014);
    settings.add(delay); settings.add(charges); settings.add(instant); settings.add(onPlace);
    settings.add(rememberTotem); settings.add(explodeSlot); settings.add(useBind);
    key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cheatclient.anchor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, C001.CAT)); }

  @Override public void toggle(){ super.toggle(); while (key.consumeClick()) { } stage = 0; }

  private void say(Minecraft mc, String m){ }
  private int explodeSlot(){ return (rememberTotem.v && totemSlot >= 0) ? totemSlot : (int) explodeSlot.v - 1; }
  private BlockHitResult anchorHit(){ return new BlockHitResult(Vec3.atCenterOf(anchorPos), Direction.UP, anchorPos, false); }
  private void use(Minecraft mc, Inventory inv, BlockHitResult hit, int slot){
    inv.setSelectedSlot(slot);
    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
    mc.player.swing(InteractionHand.MAIN_HAND); }

  @Override public void onTick(Minecraft mc){
    Inventory inv = mc.player.getInventory();

    // remember the slot a totem sits in; kept after the totem is used up
    if (rememberTotem.v) for (int i = 0; i < 9; i++) if (inv.getItem(i).is(Items.TOTEM_OF_UNDYING)) { totemSlot = i; break; }

    if (useBind.v) { bindMode(mc, inv); return; }
    stage = 0; while (key.consumeClick()) { }                 // bind mode is off: ignore the key

    // ---- automatic mode ----
    if (cd > 0) { cd--; return; }
    boolean down = mc.options.keyUse.isDown() && mc.screen == null;
    if (down && inv.getItem(inv.getSelectedSlot()).is(Items.RESPAWN_ANCHOR)) placeTimer = 12;   // you're placing an anchor
    else if (placeTimer > 0) placeTimer--;
    if (!(down || (onPlace.v && placeTimer > 0))) return;

    if (!(mc.hitResult instanceof BlockHitResult hit)) return;
    BlockState st = mc.level.getBlockState(hit.getBlockPos());
    if (!st.is(Blocks.RESPAWN_ANCHOR)) return;

    int have = st.getValue(RespawnAnchorBlock.CHARGE), wantNow = (int) charges.v;
    int glow = -1; for (int i = 0; i < 9; i++) if (inv.getItem(i).is(Items.GLOWSTONE)) { glow = i; break; }
    if (have < wantNow && glow >= 0) {
      if (instant.v) { for (int c = have; c < wantNow; c++) use(mc, inv, hit, glow); use(mc, inv, hit, explodeSlot()); }
      else use(mc, inv, hit, glow);
    } else if (have > 0) use(mc, inv, hit, explodeSlot());
    else return;
    cd = (int) delay.v;
  }

  // ---- bind mode ----
  private void bindMode(Minecraft mc, Inventory inv){
    if (stage == 0) {
      boolean pressed = false; while (key.consumeClick()) pressed = true;
      if (!pressed || mc.screen != null) return;
      if (!(mc.hitResult instanceof BlockHitResult hit) || mc.level.getBlockState(hit.getBlockPos()).isAir()) { say(mc, "Aim at a block first"); return; }
      BlockState st = mc.level.getBlockState(hit.getBlockPos());
      boolean existing = st.is(Blocks.RESPAWN_ANCHOR);                                  // aiming at an anchor already: skip placing
      boolean haveAnchor = C014.find(inv, s -> s.is(Items.RESPAWN_ANCHOR)) >= 0;
      boolean haveGlow = C014.find(inv, s -> s.is(Items.GLOWSTONE)) >= 0;
      if ((!existing && !haveAnchor) || !haveGlow) { say(mc, "Need a respawn anchor and glowstone in the hotbar"); return; }
      anchorPos = existing ? hit.getBlockPos() : hit.getBlockPos().relative(hit.getDirection());
      placeHit = hit; want = (int) charges.v; done = existing ? st.getValue(RespawnAnchorBlock.CHARGE) : 0;
      stage = existing ? 2 : 1; wait = 0;
      if (instant.v) { while (stage == 1 || stage == 2 || stage == 3) { step(mc, inv); if (stage == 0) break; } }
      return; }
    if (wait > 0) { wait--; return; }
    step(mc, inv); wait = (int) delay.v; }

  /** One step of the sequence: 1 = place the anchor, 2 = charge (once per call), 3 = explode. */
  private void step(Minecraft mc, Inventory inv){
    switch (stage) {
      case 1 -> {
        int a = C014.find(inv, s -> s.is(Items.RESPAWN_ANCHOR)); if (a < 0) { stage = 0; return; }
        use(mc, inv, placeHit, a); stage = 2; }
      case 2 -> {
        if (done >= want) { stage = 3; return; }
        int g = C014.find(inv, s -> s.is(Items.GLOWSTONE)); if (g < 0) { stage = done > 0 ? 3 : 0; return; }
        use(mc, inv, anchorHit(), g); done++; if (done >= want) stage = 3; }
      case 3 -> { use(mc, inv, anchorHit(), explodeSlot()); stage = 0; }
      default -> stage = 0;
    }
  }
}
