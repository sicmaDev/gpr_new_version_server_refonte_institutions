package com.sicmagroup.gpr.api.chat;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class InviteGuestRequest {
    private String claimCode;
    private Long guestId;

}
