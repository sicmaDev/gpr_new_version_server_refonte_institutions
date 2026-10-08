package com.sicmagroup.gpr.domain.enumeration;

public enum ClaimStatus {
    SAVED,
    TEMP_SAVED,
    AFFECTED,
    TO_APPROUVED,
    DESAPPROUVED,
    TREAT,
    SATISFIED,
    UNSATISFIED,
    PARTIAL_SATISFIED,
    LITIGATION,
    CLASSED,
    TRANSMITTED,
    // L'institution attend une pièce du client (le délai interne est en pause, pas l'échéance réglementaire)
    WAITING_CUSTOMER

}
