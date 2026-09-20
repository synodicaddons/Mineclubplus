package dk.mineclub.plus.core.api.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jetbrains.annotations.NotNull;

/**
 * The full transporter content. Sorting and filtering happen client side on this snapshot so
 * switching a filter chip never costs a request.
 *
 * <p>There is no slot count. The transporter holds an unlimited amount, so a used-of-total
 * figure would be describing a ceiling that does not exist -- the window reports how much is in
 * there, never how full it is.
 */
public class TransporterSnapshot {

  private List<TransporterItem> items;

  public TransporterSnapshot() {
  }

  public TransporterSnapshot(@NotNull List<TransporterItem> items) {
    this.items = new ArrayList<>(items);
  }

  public @NotNull List<TransporterItem> items() {
    return this.items == null ? Collections.emptyList()
        : Collections.unmodifiableList(this.items);
  }

  public double totalValue() {
    double total = 0.0D;
    for (TransporterItem item : this.items()) {
      total += item.totalValue();
    }

    return total;
  }

  public long totalAmount() {
    long total = 0L;
    for (TransporterItem item : this.items()) {
      total += item.amount();
    }

    return total;
  }
}
