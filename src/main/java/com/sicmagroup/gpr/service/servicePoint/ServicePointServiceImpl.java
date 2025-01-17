package com.sicmagroup.gpr.service.servicePoint;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import javax.naming.NameNotFoundException;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.repository.ServicePointRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ServicePointServiceImpl implements ServicePointService {

    private final ServicePointRepository repository;

    @Override
    public List<ServicePoint> all() {
        return repository.findAll();
    }

    

    @Override
    public boolean isActif(Long id) {
        try {
            ServicePoint servicePoint = repository.findByIdAndIsDeleted(id, false).orElseThrow();
            return true;
        } catch (Exception e) {
            return false;
        }
        
    }


    @Override
    public List<ServicePoint> getAll() {
        return repository.findByIsDeleted(false);
    }

    @Override
    public ServicePoint getById(Long id) throws Exception {
        return repository.findById(id).orElseThrow(() -> new Exception("Object not found"));
    }

    @Override
    public ServicePoint saveServicePoint(ServicePoint servicePoint) {
        servicePoint.setUuid(generateUuid());
        // servicePoint sp = repository.findOne (servicePoint.getDirection_id());
        return repository.save(servicePoint);
    }

    @Override
    public ServicePoint updateServicePoint(ServicePoint servicePoint) {
        return repository.save(servicePoint);
    }

    @Override
    public ServicePoint deleteTempServicePoint(Long id) throws NotFoundException {
        ServicePoint servicePoint = repository.findById(id).orElseThrow(() -> new NotFoundException());
        servicePoint.setDeleted(true);
        servicePoint.setDeletedAt(LocalDateTime.now());
        return repository.save(servicePoint);
    }
    @Override
    public ServicePoint enableServicePoint(Long id) throws NotFoundException {
        ServicePoint servicePoint = repository.findById(id).orElseThrow(() -> new NotFoundException());
        servicePoint.setDeleted(false);
        servicePoint.setDeletedAt(LocalDateTime.now());
        return repository.save(servicePoint);
    }

    @Override
    public void deleteServicePoint(ServicePoint servicePoint) throws Exception {
        try {
            repository.delete(servicePoint);
        } catch (Exception e) {
            throw new Exception(
                    "Impossible de supprimer cet point de service car il intervient dans plusieurs opérations.");
        }
    }

    @Override
    public List<ServicePoint> getAllDeleted() {
        return repository.findByIsDeleted(true);
    }

    @Override
    public ServicePoint getDeletedById(Long id, boolean deleted) throws NotFoundException {
        return repository.findByIdAndIsDeleted(id, deleted).orElseThrow(() -> new NotFoundException());
    }

    private String generateUuid() {
        String code = "ps-" + UUID.randomUUID().toString().substring(0, 5);

        while (repository.findByUuid(code).isPresent()) {
            code = "ps-" + UUID.randomUUID().toString().substring(0, 5);
        }

        return code;
    }

    public List<ServicePoint> getByDirectionId(Long servicePointId) {
        // Récupérer les points de service dont le direction_id correspond à servicePointId
        return repository.findByDirectionId(servicePointId);
    }
    

}
