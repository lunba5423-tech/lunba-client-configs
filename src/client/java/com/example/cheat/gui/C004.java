package com.example.cheat.gui;
import com.example.cheat.C001;
import com.example.cheat.C002;
import com.example.cheat.module.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.util.*;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class C004 extends Screen {
  static final int W = 112, RH = 14, SH = 13, HH = 16, BG = 0xF0101217, HEAD = 0xFF181B22, ROW = 0xFF1D2028, ACCENT = 0xFF7C6CFF, DIM = 0xFF8A8F9C;
  static final int[] PALETTE = {0xFF5555,0xFFAA00,0xFFFF55,0x55FF55,0x55FFFF,0x5555FF,0xAA55FF,0xFFFFFF};
  private static final int[] PX = new int[C012.values().length], PY = new int[C012.values().length];   // panel positions, kept between openings
  private static boolean placed;
  private final Set<C017> open = new HashSet<>();
  private C012 dragging; private int dragDx, dragDy; private KeyMapping listening;
  private int scroll;   // vertical scroll for all panels (mouse wheel)

  public C004(){ super(Component.literal("ClickGUI"));
    if (!placed) { placed = true; for (int i = 0; i < PX.length; i++) { PX[i] = 20 + i * (W + 10); PY[i] = 20; } } }
  @Override public boolean isPauseScreen(){ return false; }

  private void rrect(GuiGraphics c,int x,int y,int w,int h,int col){
    c.fill(x+2,y,x+w-2,y+h,col); c.fill(x,y+2,x+w,y+h-2,col); c.fill(x+1,y+1,x+w-1,y+h-1,col); }
  private boolean in(double mx,double my,int x,int y,int w,int h){ return mx>=x&&mx<x+w&&my>=y&&my<y+h; }
  private void text(GuiGraphics c,String s,int x,int y,int col){ c.drawString(this.font, s, x, y, col, false); }
  private int panelHeight(C012 cat){
    int t = HH + 2;
    for (C017 m : C001.MODS) if (m.category == cat) { t += RH; if (open.contains(m)) t += rows(m) * SH; }
    return t; }
  private int maxScroll(){
    int bottom = 0;
    for (C012 c : C012.values()) bottom = Math.max(bottom, PY[c.ordinal()] + panelHeight(c));
    return Math.max(0, bottom + 16 - height); }
  private static KeyMapping bindOf(C017 m){
    if (m instanceof C007 a) return a.key;
    if (m instanceof C006 a) return a.key;
    if (m instanceof C019 s) return s.key;
    if (m instanceof C013 c) return c.key;
    return null;
  }

  /** One pass over the layout: draws when c != null, handles a click when button >= 0. */
  private void walk(GuiGraphics c, int mx, int my, int button){
    for (C012 cat : C012.values()) {
      int x = PX[cat.ordinal()], y = PY[cat.ordinal()] - scroll, total = panelHeight(cat);
      if (y > height || y + total < 0) continue;                           // panel is off-screen: skip drawing and clicks
      List<C017> mods = C001.MODS.stream().filter(m->m.category==cat).toList();
      if (c != null) { rrect(c,x,y,W,total,BG); rrect(c,x,y,W,HH,HEAD); c.fill(x+6,y+HH-2,x+W-6,y+HH,ACCENT);
        text(c, cat.label.toUpperCase(), x+8, y+4, 0xFFFFFFFF); }
      y += HH + 2;
      for (C017 m : mods) {
        if (c != null) { rrect(c,x+3,y+1,W-6,RH-2, m.enabled ? 0xFF2A2547 : ROW);
          if (m.enabled) c.fill(x+3,y+3,x+5,y+RH-3,ACCENT);
          text(c, m.name, x+9, y+3, m.enabled?0xFFFFFFFF:DIM);
          text(c, open.contains(m)?"-":"+", x+W-11, y+3, DIM); }
        else if (in(mx,my,x+3,y+1,W-6,RH-2)) { if (button==0) m.toggle(); else if (button==1 && !open.remove(m)) open.add(m); }
        y += RH;
        if (!open.contains(m)) continue;
        for (C020 s : m.settings) y = C020(c, s, x, y, mx, my, button);
        if (m instanceof C011 b) y = blockButton(c, b, x, y, mx, my, button);
        y = bindRow(c, m, m.toggleKey, true, x, y, mx, my, button);
        KeyMapping km = bindOf(m); if (km != null) y = bindRow(c, m, km, false, x, y, mx, my, button);
      } } }

  private int rows(C017 m){ int r = m.settings.size() + 1; if (m instanceof C011) r++; if (bindOf(m) != null) r++; return r; }

  private int C020(GuiGraphics c, C020 s, int x, int y, int mx, int my, int button){
    int bx = x+6, bw = W-12;
    if (s instanceof C020.Bool b) {
      if (c != null) { text(c, b.name, bx+2, y+2, DIM);
        rrect(c, x+W-26, y+2, 18, 9, b.v ? ACCENT : 0xFF343844); int kx = b.v ? x+W-16 : x+W-24; c.fill(kx, y+4, kx+6, y+9, 0xFFFFFFFF); }
      else if (button==0 && in(mx,my,bx,y,bw,SH)) b.v = !b.v;
    } else if (s instanceof C020.Num n) {
      if (c != null) { c.fill(bx, y+SH-3, bx+bw, y+SH-1, 0xFF343844);
        c.fill(bx, y+SH-3, bx+(int)(bw*(n.v-n.min)/(n.max-n.min)), y+SH-1, ACCENT);
        text(c, n.name+": "+(int)n.v, bx+2, y, DIM); }
      else if (button==0 && in(mx,my,bx,y,bw,SH)) n.v = Math.round(n.min + (n.max-n.min)*Math.min(1,Math.max(0,(mx-bx)/(double)bw)));
    } else if (s instanceof C020.Color col) {
      int sw = bw / 8;
      for (int i = 0; i < PALETTE.length; i++) { int sx = bx + i*sw;
        if (c != null) { c.fill(sx+1, y+2, sx+sw-1, y+SH-2, 0xFF000000|PALETTE[i]);
          if (PALETTE[i]==col.rgb) c.fill(sx+1, y+SH-2, sx+sw-1, y+SH-1, 0xFFFFFFFF); }
        else if (button==0 && in(mx,my,sx,y+2,sw,SH-4)) col.rgb = PALETTE[i]; } }
    return y + SH; }

  private int blockButton(GuiGraphics c, C011 b, int x, int y, int mx, int my, int button){
    int bx = x+6, bw = W-12;
    if (c != null) { rrect(c, bx, y+1, bw, SH-2, 0xFF2A2547); text(c, "Choose blocks (" + b.blocks.size() + ")", bx+4, y+3, 0xFFFFFFFF); text(c, ">", x+W-14, y+3, DIM); }
    else if (button==0 && in(mx,my,bx,y,bw,SH)) this.minecraft.setScreen(new C003(this, b));
    return y + SH; }

  /** Click, then press a key to bind. Esc cancels, Delete/Backspace clears. */
  private int bindRow(GuiGraphics c, C017 m, KeyMapping km, boolean toggle, int x, int y, int mx, int my, int button){
    int bx = x+6, bw = W-12;
    if (c != null) { rrect(c, bx, y+1, bw, SH-2, listening == km ? 0xFF2A2547 : ROW);
      String label = m instanceof C006 || m instanceof C013 ? "gui.cheatclient.use_key" : "gui.cheatclient.bind";
      Component message = listening == km ? Component.translatable("gui.cheatclient.bind_listening")
        : toggle ? Component.literal("Toggle: " + km.getTranslatedKeyMessage().getString())
        : Component.translatable(label, km.getTranslatedKeyMessage());
      text(c, message.getString(), bx+4, y+3, 0xFFFFFFFF); }
    else if (button==0 && in(mx,my,bx,y,bw,SH)) listening = km;
    return y + SH; }

  private void setKey(KeyMapping km, int code){
    km.setKey(InputConstants.Type.KEYSYM.getOrCreate(code)); KeyMapping.resetMapping(); this.minecraft.options.save(); }

  @Override public void render(GuiGraphics g, int mx, int my, float d){
    int max = maxScroll(); scroll = Math.max(0, Math.min(scroll, max));
    g.fill(0,0,width,height,0x66000000); walk(g, mx, my, -1);
    if (max > 0) {                                                          // scrollbar
      int th = Math.max(20, height * height / (height + max)), ty = scroll * (height - th) / max;
      g.fill(width - 4, 0, width - 1, height, 0x33FFFFFF); g.fill(width - 4, ty, width - 1, ty + th, ACCENT); }
    text(g, max > 0 ? "Scroll for more | drag a title bar to move a panel" : "Drag a title bar to move a panel", 6, height - 12, DIM);
    rrect(g, width/2-42, height-32, 84, 20, 0xFF2A2547); text(g, "Configs", width/2-22, height-26, 0xFFFFFFFF); }

  @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick){
    int mx = (int) e.x(), my = (int) e.y();
    listening = null;
    if (e.button() == 0 && in(mx,my,width/2-42,height-32,84,20)) { this.minecraft.setScreen(new C005(this)); return true; }
    if (e.button() == 0) for (C012 cat : C012.values()) {
      int i = cat.ordinal();
      if (in(mx, my, PX[i], PY[i] - scroll, W, HH)) { dragging = cat; dragDx = mx - PX[i]; dragDy = my - (PY[i] - scroll); return true; } }
    walk(null, mx, my, e.button());
    C002.saveCurrent();
    return true; }

  @Override public boolean mouseDragged(MouseButtonEvent e, double dx, double dy){
    if (dragging == null) return false;
    int i = dragging.ordinal();
    PX[i] = Math.max(0, Math.min(width - W, (int) e.x() - dragDx));
    PY[i] = Math.max(0, Math.min(4000, (int) e.y() - dragDy + scroll));
    return true; }

  @Override public boolean mouseReleased(MouseButtonEvent e){ dragging = null; return true; }

  @Override public boolean mouseScrolled(double mx, double my, double sx, double sy){
    scroll = Math.max(0, Math.min(scroll - (int) Math.round(sy * 24), maxScroll())); return true; }

  @Override public boolean keyPressed(KeyEvent e){
    if (listening != null) {
      int k = e.key();
      if (k == GLFW.GLFW_KEY_DELETE || k == GLFW.GLFW_KEY_BACKSPACE) setKey(listening, GLFW.GLFW_KEY_UNKNOWN);
      else if (k != GLFW.GLFW_KEY_ESCAPE) setKey(listening, k);
      listening = null; C002.saveCurrent(); return true; }
    return super.keyPressed(e); }
}
