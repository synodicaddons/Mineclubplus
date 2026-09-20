package dk.mineclub.plus.core.api.model;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The response from {@code GET /v2/client/shop}: what the admin shop sells, and what it costs.
 *
 * <p>The prices live on the market square signs and nowhere else. The plugin reads them and sends
 * them out here, so the shop in the window sells the same goods at the same price as the sign.
 *
 * <p>These figures are for display. What a purchase costs is decided by the server when it takes
 * the coins; the client only states what and how much.
 */
public class ClientShopResponse {

  private boolean success;
  private boolean ready;
  private List<Item> items;

  public boolean success() {
    return this.success;
  }

  /** False while the plugin has not read the market square yet. */
  public boolean ready() {
    return this.ready;
  }

  public @NotNull List<Item> items() {
    return this.items == null ? Collections.emptyList() : this.items;
  }

  /** One item on sale. */
  public static class Item {

    private String material;
    private String displayName;
    private int amount;
    private double price;
    private double unitPrice;
    private Double sellPrice;

    /** The material, for example {@code OAK_LOG}. What is rendered and bought by. */
    public @Nullable String material() {
      return this.material;
    }

    public @Nullable String displayName() {
      return this.displayName;
    }

    /** How many the sign hands over for {@link #price()}. */
    public int amount() {
      return this.amount <= 0 ? 1 : this.amount;
    }

    /** The sign's price for {@link #amount()} of them. */
    public double price() {
      return this.price;
    }

    /** The price of a single one. */
    public double unitPrice() {
      return this.unitPrice;
    }

    /** What the sign pays for the item, or {@code null} when it only sells. */
    public @Nullable Double sellPrice() {
      return this.sellPrice;
    }

    /**
     * @param wanted how many the player wants
     * @return the cost, derived the way the server derives it: from the sign's price and batch
     *     size, rounded up to whole cents
     */
    public double total(int wanted) {
      double exact = this.price * wanted / this.amount();
      return Math.ceil(exact * 100.0D - 1.0E-9D) / 100.0D;
    }
  }
}
