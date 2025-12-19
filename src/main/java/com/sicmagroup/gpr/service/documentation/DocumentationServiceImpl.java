package com.sicmagroup.gpr.service.documentation;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.domain.model.Documentation;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.DocumentationRepository;
import com.sicmagroup.gpr.service.media.FileStorageException;
import com.sicmagroup.gpr.service.media.FileStorageProperties;
import com.sicmagroup.gpr.utils.Constante;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class DocumentationServiceImpl implements DocumentationService {

    private final DocumentationRepository documentationRepository;

    @Override
    public List<Documentation> stores(MultipartFile[] files, String[] libelle, User user) {
        List<Documentation> resuList = new ArrayList<>();

        // for (int i = 0; i < libelle.length; i++) {
            
            Documentation doc = storeOne(files[0], libelle[0], user);

            if (doc == null) {
                return null;
            } 
            resuList.add(doc);
        // }

        return resuList;
    }

    private Documentation storeOne(MultipartFile file, String libelle, User user) {
        // System.out.println("store fnction");
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_RESSOURCE :Constante.PROD_PATH_RESSOURCE)
                .toAbsolutePath().normalize();
        System.out.println(fileStorageLocation.toAbsolutePath().toString());
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.",
                    e);
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());
        List<Documentation> existing = documentationRepository.findByNameAndSize(fileName, file.getSize());
        if (!existing.isEmpty()) {
            return null;
        }
        
        if (fileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
        }

        Path targetLocation = fileStorageLocation.resolve(fileName);
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
            
        } catch (IOException e) {
            throw new FileStorageException("Sorry! Filename cann't be upload " + fileName);
        }

        Documentation documentation = Documentation
                .builder()
                .name(fileName)
                .libelle(libelle)
                .path(targetLocation.toAbsolutePath().toFile().getAbsolutePath())
                .size(file.getSize())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .user(user)
                .build();

        return documentationRepository.save(documentation);
    }

	@Override
	public Documentation getDocumentation(Long id) throws FileNotFoundException {
		return documentationRepository.findById(id).orElseThrow(() -> new  FileNotFoundException("File not found with id " + id));
	}
	@Override
	public Documentation getDocumentationByPath(String path) throws FileNotFoundException {
		return documentationRepository.findByPath(path).orElseThrow(() -> new  FileNotFoundException("File not found with id " + path));
	}

	public Resource loadAsResource(Long id) {
		   FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_RESSOURCE :Constante.PROD_PATH_RESSOURCE)
            .toAbsolutePath().normalize();  
        Documentation documentation = documentationRepository.findById(id).orElseThrow(() -> new FileStorageException("Document introuvable"));
        try {
             System.out.println("store fnction");
            Path filePath = fileStorageLocation.resolve(documentation.getName()).normalize();
             System.out.println(filePath.toUri().toString());
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()){
                return resource;
            } else {
                throw new FileStorageException("Document introuvable "+documentation.getName());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Document introuvable catch "+documentation.getName());
        }
	}
	public Resource loadAsResource(String id) {
		   FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_RESSOURCE :Constante.PROD_PATH_RESSOURCE)
            .toAbsolutePath().normalize();  
        Documentation documentation = documentationRepository.findByPath(id).orElseThrow(() -> new FileStorageException("Document introuvable"));
        try {
             System.out.println("store fnction");
            Path filePath = fileStorageLocation.resolve(documentation.getName()).normalize();
             System.out.println(filePath.toUri().toString());
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()){
                return resource;
            } else {
                throw new FileStorageException("Document introuvable "+documentation.getName());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Document introuvable catch "+documentation.getName());
        }
	}

	@Override
	public void deleteDocumentation(Long id) throws FileNotFoundException {
        Documentation documentation = documentationRepository.findById(id).orElseThrow(() -> new  FileNotFoundException("File not found with id " + id));
        documentationRepository.deleteById(id);
        
		  Path fileStorageLocation = Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_RESSOURCE :Constante.PROD_PATH_RESSOURCE)
            .toAbsolutePath().normalize();  
        try {
             System.out.println("store fnction");
            Path filePath = fileStorageLocation.resolve(documentation.getName()).normalize();
            //  System.out.println(filePath.toUri().toString());
            Resource resource = new UrlResource(filePath.toUri());
            if(resource.exists()){
                Files.delete(filePath);
            } else {
                throw new FileStorageException("Document introuvable "+documentation.getName());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Document introuvable catch "+documentation.getName());
        }
	}

    @Override
    public List<Documentation> list() {
        return documentationRepository.findAll();
    }

}
