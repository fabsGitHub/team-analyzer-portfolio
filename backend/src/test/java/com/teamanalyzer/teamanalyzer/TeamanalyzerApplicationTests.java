package com.teamanalyzer.teamanalyzer;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/** Signing keys come from {@code support.TestSigningSecrets}, generated fresh for every run. */
@SpringBootTest
@ActiveProfiles("test")
class TeamanalyzerApplicationTests {

  @Test
  void contextLoads() {
  }
}
