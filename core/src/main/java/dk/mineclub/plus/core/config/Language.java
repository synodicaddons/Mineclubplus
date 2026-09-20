package dk.mineclub.plus.core.config;

import org.jetbrains.annotations.NotNull;

/**
 * The language the window is written in.
 *
 * <p>English is the default whatever the client runs in, so every player sees the same thing the
 * first time. Danish is chosen in the settings.
 */
public enum Language {

  ENGLISH("en_us"),
  DANISH("da_dk");

  /** Filnavnet i {@code assets/mineclubplus/i18n}. */
  private final String file;

  Language(String file) {
    this.file = file;
  }

  public @NotNull String file() {
    return this.file;
  }
}
