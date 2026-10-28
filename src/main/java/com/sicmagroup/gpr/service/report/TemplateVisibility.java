package com.sicmagroup.gpr.service.report;

import java.util.Objects;
import java.util.Set;

import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Template;
import com.sicmagroup.gpr.domain.model.User;

/**
 * Qui voit et qui modifie un modèle du Rapport Global.
 * <ul>
 * <li>Pilote, DE et H14 : tous les modèles, et peuvent tous les modifier.</li>
 * <li>Tout le monde : les modèles « par défaut ».</li>
 * <li>Un RA : ses modèles et ceux des agents de son point de service et des points rattachés (tous niveaux).</li>
 * <li>Un agent : ses modèles et ceux qu'un RA de son point de service (ou d'un point auquel il est rattaché)
 * a choisi de partager.</li>
 * <li>Un modèle sans auteur (créé avant cette règle) : seulement Pilote, DE et H14.</li>
 * </ul>
 * Seuls l'auteur et Pilote / DE / H14 peuvent modifier ou supprimer un modèle.
 */
public final class TemplateVisibility {

    private TemplateVisibility() {
    }

    public static boolean seesAll(User user) {
        return user.getAdditionalrole() == Role.PILOTE || user.getAdditionalrole() == Role.DE || user.canSeeAll();
    }

    public static boolean canEdit(Template template, User viewer) {
        return seesAll(viewer) || (template.getAuthorId() != null && template.getAuthorId().equals(viewer.getId()));
    }

    /**
     * @param author        auteur du modèle (null s'il est inconnu ou supprimé)
     * @param raScopeIds    point de service du lecteur et, s'il est RA, tous ceux qui lui sont rattachés
     * @param selfAndParents point de service du lecteur et tous ceux auxquels il est rattaché
     */
    public static boolean canSee(Template template, User viewer, User author, Set<Long> raScopeIds,
            Set<Long> selfAndParents) {
        if (seesAll(viewer) || template.isDefault()) {
            return true;
        }
        if (template.getAuthorId() == null) {
            return false;
        }
        if (template.getAuthorId().equals(viewer.getId())) {
            return true;
        }
        Long authorServicePointId = author != null && author.getServicePoint() != null
                ? author.getServicePoint().getId()
                : null;
        if (authorServicePointId == null) {
            return false;
        }
        // Le RA voit ce que créent les agents de son périmètre.
        if (viewer.isRa() && raScopeIds.contains(authorServicePointId)) {
            return true;
        }
        // L'agent voit ce que son RA a choisi de partager.
        return author.isRa() && Boolean.TRUE.equals(template.getSharedWithAgents())
                && selfAndParents.stream().anyMatch(id -> Objects.equals(id, authorServicePointId));
    }
}
