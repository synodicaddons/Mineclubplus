package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.MineClubPlusAddon;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.ui.MineClubPage;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.widget.Widgets;
import net.labymod.api.Laby;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What a released client shows when it is not on MineClub.
 *
 * <p>Every figure in the window would be sample data off the network, and a dashboard full of
 * invented numbers is worse than no dashboard -- so this takes the whole window instead of
 * sitting beside a menu that leads nowhere, and it offers the one thing that helps: connecting.
 *
 * <p>The settings stay reachable from the second button. The hotkey is worth changing even when
 * there is nothing to fetch, and with the sidebar gone there would otherwise be no way in.
 *
 * <p>The dev client never sees this page; there the sample data is the point.
 *
 * <p>Takes no snapshot: it is the page shown precisely because there is nothing real to show.
 */
public final class OffNetworkPage implements PageRenderer {

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    DivWidget panel = Widgets.panel("mcp-offnetwork");

    panel.addChild(Widgets.i18n("mineclubplus.offNetwork.title", "mcp-offnetwork-title"));
    panel.addChild(Widgets.text(
        MineClubPlusAddon.SERVER_ADDRESS,
        "mcp-offnetwork-address"
    ));
    panel.addChild(Widgets.i18n("mineclubplus.offNetwork.hint", "mcp-offnetwork-hint"));

    panel.addChild(Widgets.i18nButton(
        "mineclubplus.offNetwork.connect",
        () -> connect(activity),
        "mcp-accent-button",
        "mcp-offnetwork-connect"
    ));

    panel.addChild(Widgets.i18nButton(
        "mineclubplus.offNetwork.settings",
        () -> activity.openPage(MineClubPage.SETTINGS),
        "mcp-ghost-button",
        "mcp-offnetwork-settings"
    ));

    return panel;
  }

  /**
   * Closes the window before connecting.
   *
   * <p>Joining swaps the screen out from under us, and leaving our own window on top of that
   * would draw the connect progress behind a dashboard that has nothing to show anyway.
   */
  private static void connect(MineClubPlusActivity activity) {
    activity.closeWindow();
    Laby.labyAPI().serverController().joinServer(MineClubPlusAddon.SERVER_ADDRESS);
  }
}
