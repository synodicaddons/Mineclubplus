package dk.mineclub.plus.core.config;

/**
 * How the player avatar in the sidebar is rendered.
 */
public enum AvatarStyle {

  /**
   * The head from LabyMod's own skin cache, hat layer included. Needs no third party request and
   * resolves for anyone with a Minecraft account.
   */
  HEAD,

  /**
   * Bust render fetched from mccatalog. Closer to the mock-ups, but one download per player and
   * nothing renders when the account is unknown there.
   */
  BUST
}
