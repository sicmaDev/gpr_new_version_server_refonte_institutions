package com.sicmagroup.gpr.service.collectionChannel;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.CollectionChannel;
import com.sicmagroup.gpr.domain.model.Language;
import com.sicmagroup.gpr.repository.CollectionChannelRespository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CollectionChannelServiceImpl implements CollectionChannelService {

    private final CollectionChannelRespository repository;

    @Override
    public List<CollectionChannel> getAll() {
        return repository.findByIsDeleted(false);
    }

    @Override
    public List<CollectionChannel> getAllDeleted() {
        return repository.findByIsDeleted(true);
    }

    @Override
    public CollectionChannel getById(Long id) throws NotFoundException {
        return repository.findById(id).orElseThrow(() -> new NotFoundException());
    }

    @Override
    public CollectionChannel saveCollectionChannel(CollectionChannel collectionChannel) {
        return repository.save(collectionChannel);
    }

    @Override
    public CollectionChannel updateCollectionChannel(CollectionChannel collectionChannel) {
        return repository.save(collectionChannel);
    }

    @Override
    public CollectionChannel deleteTempCollectionChannel(Long id) throws NotFoundException {
        CollectionChannel collectionChannel = repository.findById(id).orElseThrow(() -> new NotFoundException());
        collectionChannel.setDeleted(true);
        collectionChannel.setDeletedAt(LocalDateTime.now());
        collectionChannel = repository.save(collectionChannel);
        return collectionChannel;
    }

    @Override
    public void deleteCollectionChannel(CollectionChannel collectionChannel) throws Exception {
        try {
            repository.delete(collectionChannel);
        } catch (Exception e) {
            throw new Exception(
                    "Impossible de supprimer cet moyen de collecte car il intervient dans plusieurs opérations.");
        }
    }

    @Override
    public CollectionChannel getDeletedById(Long id, boolean deleted) throws NotFoundException {
        return repository.findByIdAndIsDeleted(id, deleted).orElseThrow(() -> new NotFoundException());
    }

}
