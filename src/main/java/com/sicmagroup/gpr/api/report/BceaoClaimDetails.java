package com.sicmagroup.gpr.api.report;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BceaoClaimDetails {
    private int position;
    private String product;
    private String resume;
}
