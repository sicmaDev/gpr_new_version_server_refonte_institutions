package com.sicmagroup.gpr.api.Media;

import com.sicmagroup.gpr.domain.dto.ExtraContentResponse;

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
    private boolean is_extra;
    private ExtraContentResponse extra;
    private Long size;
    private String path;
}
