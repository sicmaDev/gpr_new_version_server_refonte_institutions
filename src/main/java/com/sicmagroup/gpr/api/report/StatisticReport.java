package com.sicmagroup.gpr.api.report;

import java.util.HashMap;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticReport {
     private List<HashMap<String, Double>> ClaimStatsAndValue;
     private List<HashMap<String, Double>>  DenunStatsAndValue;
     private List<HashMap<String, Double>>  SuggestStatsAndValue;
}
