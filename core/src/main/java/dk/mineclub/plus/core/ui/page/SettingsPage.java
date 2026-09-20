package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.MineClubPlusConfiguration;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.config.AvatarStyle;
import dk.mineclub.plus.core.config.Language;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.widget.Cards;
import dk.mineclub.plus.core.ui.widget.Widgets;
import dk.mineclub.plus.core.util.Translations;
import java.util.Locale;
import java.util.function.Consumer;
import net.labymod.api.Laby;
import net.labymod.api.client.component.Component;
import net.labymod.api.client.gui.screen.key.Key;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.input.KeybindWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.SwitchWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.dropdown.DropdownWidget;
import net.labymod.api.client.gui.screen.widget.widgets.input.dropdown.renderer.DefaultEntryRenderer;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * The settings the player reaches for most, inside the window instead of three menus away.
 *
 * <p>Every control writes straight to the same {@link MineClubPlusConfiguration} properties that
 * LabyMod's own addon settings edit, so the two views can never drift apart. The hotkey is only
 * changeable here and in LabyMod's settings -- never by a stray key press in-game.
 */
public final class SettingsPage implements PageRenderer {

  /** The copyright year. A constant: it is the release year, not today's date. */
  private static final String COPYRIGHT_YEAR = "2026";

  private static final String WEBSITE = "https://synodicstudio.com";

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    MineClubPlusConfiguration configuration = activity.addon().configuration();

    VerticalListWidget<Widget> page = Widgets.column("mcp-page");
    page.addChild(this.createMenuCard(activity, configuration));
    page.addChild(this.createDataCard(activity, configuration));
    page.addChild(this.createTransporterCard(activity, configuration));
    page.addChild(this.createAboutCard());

    return Cards.scroll(page);
  }

  private Widget createMenuCard(
      MineClubPlusActivity activity,
      MineClubPlusConfiguration configuration
  ) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.settingsPage.menu");

    KeybindWidget keybind = new KeybindWidget(key -> {
      configuration.openMenuKey().set(key == null ? Key.NONE : key);
      activity.addon().save();
    });
    keybind.key(configuration.openMenuKey().get());
    keybind.addId("mcp-input");
    card.addChild(Cards.widgetRow("mineclubplus.settings.openMenuKey.name", keybind));

    DropdownWidget<Language> language = new DropdownWidget<>();
    language.addId("mcp-input");
    for (Language value : Language.values()) {
      language.add(value);
    }

    language.setEntryRenderer(entries("mineclubplus.settings.language.entries"));
    language.setSelected(configuration.language().get(), false);
    language.setChangeListener(value -> {
      configuration.language().set(value);
      Translations.language(value);
      activity.addon().save();

      // The whole window is written in the previous language, so it has to be rebuilt now.
      activity.markDirty();
    });
    card.addChild(Cards.widgetRow("mineclubplus.settings.language.name", language));

    DropdownWidget<AvatarStyle> avatar = new DropdownWidget<>();
    avatar.addId("mcp-input");
    for (AvatarStyle style : AvatarStyle.values()) {
      avatar.add(style);
    }

    avatar.setEntryRenderer(entries("mineclubplus.settings.avatarStyle.entries"));
    avatar.setSelected(configuration.avatarStyle().get(), false);
    avatar.setChangeListener(style -> {
      configuration.avatarStyle().set(style);
      activity.addon().save();
      activity.markDirty();
    });
    card.addChild(Cards.widgetRow("mineclubplus.settings.avatarStyle.name", avatar));

    card.addChild(Cards.widgetRow(
        "mineclubplus.settings.windowBackground.name",
        this.toggle(configuration.windowBackground().get(), value -> {
          configuration.windowBackground().set(value);
          activity.addon().save();

          // The backdrop is part of the window itself, so it only changes on a rebuild.
          activity.markDirty();
        })
    ));

    return card;
  }

  /**
   * The entries of a dropdown, in the language the player picked.
   *
   * <p>{@code setTranslationKeyPrefix} would go through LabyMod, which only knows the client's
   * own language: a Danish client would get Danish entries inside an English menu.
   *
   * @param prefix the key in front of each value, e.g. {@code ...avatarStyle.entries}
   */
  private static <T extends Enum<T>> DefaultEntryRenderer<T> entries(String prefix) {
    DefaultEntryRenderer<T> renderer = new DefaultEntryRenderer<>();
    // Lower case: that is how the keys are written in the language files, and what LabyMod's
    // own renderer would have looked up.
    renderer.setDisplayNameProvider(
        value -> Component.text(
            Translations.get(prefix + "." + value.name().toLowerCase(Locale.ROOT))
        )
    );

    return renderer;
  }

  private Widget createDataCard(
      MineClubPlusActivity activity,
      MineClubPlusConfiguration configuration
  ) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.settingsPage.data");

    card.addChild(Cards.widgetRow(
        "mineclubplus.settings.refreshWhileOpen.name",
        this.toggle(configuration.refreshWhileOpen().get(), value -> {
          configuration.refreshWhileOpen().set(value);
          activity.addon().save();
        })
    ));

    card.addChild(Cards.translatedRow(
        "mineclubplus.settings.refreshIntervalSeconds.name",
        configuration.refreshIntervalSeconds().get() + " s"
    ));

    card.addChild(Widgets.i18n("mineclubplus.settingsPage.hint", "mcp-settings-hint"));

    return card;
  }

  /**
   * Who owns the addon, and what may be done with it.
   *
   * <p>Shown in the window rather than only in a file beside it: a licence nobody reads is no
   * licence.
   */
  private Widget createAboutCard() {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.settingsPage.about");

    card.addChild(Widgets.i18nArgs(
        "mineclubplus.legal.copyright",
        new Object[]{COPYRIGHT_YEAR},
        "mcp-legal-line"
    ));
    card.addChild(Widgets.i18n("mineclubplus.legal.terms", "mcp-settings-hint"));

    card.addChild(Widgets.i18nButton(
        "mineclubplus.legal.website",
        () -> Laby.labyAPI().minecraft().chatExecutor().openUrl(WEBSITE),
        "mcp-ghost-button",
        "mcp-settings-button"
    ));

    return card;
  }

  private Widget createTransporterCard(
      MineClubPlusActivity activity,
      MineClubPlusConfiguration configuration
  ) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.settingsPage.transporter");

    card.addChild(Cards.translatedRow(
        "mineclubplus.settings.quickAmount.name",
        String.valueOf(configuration.quickAmount().get())
    ));
    card.addChild(Cards.widgetRow(
        "mineclubplus.settings.confirmTransporterActions.name",
        this.toggle(configuration.confirmTransporterActions().get(), value -> {
          configuration.confirmTransporterActions().set(value);
          activity.addon().save();
        })
    ));

    return card;
  }

  private SwitchWidget toggle(boolean value, Consumer<Boolean> consumer) {
    SwitchWidget widget = SwitchWidget.create(consumer::accept);
    widget.setValue(value);
    widget.addId("mcp-toggle");
    return widget;
  }
}
