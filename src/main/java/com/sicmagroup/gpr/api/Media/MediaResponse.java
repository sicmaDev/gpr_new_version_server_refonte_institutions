package com.sicmagroup.gpr.api.Media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponse {
    private String name;
    private Long id;
    private Long size;
    private String path;
}
