package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.api.model.LogEntry;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.widget.Cards;
import dk.mineclub.plus.core.ui.widget.ItemIcons;
import dk.mineclub.plus.core.ui.widget.Pager;
import dk.mineclub.plus.core.ui.widget.States;
import dk.mineclub.plus.core.ui.widget.Widgets;
import dk.mineclub.plus.core.util.Formats;
import dk.mineclub.plus.core.util.Translations;
import java.util.List;
import java.util.UUID;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The player's own log.
 *
 * <p>Every row shows what happened and with whom: a purchase or a sale renders the block itself,
 * while a trade or transfer with another player renders their head. An admin shop purchase has no
 * counterparty, and then nothing is drawn rather than an empty head.
 *
 * <p>The rows arrive sorted from the server. Shop, transfers and transporter are three separate
 * logs, merged there so the order is the same for everyone.
 */
public final class LogsPage implements PageRenderer {

  /**
   * The head used for the console and anything else that is not a player.
   *
   * <p>The same one the panel's coin log uses, so a row looks alike in both places. A lookup on
   * the name "Konsol" would return a stranger's skin.
   */
  private static final UUID SYSTEM_HEAD =
      UUID.fromString("f78a4d8d-d51b-4b39-98a3-230f2de0c670");

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    if (data == null) {
      return activity.addon().api().snapshot().error() == null
          ? States.loading()
          : States.failed(() -> activity.addon().api().refresh(true));
    }

    List<LogEntry> entries = data.logs();
    if (entries.isEmpty()) {
      // No retry button: fetching an empty log again does not make it less empty.
      return States.message("mineclubplus.logs.empty", null, false);
    }

    // No scrolling: the rows render real blocks, and those are drawn after the scissor around
    // a scrolling area has been popped, so a half scrolled row would sit past the window.
    int perPage = activity.layout().listRows();
    int pages = Math.max(1, (entries.size() + perPage - 1) / perPage);
    int current = Math.min(activity.listPage(), pages - 1);

    DivWidget frame = Widgets.panel("mcp-paged");
    VerticalListWidget<Widget> page = Widgets.column("mcp-paged-body");
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.logs.title");

    int from = current * perPage;
    int to = Math.min(from + perPage, entries.size());
    for (int index = from; index < to; index++) {
      card.addChild(this.createRow(entries.get(index)));
    }

    page.addChild(card);

    // The arrows sit on their own at the bottom. Placed after the card in the list, one row
    // too many would push them past the window edge instead of being clipped.
    frame.addChild(page);
    frame.addChild(Pager.create(
        current, pages, entries.size(), activity::setListPage, "mcp-paged-pager"));

    return frame;
  }

  /**
   * One row: time, image, text and what it cost.
   *
   * <p>The image is the block when an item was involved, and otherwise the other player's head --
   * a plain coin transfer has no item to show.
   */
  private Widget createRow(LogEntry entry) {
    DivWidget row = Widgets.panel("mcp-log-row");

    row.addChild(Widgets.text(Formats.time(entry.timestamp()), "mcp-log-time"));
    row.addChild(this.createIcon(entry));
    row.addChild(Widgets.text(describe(entry), "mcp-log-text"));

    if (entry.playerUuid() != null || !entry.playerName().isEmpty()) {
      row.addChild(Widgets.text(entry.playerName(), "mcp-log-player"));
    }

    if (entry.coins() != 0.0D) {
      row.addChild(Widgets.text(
          Formats.signed(entry.coins()),
          "mcp-log-coins",
          entry.coins() >= 0.0D ? "mcp-positive" : "mcp-negative"
      ));
    }

    return row;
  }

  private Widget createIcon(LogEntry entry) {
    String item = entry.item();
    if (item != null && !item.isEmpty()) {
      return ItemIcons.of(item, "mcp-log-icon");
    }

    UUID uuid = parseUuid(entry.playerUuid());
    if (uuid != null && entry.isPlayer()) {
      return Widgets.icon(Icon.head(uuid), "mcp-log-head");
    }

    if (!entry.playerName().isEmpty()) {
      // The console, a plot or another service: a fixed head rather than a lookup by name.
      return Widgets.icon(Icon.head(SYSTEM_HEAD), "mcp-log-head");
    }

    // Neither item nor counterparty, a fee for instance. An empty slot keeps the rest
    // aligned.
    return Widgets.panel("mcp-log-head");
  }

  /**
   * @return what happened, for example "Sold 64x Diamond" or "Sent 12x Cobblestone"
   */
  private static String describe(LogEntry entry) {
    String what = entry.itemName().isEmpty()
        ? Translations.get("mineclubplus.logs.coins")
        : Formats.number(entry.amount()) + "x " + entry.itemName();

    return Translations.get(entry.actionKey()) + " " + what;
  }

  private static @Nullable UUID parseUuid(@Nullable String raw) {
    if (raw == null || raw.isEmpty()) {
      return null;
    }

    try {
      return UUID.fromString(raw);
    } catch (IllegalArgumentException exception) {
      return null;
    }
  }
}
