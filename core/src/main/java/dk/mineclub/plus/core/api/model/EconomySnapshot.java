package dk.mineclub.plus.core.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * Everything the economy pages need in one response, so opening the menu costs a single request
 * instead of one per card.
 */
public class EconomySnapshot {

  private double balance;
  private double balanceChange24h;
  private double netWorth;
  private double netWorthChange24h;
  private double earnedToday;
  private double spentToday;
  private List<Transaction> transactions;

  public EconomySnapshot() {
  }

  public EconomySnapshot(
      double balance,
      double balanceChange24h,
      double netWorth,
      double netWorthChange24h,
      double earnedToday,
      double spentToday,
      @NotNull List<Transaction> transactions
  ) {
    this.balance = balance;
    this.balanceChange24h = balanceChange24h;
    this.netWorth = netWorth;
    this.netWorthChange24h = netWorthChange24h;
    this.earnedToday = earnedToday;
    this.spentToday = spentToday;
    this.transactions = new ArrayList<>(transactions);
  }

  public double balance() {
    return this.balance;
  }

  public double balanceChange24h() {
    return this.balanceChange24h;
  }

  public double netWorth() {
    return this.netWorth;
  }

  public double netWorthChange24h() {
    return this.netWorthChange24h;
  }

  public double earnedToday() {
    return this.earnedToday;
  }

  public double spentToday() {
    return this.spentToday;
  }

  public @NotNull List<Transaction> transactions() {
    return this.transactions == null ? Collections.emptyList()
        : Collections.unmodifiableList(this.transactions);
  }
}
