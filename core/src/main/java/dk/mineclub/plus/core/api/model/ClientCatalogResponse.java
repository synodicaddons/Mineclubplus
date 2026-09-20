package dk.mineclub.plus.core.api.model;

import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The response from {@code GET /v2/client/catalog}: machine prices and the server's own recipes.
 *
 * <p>Both live in the lobby plugin's configuration, so a corrected price or a new recipe takes
 * effect without rebuilding the addon.
 */
public class ClientCatalogResponse {

  private boolean success;
  private boolean ready;
  private double accessPrice;
  private List<Machine> machines;
  private List<Recipe> recipes;

  public boolean success() {
    return this.success;
  }

  /** False while the plugin has not written the lists yet. */
  public boolean ready() {
    return this.ready;
  }

  /** The one-off price of being allowed to use machines at all. */
  public double accessPrice() {
    return this.accessPrice;
  }

  public @NotNull List<Machine> machines() {
    return this.machines == null ? Collections.emptyList() : this.machines;
  }

  public @NotNull List<Recipe> recipes() {
    return this.recipes == null ? Collections.emptyList() : this.recipes;
  }

  public static class Machine {

    private String name;
    private String displayName;
    private String block;
    private double price;

    public @Nullable String name() {
      return this.name;
    }

    public @Nullable String displayName() {
      return this.displayName;
    }

    /** The block the machine places, so the row can render the right item. */
    public @Nullable String block() {
      return this.block;
    }

    public double price() {
      return this.price;
    }
  }

  public static class Recipe {

    private String key;
    private String type;
    private List<String> shape;
    private List<Ingredient> ingredients;
    private String result;
    private String resultName;
    private long resultAmount;

    public @Nullable String key() {
      return this.key;
    }

    /** {@code SHAPED} eller {@code SHAPELESS}. */
    public @Nullable String type() {
      return this.type;
    }

    /** De tre raekker i moenstret. Tom for uformede opskrifter. */
    public @NotNull List<String> shape() {
      return this.shape == null ? Collections.emptyList() : this.shape;
    }

    public @NotNull List<Ingredient> ingredients() {
      return this.ingredients == null ? Collections.emptyList() : this.ingredients;
    }

    public @Nullable String result() {
      return this.result;
    }

    public @Nullable String resultName() {
      return this.resultName;
    }

    public long resultAmount() {
      return this.resultAmount;
    }
  }

  public static class Ingredient {

    private String slot;
    private String item;
    private String itemName;
    private long amount;

    /** Bogstavet i moenstret. Tomt for uformede opskrifter. */
    public @Nullable String slot() {
      return this.slot;
    }

    public @Nullable String item() {
      return this.item;
    }

    public @Nullable String itemName() {
      return this.itemName;
    }

    public long amount() {
      return this.amount;
    }
  }
}
