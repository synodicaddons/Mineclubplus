package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.api.model.EconomySnapshot;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.api.model.Transaction;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.widget.Cards;
import dk.mineclub.plus.core.ui.widget.Widgets;
import dk.mineclub.plus.core.util.Formats;
import java.util.List;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.ComponentWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.HorizontalListWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Coins in, coins out. Earnings and spendings are summed from the transaction log for the selected
 * period, so the numbers always agree with the list below them.
 */
public final class EconomyPage implements PageRenderer {

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    EconomySnapshot economy = data == null ? null : data.economy();
    if (economy == null) {
      return Cards.placeholder("mineclubplus.state.loading");
    }

    List<Transaction> transactions = activity.economyPeriod().filter(economy.transactions());

    double earned = 0.0D;
    double spent = 0.0D;
    for (Transaction transaction : transactions) {
      if (transaction.income()) {
        earned += transaction.amount();
      } else {
        spent += Math.abs(transaction.amount());
      }
    }

    VerticalListWidget<Widget> page = Widgets.column("mcp-page");
    page.addChild(this.createPeriods(activity));

    HorizontalListWidget stats = Widgets.row("mcp-stat-row");
    stats.addEntry(Cards.stat(
        "mineclubplus.economy.balance",
        Formats.number(economy.balance()),
        Component.text(Formats.signed(economy.balanceChange24h())),
        economy.balanceChange24h() >= 0.0D
    ));
    stats.addEntry(Cards.stat(
        "mineclubplus.economy.earned",
        Formats.number(earned),
        null,
        true
    ));
    stats.addEntry(Cards.stat(
        "mineclubplus.economy.spent",
        Formats.number(spent),
        null,
        false
    ));
    stats.addEntry(Cards.stat(
        "mineclubplus.economy.net",
        Formats.signed(earned - spent),
        null,
        earned - spent >= 0.0D
    ));
    page.addChild(stats);

    page.addChild(this.createTransactions(transactions));

    return Cards.scroll(page);
  }

  private Widget createPeriods(MineClubPlusActivity activity) {
    HorizontalListWidget chips = Widgets.row("mcp-chips", "mcp-period-chips");

    for (EconomyPeriod period : EconomyPeriod.values()) {
      boolean active = activity.economyPeriod() == period;

      ComponentWidget chip = Widgets.i18n(period.translationKey(), "mcp-filter-chip");
      Widgets.clickable(chip, () -> activity.setEconomyPeriod(period));
      chip.setActive(active);
      if (active) {
        chip.addId("mcp-filter-chip-active");
      }

      chips.addEntry(chip);
    }

    return chips;
  }

  private Widget createTransactions(List<Transaction> transactions) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.economy.transactions");
    if (transactions.isEmpty()) {
      card.addChild(Cards.placeholder("mineclubplus.state.empty"));
      return card;
    }

    for (Transaction transaction : transactions) {
      card.addChild(OverviewPage.transactionRow(transaction, false));
    }

    return card;
  }
}
