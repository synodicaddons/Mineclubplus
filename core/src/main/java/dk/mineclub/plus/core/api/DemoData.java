package dk.mineclub.plus.core.api;

import dk.mineclub.plus.core.api.model.EconomySnapshot;
import dk.mineclub.plus.core.api.model.MineClubSnapshot;
import dk.mineclub.plus.core.api.model.PlayerProfile;
import dk.mineclub.plus.core.api.model.ServerStatus;
import dk.mineclub.plus.core.api.model.SkillSnapshot;
import dk.mineclub.plus.core.api.model.Transaction;
import dk.mineclub.plus.core.api.model.TransporterItem;
import dk.mineclub.plus.core.api.model.TransporterSnapshot;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import net.labymod.api.Laby;
import net.labymod.api.client.session.Session;
import org.jetbrains.annotations.NotNull;

/**
 * Sample content used while no MineClub API endpoint is configured, and whenever a request fails.
 *
 * <p>It exists so the menu is never a wall of dashes: the layout can be reviewed, the interactions
 * are testable, and the status bar marks the data as demo so nobody mistakes it for live numbers.
 */
public final class DemoData {

  private DemoData() {
  }

  public static @NotNull MineClubSnapshot snapshot() {
    return new MineClubSnapshot(
        profile(), economy(), transporter(), server(), skills(), new ArrayList<>());
  }

  private static PlayerProfile profile() {
    Session session = Laby.labyAPI().minecraft().sessionAccessor().getSession();
    UUID uuid = session == null ? null : session.getUniqueId();
    String name = session == null ? "Spiller" : session.getUsername();

    return new PlayerProfile(uuid, name, "SPILLER", "2024", TimeUnit.HOURS.toMinutes(612), true);
  }

  private static EconomySnapshot economy() {
    long now = System.currentTimeMillis();
    long minute = TimeUnit.MINUTES.toMillis(1L);

    List<Transaction> transactions = new ArrayList<>(Arrays.asList(
        new Transaction(now - 14 * minute, "Solgt 64x Diamant", "auktionshus", 79_360.0D),
        new Transaction(now - 96 * minute, "Koebt 1x Elytra", "auktionshus", -320_000.0D),
        new Transaction(now - 188 * minute, "Dagligt login - dag 14 i straek", "bonus", 5_000.0D),
        new Transaction(now - 320 * minute, "Solgt 12x Netherite-barre", "auktionshus", 1_014_000.0D),
        new Transaction(now - 640 * minute, "Warp-gebyr", "warp", -250.0D),
        new Transaction(now - 900 * minute, "Solgt 2304x Sten", "shop", 11_520.0D)
    ));

    return new EconomySnapshot(
        1_248_532.0D,
        18_420.0D,
        3_912_740.0D,
        124_300.0D,
        96_880.0D,
        320_250.0D,
        transactions
    );
  }

  private static TransporterSnapshot transporter() {
    // The whole vanilla catalogue, so every block and item the client knows is in the grid.
    List<TransporterItem> items = VanillaCatalog.items();

    // One slot per distinct item, so the capacity bar agrees with the grid instead of claiming
    // 214 of 512 slots while holding fifteen hundred kinds of block.
    return new TransporterSnapshot(items);
  }

  private static ServerStatus server() {
    return new ServerStatus("mc.mineclub.dk", "verden_1", "Survival", 0);
  }

  private static List<SkillSnapshot> skills() {
    return new ArrayList<>(Arrays.asList(
        new SkillSnapshot("mining", "Minedrift", 742, 184_320L, 240_000L),
        new SkillSnapshot("excavation", "Udgravning", 388, 92_140L, 160_000L),
        new SkillSnapshot("woodcutting", "Træfældning", 511, 61_900L, 120_000L),
        new SkillSnapshot("herbalism", "Urtelære", 296, 40_500L, 96_000L),
        new SkillSnapshot("fishing", "Fiskeri", 154, 12_800L, 48_000L)
    ));
  }
}
