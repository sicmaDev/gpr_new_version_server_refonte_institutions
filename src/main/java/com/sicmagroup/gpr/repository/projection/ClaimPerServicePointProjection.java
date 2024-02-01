package com.sicmagroup.gpr.repository.projection;

import java.beans.JavaBean;

import lombok.Data;

public interface ClaimPerServicePointProjection {
    Long getServicePointId();
    String getLibelle();
    Long getTotal();
}
