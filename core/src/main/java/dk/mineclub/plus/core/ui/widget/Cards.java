package dk.mineclub.plus.core.ui.widget;

import dk.mineclub.plus.core.util.Formats;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.ScrollWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The repeated building blocks of the dashboard: cards, stat tiles and label/value rows.
 *
 * <p>Keeping them here is what makes the pages feel like one product -- a card is a card on every
 * screen because there is exactly one place that builds it.
 */
public final class Cards {

  private Cards() {
  }

  /**
   * A titled panel. Callers append their content to the returned column.
   */
  public static @NotNull VerticalListWidget<Widget> card(String titleKey, String... extraIds) {
    VerticalListWidget<Widget> card = Widgets.column("mcp-card");
    for (String id : extraIds) {
      card.addId(id);
    }

    card.addChild(Widgets.i18n(titleKey, "mcp-card-title"));
    return card;
  }

  /**
   * As {@link #card}, but with a title that is already written.
   *
   * <p>Some titles are built from numbers and names -- "8x Glass" -- and cannot be a key in a
   * language file.
   */
  public static @NotNull VerticalListWidget<Widget> titled(String title, String... extraIds) {
    VerticalListWidget<Widget> card = Widgets.column("mcp-card");
    for (String id : extraIds) {
      card.addId(id);
    }

    card.addChild(Widgets.text(title, "mcp-card-title"));
    return card;
  }

  /**
   * A number tile.
   *
   * <p>Laid out as a plain box with each line pinned to an edge, so the caption, the figure and
   * the footer sit on the same baselines in every tile no matter how many lines each one carries.
   *
   * @param footer the line under the figure, or {@code null} to leave it out
   */
  public static @NotNull DivWidget stat(
      String captionKey,
      String value,
      @Nullable Component footer,
      boolean positive
  ) {
    DivWidget tile = Widgets.panel("mcp-stat");
    tile.addChild(Widgets.i18n(captionKey, "mcp-stat-caption"));
    tile.addChild(Widgets.text(value, "mcp-stat-value"));

    if (footer != null) {
      tile.addChild(Widgets.component(
          footer,
          "mcp-stat-delta",
          positive ? "mcp-positive" : "mcp-negative"
      ));
    }

    return tile;
  }

  /**
   * A label pinned left and a value pinned right. Both sit in a plain box and place themselves, so
   * the row needs no layout pass of its own.
   */
  public static @NotNull DivWidget row(String label, String value, String... valueIds) {
    DivWidget row = Widgets.panel("mcp-row");
    row.addChild(Widgets.text(label, "mcp-row-label"));
    row.addChild(Widgets.text(value, ids("mcp-row-value", valueIds)));
    return row;
  }

  public static @NotNull DivWidget translatedRow(
      String labelKey,
      String value,
      String... valueIds
  ) {
    DivWidget row = Widgets.panel("mcp-row");
    row.addChild(Widgets.i18n(labelKey, "mcp-row-label"));
    row.addChild(Widgets.text(value, ids("mcp-row-value", valueIds)));
    return row;
  }

  /**
   * A row whose right-hand side is a control rather than a string. Controls are taller than text,
   * so this row is taller than {@link #row} instead of letting the widget spill out of it.
   */
  public static @NotNull DivWidget widgetRow(String labelKey, Widget value) {
    DivWidget row = Widgets.panel("mcp-field");
    row.addChild(Widgets.i18n(labelKey, "mcp-field-label"));
    row.addChild(value.addId("mcp-field-value"));
    return row;
  }

  /**
   * Signed percentage styled green or red.
   */
  public static @NotNull Widget change(double percent, String... extraIds) {
    String[] all = new String[extraIds.length + 2];
    all[0] = "mcp-change";
    all[1] = percent >= 0.0D ? "mcp-positive" : "mcp-negative";
    System.arraycopy(extraIds, 0, all, 2, extraIds.length);

    return Widgets.text(Formats.percent(percent), all);
  }

  /**
   * Wraps a column so it scrolls when it outgrows the area it is placed in.
   */
  public static @NotNull ScrollWidget scroll(VerticalListWidget<Widget> content,
      String... extraIds) {
    ScrollWidget scroll = new ScrollWidget(content);
    scroll.addId("mcp-scroll");
    for (String id : extraIds) {
      scroll.addId(id);
    }

    return scroll;
  }

  /**
   * The empty state used while the first request is still running.
   */
  public static @NotNull Widget placeholder(String messageKey) {
    return Widgets.i18n(messageKey, "mcp-placeholder-text");
  }

  private static String[] ids(String first, String[] rest) {
    String[] all = new String[rest.length + 1];
    all[0] = first;
    System.arraycopy(rest, 0, all, 1, rest.length);
    return all;
  }
}
