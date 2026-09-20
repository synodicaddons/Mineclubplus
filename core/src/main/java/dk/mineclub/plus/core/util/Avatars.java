package dk.mineclub.plus.core.util;

import dk.mineclub.plus.core.config.AvatarStyle;
import java.util.UUID;
import net.labymod.api.Laby;
import net.labymod.api.client.entity.player.ClientPlayer;
import net.labymod.api.client.gui.icon.Icon;
import net.labymod.api.client.resources.ResourceLocation;
import net.labymod.api.client.session.Session;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Builds the avatar of the person using the addon.
 *
 * <p>The skin the game is already rendering the player with comes first. It is the only source
 * that cannot fail: it exists the moment the player is in the world, it is the skin they actually
 * see, and it still resolves for accounts Mojang has never heard of -- an offline or development
 * client included, where a lookup by name or uuid returns nothing and the avatar stays blank.
 *
 * <p>Outside a world there is no client player, so it falls back to LabyMod's skin cache by uuid
 * and then by name. {@link AvatarStyle#BUST} asks mccatalog for a full bust render instead, which
 * needs a Mojang-known uuid.
 */
public final class Avatars {

  /**
   * The one third-party request this addon can make, and only on an explicit opt-in.
   *
   * <p>Addon store guideline 2.3 rules out sending anything that uniquely identifies the player
   * to an external service without explicit consent, so this is documented here as the
   * guidelines require. LabyMod renders heads itself ({@link Icon#head}) but has no bust render,
   * so {@link AvatarStyle#BUST} is the only style that needs an outside service. What leaves the
   * client is the player's own uuid in the request path, to render their own skin, and nothing
   * else -- no session token, no server address, no MineClub data.
   *
   * <p>{@link AvatarStyle#HEAD} is the default and never touches this: it draws the skin the game
   * has already loaded, or LabyMod's own cache. The setting description names the service so the
   * choice is informed, and picking {@code HEAD} stops the request for good.
   *
   * <p>{@code facing=right} turns the model so the bust faces right. That is a rotation in the
   * render rather than a mirrored image: mirroring would swap the skin's own left and right, and
   * any text on it would read backwards.
   */
  private static final String BUST_URL =
      "https://mccatalog.com/api/skins/%s/bust?size=128&facing=right";

  private Avatars() {
  }

  /**
   * @param uuid the profile uuid, or {@code null} to fall back to the session
   * @param name the profile name, used when there is no uuid to look up
   */
  public static @NotNull Icon of(
      @Nullable UUID uuid,
      @Nullable String name,
      @NotNull AvatarStyle style
  ) {
    UUID resolved = uuid != null ? uuid : sessionUuid();

    if (style == AvatarStyle.BUST && resolved != null) {
      return Icon.url(String.format(BUST_URL, resolved));
    }

    Icon inGame = clientPlayerHead();
    if (inGame != null) {
      return inGame;
    }

    if (resolved != null) {
      return Icon.head(resolved);
    }

    String resolvedName = name != null && !name.isBlank() ? name : sessionName();
    return Icon.head(resolvedName);
  }

  /**
   * @return the head of the skin the game is rendering this player with, or {@code null} when not
   * in a world
   */
  private static @Nullable Icon clientPlayerHead() {
    ClientPlayer player = Laby.labyAPI().minecraft().getClientPlayer();
    if (player == null) {
      return null;
    }

    ResourceLocation skin = player.skinTexture();
    return skin == null ? null : Icon.head(skin);
  }

  private static @Nullable UUID sessionUuid() {
    Session session = Laby.labyAPI().minecraft().sessionAccessor().getSession();
    return session != null && session.hasUniqueId() ? session.getUniqueId() : null;
  }

  private static @NotNull String sessionName() {
    Session session = Laby.labyAPI().minecraft().sessionAccessor().getSession();
    return session == null ? "Steve" : session.getUsername();
  }
}
