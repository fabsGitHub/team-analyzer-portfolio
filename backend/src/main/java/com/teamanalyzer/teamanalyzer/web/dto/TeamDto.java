package com.teamanalyzer.teamanalyzer.web.dto;

import java.util.UUID;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class TeamDto {
    UUID id;
    String name;
}
