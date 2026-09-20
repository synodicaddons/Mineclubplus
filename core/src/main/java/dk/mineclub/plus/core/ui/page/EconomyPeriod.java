package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.api.model.Transaction;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;
import org.jetbrains.annotations.NotNull;

/**
 * Time windows for the economy page.
 */
public enum EconomyPeriod {

  DAY(TimeUnit.DAYS.toMillis(1L)),
  WEEK(TimeUnit.DAYS.toMillis(7L)),
  MONTH(TimeUnit.DAYS.toMillis(30L)),
  YEAR(TimeUnit.DAYS.toMillis(365L)),
  ALL(Long.MAX_VALUE);

  private final long windowMillis;

  EconomyPeriod(long windowMillis) {
    this.windowMillis = windowMillis;
  }

  public @NotNull String translationKey() {
    return "mineclubplus.economy.period." + this.name().toLowerCase(Locale.ROOT);
  }

  /**
   * @return the transactions inside this window, oldest entries dropped
   */
  public @NotNull List<Transaction> filter(@NotNull List<Transaction> transactions) {
    if (this == ALL) {
      return transactions;
    }

    long cutoff = System.currentTimeMillis() - this.windowMillis;
    List<Transaction> filtered = new ArrayList<>();
    for (Transaction transaction : transactions) {
      if (transaction.timestamp() >= cutoff) {
        filtered.add(transaction);
      }
    }

    return filtered;
  }
}
