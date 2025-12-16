package com.sicmagroup.gpr.api.config.product;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.ProductDto;
import com.sicmagroup.gpr.domain.model.Product;
import com.sicmagroup.gpr.service.product.ProductServiceImpl;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;

@RestController
@CrossOrigin

@RequestMapping("/api/v1/config/product")
@RequiredArgsConstructor
@RolesAllowed("H12")
public class ProductController {

    private final ModelMapper modelMapper;
    private final ProductServiceImpl productServiceImpl;

    @GetMapping("/list/{deleted}")
    public ResponseEntity<ApiResponseDto> getAll(@PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        if (deleted) {
            List<Product> allProducts = productServiceImpl.getAllDeleted();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allProducts.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } else {
            List<Product> allProducts = productServiceImpl.getAll();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allProducts.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @GetMapping("/{id}/{deleted}")
    public ResponseEntity<ApiResponseDto> get(@PathVariable(name = "id", required = true) Long id,
            @PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        Product product;
        try {
            product = productServiceImpl.getDeletedById(id, deleted);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(product))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Product not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PostMapping("/add")
    public ResponseEntity<ApiResponseDto> addProduct(@RequestBody ProductDto productDto) {
        ApiResponseDto apiResponseDto;

        Product product;
        try {
            product = productServiceImpl.saveProduct(convertFromDtoToEntity(productDto));
             apiResponseDto = ApiResponseDto.builder()
                .status(true)
                .content(convertToDto(product))
                .build();

            return ResponseEntity.ok(apiResponseDto);
        } catch (DataIntegrityViolationException e) {

           apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder()
                            .title("Erreur de duplication")
                            .message("Une configuration avec le même libellé existe déjà.")
                            .build())
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Product not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

       
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<ApiResponseDto> updateProduct(@PathVariable(name = "id") Long id,
            @RequestBody ProductDto productDto) {
        ApiResponseDto apiResponseDto;
        Long idParsed = id.longValue();
        Long productId = productDto.getId().longValue();
        if (!idParsed.equals(productId)){
            throw new IllegalArgumentException("Les ID ne correspondent pas");
        } else {
            Product product;
            try {
                product = productServiceImpl.updateProduct(convertFromDtoToEntity(productDto));
                apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(product))
                    .build();

                return ResponseEntity.ok(apiResponseDto);
             } catch (DataIntegrityViolationException e) {

                apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder()
                            .title("Erreur de duplication")
                            .message("Une configuration avec le même libellé existe déjà.")
                            .build())
                    .build();

                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
            } catch (NotFoundException e) {
                e.printStackTrace();
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().title("NOT FOUND").message("Product not found").build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }
            
        }

    }

    @DeleteMapping("/{id}/delete_temp")
    public ResponseEntity<ApiResponseDto> deleteTempProduct(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            Product product = productServiceImpl.deleteTempProduct(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(product))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Product not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);

        }
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteProduct(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        Product product = new Product();
        // System.err.println("id of product "+id);
        try {
            product = productServiceImpl.getById(id);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse
                            .builder()
                            .title("NOT FOUND")
                            .message("Produit introuvable")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        if (product.getClaims().isEmpty()) {
            try {
                productServiceImpl.deleteProduct(product);
                 apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(true)
                    .build();
            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().title("Something wrong").message(e.getMessage()).build())
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
            }
           
            return ResponseEntity.ok(apiResponseDto);
        } else {
             apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse
                            .builder()
                            .title("Opération impossible")
                            .message("Le produit/service intervient dans certaine(s) réclamation, il ne peut être supprimé")
                            .build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }
      
    }

    private ProductDto convertToDto(Product product) {
        ProductDto productDto = modelMapper.map(product, ProductDto.class);
        return productDto;
    }

    private Product convertFromDtoToEntity(ProductDto productDto) throws NotFoundException {
        Product product = modelMapper.map(productDto, Product.class);

        if (product.getId() != null) {
            Product oldProduct = productServiceImpl.getById(productDto.getId());
            product.setCreatedAt(oldProduct.getCreatedAt());
            product.setUpdatedAt(LocalDateTime.now());

        } else {
            product.setCreatedAt(LocalDateTime.now());
            product.setDeleted(false);
        }
        return product;
    }
}
