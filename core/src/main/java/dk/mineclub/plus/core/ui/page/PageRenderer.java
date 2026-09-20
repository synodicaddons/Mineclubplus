package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import net.labymod.api.client.gui.screen.widget.Widget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the body of one navigation entry.
 *
 * <p>A renderer is created once per activity and called again on every rebuild, so it must stay
 * stateless -- all page state lives on {@link MineClubPlusActivity}.
 */
@FunctionalInterface
public interface PageRenderer {

  /**
   * @param activity the window asking for the page
   * @param data     the current snapshot, or {@code null} while the first request is running
   */
  @NotNull Widget create(@NotNull MineClubPlusActivity activity, @Nullable MineClubSnapshot data);
}
