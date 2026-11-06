package com.teamanalyzer.teamanalyzer.domain;

import java.util.UUID;

/**
 * The generated accounts behind "Try the live demo": {@code demo+<uuid>@example.invalid}.
 * {@code .invalid} is a reserved top-level domain, so no real mailbox can have such an address,
 * and registration refuses it.
 */
public final class DemoAccounts {

  private static final String PREFIX = "demo+";
  private static final String DOMAIN = "@example.invalid";

  /** SQL {@code LIKE} pattern for demo accounts. Emails are stored in canonical lower case. */
  public static final String EMAIL_LIKE = PREFIX + "%" + DOMAIN;

  private DemoAccounts() {
  }

  public static String emailFor(UUID workspaceKey) {
    return PREFIX + workspaceKey + DOMAIN;
  }

  public static boolean isDemoEmail(String email) {
    return email != null && email.startsWith(PREFIX) && email.endsWith(DOMAIN);
  }
}
