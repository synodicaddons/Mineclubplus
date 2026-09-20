package dk.mineclub.plus.core.service;

import java.util.Locale;
import net.labymod.api.Laby;
import net.labymod.api.client.chat.ChatExecutor;
import org.jetbrains.annotations.NotNull;

/**
 * Sends the MineClub commands that back the transporter actions.
 *
 * <p>The addon deliberately does not move items itself -- it asks the server, which keeps the
 * lobby plugin as the single authority over inventories and means every action behaves exactly
 * like typing it in chat.
 *
 * <p>The sub-commands and the uppercase material names match
 * {@code dk.mineclub.lobbyplugin.transporter.commands}.
 */
public final class ServerCommands {

  private ServerCommands() {
  }

  /**
   * {@code /transporter get <MATERIAL> <amount>}
   */
  public static void takeOut(@NotNull String itemId, long amount) {
    run("/transporter get %s %d", material(itemId), amount);
  }

  /**
   * {@code /transporter get <MATERIAL>} -- with no amount the plugin takes everything that fits
   * in the inventory, so "take all" is the same call without the last argument.
   */
  public static void takeAll(@NotNull String itemId) {
    run("/transporter get %s", material(itemId));
  }

  /**
   * {@code /transporter put <MATERIAL> <amount>}
   */
  public static void putIn(@NotNull String itemId, long amount) {
    run("/transporter put %s %d", material(itemId), amount);
  }

  /**
   * {@code /transporter putall}
   */
  public static void putAll() {
    run("/transporter putall");
  }

  /**
   * {@code /transporter open} -- the in-game transporter menu.
   */
  public static void openInGame() {
    run("/transporter open");
  }

  /**
   * {@code /maskinadgang} -- the shop where machines are bought.
   *
   * <p>No command buys a specific machine; the plugin sells them through the menu, so that is
   * what this opens.
   */
  public static void openMachineShop() {
    run("/maskinadgang");
  }

  /**
   * {@code /shopbuy <MATERIAL> <amount>} -- buys from the admin shop.
   *
   * <p>The price is not part of the call. The server looks it up on the sign and charges it; this
   * only states what and how much.
   */
  public static void buyFromShop(@NotNull String itemId, int amount) {
    run("/shopbuy %s %d", material(itemId), amount);
  }

  /**
   * {@code /transporter info <MATERIAL>}
   */
  public static void info(@NotNull String itemId) {
    run("/transporter info %s", material(itemId));
  }

  /**
   * Sends a raw command the user picked from the quick actions.
   */
  public static void raw(@NotNull String command) {
    String trimmed = command.trim();
    if (trimmed.isEmpty()) {
      return;
    }

    // Sent verbatim: the text may contain percent signs.
    send(trimmed.startsWith("/") ? trimmed : "/" + trimmed);
  }

  /**
   * The plugin matches Bukkit material names, which are upper case.
   */
  private static String material(String itemId) {
    return itemId.toUpperCase(Locale.ROOT);
  }

  private static void run(String format, Object... arguments) {
    send(String.format(Locale.ROOT, format, arguments));
  }

  private static void send(String command) {
    ChatExecutor chat = Laby.labyAPI().minecraft().chatExecutor();
    if (chat == null) {
      return;
    }

    chat.chat(command, true);
  }
}
