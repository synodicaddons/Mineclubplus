package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.ui.MineClubPage;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.widget.Cards;
import dk.mineclub.plus.core.ui.widget.Widgets;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Stands in for a navigation entry that is not built yet, so the rail stays complete and the
 * player is told what is coming instead of clicking into an empty panel.
 */
public final class PlaceholderPage implements PageRenderer {

  private final MineClubPage page;

  public PlaceholderPage(@NotNull MineClubPage page) {
    this.page = page;
  }

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    VerticalListWidget<Widget> card = Cards.card(this.page.titleKey());
    card.addChild(Widgets.i18n(this.page.subtitleKey(), "mcp-placeholder-text"));
    card.addChild(Widgets.i18n("mineclubplus.state.comingSoon", "mcp-placeholder-hint"));

    VerticalListWidget<Widget> page = Widgets.column("mcp-page");
    page.addChild(card);

    return Cards.scroll(page);
  }
}
