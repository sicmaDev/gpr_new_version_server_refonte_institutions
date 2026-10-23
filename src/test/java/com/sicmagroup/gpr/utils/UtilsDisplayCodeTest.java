package com.sicmagroup.gpr.utils;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/** Code affiché dans le Log Système : le code client (REC-xxxx) quand il existe, jamais « null ». */
class UtilsDisplayCodeTest {

    @Test
    void lesCodesAffichesSontLisibles() {
        assertThat(Utils.displayCode("REC-ab12", "uuid-long")).isEqualTo("REC-ab12");
        assertThat(Utils.displayCode(" ", "uuid-long")).isEqualTo("uuid-long");
        assertThat(Utils.displayCode(null, "uuid-long")).isEqualTo("uuid-long");
        assertThat(Utils.displayCode(null, null)).isEmpty();
    }
}
