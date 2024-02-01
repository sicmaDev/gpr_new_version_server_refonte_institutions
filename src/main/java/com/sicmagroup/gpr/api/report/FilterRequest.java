package com.sicmagroup.gpr.api.report;

import java.time.LocalDateTime;
import java.util.List;

import com.sicmagroup.gpr.domain.enumeration.ClaimStatus;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FilterRequest {
    private List<Long> objets;
    private Long year;
    private List<Long> products;
    private List<Long> savedBy;
    private List<Long> servicePoints;
    private String receiveStart;
    private String receiveEnd;
    private List<ClaimStatus> etats;
    private List<Long> canals;

}
