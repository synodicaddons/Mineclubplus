package dk.mineclub.plus.core.service;

import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * A one-click server command shown in the overview rail.
 *
 * @param id      used to build the translation keys
 * @param command the command sent to the server, including the leading slash
 */
public record QuickAction(@NotNull String id, @NotNull String command) {

  /**
   * The commands the MineClub plugins actually register today -- the transporter sub-commands from
   * the lobby plugin and the economy plugin's balance. Warps and homes are added once the API
   * exposes them.
   */
  public static @NotNull List<QuickAction> defaults() {
    return Arrays.asList(
        new QuickAction("putall", "/transporter putall"),
        new QuickAction("open", "/transporter open"),
        new QuickAction("list", "/transporter list"),
        new QuickAction("balance", "/balance")
    );
  }

  public @NotNull String labelKey() {
    return "mineclubplus.quick." + this.id + ".label";
  }

  public @NotNull String descriptionKey() {
    return "mineclubplus.quick." + this.id + ".description";
  }
}
