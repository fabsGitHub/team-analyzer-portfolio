package com.teamanalyzer.teamanalyzer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

@SpringBootApplication
@ConfigurationPropertiesScan("com.teamanalyzer.teamanalyzer")
public class TeamanalyzerApplication {

	public static void main(String[] args) {
		SpringApplication.run(TeamanalyzerApplication.class, args);
	}

}
