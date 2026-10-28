package com.example.cheat.module;
public abstract class C020 {
  public final String name; protected C020(String n){name=n;}
  public static class Bool extends C020 { public boolean v; public Bool(String n, boolean v){super(n);this.v=v;} }
  public static class Num extends C020 { public double v,min,max;
    public Num(String n,double v,double min,double max){super(n);this.v=v;this.min=min;this.max=max;} }
  public static class Color extends C020 { public int rgb; public Color(String n,int rgb){super(n);this.rgb=rgb;} }
}
