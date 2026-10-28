package com.example.cheat.module;
import com.example.cheat.C024;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.phys.AABB;
public class C011 extends C017 {
  public final C020.Color color = new C020.Color("Colour", 0x55FFFF);
  public final C020.Bool C022 = new C020.Bool("Tracers", true);
  public final C020.Num range = new C020.Num("Range", 24, 8, 48);
  public final C020.Num tracerWidth = new C020.Num("Tracer width", 5, 1, 15);
  public final C020.Num tracerOpacity = new C020.Num("Tracer opacity %", 100, 10, 100);
  public final List<String> blocks = new ArrayList<>(List.of("minecraft:diamond_ore", "minecraft:ancient_debris"));
  private final List<BlockPos> found = new ArrayList<>(); private int timer;
  public C011(){ super("Block ESP", C012.RENDER); settings.add(color); settings.add(C022); settings.add(tracerWidth); settings.add(tracerOpacity); settings.add(range); }
  @Override public void onTick(Minecraft mc){
    if (timer-- > 0) return; timer = 20; found.clear();
    Set<Block> set = new HashSet<>();
    for (String s : blocks) { Identifier id = Identifier.tryParse(s);
      if (id != null && BuiltInRegistries.BLOCK.containsKey(id)) set.add(BuiltInRegistries.BLOCK.getValue(id)); }
    if (set.isEmpty()) return;
    int r = (int) range.v; BlockPos c = mc.player.blockPosition();
    for (BlockPos p : BlockPos.betweenClosed(c.offset(-r,-r,-r), c.offset(r,r,r))) {
      if (set.contains(mc.level.getBlockState(p).getBlock())) { found.add(p.immutable()); if (found.size() >= 300) return; } } }
  @Override public void onRender(Minecraft mc, float td){
    for (BlockPos p : found) { AABB bb = new AABB(p);
      C024.box(bb, color.rgb, true);
      if (C022.v) C024.tracer(mc, td, bb.getCenter(), color.rgb, tracerWidth.v, (float) (tracerOpacity.v / 100.0)); } }
}
