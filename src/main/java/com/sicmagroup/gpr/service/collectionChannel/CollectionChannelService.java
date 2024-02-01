package com.sicmagroup.gpr.service.collectionChannel;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import com.sicmagroup.gpr.domain.model.CollectionChannel;

public interface CollectionChannelService {
    
    public List<CollectionChannel> getAll();

    public List<CollectionChannel> getAllDeleted();

    public CollectionChannel getById(Long id) throws NotFoundException;

    public CollectionChannel saveCollectionChannel(CollectionChannel collectionChannel);

    public CollectionChannel updateCollectionChannel(CollectionChannel collectionChannel);

    public CollectionChannel deleteTempCollectionChannel(Long id) throws NotFoundException;

    public void deleteCollectionChannel(CollectionChannel collectionChannel) throws Exception ;

    public CollectionChannel getDeletedById(Long id, boolean deleted) throws NotFoundException;
}
