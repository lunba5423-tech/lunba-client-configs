package com.example.cheat;

import com.example.cheat.module.*;
import com.google.gson.*;
import com.mojang.blaze3d.platform.InputConstants;
import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;

/** Stores the current client state and user-named config profiles. */
public final class C002 {
  private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
  private static final Path DIR = FabricLoader.getInstance().getConfigDir().resolve("lunba-client");
  private static final Path PROFILES = DIR.resolve("configs");
  private static final Path CURRENT = DIR.resolve("current.json");

  private C002() {}

  public static void loadCurrent(){ apply(read(CURRENT)); }
  public static void saveCurrent(){ write(CURRENT, capture()); }
  public static String exportCurrent(){ return GSON.toJson(capture()); }
  public static boolean loadJson(String json){
    try {
      JsonObject data = JsonParser.parseString(json).getAsJsonObject();
      if (!data.has("modules") || !data.get("modules").isJsonObject()) return false;
      apply(data); saveCurrent(); return true;
    } catch (RuntimeException ignored) { return false; }
  }

  public static List<String> listConfigs(){
    try {
      Files.createDirectories(PROFILES);
      try (var files = Files.list(PROFILES)) {
        return files.filter(Files::isRegularFile).map(p -> p.getFileName().toString())
          .filter(n -> n.toLowerCase(Locale.ROOT).endsWith(".json"))
          .map(n -> n.substring(0, n.length() - 5)).sorted(String.CASE_INSENSITIVE_ORDER).toList();
      }
    } catch (IOException ignored) { return List.of(); }
  }

  public static boolean saveNamed(String rawName){
    String name = cleanName(rawName); if (name.isEmpty()) return false;
    write(PROFILES.resolve(name + ".json"), capture()); return true;
  }

  public static boolean loadNamed(String rawName){
    String name = cleanName(rawName); if (name.isEmpty()) return false;
    Path file = PROFILES.resolve(name + ".json"); if (!Files.isRegularFile(file)) return false;
    JsonObject data = read(file); if (data == null) return false;
    apply(data); saveCurrent(); return true;
  }

  public static void deleteNamed(String rawName){
    String name = cleanName(rawName); if (name.isEmpty()) return;
    try { Files.deleteIfExists(PROFILES.resolve(name + ".json")); } catch (IOException ignored) { }
  }

  private static String cleanName(String raw){
    if (raw == null) return "";
    String name = raw.trim().replaceAll("[^A-Za-z0-9 _-]", "").replaceAll("\\s+", " ");
    return name.length() > 40 ? name.substring(0, 40).trim() : name;
  }

  private static JsonObject capture(){
    JsonObject root = new JsonObject(), modules = new JsonObject();
    for (C017 m : C001.MODS) {
      JsonObject data = new JsonObject(); data.addProperty("enabled", m.enabled);
      JsonObject settings = new JsonObject();
      for (C020 s : m.settings) {
        if (s instanceof C020.Bool b) settings.addProperty(s.name, b.v);
        else if (s instanceof C020.Num n) settings.addProperty(s.name, n.v);
        else if (s instanceof C020.Color c) settings.addProperty(s.name, c.rgb);
      }
      data.add("settings", settings);
      if (m instanceof C011 esp) { JsonArray blocks = new JsonArray(); esp.blocks.forEach(blocks::add); data.add("blocks", blocks); }
      KeyMapping key = keyOf(m); if (key != null) data.addProperty("key", key.saveString());
      data.addProperty("toggleKey", m.toggleKey.saveString());
      modules.add(m.name, data);
    }
    root.addProperty("version", 1); root.add("modules", modules); return root;
  }

  private static void apply(JsonObject root){
    if (root == null || !root.has("modules") || !root.get("modules").isJsonObject()) return;
    JsonObject modules = root.getAsJsonObject("modules");
    for (C017 m : C001.MODS) {
      if (!modules.has(m.name) || !modules.get(m.name).isJsonObject()) continue;
      JsonObject data = modules.getAsJsonObject(m.name);
      if (data.has("enabled")) m.enabled = data.get("enabled").getAsBoolean();
      if (data.has("settings") && data.get("settings").isJsonObject()) {
        JsonObject settings = data.getAsJsonObject("settings");
        for (C020 s : m.settings) {
          if (!settings.has(s.name)) continue;
          JsonElement value = settings.get(s.name);
          try {
            if (s instanceof C020.Bool b) b.v = value.getAsBoolean();
            else if (s instanceof C020.Num n) n.v = Math.max(n.min, Math.min(n.max, value.getAsDouble()));
            else if (s instanceof C020.Color c) c.rgb = value.getAsInt();
          } catch (RuntimeException ignored) { }
        }
      }
      if (m instanceof C011 esp && data.has("blocks") && data.get("blocks").isJsonArray()) {
        esp.blocks.clear();
        for (JsonElement block : data.getAsJsonArray("blocks")) if (block.isJsonPrimitive()) esp.blocks.add(block.getAsString());
      }
      KeyMapping key = keyOf(m);
      if (key != null && data.has("key")) {
        try {
          JsonElement savedKey = data.get("key");
          key.setKey(savedKey.getAsJsonPrimitive().isNumber()
            ? InputConstants.Type.KEYSYM.getOrCreate(savedKey.getAsInt())
            : InputConstants.getKey(savedKey.getAsString()));
        }
        catch (RuntimeException ignored) { }
      }
      if (data.has("toggleKey")) {
        try {
          JsonElement savedKey = data.get("toggleKey");
          m.toggleKey.setKey(savedKey.getAsJsonPrimitive().isNumber()
            ? InputConstants.Type.KEYSYM.getOrCreate(savedKey.getAsInt())
            : InputConstants.getKey(savedKey.getAsString()));
        }
        catch (RuntimeException ignored) { }
      }
    }
    KeyMapping.resetMapping();
  }

  private static KeyMapping keyOf(C017 m){
    if (m instanceof C006 a) return a.key;
    if (m instanceof C019 s) return s.key;
    if (m instanceof C007 a) return a.key;
    if (m instanceof C013 c) return c.key;
    return null;
  }

  private static JsonObject read(Path file){
    try { if (!Files.isRegularFile(file)) return null; return JsonParser.parseString(Files.readString(file)).getAsJsonObject(); }
    catch (IOException | RuntimeException ignored) { return null; }
  }

  private static void write(Path file, JsonObject data){
    try {
      Files.createDirectories(file.getParent());
      Path temp = file.resolveSibling(file.getFileName() + ".tmp");
      Files.writeString(temp, GSON.toJson(data));
      try { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE); }
      catch (AtomicMoveNotSupportedException ignored) { Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING); }
    } catch (IOException ignored) { }
  }
}
