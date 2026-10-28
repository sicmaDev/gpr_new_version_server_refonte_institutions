package com.sicmagroup.gpr.service.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.Template;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.TemplateRepository;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.service.servicePoint.ServicePointServiceImpl;
import com.sicmagroup.gpr.utils.CurrentUserUtils;

import lombok.RequiredArgsConstructor;

/** Modèles du Rapport Global, filtrés selon {@link TemplateVisibility}. */
@Service
@RequiredArgsConstructor
@Transactional
public class TemplateServiceImpl implements TemplateService {
    private final TemplateRepository repository;
    private final CurrentUserUtils currentUser;
    private final UserRepository userRepository;
    private final ServicePointServiceImpl spService;

    @Override
    public Template createOrUpdate(Template template) {
        User viewer = connectedUser();
        boolean seesAll = TemplateVisibility.seesAll(viewer);
        // Un modèle « par défaut » est visible par tous : seuls Pilote, DE et H14 peuvent le choisir.
        boolean wantsDefault = template.isDefault() && seesAll;

        Template existing = template.getId() == null ? null : repository.findById(template.getId()).orElse(null);
        Template saved;
        if (existing != null) {
            if (!TemplateVisibility.canEdit(existing, viewer)) {
                throw new AccessDeniedException("Vous ne pouvez modifier que vos propres modèles.");
            }
            existing.setUpdatedAt(LocalDateTime.now());
            existing.setValeurs(template.getValeurs());
            existing.setTitle(template.getTitle());
            if (seesAll) {
                existing.setDefault(wantsDefault);
            }
            // Le partage reste le choix du RA auteur : un Pilote qui modifie le modèle n'y touche pas.
            if (viewer.isRa() && viewer.getId().equals(existing.getAuthorId())) {
                existing.setSharedWithAgents(Boolean.TRUE.equals(template.getSharedWithAgents()));
            }
            saved = repository.save(existing);
        } else {
            template.setId(null);
            template.setAuthorId(viewer.getId());
            template.setAuthorName(viewer.getFirstandlastname());
            template.setSharedWithAgents(viewer.isRa() && Boolean.TRUE.equals(template.getSharedWithAgents()));
            template.setDefault(wantsDefault);
            template.setCreatedAt(LocalDateTime.now());
            template.setUpdatedAt(LocalDateTime.now());
            saved = repository.save(template);
        }

        if (saved.isDefault()) {
            repository.findAll().stream()
                    .filter(t -> t.isDefault() && !t.getId().equals(saved.getId()))
                    .forEach(t -> {
                        t.setDefault(false);
                        repository.save(t);
                    });
        }
        saved.setCanEdit(true);
        return saved;
    }

    @Override
    public List<Template> findAll() {
        User viewer;
        try {
            viewer = connectedUser();
        } catch (AccessDeniedException e) {
            return new ArrayList<>();
        }
        List<Template> all = repository.findAll();
        if (TemplateVisibility.seesAll(viewer)) {
            all.forEach(t -> t.setCanEdit(true));
            return all;
        }

        Set<Long> authorIds = all.stream().map(Template::getAuthorId).filter(Objects::nonNull).collect(Collectors.toSet());
        Map<Long, User> authors = userRepository.findAllById(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Set<Long> raScope = new HashSet<>();
        Set<Long> selfAndParents = new HashSet<>();
        ServicePoint sp = viewer.getServicePoint();
        if (sp != null) {
            raScope.add(sp.getId());
            if (viewer.isRa()) {
                spService.getAllDescendants(sp.getId()).forEach(d -> raScope.add(d.getId()));
            }
            selfAndParents.addAll(spService.getSelfAndAncestorIds(sp.getId()));
        }

        return all.stream()
                .filter(t -> TemplateVisibility.canSee(t, viewer, authors.get(t.getAuthorId()), raScope, selfAndParents))
                .peek(t -> t.setCanEdit(TemplateVisibility.canEdit(t, viewer)))
                .collect(Collectors.toList());
    }

    @Override
    public Template findById(Long id) {
        try {
            return repository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Introuvable"));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public void delete(Long id) {
        Template template = repository.findById(id).orElseThrow(() -> new RuntimeException("Modèle not found"));
        if (!TemplateVisibility.canEdit(template, connectedUser())) {
            throw new AccessDeniedException("Vous ne pouvez supprimer que vos propres modèles.");
        }
        repository.delete(template);
    }

    private User connectedUser() {
        try {
            return currentUser.getUser();
        } catch (Exception e) {
            throw new AccessDeniedException("Utilisateur connecté introuvable");
        }
    }
}
