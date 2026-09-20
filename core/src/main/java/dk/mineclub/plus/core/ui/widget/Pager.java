package dk.mineclub.plus.core.ui.widget;

import dk.mineclub.plus.core.util.Formats;
import java.util.function.IntConsumer;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.input.ButtonWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import org.jetbrains.annotations.NotNull;

/**
 * The arrows and "Page 3 of 7" under a list.
 *
 * <p>Paging is not decoration: Minecraft flushes rendered items in a pass of their own, after the
 * scissor around a scrolling area has been popped, so a half scrolled row draws its blocks past
 * the window. One page at a time removes the overlap at its source.
 */
public final class Pager {

  private Pager() {
  }

  /**
   * @param page    the page shown, counted from 0
   * @param pages   how many pages there are
   * @param total   how many entries there are, for the label
   * @param onPage  called with the page to show
   */
  public static @NotNull Widget create(
      int page,
      int pages,
      int total,
      IntConsumer onPage,
      String... ids
  ) {
    // A list, so the arrows sit beside the label instead of being nudged onto it.
    HorizontalListWidget pager = Widgets.row("mcp-pager");
    for (String id : ids) {
      pager.addId(id);
    }

    ButtonWidget previous = Widgets.button("<", () -> onPage.accept(page - 1),
        "mcp-pager-button");
    previous.setEnabled(page > 0);
    pager.addEntry(previous);

    pager.addEntry(Widgets.i18nArgs(
        "mineclubplus.transporter.pageOf",
        new Object[]{page + 1, pages, Formats.number(total)},
        "mcp-pager-label"
    ));

    ButtonWidget next = Widgets.button(">", () -> onPage.accept(page + 1), "mcp-pager-button");
    next.setEnabled(page < pages - 1);
    pager.addEntry(next);

    return pager;
  }
}
