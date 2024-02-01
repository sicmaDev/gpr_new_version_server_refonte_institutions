package com.sicmagroup.gpr.repository.projection;

public interface ClaimPerObjLevelAndAgenceProjection {
    Long getSpId();
    String getSpLib();
    Long getObjId();
    String getObjLib();
    String getObjNiveau();
    Long getTotal();
}
