package com.sicmagroup.gpr.domain.dto;

import java.time.LocalDateTime;

import com.sicmagroup.gpr.domain.dto.claimResponse.UserResponse;
import com.sicmagroup.gpr.domain.enumeration.ClaimType;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.utils.Utils;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrashDto {
    private Long id;
    private ClaimType type;
    private String codeClient;
    private String clientFirstAndLastName;

    private boolean isDeleted;
    private boolean isRestored;
    private UserResponse deletedBy;
    private UserResponse restoredBy;
    private LocalDateTime deletedAt;
    private LocalDateTime restoredAt;
    private String delete_reason;
  
    // autres infos utiles
    public String convertDate(LocalDateTime dateTime){
        return Utils.convertLocalDateTimeToString(dateTime);
    }   

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

  
    // public TrashDto() {}

    // --- Constructeur complet avec tous les champs ---
    public TrashDto(
            Long id,
            ClaimType type,
            String codeClient,
            String clientFirstAndLastName,
            boolean isDeleted,
            UserResponse deletedBy,
            LocalDateTime deletedAt,
            String delete_reason
    ) {
        this.id = id;
        this.type = type;
        this.codeClient = codeClient;
        this.clientFirstAndLastName = clientFirstAndLastName;
        this.isDeleted = isDeleted;
        this.deletedBy = deletedBy;
        this.deletedAt = deletedAt;
        this.delete_reason = delete_reason;
    }

    
}


