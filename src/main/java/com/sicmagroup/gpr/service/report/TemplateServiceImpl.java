package com.sicmagroup.gpr.service.report;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.sicmagroup.gpr.domain.model.Template;
import com.sicmagroup.gpr.repository.TemplateRepository;
import com.sicmagroup.gpr.utils.CurrentUserUtils;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateServiceImpl implements TemplateService {
    private final TemplateRepository repository;
    private final CurrentUserUtils currentUser;

    @Override
    public Template createOrUpdate(Template template) {
        try {
            if (template.isDefault()) {
                repository.findAll()
                    .stream()
                    .filter(Template::isDefault)
                    .forEach(model -> {
                        model.setDefault(false);
                        repository.save(model);
                    });
            }

            if (template.getId() == null) {
                template.setCreatedAt(LocalDateTime.now());
                template.setUpdatedAt(LocalDateTime.now());
                return repository.save(template);
            }

            return repository.findById(template.getId())
                .map(old -> {
                    old.setUpdatedAt(LocalDateTime.now());
                    old.setValeurs(template.getValeurs());
                    old.setTitle(template.getTitle());
                    old.setDefault(template.isDefault());
                    return repository.save(old);
                })
                .orElseGet(() -> {
                    template.setCreatedAt(LocalDateTime.now());
                    template.setUpdatedAt(LocalDateTime.now());
                    return repository.save(template);
                });

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    public List<Template> findAll() {
        try {
            return repository.findAll();
        } catch (Exception e) {
            e.printStackTrace();
            return new ArrayList<>();
        }
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
        try {
            repository.deleteById(id);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
