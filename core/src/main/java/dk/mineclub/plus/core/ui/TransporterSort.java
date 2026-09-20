package dk.mineclub.plus.core.ui;

import dk.mineclub.plus.core.api.model.TransporterItem;
import java.util.Comparator;
import java.util.Locale;
import org.jetbrains.annotations.NotNull;

/**
 * Sort orders for the transporter grid.
 */
public enum TransporterSort {

  VALUE(Comparator.comparingDouble(TransporterItem::totalValue).reversed()),
  AMOUNT(Comparator.comparingLong(TransporterItem::amount).reversed()),
  NAME(Comparator.comparing(item -> item.displayName().toLowerCase(Locale.ROOT)));

  private final Comparator<TransporterItem> comparator;

  TransporterSort(Comparator<TransporterItem> comparator) {
    this.comparator = comparator;
  }

  public @NotNull Comparator<TransporterItem> comparator() {
    return this.comparator;
  }

  public @NotNull String translationKey() {
    return "mineclubplus.transporter.sort." + this.name().toLowerCase(Locale.ROOT);
  }

  public @NotNull TransporterSort next() {
    TransporterSort[] values = values();
    return values[(this.ordinal() + 1) % values.length];
  }
}
