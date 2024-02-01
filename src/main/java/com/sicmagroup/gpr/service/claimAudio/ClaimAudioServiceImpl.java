package com.sicmagroup.gpr.service.claimAudio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.api.claimAudio.ClaimAudioResponse;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.repository.ClaimAudioRepository;
import com.sicmagroup.gpr.repository.MediaRepository;
import com.sicmagroup.gpr.service.media.FileStorageException;
import com.sicmagroup.gpr.service.media.FileStorageProperties;
import com.sicmagroup.gpr.utils.Constante;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class ClaimAudioServiceImpl implements ClaimAudioService {

    private final ClaimAudioRepository repository;

    @Override
    public List<ClaimAudio> store(MultipartFile[] files, Claim claim) {
        List<ClaimAudio> medias = new ArrayList<>();
        for (MultipartFile file : files) {
            medias.add(storeOneFile(file, claim));
        }

        return medias;
    }

    @Override
    public List<ClaimAudio> store(MultipartFile[] files, Suggestion suggestion) {
        List<ClaimAudio> medias = new ArrayList<>();
        for (MultipartFile file : files) {
            medias.add(storeOneFile(file, suggestion));
        }

        return medias;
    }

    @Override
    public Resource loadAsResource(Long mediaId) {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_AUDIO :Constante.PROD_PATH_AUDIO)
                .toAbsolutePath().normalize();
        ClaimAudio media = repository.findById(mediaId)
                .orElseThrow(() -> new FileStorageException("Media introuvable"));
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

    private Resource loadAsResource(ClaimAudio audio) {
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_AUDIO :Constante.PROD_PATH_AUDIO)
                .toAbsolutePath().normalize();
        try {
            // System.out.println("store fnction");
            Path filePath = fileStorageLocation.resolve(audio.getName()).normalize();
            // System.out.println(filePath.toUri().toString());
            Resource resource = new FileSystemResource(filePath.toFile()); // new UrlResource(filePath.toUri());
            if (resource.exists()) {
                return resource;
            } else {
                throw new FileStorageException("Media introuvable " + audio.getName());
            }
        } catch (Exception e) {
            System.out.println(e.getMessage());
            throw new FileStorageException("Media introuvable catch " + audio.getName());
        }
    }

    @Override
    public ClaimAudio getAudio(Long id) throws FileNotFoundException {
        return repository.findById(id).orElseThrow(() -> new FileNotFoundException("File not found with id " + id));

    }

    @Override
    public List<ClaimAudioResponse> getAudioByClaim(Claim claim) {
        List<ClaimAudio> list = repository.findByClaim(claim);
        List<ClaimAudioResponse> responses = new ArrayList<>();
        for (ClaimAudio audio : list) {
            ClaimAudioResponse claimAudioResponse;
            try {
                claimAudioResponse = ClaimAudioResponse
                        .builder()
                        .id(audio.getId())
                        .name(audio.getName())
                        .path(audio.getPath())
                        .size(audio.getSize())
                        .data(loadAsResource(audio).getContentAsByteArray())
                        .build();
                responses.add(claimAudioResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return responses;
    }

    @Override
    public List<ClaimAudio> getAudiosBySuggestion(Suggestion suggestion) {
        return repository.findBySuggestion(suggestion);
    }

    private ClaimAudio storeOneFile(MultipartFile file, Claim claim) {
        // System.out.println("store fnction");
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_AUDIO :Constante.PROD_PATH_AUDIO)
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

        ClaimAudio media;
        media = ClaimAudio
                .builder()
                .claim(claim)
                .name(fileName)
                .size(file.getSize())
                .path(targetLocation.toAbsolutePath().toFile().getAbsolutePath())
                .build();
        return repository.save(media);
    }

    private ClaimAudio storeOneFile(MultipartFile file, Suggestion suggestion) {
        System.out.println("store fnction");
        FileStorageProperties fileStorageProperties = new FileStorageProperties();
        Path fileStorageLocation = Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_AUDIO :Constante.PROD_PATH_AUDIO)
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

        ClaimAudio media;
        media = ClaimAudio
                .builder()
                .suggestion(suggestion)
                .name(fileName)
                .size(file.getSize())
                .path(targetLocation.toAbsolutePath().toFile().getAbsolutePath())
                .build();
        return repository.save(media);
    }

}
