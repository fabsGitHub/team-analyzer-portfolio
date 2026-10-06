// infra/time/SystemClock.java
package com.teamanalyzer.teamanalyzer.infra.time;

import com.teamanalyzer.teamanalyzer.port.AppClock;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

@Component
public class SystemClock implements AppClock {
  private final Clock clock = Clock.system(ZoneOffset.UTC); // immer UTC speichern

  @Override
  public Instant now() {
    return Instant.now(clock);
  }

  @Override
  public Clock asJavaClock() {
    return clock;
  }
}
