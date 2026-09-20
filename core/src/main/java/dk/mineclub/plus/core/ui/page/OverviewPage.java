package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.api.model.EconomySnapshot;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.api.model.PlayerProfile;
import dk.mineclub.plus.core.api.model.ServerStatus;
import dk.mineclub.plus.core.api.model.Transaction;
import dk.mineclub.plus.core.api.model.TransporterItem;
import dk.mineclub.plus.core.api.model.TransporterSnapshot;
import dk.mineclub.plus.core.service.QuickAction;
import dk.mineclub.plus.core.service.ServerCommands;
import dk.mineclub.plus.core.ui.MineClubPage;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.widget.Cards;
import dk.mineclub.plus.core.ui.widget.ItemIcons;
import dk.mineclub.plus.core.ui.widget.Widgets;
import dk.mineclub.plus.core.util.Avatars;
import dk.mineclub.plus.core.util.Formats;
import dk.mineclub.plus.core.util.Ranks;
import dk.mineclub.plus.core.util.Translations;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.entity.player.ClientPlayer;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.network.NetworkPlayerInfo;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The landing page: who you are, the three numbers that matter, your biggest holdings, the latest
 * economy activity, and the server rail with the quick commands.
 */
public final class OverviewPage implements PageRenderer {

  private static final int TOP_HOLDINGS = 8;
  private static final int RECENT_ACTIVITY = 4;

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    if (data == null) {
      return Cards.placeholder("mineclubplus.state.loading");
    }

    // Stacked: the holdings card is pinned to the floor and everything else scrolls above it.
    //
    // It would be simpler to put the whole page in one scroll, and that is what this used to do
    // -- at the cost of the holdings falling back to colour chips, because Minecraft draws item
    // blocks in a pass of its own, after any scissor has been popped, so a scrolled block lands
    // outside the card and over whatever is below the window. Pinning the card is what lets the
    // small layout show the same real blocks as the wide one.
    if (activity.isStacked()) {
      DivWidget stacked = Widgets.panel("mcp-overview-stacked");

      VerticalListWidget<Widget> above = Widgets.column("mcp-page");
      above.addChild(this.createWelcome(activity, data.profile()));
      above.addChild(this.createStats(data, false));
      above.addChild(this.createServerCard(data.server()));
      above.addChild(this.createQuickActions(activity));
      stacked.addChild(Cards.scroll(above, "mcp-overview-stacked-scroll"));

      stacked.addChild(this.createHoldings(
          data.transporter(),
          activity.layout().stackedHoldingRows(),
          "mcp-overview-stacked-holdings"
      ));

      return stacked;
    }

    DivWidget page = Widgets.panel("mcp-overview");

    // The left column does not scroll. That is what lets the holdings show real blocks: a
    // rendered item cannot be clipped by a scrolling area, so it must not live in one.
    DivWidget main = Widgets.panel("mcp-overview-main");
    main.addChild(this.createWelcome(activity, data.profile()));
    main.addChild(this.createStats(data, true));
    main.addChild(this.createHoldings(
        data.transporter(),
        activity.layout().holdingRows(),
        "mcp-holdings-card"
    ));
    page.addChild(main);

    VerticalListWidget<Widget> rail = Widgets.column("mcp-overview-rail");
    rail.addChild(this.createServerCard(data.server()));
    rail.addChild(this.createQuickActions(activity));
    rail.addChild(this.createActivity(data.economy()));
    page.addChild(Cards.scroll(rail, "mcp-overview-rail-scroll"));

