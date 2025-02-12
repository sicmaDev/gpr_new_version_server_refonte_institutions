package com.sicmagroup.gpr.service.media;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Inbox;
import com.sicmagroup.gpr.domain.model.InboxMessage;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.InboxMessageRepository;
import com.sicmagroup.gpr.repository.InboxRepository;
import com.sicmagroup.gpr.repository.MediaRepository;
import com.sicmagroup.gpr.utils.Constante;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {

    private final MediaRepository repository;
    private final InboxMessageRepository messageRepository;
    private final InboxRepository inboxRepository;

    @Override
    public List<Media> store(MultipartFile[] files, Claim claim) {
        List<Media> medias = new ArrayList<>();
        for (MultipartFile file : files) {
            medias.add(storeOneFile(file, claim));
        }

        return medias;
    }

    @Override
    public Stream<Path> loadAll(Long claimId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'loadAll'");
    }

    @Override
    public Path load(Long mediaId) {
        // TODO Auto-generated method stub
        throw new UnsupportedOperationException("Unimplemented method 'load'");
    }

    @Override
    public Resource loadAsResource(Long mediaId) {
        // FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths
                .get(Constante.DEVMODE ? Constante.TEST_PATH_PIECE_JOINTES : Constante.PROD_PATH_PIECE_JOINTES)
                .toAbsolutePath().normalize();
        Media media = repository.findById(mediaId).orElseThrow(() -> new FileStorageException("Media introuvable"));
        try {
            System.out.println("store fnction");
            Path filePath = fileStorageLocation.resolve(media.getName()).normalize();
            System.out.println(filePath.toUri().toString());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("Media introuvable " + media.getName());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Media introuvable catch " + media.getName());
        }
    }
    @Override
    public Resource loadAsResource(String mediaId) {
        // FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths
                .get(Constante.DEVMODE ? Constante.TEST_PATH_PIECE_JOINTES : Constante.PROD_PATH_PIECE_JOINTES)
                .toAbsolutePath().normalize();
        Media media = repository.findByPath(mediaId).orElseThrow(() -> new FileStorageException("Media introuvable"));
        try {
            System.out.println("store fnction");
            Path filePath = fileStorageLocation.resolve(media.getName()).normalize();
            System.out.println(filePath.toUri().toString());
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("Media introuvable " + media.getName());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Media introuvable catch " + media.getName());
        }
    }

    private Media storeOneFile(MultipartFile file, Claim claim) {
        // System.out.println("store fnction");
        // FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths
                .get(Constante.DEVMODE ? Constante.TEST_PATH_PIECE_JOINTES : Constante.PROD_PATH_PIECE_JOINTES)
                .toAbsolutePath().normalize();
        // System.out.println(fileStorageLocation.toAbsolutePath().toString());
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.",
                    e);
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        if (fileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
        }

        Path targetLocation = fileStorageLocation.resolve(fileName);
        try {

            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Sorry! Filename cann't be upload " + fileName);
        }

        Media media;
        media = Media
                .builder()
                .claim(claim)
                .name(fileName)
                .size(file.getSize())
                .path(targetLocation.toAbsolutePath().toFile().getAbsolutePath())
                .build();
        return repository.save(media);
    }

    private Media storeOneFile(InboxMessage inboxMessage, String mimeType) {
        // System.out.println("store fnction");
        // FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths
                .get(Constante.DEVMODE ? Constante.TEST_PATH_PIECE_JOINTES : Constante.PROD_PATH_PIECE_JOINTES)
                .toAbsolutePath().normalize();
        // System.out.println(fileStorageLocation.toAbsolutePath().toString());
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.",
                    e);
        }

        String extension = mimeType.substring(mimeType.lastIndexOf('/') + 1);
        String fileName = inboxMessage.getInbox().getId()+"-"+UUID.randomUUID().toString() + "." + extension;

        if (fileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
        }

        Path targetLocation = fileStorageLocation.resolve(fileName);
        try {
            byte[] file = Base64.getDecoder().decode(inboxMessage.getContent());
            Files.write(targetLocation, file);
            // Files.copy(file.getInputStream(), targetLocation,
            // StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Sorry! Filename cann't be upload " + fileName);
        }

        Media media;
        media = Media
                .builder()
                .name(fileName)
                .size((long) inboxMessage.getContent().length())
                .path(targetLocation.toAbsolutePath().toFile().getAbsolutePath())
                .build();
        return repository.save(media);
    }

    private Media storeOneFile(MultipartFile file, Suggestion suggestion) {
        System.out.println("store fnction");
        // FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths
                .get(Constante.DEVMODE ? Constante.TEST_PATH_PIECE_JOINTES : Constante.PROD_PATH_PIECE_JOINTES)
                .toAbsolutePath().normalize();
        // System.out.println(fileStorageLocation.toAbsolutePath().toString());
        try {
            Files.createDirectories(fileStorageLocation);
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Could not create the directory where the uploaded files will be stored.",
                    e);
        }

        String fileName = StringUtils.cleanPath(file.getOriginalFilename());

        if (fileName.contains("..")) {
            throw new FileStorageException("Sorry! Filename contains invalid path sequence " + fileName);
        }

        Path targetLocation = fileStorageLocation.resolve(fileName);
        try {
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new FileStorageException("Sorry! Filename cann't be upload " + fileName);
        }

        Media media;
        media = Media
                .builder()
                .suggestion(suggestion)
                .name(fileName)
                .size(file.getSize())
                .path(targetLocation.toAbsolutePath().toFile().getAbsolutePath())
                .build();
        return repository.save(media);
    }

    @Override
    public Media getFile(Long id) throws FileNotFoundException {
        return repository.findById(id).orElseThrow(() -> new FileNotFoundException("File not found with id " + id));
    }
    @Override
    public Media getFileByPath(String path) throws FileNotFoundException {
        return repository.findByName(path).orElseThrow(() -> new FileNotFoundException("File not found with path " + path));
    }

    @Override
    public List<Media> storeFromString(List<String> files, Claim claim) {
        List<Media> medias = new ArrayList<>();
        // for (String file : files) {
        // medias.add(storeOneFileFromString(file, claim));
        // }

        return medias;
    }

    @Override
    public Media storeFileWhatsapp(InboxMessage inboxMessage, String mimeType) {
        Media media = storeOneFile(inboxMessage, mimeType);
        return media;
    }
    @Override
    public Boolean attachFileToClaim(Claim claim, List<InboxMessage> inboxMessages) {
        for (InboxMessage message : inboxMessages) {
            Optional<Media> media =  repository.findByName(message.getContent());
            if(media.isPresent()){
                Media median = media.get();
                median.setClaim(claim);
                repository.save(median);
            }

        }
    
        return true;
    }
    @Override
    public Boolean attachFileToClaim(Suggestion suggestion, List<InboxMessage> inboxMessages) {
        for (InboxMessage message : inboxMessages) {
            Optional<Media> media =  repository.findByName(message.getContent());
            if(media.isPresent()){
                Media median = media.get();
                median.setSuggestion(suggestion);
                repository.save(median);
            }

        }
    
        return true;
    }



    // private Media storeOneFileFromString(String file, Claim claim){
    // // byte[] filecontent = Base64.getDecoder().decode(file);
    // String fileName = UUID.randomUUID().toString().substring(0, 5) +"_file" +
    // "."+ Utils.guessFileExtension(file);
    // Media media = Media
    // .builder()
    // .claim(claim)
    // .name(fileName)
    // .file(file.getBytes())
    // .build();

    // return repository.save(media);
    // }

    @Override
    public List<Media> getFileByClaim(Claim claim) {
        return repository.findByClaim(claim);
    }

    @Override
    public List<Media> getFilesBySuggestion(Suggestion suggestion) {
        return repository.findBySuggestion(suggestion);
    }

    @Override
    public List<Media> store(MultipartFile[] files, Suggestion suggestion) {
        List<Media> medias = new ArrayList<>();
        for (MultipartFile file : files) {
            medias.add(storeOneFile(file, suggestion));
        }

        return medias;
    }

}
