package dk.mineclub.plus.core.api.model;

import org.jetbrains.annotations.Nullable;

/**
 * The response from {@code GET /v2/client/overview}: coins, the transporter's total value and the
 * two combined, for the player the token was issued to.
 */
public class ClientOverviewResponse {

  private boolean success;
  private Playtime playtime;
  private Coins coins;
  private Transporter transporter;
  private NetWorth netWorth;

  public boolean success() {
    return this.success;
  }

  public @Nullable Playtime playtime() {
    return this.playtime;
  }

  public @Nullable Coins coins() {
    return this.coins;
  }

  public @Nullable Transporter transporter() {
    return this.transporter;
  }

  public @Nullable NetWorth netWorth() {
    return this.netWorth;
  }

  /** Play time, derived from the same join and quit logs the staff panel uses. */
  public static class Playtime {

    private long seconds;
    private long sessions;
    private String firstSeen;
    private String lastSeen;

    public long seconds() {
      return this.seconds;
    }

    public long sessions() {
      return this.sessions;
    }

    /** The first time the player was seen on the network, as an ISO timestamp. */
    public @Nullable String firstSeen() {
      return this.firstSeen;
    }

    public @Nullable String lastSeen() {
      return this.lastSeen;
    }
  }

  public static class Coins {

    private double balance;
    private double today;
    private double earnedToday;
    private double spentToday;

    public double balance() {
      return this.balance;
    }

    /**
     * @return today's net movement, which is the figure shown under the balance
     */
    public double today() {
      return this.today;
    }

    public double earnedToday() {
      return this.earnedToday;
    }

    public double spentToday() {
      return this.spentToday;
    }
  }

  public static class Transporter {

    private long distinctItems;
    private long totalItems;
    private double totalValue;
    private double today;

    public long distinctItems() {
      return this.distinctItems;
    }

    public long totalItems() {
      return this.totalItems;
    }

    public double totalValue() {
      return this.totalValue;
    }

    public double today() {
      return this.today;
    }
  }

  public static class NetWorth {

    private double total;
    private double today;

    public double total() {
      return this.total;
    }

    public double today() {
      return this.today;
    }
  }
}
