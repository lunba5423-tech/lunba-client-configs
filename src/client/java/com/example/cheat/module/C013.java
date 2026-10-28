package com.example.cheat.module;
import com.example.cheat.C001;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.lwjgl.glfw.GLFW;

/**
 * Places a cobweb on the player you're aiming at, at their feet.
 * Press its key (bind it in the ClickGUI or Controls), or switch on "Auto" to place whenever you aim at a player in reach.
 * Needs cobwebs in the hotbar and the target standing on a block.
 */
public class C013 extends C017 {
  public final C020.Bool auto = new C020.Bool("Auto (no key)", false);
  public final C020.Num cooldown = new C020.Num("Cooldown (ticks)", 20, 5, 60);
  public final C020.Bool swapBack = new C020.Bool("Swap back", true);
  public final KeyMapping key;
  private int cd;

  public C013(){ super("Cobweb Placer", C012.C014);
    settings.add(auto); settings.add(cooldown); settings.add(swapBack);
    key = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.cheatclient.cobweb", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_UNKNOWN, C001.CAT)); }

  @Override public void toggle(){ super.toggle(); while (key.consumeClick()) { } }          // drop presses made while it was off

  private void say(Minecraft mc, String m){ }

  @Override public void onTick(Minecraft mc){
    boolean pressed = false; while (key.consumeClick()) pressed = true;
    if (cd > 0) { cd--; return; }
    if (mc.screen != null || (!pressed && !auto.v)) return;

    LivingEntity t = C014.target(mc);
    if (!(t instanceof Player)) { if (pressed) say(mc, "Aim at a player first"); return; }
    if (mc.player.getEyePosition().distanceTo(t.position()) > 4.5) { if (pressed) say(mc, "Target is out of reach"); return; }

    BlockPos feet = t.blockPosition(), below = feet.below();
    if (!mc.level.getBlockState(feet).canBeReplaced() || mc.level.getBlockState(below).isAir()) { if (pressed) say(mc, "No free spot at their feet"); return; }

    Inventory inv = mc.player.getInventory();
    int web = C014.find(inv, s -> s.is(Items.COBWEB));
    if (web < 0) { if (pressed) say(mc, "No cobwebs in the hotbar"); return; }

    int prev = inv.getSelectedSlot();
    inv.setSelectedSlot(web);
    mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, new BlockHitResult(Vec3.atCenterOf(below).add(0, 0.5, 0), Direction.UP, below, false));
    mc.player.swing(InteractionHand.MAIN_HAND);
    if (swapBack.v) inv.setSelectedSlot(prev);
    cd = (int) cooldown.v;
  }
}
