// backend/src/main/java/com/teamanalyzer/teamanalyzer/port/AppClock.java
package com.teamanalyzer.teamanalyzer.port;

import java.time.Clock;
import java.time.Instant;

public interface AppClock {
  Instant now();

  Clock asJavaClock(); // optional nützlich für APIs, die Clock erwarten
}
