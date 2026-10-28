package com.example.cheat.gui;

import com.example.cheat.C002;
import com.example.cheat.PublicConfigs;
import com.example.cheat.PublicConfigs.Entry;
import java.util.*;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

/** Menu for saving, loading, and sharing named configs. */
public class C005 extends Screen {
  private static final int PANEL_W = 360, ROW_H = 22;
  private final Screen parent;
  private String name = "";
  private boolean editingName, publicTab, publishOnSave, loadingPublic;
  private int scroll;
  private String status = "";
  private List<Entry> remoteConfigs = List.of();

  public C005(Screen parent){ super(Component.literal("Configs")); this.parent = parent; }
  @Override public boolean isPauseScreen(){ return false; }
  @Override public void onClose(){ minecraft.setScreen(parent); }

  private int left(){ return (width - PANEL_W) / 2; }
  private boolean inside(double mx,double my,int x,int y,int w,int h){ return mx>=x&&mx<x+w&&my>=y&&my<y+h; }
  private void text(GuiGraphics g,String s,int x,int y,int color){ g.drawString(font,s,x,y,color,false); }
  private void refreshPublic(){
    if (loadingPublic) return;
    loadingPublic=true; status="Loading public configs...";
    PublicConfigs.list().whenComplete((files,error)->Minecraft.getInstance().execute(()->{
      loadingPublic=false;
      if(error!=null){ remoteConfigs=List.of(); status=PublicConfigs.message(error); }
      else { remoteConfigs=files; status=files.isEmpty()?"No public configs yet.":files.size()+" public config(s) available."; }
    }));
  }
  private void saveConfig(){
    if(!C002.saveNamed(name)){ status="Enter a name before saving."; return; }
    String savedName=name; name=""; editingName=false;
    if(!publishOnSave){ status="Saved locally."; return; }
    status="Publishing to GitHub...";
    PublicConfigs.publish(savedName,C002.exportCurrent()).whenComplete((ok,error)->Minecraft.getInstance().execute(()->{
      status=error==null?"Published publicly as "+savedName+".":PublicConfigs.message(error);
      if(error==null&&publicTab) refreshPublic();
    }));
  }
  private void loadPublic(Entry entry){
    status="Downloading "+entry.name()+"...";
    PublicConfigs.download(entry).whenComplete((json,error)->Minecraft.getInstance().execute(()->{
      if(error!=null){ status=PublicConfigs.message(error); return; }
      if(!C002.loadJson(json)){ status="That public file is not a valid Lunba config."; return; }
      minecraft.setScreen(parent);
    }));
  }

  @Override public void render(GuiGraphics g,int mx,int my,float delta){
    g.fill(0,0,width,height,0x99000000);
    int x=left(), top=24, panelH=height-48;
    g.fill(x,top,x+PANEL_W,top+panelH,0xF0101217);
    text(g,"CONFIGS",x+14,top+12,0xFFFFFFFF);
    text(g,"Your latest settings are saved automatically.",x+14,top+28,0xFF8A8F9C);
    g.fill(x+12,top+44,x+112,top+64,publicTab?0xFF1D2028:0xFF2A2547); text(g,"Local",x+45,top+50,0xFFFFFFFF);
    g.fill(x+118,top+44,x+218,top+64,publicTab?0xFF2A2547:0xFF1D2028); text(g,"Public",x+150,top+50,0xFFFFFFFF);
    if(publicTab){
      g.fill(x+PANEL_W-96,top+44,x+PANEL_W-12,top+64,0xFF1D2028); text(g,loadingPublic?"Loading...":"Refresh",x+PANEL_W-82,top+50,0xFFFFFFFF);
      text(g,"Public profiles from GitHub; anyone can view them.",x+14,top+74,0xFF8A8F9C);
      text(g,status,x+14,top+88,0xFFB5B8C2);
      List<Entry> files=remoteConfigs;
      int listTop=top+108, listBottom=top+panelH-36, visible=Math.max(0,(listBottom-listTop)/ROW_H);
      scroll=Math.max(0,Math.min(scroll,Math.max(0,files.size()-visible)));
      if(files.isEmpty()&&!loadingPublic) text(g,"No public configs found.",x+16,listTop+6,0xFF8A8F9C);
      for(int i=scroll;i<Math.min(files.size(),scroll+visible);i++){
        int y=listTop+(i-scroll)*ROW_H; g.fill(x+12,y,x+PANEL_W-12,y+ROW_H-2,0xFF1D2028);
        text(g,files.get(i).name(),x+20,y+6,0xFFFFFFFF); text(g,"Load",x+PANEL_W-52,y+6,0xFF7C6CFF);
      }
      if(inside(mx,my,x+12,listTop,PANEL_W-24,listBottom-listTop)){
        int idx=scroll+(int)((my-listTop)/ROW_H);
        if(idx>=0&&idx<files.size()) text(g,"Click a profile row to download and load it.",x+14,top+panelH-22,0xFF8A8F9C);
      }
    } else {
      g.fill(x+12,top+70,x+164,top+92,0xFF1D2028);
      text(g,name.isEmpty()?(editingName?"Enter config name...":"Name this config..."):name+(editingName?"_":""),x+19,top+77,editingName?0xFFFFFFFF:0xFF8A8F9C);
      g.fill(x+168,top+70,x+238,top+92,publishOnSave?0xFF2A2547:0xFF1D2028);
      text(g,publishOnSave?"Public ✓":"Public",x+177,top+77,0xFFFFFFFF);
      g.fill(x+244,top+70,x+PANEL_W-12,top+92,0xFF2A2547);
      text(g,publishOnSave?"Save + publish":"Save current",x+252,top+77,0xFFFFFFFF);
      text(g,status,x+14,top+99,0xFFB5B8C2);
      text(g,"SAVED CONFIGS",x+14,top+114,0xFF8A8F9C);
      List<String> configs=C002.listConfigs();
      int listTop=top+132, listBottom=top+panelH-36, visible=Math.max(0,(listBottom-listTop)/ROW_H);
      scroll=Math.max(0,Math.min(scroll,Math.max(0,configs.size()-visible)));
      if(configs.isEmpty()) text(g,"No saved configs yet.",x+16,listTop+6,0xFF8A8F9C);
      for(int i=scroll;i<Math.min(configs.size(),scroll+visible);i++){
        int y=listTop+(i-scroll)*ROW_H; g.fill(x+12,y,x+PANEL_W-12,y+ROW_H-2,0xFF1D2028);
        text(g,configs.get(i),x+20,y+6,0xFFFFFFFF); text(g,"Load",x+PANEL_W-70,y+6,0xFF7C6CFF); text(g,"×",x+PANEL_W-34,y+5,0xFFFF7777);
      }
    }
    g.fill(x+12,top+panelH-28,x+PANEL_W-12,top+panelH-8,0xFF1D2028);
    text(g,"Back",x+PANEL_W/2-12,top+panelH-22,0xFFFFFFFF);
  }