    return page;
  }

  private Widget createWelcome(MineClubPlusActivity activity, @Nullable PlayerProfile profile) {
    DivWidget card = Widgets.panel("mcp-card", "mcp-welcome");

    card.addChild(Widgets.icon(
        Avatars.of(
            profile == null ? null : profile.uniqueId(),
            profile == null ? null : profile.name(),
            activity.addon().configuration().avatarStyle().get()
        ),
        "mcp-welcome-avatar"
    ));

    card.addChild(Widgets.i18n("mineclubplus.overview.welcome", "mcp-welcome-caption"));
    card.addChild(Widgets.text(profile == null ? "..." : profile.name(), "mcp-welcome-name"));

    HorizontalListWidget meta = Widgets.row("mcp-welcome-meta");
    if (profile != null) {
      meta.addEntry(Widgets.text(
          Ranks.displayName(profile.rank()),
          "mcp-chip",
          Ranks.styleId(profile.rank())
      ));
      if (profile.playtimeMinutes() > 0L) {
        meta.addEntry(Widgets.text(
            Formats.playtime(profile.playtimeMinutes()),
            "mcp-welcome-detail"
        ));
      }
    }
    card.addChild(meta);

    card.addChild(Widgets.i18nButton(
        "mineclubplus.overview.openTransporter",
        () -> activity.openPage(MineClubPage.TRANSPORTER),
        "mcp-accent-button",
        "mcp-welcome-button"
    ));

    return card;
  }

  /**
   * @param pinned true for the two-column layout, where the row is placed by edge. The stacked
   *               layout leaves that id off: it carries {@code top: 68} for the pinned page, and
   *               inside a list that only pushes the row down.
   */
  private Widget createStats(MineClubSnapshot data, boolean pinned) {
    EconomySnapshot economy = data.economy();
    TransporterSnapshot transporter = data.transporter();

    HorizontalListWidget row = pinned
        ? Widgets.row("mcp-stat-row", "mcp-overview-stats")
        : Widgets.row("mcp-stat-row");

    row.addEntry(Cards.stat(
        "mineclubplus.overview.coins",
        economy == null ? "-" : Formats.number(economy.balance()),
        economy == null ? null : today(economy.balanceChange24h()),
        economy == null || economy.balanceChange24h() >= 0.0D
    ));

    row.addEntry(Cards.stat(
        "mineclubplus.overview.transporter",
        transporter == null ? "-" : Formats.number(transporter.totalAmount()),
        transporter == null ? null : Component.text(Translations.get(
            "mineclubplus.transporter.subtitle",
            Formats.number(transporter.items().size())
        )),
        true
    ));

    return row;
  }

  /**
   * @return the change since yesterday, signed and labelled
   */
  private static Component today(double change) {
    return Component.text(Translations.get(
        "mineclubplus.overview.today",
        Formats.signed(change)
    ));
  }

  /**
   * @param rows   how many holdings to list; the caller measures what fits
   * @param cardId where the card is placed -- it fills the rest of the column on the wide page,
   *               and the floor of the window on the stacked one. Either way it is placed by
   *               edge and never scrolls, which is what lets every row draw a real item block.
   */
  private Widget createHoldings(
      @Nullable TransporterSnapshot transporter,
      int rows,
      String cardId
  ) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.overview.holdings", cardId);
    if (transporter == null || transporter.items().isEmpty()) {
      card.addChild(Cards.placeholder("mineclubplus.state.empty"));
      return card;
    }

    // Ranked by how much of it you hold, not by what it is worth: put in 110 oak logs and it
    // takes 111 diamond blocks to push the logs down to second.
    List<TransporterItem> items = new ArrayList<>(transporter.items());
    items.sort(Comparator.comparingLong(TransporterItem::amount).reversed());

    int limit = Math.min(rows, items.size());
    for (int index = 0; index < limit; index++) {
      TransporterItem item = items.get(index);

      DivWidget row = Widgets.panel("mcp-holding");
      row.addChild(ItemIcons.of(item.id(), "mcp-holding-icon"));
      row.addChild(Widgets.component(
          ItemIcons.nameOf(item.id(), item.displayName()),
          "mcp-holding-name"
      ));
      row.addChild(Widgets.text(Formats.compact(item.amount()), "mcp-holding-value"));
      row.addChild(Cards.change(item.change24h(), "mcp-holding-change"));

      card.addChild(row);
    }

    return card;
  }

  private Widget createActivity(@Nullable EconomySnapshot economy) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.overview.activity");
    if (economy == null || economy.transactions().isEmpty()) {
      card.addChild(Cards.placeholder("mineclubplus.state.empty"));
      return card;
    }

    List<Transaction> transactions = economy.transactions();
    int limit = Math.min(RECENT_ACTIVITY, transactions.size());
    for (int index = 0; index < limit; index++) {
      card.addChild(transactionRow(transactions.get(index), true));
    }

    return card;
  }

  /**
   * Shared with the economy page so a transaction reads the same everywhere.
   *
   * @param compact drops the time column, for the narrow rail where the label would otherwise
   *                run under the amount
   */
  static DivWidget transactionRow(Transaction transaction, boolean compact) {
    DivWidget row = Widgets.panel("mcp-transaction");
    if (compact) {
      row.addId("mcp-transaction-compact");
    } else {
      row.addChild(Widgets.text(Formats.time(transaction.timestamp()), "mcp-transaction-time"));
    }

    row.addChild(Widgets.text(transaction.label(), "mcp-transaction-label"));
    row.addChild(Widgets.text(
        Formats.compact(transaction.amount()),
        "mcp-transaction-amount",
        transaction.income() ? "mcp-positive" : "mcp-negative"
    ));

    return row;
  }

  private Widget createServerCard(@Nullable ServerStatus server) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.overview.server");
    if (server == null) {
      card.addChild(Cards.placeholder("mineclubplus.state.offline"));
      return card;
    }

    card.addChild(Widgets.text(server.host(), "mcp-server-host"));
    card.addChild(Cards.translatedRow(
        "mineclubplus.overview.playersOnline",
        Formats.number(server.onlinePlayers())
    ));
    card.addChild(Cards.translatedRow("mineclubplus.overview.ping", ping()));

    return card;
  }

  /**
   * Latency is client side state rather than anything the API reports, so it is read from the
   * connection each time the card is built.
   */
  private static String ping() {
    ClientPlayer player = Laby.labyAPI().minecraft().getClientPlayer();
    NetworkPlayerInfo info = player == null ? null : player.getNetworkPlayerInfo();
    if (info == null) {
      return "-";
    }

    return info.getCurrentPing() + " ms";
  }

  private Widget createQuickActions(MineClubPlusActivity activity) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.overview.quickActions");

    for (QuickAction action : QuickAction.defaults()) {
      DivWidget row = Widgets.panel("mcp-quick");
      Widgets.clickable(row, () -> {
        ServerCommands.raw(action.command());
        activity.closeWindow();
      });

      row.addChild(Widgets.text(action.command(), "mcp-quick-command"));
      row.addChild(Widgets.i18n(action.descriptionKey(), "mcp-quick-description"));

      card.addChild(row);
    }

    return card;
  }
}
