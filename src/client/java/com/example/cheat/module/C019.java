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
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RespawnAnchorBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/** Places and charges a respawn anchor, then puts a glowstone block on the player's side as cover. */
public class C019 extends C017 {
  public final C020.Num charges = new C020.Num("Charges", 1, 1, 4);
  public final C020.Num delay = new C020.Num("Delay (ticks)", 1, 0, 10);
  public final KeyMapping key;

  private int stage, wait, prevSlot, targetCharge;
  private BlockPos anchorPos, coverPos;
  private BlockHitResult placeHit;

  public C019(){
    super("Safe Anchor", C012.C014);
    settings.add(charges); settings.add(delay);
    key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cheatclient.safeanchor", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, C001.CAT));
  }

  @Override public void toggle(){
    super.toggle();
    while (key.consumeClick()) { }
    if (!enabled && stage != 0) finish(Minecraft.getInstance());
  }

  @Override public void onTick(Minecraft mc){
    Inventory inv = mc.player.getInventory();
    if (stage == 0) {
      boolean pressed = false; while (key.consumeClick()) pressed = true;
      if (!pressed || mc.screen != null) return;
      begin(mc, inv); return;
    }
    while (key.consumeClick()) { }
    if (wait > 0) { wait--; return; }
    advance(mc, inv);
    if (stage != 0) wait = (int) delay.v;
  }

  private void begin(Minecraft mc, Inventory inv){
    if (!(mc.hitResult instanceof BlockHitResult hit)) return;
    BlockPos aimed = hit.getBlockPos();
    boolean existingAnchor = mc.level.getBlockState(aimed).is(Blocks.RESPAWN_ANCHOR);
    anchorPos = existingAnchor ? aimed : aimed.relative(hit.getDirection());
    if (!existingAnchor && !mc.level.getBlockState(anchorPos).canBeReplaced()) return;

    targetCharge = (int) charges.v;
    if (existingAnchor) targetCharge = Math.max(targetCharge, mc.level.getBlockState(anchorPos).getValue(RespawnAnchorBlock.CHARGE));
    int currentCharge = existingAnchor ? mc.level.getBlockState(anchorPos).getValue(RespawnAnchorBlock.CHARGE) : 0;
    int glowstoneNeeded = Math.max(0, targetCharge - currentCharge) + 1;
    if (countHotbar(inv, Items.GLOWSTONE) < glowstoneNeeded) return;
    if (!existingAnchor && C014.find(inv, s -> s.is(Items.RESPAWN_ANCHOR)) < 0) return;

    Direction towardPlayer = directionToPlayer(mc, anchorPos);
    coverPos = anchorPos.relative(towardPlayer);
    if (!mc.level.getBlockState(coverPos).canBeReplaced()) return;

    prevSlot = inv.getSelectedSlot();
    placeHit = hit;
    stage = existingAnchor ? 2 : 1;
    wait = 0;
    advance(mc, inv);
    if (stage != 0) wait = (int) delay.v;
  }

  private void advance(Minecraft mc, Inventory inv){
    switch (stage) {
      case 1 -> {
        int slot = C014.find(inv, s -> s.is(Items.RESPAWN_ANCHOR));
        if (slot < 0) { finish(mc); return; }
        use(mc, inv, placeHit, slot); stage = 2;
      }
      case 2 -> {
        if (!mc.level.getBlockState(anchorPos).is(Blocks.RESPAWN_ANCHOR)) { finish(mc); return; }
        int charge = mc.level.getBlockState(anchorPos).getValue(RespawnAnchorBlock.CHARGE);
        if (charge >= targetCharge) { stage = 3; advance(mc, inv); return; }
        int slot = C014.find(inv, s -> s.is(Items.GLOWSTONE));
        if (slot < 0) { finish(mc); return; }
        use(mc, inv, anchorHit(), slot);
      }
      case 3 -> {
        if (!mc.level.getBlockState(anchorPos).is(Blocks.RESPAWN_ANCHOR)
            || !mc.level.getBlockState(coverPos).canBeReplaced()) { finish(mc); return; }
        int slot = C014.find(inv, s -> s.is(Items.GLOWSTONE));
        BlockHitResult supportHit = supportFor(mc, coverPos);
        if (slot < 0 || supportHit == null) { finish(mc); return; }
        use(mc, inv, supportHit, slot);
        finish(mc);
      }
      default -> finish(mc);
    }
  }

  private BlockHitResult anchorHit(){
    return new BlockHitResult(Vec3.atCenterOf(anchorPos), Direction.UP, anchorPos, false);
  }

  private BlockHitResult supportFor(Minecraft mc, BlockPos target){
    for (Direction face : Direction.values()) {
      BlockPos support = target.relative(face.getOpposite());
      if (support.equals(anchorPos)) continue;
      var state = mc.level.getBlockState(support);
      if (state.isAir() || state.canBeReplaced()) continue;
      Vec3 hit = Vec3.atCenterOf(support).add(face.getStepX() * 0.5, face.getStepY() * 0.5, face.getStepZ() * 0.5);
      return new BlockHitResult(hit, face, support, false);
    }
    return null;
  }

  private static Direction directionToPlayer(Minecraft mc, BlockPos pos){
    double dx = mc.player.getX() - (pos.getX() + 0.5);
    double dy = mc.player.getEyeY() - (pos.getY() + 0.5);
    double dz = mc.player.getZ() - (pos.getZ() + 0.5);
    if (Math.abs(dx) >= Math.abs(dy) && Math.abs(dx) >= Math.abs(dz)) return dx < 0 ? Direction.WEST : Direction.EAST;
    if (Math.abs(dz) >= Math.abs(dy)) return dz < 0 ? Direction.NORTH : Direction.SOUTH;
    return dy < 0 ? Direction.DOWN : Direction.UP;
  }

  private static int countHotbar(Inventory inv, Item item){
    int count = 0;
    for (int i = 0; i < 9; i++) if (inv.getItem(i).is(item)) count += inv.getItem(i).getCount();
    return count;
  }

  private static void use(Minecraft mc, Inventory inv, BlockHitResult hit, int slot){
    inv.setSelectedSlot(slot);
    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, hit);
    mc.player.swing(InteractionHand.MAIN_HAND);
  }

  private void finish(Minecraft mc){
    if (mc.player != null && prevSlot >= 0 && prevSlot < 9) mc.player.getInventory().setSelectedSlot(prevSlot);
    stage = 0; wait = 0; anchorPos = null; coverPos = null; placeHit = null;
  }
}