  @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick){
    if(e.button()!=0) return true;
    int x=left(), top=24, panelH=height-48; double mx=e.x(),my=e.y();
    if(inside(mx,my,x+12,top+panelH-28,PANEL_W-24,20)){ onClose(); return true; }
    if(inside(mx,my,x+12,top+44,100,20)){ publicTab=false; scroll=0; status=""; return true; }
    if(inside(mx,my,x+118,top+44,100,20)){ publicTab=true; scroll=0; refreshPublic(); return true; }
    if(publicTab){
      if(inside(mx,my,x+PANEL_W-96,top+44,84,20)){ refreshPublic(); return true; }
      int listTop=top+108, listBottom=top+panelH-36;
      if(inside(mx,my,x+12,listTop,PANEL_W-24,listBottom-listTop)){
        int idx=scroll+(int)((my-listTop)/ROW_H);
        if(idx>=0&&idx<remoteConfigs.size()) loadPublic(remoteConfigs.get(idx));
        return true;
      }
      return true;
    }
    if(inside(mx,my,x+12,top+70,152,22)){ editingName=true; return true; }
    if(inside(mx,my,x+168,top+70,70,22)){ publishOnSave=!publishOnSave; return true; }
    if(inside(mx,my,x+244,top+70,PANEL_W-256,22)){ saveConfig(); return true; }
    int listTop=top+132, listBottom=top+panelH-36;
    if(inside(mx,my,x+12,listTop,PANEL_W-24,listBottom-listTop)){
      int idx=scroll+(int)((my-listTop)/ROW_H); List<String> configs=C002.listConfigs();
      if(idx>=0&&idx<configs.size()){
        if(mx>=x+PANEL_W-44){ C002.deleteNamed(configs.get(idx)); status="Deleted local config."; }
        else if(C002.loadNamed(configs.get(idx))) minecraft.setScreen(parent);
      }
      return true;
    }
    editingName=false; return true;
  }

  @Override public boolean mouseScrolled(double mx,double my,double sx,double sy){ scroll=Math.max(0,scroll-(int)Math.signum(sy)*3); return true; }
  @Override public boolean charTyped(CharacterEvent e){
    if(!editingName) return false; int cp=e.codepoint();
    if(cp>=32&&cp!=127&&name.length()<40) name+=(char)cp;
    return true;
  }
  @Override public boolean keyPressed(KeyEvent e){
    if(editingName){
      if(e.key()==GLFW.GLFW_KEY_BACKSPACE){ if(!name.isEmpty()) name=name.substring(0,name.length()-1); return true; }
      if(e.key()==GLFW.GLFW_KEY_ENTER){ saveConfig(); return true; }
      if(e.key()==GLFW.GLFW_KEY_ESCAPE){ editingName=false; return true; }
    }
    if(e.key()==GLFW.GLFW_KEY_ESCAPE){ onClose(); return true; }
    return super.keyPressed(e);
  }
}
