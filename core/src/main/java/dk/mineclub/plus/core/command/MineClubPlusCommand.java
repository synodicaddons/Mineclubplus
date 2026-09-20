package dk.mineclub.plus.core.command;

import dk.mineclub.plus.core.util.Translations;
import dk.mineclub.plus.core.MineClubPlusAddon;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import net.labymod.api.client.chat.command.Command;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.component.format.NamedTextColor;
import org.jetbrains.annotations.NotNull;

/**
 * {@code /mineclub} (alias {@code /mcp}) opens the dashboard, {@code /mineclub refresh} forces a
 * new request. Useful when the hotkey is taken by another mod.
 */
public class MineClubPlusCommand extends Command {

  private final MineClubPlusAddon addon;

  public MineClubPlusCommand(@NotNull MineClubPlusAddon addon) {
    super("mineclub", "mcp");

    this.addon = addon;
  }

  @Override
  public boolean execute(String prefix, String[] arguments) {
    // Switched off, the addon has to behave as though it were not installed. Returning false
    // passes the line through untouched rather than swallowing it, so the player still gets an
    // answer -- from the server -- instead of silence.
    if (!this.addon.configuration().enabled().get()) {
      return false;
    }

    if (arguments.length > 0 && arguments[0].equalsIgnoreCase("refresh")) {
      this.addon.api().refresh(true);
      this.displayMessage(Component.text(
          Translations.get("mineclubplus.command.refreshing"),
          NamedTextColor.GRAY
      ));
      return true;
    }

    this.openActivity(new MineClubPlusActivity(this.addon));
    return true;
  }
}
