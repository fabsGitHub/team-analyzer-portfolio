package com.teamanalyzer.teamanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConfigurationPropertiesScan("com.teamanalyzer.teamanalyzer")
@EnableScheduling
public class TeamanalyzerApplication {

  public static void main(String[] args) {
    SpringApplication.run(TeamanalyzerApplication.class, args);
  }
}
