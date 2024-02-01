package com.sicmagroup.gpr.service.product;

import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;

import com.sicmagroup.gpr.domain.model.Product;

public interface ProductService {
     public List<Product> getAll();

    public List<Product> getAllDeleted();

    public Product getById(Long id) throws NotFoundException;

    public Product saveProduct(Product product);

    public Product updateProduct(Product product);

    public Product deleteTempProduct(Long id) throws NotFoundException;

    public void deleteProduct(Product product) throws Exception;

    public Product getDeletedById (Long id, boolean deleted) throws NotFoundException ;
}
