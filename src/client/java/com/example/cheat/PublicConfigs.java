package com.example.cheat;

import com.google.gson.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.*;
import net.fabricmc.loader.api.FabricLoader;

/** Publishes and browses named profiles in the public GitHub config library. */
public final class PublicConfigs {
  private static final String OWNER = "lunba38-pixel";
  private static final String REPO = "lunba-client-configs";
  private static final String API = "https://api.github.com/repos/" + OWNER + "/" + REPO;
  private static final String API_VERSION = "2026-03-10";
  private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
  private static final Gson GSON = new Gson();
  private static final Path TOKEN_FILE = FabricLoader.getInstance().getConfigDir()
    .resolve("lunba-client").resolve("github-token.txt");

  private PublicConfigs() {}
  public record Entry(String name, String downloadUrl) {}

  public static CompletableFuture<List<Entry>> list(){
    return CompletableFuture.supplyAsync(() -> {
      try {
        HttpRequest request = HttpRequest.newBuilder(URI.create(API + "/contents/configs?ref=main"))
          .timeout(Duration.ofSeconds(20)).header("Accept", "application/vnd.github+json")
          .header("X-GitHub-Api-Version", API_VERSION).GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() == 404) return List.of();
        if (response.statusCode() != 200) throw new IOException("GitHub returned HTTP " + response.statusCode() + ".");
        JsonElement root = JsonParser.parseString(response.body());
        if (!root.isJsonArray()) throw new IOException("The public configs folder could not be read.");
        List<Entry> entries = new ArrayList<>();
        for (JsonElement item : root.getAsJsonArray()) {
          if (!item.isJsonObject()) continue;
          JsonObject file = item.getAsJsonObject();
          String name = file.has("name") ? file.get("name").getAsString() : "";
          String url = file.has("download_url") && !file.get("download_url").isJsonNull() ? file.get("download_url").getAsString() : "";
          if (name.toLowerCase(Locale.ROOT).endsWith(".json") && !url.isBlank()) entries.add(new Entry(name.substring(0, name.length()-5), url));
        }
        entries.sort(Comparator.comparing(Entry::name, String.CASE_INSENSITIVE_ORDER));
        return List.copyOf(entries);
      } catch (Exception e) { throw new CompletionException(e); }
    });
  }

  public static CompletableFuture<String> download(Entry entry){
    return CompletableFuture.supplyAsync(() -> {
      try {
        HttpRequest request = HttpRequest.newBuilder(URI.create(entry.downloadUrl()))
          .timeout(Duration.ofSeconds(20)).header("Accept", "application/vnd.github+json").GET().build();
        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());
        if (response.statusCode() != 200) throw new IOException("Could not download that config (HTTP " + response.statusCode() + ").");
        return response.body();
      } catch (Exception e) { throw new CompletionException(e); }
    });
  }

  public static CompletableFuture<Void> publish(String rawName, String json){
    String slug = rawName == null ? "" : rawName.trim().replaceAll("[^A-Za-z0-9._-]+", "-").replaceAll("^-+|-+$", "");
    if (slug.isBlank()) return CompletableFuture.failedFuture(new IllegalArgumentException("Enter a config name using letters or numbers."));
    if (slug.length() > 64) slug = slug.substring(0, 64);
    final String path = "configs/" + slug + ".json";
    final String file = slug;
    return CompletableFuture.runAsync(() -> {
      try {
        if (!Files.isRegularFile(TOKEN_FILE)) throw new IOException("Create " + TOKEN_FILE + " and paste your fine-grained token inside.");
        String token = Files.readString(TOKEN_FILE, StandardCharsets.UTF_8).trim();
        if (token.isBlank()) throw new IOException("Paste your GitHub token into " + TOKEN_FILE + ".");
        String endpoint = API + "/contents/" + path;
        String sha = null;
        HttpRequest check = authorized(token, endpoint).GET().build();
        HttpResponse<String> prior = HTTP.send(check, HttpResponse.BodyHandlers.ofString());
        if (prior.statusCode() == 200) {
          JsonObject priorJson = JsonParser.parseString(prior.body()).getAsJsonObject();
          if (priorJson.has("sha")) sha = priorJson.get("sha").getAsString();
        } else if (prior.statusCode() != 404) {
          throw new IOException("Could not check the public config repository (HTTP " + prior.statusCode() + ").");
        }
        JsonObject body = new JsonObject();
        body.addProperty("message", "Publish config: " + file);
        body.addProperty("content", Base64.getEncoder().encodeToString(json.getBytes(StandardCharsets.UTF_8)));
        if (sha != null) body.addProperty("sha", sha);
        HttpRequest upload = authorized(token, endpoint).header("Content-Type", "application/json")
          .PUT(HttpRequest.BodyPublishers.ofString(GSON.toJson(body))).build();
        HttpResponse<String> result = HTTP.send(upload, HttpResponse.BodyHandlers.ofString());
        if (result.statusCode() < 200 || result.statusCode() >= 300)
          throw new IOException("GitHub could not publish the config (HTTP " + result.statusCode() + "). Check the token and repository permissions.");
      } catch (Exception e) { throw new CompletionException(e); }
    });
  }

  private static HttpRequest.Builder authorized(String token, String url){
    return HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(20))
      .header("Accept", "application/vnd.github+json").header("Authorization", "Bearer " + token)
      .header("X-GitHub-Api-Version", API_VERSION);
  }

  public static String message(Throwable error){
    Throwable cause = error;
    while ((cause instanceof CompletionException || cause instanceof ExecutionException) && cause.getCause() != null) cause = cause.getCause();
    return cause.getMessage() == null ? "GitHub request failed." : cause.getMessage();
  }
}
