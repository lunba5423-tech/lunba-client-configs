package com.example.cheat.module;
import com.example.cheat.C001;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.List;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Bow TNT-minecart macro, started by its own key (bind it in Options > Controls > Key Binds).
 * Press the key: switches to the bow, fires at minimal charge, predicts where the arrow will land,
 * and a tick or so before impact places a rail and a TNT minecart on that spot.
 * Needs a bow, arrows, a rail and a TNT minecart in the hotbar. The module must be switched on in the ClickGUI.
 * Optional: light the cart with flint and steel / fire charge (otherwise use a flame arrow).
 */
public class C007 extends C017 {
  public final C020.Num chargeTicks = new C020.Num("Charge (ticks)", 4, 3, 20);   // 3 is about the minimum that still fires
  public final C020.Num lead = new C020.Num("Place before impact (ticks)", 1, 0, 3);
  public final C020.Bool light = new C020.Bool("Light with flint", false);
  public final C020.Bool swapBack = new C020.Bool("Swap back", true);
  public final KeyMapping key;
  private int stage, timer, prevSlot; private Entity arrow; private BlockHitResult landing; private BlockPos railPos;

  public C007(){ super("Auto Cart", C012.C014);
    settings.add(chargeTicks); settings.add(lead); settings.add(light); settings.add(swapBack);
    key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cheatclient.autocart", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, C001.CAT)); }

  @Override public void toggle(){
    super.toggle(); while (key.consumeClick()) { }                    // drop key presses made while the module was off
    if (!enabled) { stage = 0; Minecraft.getInstance().options.keyUse.setDown(false); } }

  private static boolean lighter(net.minecraft.world.item.ItemStack s){ return s.is(Items.FLINT_AND_STEEL) || s.is(Items.FIRE_CHARGE); }
  private void say(Minecraft mc, String m){ }

  @Override public void onTick(Minecraft mc){
    Inventory inv = mc.player.getInventory();
    if (stage == 0) {
      boolean pressed = false; while (key.consumeClick()) pressed = true;
      if (!pressed || mc.screen != null) return;
      int bow = C014.find(inv, s -> s.is(Items.BOW)), rail = C014.find(inv, s -> s.is(ItemTags.RAILS)), cart = C014.find(inv, s -> s.is(Items.TNT_MINECART));
      if (bow < 0 || rail < 0 || cart < 0) { say(mc, "Auto Cart needs a bow, a rail and a TNT minecart in the hotbar"); return; }
      prevSlot = inv.getSelectedSlot(); inv.setSelectedSlot(bow);
      mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);        // start drawing the bow
      mc.options.keyUse.setDown(true);                                  // hold "use" so vanilla doesn't release it at once
      stage = 1; timer = 0; return; }

    timer++;
    switch (stage) {
      case 1 -> {                                                       // charging
        mc.options.keyUse.setDown(true);
        if (timer >= (int) chargeTicks.v) { mc.options.keyUse.setDown(false); mc.gameMode.releaseUsingItem(mc.player); stage = 2; timer = 0; } }
      case 2 -> {                                                       // wait for our arrow to appear
        List<Entity> l = mc.level.getEntities((Entity) null, mc.player.getBoundingBox().inflate(6.0), e -> e.getType() == EntityType.ARROW && e.tickCount < 8);
        if (!l.isEmpty()) { arrow = l.get(0); stage = 3; timer = 0; }
        else if (timer > 25) abort(mc, "no arrow was fired (do you have arrows?)"); }
      case 3 -> track(mc, inv);
      case 4 -> lightCart(mc, inv);
      default -> stage = 0;
    }
  }

  /** Simulates the arrow (gravity 0.05, drag 0.99) until it hits a block. ticks[0] = steps until impact. */
  private BlockHitResult predict(Minecraft mc, Entity a, int[] ticks){
    Vec3 pos = a.position(), vel = a.getDeltaMovement();
    for (int i = 1; i <= 100; i++) {
      Vec3 next = pos.add(vel);
      BlockHitResult r = mc.level.clip(new ClipContext(pos, next, ClipContext.Block.COLLIDER, ClipContext.Fluid.NONE, a));
      if (r.getType() != HitResult.Type.MISS) { ticks[0] = i; return r; }
      pos = next; vel = vel.scale(0.99).add(0, -0.05, 0); }
    return null; }

  private void track(Minecraft mc, Inventory inv){
    if (timer > 120) { abort(mc, "gave up waiting for the arrow to land"); return; }
    boolean gone = arrow == null || arrow.isRemoved();
    if (!gone) {
      int[] n = new int[1]; BlockHitResult r = predict(mc, arrow, n);
      if (r != null) { landing = r; if (n[0] <= (int) lead.v) place(mc, inv); }
      else if (timer > 40) abort(mc, "the arrow isn't going to land anywhere reachable");
      return; }
    if (landing != null) place(mc, inv); else abort(mc, "lost the arrow");   // arrow vanished: use the last predicted spot
  }

  private void place(Minecraft mc, Inventory inv){
    BlockPos below = landing.getBlockPos();
    if (landing.getDirection() != Direction.UP) { abort(mc, "the arrow lands on a wall/ceiling, not a floor"); return; }
    if (mc.player.getEyePosition().distanceTo(Vec3.atCenterOf(below)) > 5.5) { abort(mc, "landing spot is out of reach (aim closer)"); return; }
    int rail = C014.find(inv, s -> s.is(ItemTags.RAILS)), cart = C014.find(inv, s -> s.is(Items.TNT_MINECART));
    if (rail < 0 || cart < 0) { abort(mc, "ran out of rails or TNT minecarts"); return; }
    railPos = below.above();
    inv.setSelectedSlot(rail);
    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false));
    mc.player.swing(InteractionHand.MAIN_HAND);
    inv.setSelectedSlot(cart);
    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(railPos), Direction.UP, railPos, false));
    mc.player.swing(InteractionHand.MAIN_HAND);
    if (light.v) { stage = 4; timer = 0; } else finish(mc, inv); }

  private void lightCart(Minecraft mc, Inventory inv){
    if (timer > 20) { finish(mc, inv); return; }
    List<Entity> carts = mc.level.getEntities((Entity) null, new AABB(railPos).inflate(1.0), e -> e.getType() == EntityType.TNT_MINECART);
    int l = C014.find(inv, C007::lighter);
    if (carts.isEmpty() || l < 0) { if (l < 0) finish(mc, inv); return; }
    inv.setSelectedSlot(l);
    mc.gameMode.interact(mc.player, carts.get(0), InteractionHand.MAIN_HAND);
    mc.player.swing(InteractionHand.MAIN_HAND);
    finish(mc, inv); }

  private void finish(Minecraft mc, Inventory inv){
    mc.options.keyUse.setDown(false);
    if (swapBack.v) inv.setSelectedSlot(prevSlot);
    stage = 0; arrow = null; landing = null; }

  private void abort(Minecraft mc, String why){ say(mc, "Auto Cart stopped: " + why); finish(mc, mc.player.getInventory()); }
}
