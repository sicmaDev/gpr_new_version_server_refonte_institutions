package com.sicmagroup.gpr.service.servicePoint;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import com.sicmagroup.gpr.domain.model.ServicePoint;

public interface ServicePointService {
    public List<ServicePoint> all();

    public boolean isActif(Long id);

    public List<ServicePoint> getAll();


    public List<ServicePoint> getAllDeleted();

    public ServicePoint getById(Long id) throws Exception;

    public ServicePoint saveServicePoint(ServicePoint servicePoint);

    public ServicePoint updateServicePoint(ServicePoint servicePoint);

    public ServicePoint deleteTempServicePoint(Long id) throws NotFoundException;
    
    public ServicePoint enableServicePoint(Long id) throws NotFoundException;

    public void deleteServicePoint(ServicePoint servicePoint) throws Exception ;

    public ServicePoint getDeletedById (Long id, boolean deleted) throws NotFoundException ;
    
    public ServicePoint findServicePointByUuid(String uuid);

    
}
