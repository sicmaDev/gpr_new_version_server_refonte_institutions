package com.sicmagroup.gpr.repository.projection;

public interface ObjectTotalPerStatusProjection {
    String getStatus();
    boolean getAccepted();
    Long getTotal();

}
