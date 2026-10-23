package com.sicmagroup.gpr.domain.dto.reports.StackedBar;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

// Les champs vides (ex: borderColor) ne sont pas envoyés : un "borderColor": null fait planter
// Chart.js au survol (il calcule la couleur de survol à partir de cette valeur), alors qu'un champ
// absent lui fait utiliser sa couleur par défaut.
@JsonInclude(JsonInclude.Include.NON_NULL)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StackedBarDataset {
    private String label;
    private Long id;
    private List<Double> data;
    private String backgroundColor;
    private String borderColor;
}
