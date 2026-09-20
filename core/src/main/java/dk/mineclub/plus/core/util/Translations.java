package dk.mineclub.plus.core.util;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import dk.mineclub.plus.core.config.Language;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import net.labymod.api.util.I18n;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The window's text, in the language the player picked.
 *
 * <p>LabyMod always translates into the client's own language and offers no way around it:
 * {@link I18n#translate} takes no locale. Letting the player choose therefore means looking the
 * keys up in the addon's own files, which is what happens here -- the same two json files LabyMod
 * would have read, loaded once at startup and flattened to key/text pairs.
 *
 * <p>A key missing in the chosen language falls back to English, and then to LabyMod itself, so a
 * key that belongs to LabyMod rather than to the addon is still translated.
 */
public final class Translations {

  private static final String PATH = "/assets/mineclubplus/i18n/%s.json";
  private static final Gson GSON = new Gson();

  private static final Map<Language, Map<String, String>> LOADED =
      new EnumMap<>(Language.class);

  private static volatile Language language = Language.ENGLISH;

  private Translations() {
  }

  /** Loads both languages. Called once when the addon starts. */
  public static void load() {
    for (Language value : Language.values()) {
      LOADED.put(value, read(value));
    }
  }

  public static void language(@NotNull Language value) {
    language = value;
  }

  public static @NotNull Language language() {
    return language;
  }

  /**
   * @param key the key, for example {@code mineclubplus.transporter.takeButton}
   * @param args the placeholders in the text, in the order they appear
   * @return the text in the chosen language
   */
  public static @NotNull String get(@NotNull String key, Object... args) {
    String text = lookup(language, key);
    if (text == null && language != Language.ENGLISH) {
      text = lookup(Language.ENGLISH, key);
    }

    if (text == null) {
      // Not one of our keys, so it is LabyMod's -- which only knows the client's language.
      return I18n.translate(key, args);
    }

    if (args == null || args.length == 0) {
      return text;
    }

    try {
      return String.format(Locale.ROOT, text, args);
    } catch (RuntimeException exception) {
      // A malformed placeholder must not take the page down; show the text unformatted.
      return text;
    }
  }

  /**
   * As {@link #get}, but without falling back to LabyMod or to the key itself.
   *
   * @return the text, or {@code null} when the key is not one of the addon's, leaving the caller
   *     to decide what to show instead
   */
  public static @Nullable String find(@NotNull String key) {
    String text = lookup(language, key);
    return text != null || language == Language.ENGLISH
        ? text
        : lookup(Language.ENGLISH, key);
  }

  private static @Nullable String lookup(Language value, String key) {
    Map<String, String> texts = LOADED.get(value);
    return texts == null ? null : texts.get(key);
  }

  private static Map<String, String> read(Language value) {
    Map<String, String> texts = new HashMap<>();
    String path = String.format(Locale.ROOT, PATH, value.file());

    try (InputStream stream = Translations.class.getResourceAsStream(path)) {
      if (stream == null) {
        return texts;
      }

      JsonObject root = GSON.fromJson(
          new InputStreamReader(stream, StandardCharsets.UTF_8),
          JsonObject.class
      );
      flatten("", root, texts);
    } catch (IOException | RuntimeException exception) {
      // Without the file the keys show instead of the texts, which still beats taking the
      // whole addon down over a missing translation.
      return texts;
    }

    return texts;
  }

  /**
   * The json is nested ({@code mineclubplus.transporter.take}) while lookups are a single key.
   * This walks the tree once and flattens it.
   */
  private static void flatten(String prefix, JsonObject object, Map<String, String> into) {
    for (Map.Entry<String, JsonElement> entry : object.entrySet()) {
      String key = prefix.isEmpty() ? entry.getKey() : prefix + "." + entry.getKey();
      JsonElement value = entry.getValue();

      if (value.isJsonObject()) {
        flatten(key, value.getAsJsonObject(), into);
      } else if (value.isJsonPrimitive()) {
        into.put(key, value.getAsString());
      }
    }
  }
}
