package com.sicmagroup.gpr.domain.dto.reports.tableTotal;

import java.util.Map;
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
public class ObjectTotal {
    private int total;
    private HashMap<String, Long> statusAndValue;
}
