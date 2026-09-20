package dk.mineclub.plus.core.ui.page;

import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.api.model.PlayerProfile;
import dk.mineclub.plus.core.api.model.SkillSnapshot;
import dk.mineclub.plus.core.api.model.TransporterSnapshot;
import dk.mineclub.plus.core.ui.MineClubPlusActivity;
import dk.mineclub.plus.core.ui.widget.Cards;
import dk.mineclub.plus.core.ui.widget.Widgets;
import dk.mineclub.plus.core.util.Formats;
import dk.mineclub.plus.core.util.Ranks;
import dk.mineclub.plus.core.util.Translations;
import java.util.List;
import net.labymod.api.client.gui.screen.widget.Widget;
import net.labymod.api.client.gui.screen.widget.widgets.DivWidget;
import net.labymod.api.client.gui.screen.widget.widgets.layout.list.VerticalListWidget;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Play time, transporter totals and the mcMMO skills.
 *
 * <p>The skills section only appears when the API actually returns skills, so a server without
 * mcMMO shows one section fewer instead of a row of zeroes.
 */
public final class StatisticsPage implements PageRenderer {

  @Override
  public @NotNull Widget create(
      @NotNull MineClubPlusActivity activity,
      @Nullable MineClubSnapshot data
  ) {
    if (data == null) {
      return Cards.placeholder("mineclubplus.state.loading");
    }

    VerticalListWidget<Widget> page = Widgets.column("mcp-page");
    page.addChild(this.createPlayerCard(data.profile()));
    page.addChild(this.createTransporterCard(data.transporter()));

    List<SkillSnapshot> skills = data.skills();
    if (!skills.isEmpty()) {
      page.addChild(this.createSkillsCard(skills));
    }

    return Cards.scroll(page);
  }

  private Widget createPlayerCard(@Nullable PlayerProfile profile) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.statistics.player");
    if (profile == null) {
      card.addChild(Cards.placeholder("mineclubplus.state.empty"));
      return card;
    }

    card.addChild(Cards.translatedRow("mineclubplus.statistics.name", profile.name()));
    card.addChild(Cards.translatedRow(
        "mineclubplus.statistics.rank",
        Ranks.displayName(profile.rank())
    ));
    card.addChild(Cards.translatedRow(
        "mineclubplus.statistics.playtime",
        Formats.playtime(profile.playtimeMinutes())
    ));
    card.addChild(Cards.translatedRow(
        "mineclubplus.statistics.memberSince",
        profile.memberSince()
    ));

    return card;
  }

  private Widget createTransporterCard(@Nullable TransporterSnapshot transporter) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.statistics.transporter");
    if (transporter == null) {
      card.addChild(Cards.placeholder("mineclubplus.state.empty"));
      return card;
    }

    card.addChild(Cards.translatedRow(
        "mineclubplus.statistics.distinctItems",
        Formats.number(transporter.items().size())
    ));
    card.addChild(Cards.translatedRow(
        "mineclubplus.statistics.totalItems",
        Formats.number(transporter.totalAmount())
    ));
    card.addChild(Cards.translatedRow(
        "mineclubplus.statistics.totalValue",
        Formats.number(transporter.totalValue())
    ));

    return card;
  }

  private Widget createSkillsCard(List<SkillSnapshot> skills) {
    VerticalListWidget<Widget> card = Cards.card("mineclubplus.statistics.skills");

    int powerLevel = 0;
    for (SkillSnapshot skill : skills) {
      powerLevel += skill.level();
    }

    card.addChild(Cards.translatedRow(
        "mineclubplus.statistics.powerLevel",
        Formats.number(powerLevel)
    ));

    for (SkillSnapshot skill : skills) {
      DivWidget head = Widgets.panel("mcp-row");
      head.addChild(Widgets.text(skillName(skill), "mcp-row-label"));
      head.addChild(Widgets.text(Formats.number(skill.level()), "mcp-row-value", "mcp-accent"));
      card.addChild(head);

      card.addChild(Widgets.bar(skill.progress(), "mcp-bar", "mcp-skill-bar"));
    }

    return card;
  }

  /**
   * The name of a skill, in the language the player picked.
   *
   * <p>The server sends mcMMO's own name, but in the server's language, and an English player
   * should not have to read "Minedrift". Known skills are named here; anything else keeps the
   * server's name, so an untranslated skill still has one.
   */
  private static String skillName(SkillSnapshot skill) {
    String translated = Translations.find("mineclubplus.skill." + skill.id());
    return translated == null ? skill.displayName() : translated;
  }
}
