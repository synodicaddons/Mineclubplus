package dk.mineclub.plus.core.ui.widget;

import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.action.Pressable;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * What a page shows when there is nothing to show.
 *
 * <p>The message sits in the middle of the area rather than in a corner, and sets all four of its
 * own edges: a text with no bounds is drawn outside the window, because it would otherwise
 * inherit them from the scrolling area it usually sits in.
 */
public final class States {

  private States() {
  }

  /**
   * @param messageKey the text to show
   * @param retry      the button that tries again, or {@code null} when there is nothing to retry
   *                   -- fetching an empty log again does not make it less empty
   * @param failed     true for an error, which puts the message in red
   */
  public static @NotNull Widget message(
      @NotNull String messageKey,
      @Nullable Pressable retry,
      boolean failed
  ) {
    DivWidget panel = Widgets.panel("mcp-state");

    panel.addChild(Widgets.i18n(
        messageKey,
        "mcp-state-text",
        failed ? "mcp-state-failed" : "mcp-state-quiet"
    ));

    if (retry != null) {
      panel.addChild(Widgets.i18nButton(
          "mineclubplus.action.retry",
          retry,
          "mcp-ghost-button",
          "mcp-state-button"
      ));
    }

    return panel;
  }

  /** The message shown while loading. No button: something is already running. */
  public static @NotNull Widget loading() {
    return message("mineclubplus.state.loading", null, false);
  }

  /** The message shown when the request failed. */
  public static @NotNull Widget failed(@NotNull Pressable retry) {
    return message("mineclubplus.state.failed", retry, true);
  }
}
