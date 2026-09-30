package dk.mineclub.plus.core.util;

import java.util.Locale;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Turns whatever the server calls an item into an id Minecraft will accept.
 *
 * <p>The agreed shape is a Bukkit material name -- {@code DIAMOND_BLOCK} -- but the log merges
 * rows from three separate plugins and not all of them keep to it: a row can arrive carrying a
 * display name, {@code Dandelion (Dandelion)} among them. Minecraft refuses a path with anything
 * outside {@code [a-z0-9/._-]} by throwing, and a throw while the widget tree is being built
 * closes the whole window, so no id reaches a resource location without passing through here.
 *
 * <p>The normalisation is deliberately narrow: a display name differs from its id by case, spaces
 * and a parenthesised suffix, and undoing those three lands on the right item or on nothing at
 * all. An id that still does not fit is reported as unknown, which the caller already draws as a
 * coloured chip.
 */
public final class ItemIds {

  private static final String VANILLA = "minecraft";

  private ItemIds() {
  }

  /**
   * @param raw the id as the server wrote it, in either case
   * @return the split id, or {@code null} when nothing usable is left
   */
  public static @Nullable ItemId parse(@Nullable String raw) {
    if (raw == null) {
      return null;
    }

    String value = raw.toLowerCase(Locale.ROOT).trim();

    // "dandelion (dandelion)" -- a display name carrying its own id in brackets.
    int bracket = value.indexOf('(');
    if (bracket > 0) {
      value = value.substring(0, bracket).trim();
    }

    value = value.replace(' ', '_');
    if (value.isEmpty()) {
      return null;
    }

    // A namespaced id from the server wins over the vanilla default.
    String namespace = VANILLA;
    int colon = value.indexOf(':');
    if (colon > 0) {
      namespace = value.substring(0, colon);
      value = value.substring(colon + 1);
    }

    if (!isValidNamespace(namespace) || !isValidPath(value)) {
      return null;
    }

    return new ItemId(namespace, value);
  }

  /** The characters Minecraft allows in a namespace. */
  private static boolean isValidNamespace(String namespace) {
    if (namespace.isEmpty()) {
      return false;
    }

    for (int index = 0; index < namespace.length(); index++) {
      char character = namespace.charAt(index);
      if (!isIdCharacter(character)) {
        return false;
      }
    }

    return true;
  }

  /** The same set as a namespace, plus the folder separator. */
  private static boolean isValidPath(String path) {
    if (path.isEmpty()) {
      return false;
    }

    for (int index = 0; index < path.length(); index++) {
      char character = path.charAt(index);
      if (character != '/' && !isIdCharacter(character)) {
        return false;
      }
    }

    return true;
  }

  private static boolean isIdCharacter(char character) {
    return (character >= 'a' && character <= 'z')
        || (character >= '0' && character <= '9')
        || character == '_'
        || character == '-'
        || character == '.';
  }

  /** An id Minecraft will accept, split into its two halves. */
  public record ItemId(@NotNull String namespace, @NotNull String path) {

  }
}
