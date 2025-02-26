package com.sicmagroup.gpr.service.product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.domain.model.Objet;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductServiceImpl implements ProductService {

    private final ProductRepository repository;

    @Override
    public List<Product> getAll() {
        return repository.findByIsDeleted(false);
    }

    @Override
    public List<Product> getAllDeleted() {
        return repository.findByIsDeleted(true);
    }

    @Override
    public Product getById(Long id) throws NotFoundException {
        return repository.findById(id).orElseThrow(() -> new NotFoundException());
    }

    @Override
    public Product saveProduct(Product product) {
        product.setUuid(generateUuid());
        return repository.save(product);
    }

    @Override
    public Product updateProduct(Product product) {
        Product existingProduct = repository.findById(product.getId()).orElse(null);
            if (existingProduct  != null && existingProduct.getUuid() != null) {
                product.setUuid(existingProduct.getUuid());
            }
            else{
                product.setUuid(generateUuid());
            }
        return repository.save(product);
    }

    @Override
    public Product deleteTempProduct(Long id) throws NotFoundException {
        Product product = repository.findById(id).orElseThrow(() -> new NotFoundException());
        product.setDeleted(true);
        product.setDeletedAt(LocalDateTime.now());
        product = repository.save(product);
        return product;
    }

    @Override
    public void deleteProduct(Product product) throws Exception {
        try {

            repository.delete(product);

        } catch (Exception e) {
            throw new Exception(
                    "Impossible de supprimer cet produit ou service car il intervient dans plusieurs opérations.");
        }

    }

    @Override
    public Product getDeletedById(Long id, boolean deleted) throws NotFoundException {
        return repository.findByIdAndIsDeleted(id, deleted).orElseThrow(() -> new NotFoundException());

    }

     private String generateUuid() {
        String code = "pr-" + UUID.randomUUID().toString().substring(0, 5);

        while (repository.findByUuid(code).isPresent()) {
            code = "pr-" + UUID.randomUUID().toString().substring(0, 5);
        }

        return code;
    }

     @Override
    public Product findProductByUuid(String uuid){
        try {
            return repository.findByUuid(uuid).orElseThrow(() -> new Exception("Ce point de service n'existe pas."));
        } catch (Exception e) {
            throw new RuntimeException("Ce point de service n'existe pas.", e);
        }
    }


}
