package com.example.cheat.gui;
import com.example.cheat.module.C011;
import com.example.cheat.C002;
import java.util.*;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import org.lwjgl.glfw.GLFW;

/** Scrollable, searchable list of every block. Click a row to add/remove it from Block ESP. */
public class C003 extends Screen {
  private record Entry(String id, String name, String lower, ItemStack icon) {}
  private static List<Entry> ALL;

  static final int PW = 300, ROW = 20, BG = 0xF0101217, FIELD = 0xFF1D2028, ACCENT = 0xFF7C6CFF, SEL = 0xFF2A2547, DIM = 0xFF8A8F9C, LIST_TOP = 82;
  private final Screen parent; private final C011 esp;
  private String query = ""; private boolean selectedOnly; private int scroll;
  private List<Entry> view = new ArrayList<>();

  public C003(Screen parent, C011 esp){
    super(Component.literal("Select blocks")); this.parent = parent; this.esp = esp; buildAll(); refilter(); }

  private static void buildAll(){
    if (ALL != null) return;
    List<Entry> l = new ArrayList<>();
    for (Block b : BuiltInRegistries.BLOCK) {
      String id = BuiltInRegistries.BLOCK.getKey(b).toString();
      if (id.endsWith("air")) continue;                                    // air, cave_air, void_air
      String name = b.getName().getString();
      l.add(new Entry(id, name, (name + " " + id).toLowerCase(), b.asItem().getDefaultInstance())); }
    l.sort(Comparator.comparing(Entry::name));
    ALL = l; }

  private void refilter(){
    String q = query.toLowerCase().trim();
    view = ALL.stream().filter(e -> (!selectedOnly || esp.blocks.contains(e.id())) && e.lower().contains(q)).toList();
    clamp(); }
  private int visible(){ return Math.max(1, (height - 24 - LIST_TOP) / ROW); }
  private void clamp(){ scroll = Math.max(0, Math.min(scroll, Math.max(0, view.size() - visible()))); }
  private int px(){ return (width - PW) / 2; }

  private void rrect(GuiGraphics c,int x,int y,int w,int h,int col){
    c.fill(x+2,y,x+w-2,y+h,col); c.fill(x,y+2,x+w,y+h-2,col); c.fill(x+1,y+1,x+w-1,y+h-1,col); }
  private boolean in(double mx,double my,int x,int y,int w,int h){ return mx>=x&&mx<x+w&&my>=y&&my<y+h; }
  private void text(GuiGraphics c,String s,int x,int y,int col){ c.drawString(this.font, s, x, y, col, false); }

  @Override public boolean isPauseScreen(){ return false; }
  @Override public void onClose(){ this.minecraft.setScreen(parent); }

  @Override public void render(GuiGraphics g, int mx, int my, float d){
    int x = px(), listBottom = height - 24, vis = visible();
    g.fill(0, 0, width, height, 0x99000000);
    rrect(g, x, 16, PW, height - 32, BG);
    text(g, "SELECT BLOCKS", x + 12, 24, 0xFFFFFFFF);
    String sel = esp.blocks.size() + " selected"; text(g, sel, x + PW - 12 - this.font.width(sel), 24, DIM);

    rrect(g, x + 10, 40, PW - 20, 18, FIELD);                                              // search box
    text(g, query.isEmpty() ? "Search blocks (type to filter)..." : query + "_", x + 16, 45, query.isEmpty() ? DIM : 0xFFFFFFFF);

    rrect(g, x + 10, 62, 80, 14, FIELD);              text(g, "Clear all", x + 22, 65, 0xFFFF6666);
    rrect(g, x + 96, 62, 110, 14, selectedOnly ? SEL : FIELD); text(g, "Selected only", x + 108, 65, selectedOnly ? 0xFFFFFFFF : DIM);
    rrect(g, x + PW - 80, 62, 70, 14, FIELD);         text(g, "Done", x + PW - 58, 65, 0xFFFFFFFF);

    for (int i = scroll; i < Math.min(view.size(), scroll + vis); i++) {
      Entry e = view.get(i); int y = LIST_TOP + (i - scroll) * ROW; boolean on = esp.blocks.contains(e.id());
      if (on) { g.fill(x + 8, y, x + PW - 14, y + ROW - 1, SEL); g.fill(x + 8, y, x + 10, y + ROW - 1, ACCENT); }
      else if (in(mx, my, x + 8, y, PW - 22, ROW - 1)) g.fill(x + 8, y, x + PW - 14, y + ROW - 1, 0xFF1A1D24);
      if (!e.icon().isEmpty()) g.renderItem(e.icon(), x + 14, y + 2);
      text(g, e.name(), x + 36, y + 6, on ? 0xFFFFFFFF : 0xFFC8CCD6);
      g.fill(x + PW - 36, y + 5, x + PW - 26, y + 15, on ? ACCENT : 0xFF343844);          // checkbox
      if (on) g.fill(x + PW - 34, y + 7, x + PW - 28, y + 13, 0xFFFFFFFF); }
    if (view.isEmpty()) text(g, "No blocks match.", x + 16, LIST_TOP + 6, DIM);

    int trackH = listBottom - LIST_TOP;                                                       // scrollbar
    g.fill(x + PW - 10, LIST_TOP, x + PW - 6, listBottom, 0xFF1D2028);
    if (view.size() > vis) { int th = Math.max(16, trackH * vis / view.size());
      int ty = LIST_TOP + (trackH - th) * scroll / Math.max(1, view.size() - vis);
      g.fill(x + PW - 10, ty, x + PW - 6, ty + th, ACCENT); }
  }

  @Override public boolean mouseClicked(MouseButtonEvent e, boolean doubleClick){
    if (e.button() != 0) return true;
    double mx = e.x(), my = e.y(); int x = px(), listBottom = height - 24;
    if (in(mx, my, x + 10, 62, 80, 14)) { esp.blocks.clear(); refilter(); C002.saveCurrent(); return true; }
    if (in(mx, my, x + 96, 62, 110, 14)) { selectedOnly = !selectedOnly; scroll = 0; refilter(); return true; }
    if (in(mx, my, x + PW - 80, 62, 70, 14)) { onClose(); return true; }
    if (in(mx, my, x + PW - 14, LIST_TOP, 12, listBottom - LIST_TOP)) {                      // click on scrollbar track
      int max = Math.max(0, view.size() - visible());
      scroll = (int) Math.round(max * (my - LIST_TOP) / (double) (listBottom - LIST_TOP)); clamp(); return true; }
    if (in(mx, my, x + 8, LIST_TOP, PW - 22, listBottom - LIST_TOP)) {
      int idx = scroll + (int) ((my - LIST_TOP) / ROW);
      if (idx >= 0 && idx < view.size()) { String id = view.get(idx).id();
        if (!esp.blocks.remove(id)) esp.blocks.add(id);
        C002.saveCurrent();
        if (selectedOnly) refilter(); } }
    return true; }

  @Override public boolean mouseScrolled(double mx, double my, double sx, double sy){
    scroll -= (int) Math.signum(sy) * 3; clamp(); return true; }

  @Override public boolean charTyped(CharacterEvent e){
    int cp = e.codepoint();
    if (cp >= 32 && cp != 127) { query += (char) cp; scroll = 0; refilter(); return true; }
    return false; }

  @Override public boolean keyPressed(KeyEvent e){
    if (e.key() == GLFW.GLFW_KEY_BACKSPACE) { if (!query.isEmpty()) { query = query.substring(0, query.length() - 1); scroll = 0; refilter(); } return true; }
    return super.keyPressed(e); }   // ESC closes via onClose()
}
