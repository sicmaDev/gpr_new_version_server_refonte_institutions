package com.sicmagroup.gpr.domain.dto.ai;

import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ExportReferentielsResponse {

    private List<ServicePointReferentielDto> servicePoints;
    private List<CollectionChannelReferentielDto> collectionChannels;
    private List<ProductReferentielDto> products;
    private List<CategorieObjetReferentielDto> categories;
    private List<ObjetReferentielDto> objets;
    private List<UserReferentielDto> users;
    private List<PosteReferentielDto> postes;

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ServicePointReferentielDto {
        private Long id;
        private String libelle;
        private String description;
        private String type;
        private Boolean isPrincipalAgence;
        private Long directionId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CollectionChannelReferentielDto {
        private Long id;
        private String libelle;
        private String description;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ProductReferentielDto {
        private Long id;
        private String libelle;
        private String description;
        private Boolean isDeleted;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class CategorieObjetReferentielDto {
        private Long id;
        private String libelle;
        private String description;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ObjetReferentielDto {
        private Long id;
        private Long categorieId;
        private String libelle;
        private String risqueLevel;
        private Integer processingTime;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class UserReferentielDto {
        private Long id;
        private String code;
        private String firstandlastname;
        private String email;
        private String additionalrole;
        private Long servicePointId;
        private Long posteId;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PosteReferentielDto {
        private Long id;
        private String libelle;
        private String description;
    }
}
