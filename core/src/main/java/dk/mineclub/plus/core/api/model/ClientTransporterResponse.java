package dk.mineclub.plus.core.api.model;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The response from {@code GET /v2/client/transporter}.
 *
 * <p>The addon fetches the whole transporter in one call and sorts and filters it
 * locally, so a new category or a search costs no round trip.
 */
public class ClientTransporterResponse {

  private boolean success;
  private Summary summary;
  private List<Item> data;

  public boolean success() {
    return this.success;
  }

  public @Nullable Summary summary() {
    return this.summary;
  }

  public @NotNull List<Item> data() {
    return this.data == null ? Collections.emptyList() : this.data;
  }

  public static class Summary {

    private long distinctItems;
    private long totalItems;
    private double totalValue;

    public long distinctItems() {
      return this.distinctItems;
    }

    public long totalItems() {
      return this.totalItems;
    }

    public double totalValue() {
      return this.totalValue;
    }
  }

  public static class Item {

    private String key;
    private String name;
    private String category;
    private long amount;
    private double unitPrice;
    private double value;
    private Double change;

    /**
     * @return the material name, for example {@code DIAMOND_BLOCK}
     */
    public @Nullable String key() {
      return this.key;
    }

    public @Nullable String name() {
      return this.name;
    }

    public @Nullable String category() {
      return this.category;
    }

    public long amount() {
      return this.amount;
    }

    public double unitPrice() {
      return this.unitPrice;
    }

    public double value() {
      return this.value;
    }

    /**
     * @return the price movement in percent, or {@code null} when too little has been traded to
     * say anything
     */
    public @Nullable Double change() {
      return this.change;
    }
  }
}
