package com.sicmagroup.gpr.service.claimAudio;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.modelmapper.ModelMapper;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.api.claimAudio.ClaimAudioResponse;
import com.sicmagroup.gpr.domain.dto.ExtraContentResponse;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.ExtraContent;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.repository.ClaimAudioRepository;
import com.sicmagroup.gpr.repository.MediaRepository;
import com.sicmagroup.gpr.service.media.FileStorageException;
import com.sicmagroup.gpr.service.media.FileStorageProperties;
import com.sicmagroup.gpr.utils.Constante;

import jakarta.annotation.Nullable;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor

public class ClaimAudioServiceImpl implements ClaimAudioService {

    private final ClaimAudioRepository repository;
    private final ModelMapper modelMapper;

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
    public List<ClaimAudio> store(MultipartFile[] files, Claim claim,ExtraContent extraContent) {
        List<ClaimAudio> medias = new ArrayList<>();
        for (MultipartFile file : files) {
            medias.add(storeOneFile(file, claim,extraContent));
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
            Resource resource = new FileSystemResource(filePath.toFile());
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

    /** Enregistre la transcription IA d'un audio. Les champs absents de la demande ne sont pas modifiés. */
    public ClaimAudio enregistrerTranscription(Long id, com.sicmagroup.gpr.api.claimAudio.TranscriptionAudioRequest r)
            throws FileNotFoundException {
        ClaimAudio audio = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found with id " + id));
        if (r.getTranscriptionRaw() != null) audio.setTranscriptionRaw(r.getTranscriptionRaw());
        if (r.getTranscriptionCorrected() != null) audio.setTranscriptionCorrected(r.getTranscriptionCorrected());
        if (r.getTranscriptionModel() != null) audio.setTranscriptionModel(limiter(r.getTranscriptionModel(), 150));
        if (r.getTranscriptionStatus() != null) audio.setTranscriptionStatus(limiter(r.getTranscriptionStatus(), 40));
        if (r.getTranscriptionLanguage() != null) audio.setTranscriptionLanguage(limiter(r.getTranscriptionLanguage(), 20));
        if (r.getTranscriptionDuration() != null) audio.setTranscriptionDuration(r.getTranscriptionDuration());
        if (r.getSegmentsJson() != null) audio.setSegmentsJson(r.getSegmentsJson());
        if (r.getSpeakersJson() != null) audio.setSpeakersJson(r.getSpeakersJson());
        if (r.getTranscriptionError() != null) audio.setTranscriptionError(r.getTranscriptionError());
        if (r.getTranscriptionAdded() != null) audio.setTranscriptionAdded(r.getTranscriptionAdded());
        java.time.Instant maintenant = java.time.Instant.now();
        if (audio.getTranscriptionCreatedAt() == null) audio.setTranscriptionCreatedAt(maintenant);
        audio.setTranscriptionUpdatedAt(maintenant);
        return repository.save(audio);
    }

    private static String limiter(String s, int max) {
        return s.length() <= max ? s : s.substring(0, max);
    }

    @Override
    public void deleteAudio(Long id, User connectedUser) throws Exception {
        ClaimAudio audio = repository.findById(id)
                .orElseThrow(() -> new FileNotFoundException("File not found with id " + id));

        if (audio.getExtraContent() == null || audio.getExtraContent().getUser() == null
                || !audio.getExtraContent().getUser().getId().equals(connectedUser.getId())) {
            throw new Exception("Vous n'êtes pas autorisé à supprimer cet audio");
        }

        try {
            // Chemin enregistré avec l'audio (ex. audios WhatsApp) ; à défaut, dossier des audios + nom du fichier.
            Path audioFile = (audio.getPath() != null && !audio.getPath().isBlank())
                    ? Paths.get(audio.getPath())
                    : Paths.get(Constante.DEVMODE ? Constante.TEST_PATH_AUDIO : Constante.PROD_PATH_AUDIO)
                            .toAbsolutePath().normalize().resolve(audio.getName());
            Files.deleteIfExists(audioFile);
        } catch (IOException e) {
            System.out.println("Impossible de supprimer le fichier audio physique : " + e.getMessage());
        }

        repository.delete(audio);
    }

    // @Override
    // public List<ClaimAudioResponse> getAudioByClaim(Claim claim) {
    //     List<ClaimAudio> list = repository.findByClaim(claim);
    //     List<ClaimAudioResponse> responses = new ArrayList<>();
    //     for (ClaimAudio audio : list) {
    //         ClaimAudioResponse claimAudioResponse;
    //         try {
    //             claimAudioResponse = ClaimAudioResponse
    //                     .builder()
    //                     .id(audio.getId())
    //                     .name(audio.getName())
    //                     .path(audio.getPath())
    //                     .size(audio.getSize())
    //                     .data(loadAsResource(audio).getContentAsByteArray())
    //                     .build();
    //             responses.add(claimAudioResponse);
    //         } catch (IOException e) {
    //             e.printStackTrace();
    //         }

    //     }
    //     return responses;
    // }

    
    @Override
    public List<ClaimAudioResponse> getAudioByClaim(Claim claim) {
        List<ClaimAudio> list = repository.findByClaim(claim);
        List<ClaimAudioResponse> responses = new ArrayList<>();
        for (ClaimAudio audio : list) {
            ClaimAudioResponse claimAudioResponse;
            try {
                ExtraContentResponse extra = null;
                if(audio.getExtraContent() instanceof ExtraContent){
                    extra = modelMapper.map(audio.getExtraContent(),ExtraContentResponse.class);
                }
                
                
                claimAudioResponse = ClaimAudioResponse
                        .builder()
                        .id(audio.getId())
                        .name(audio.getName())
                        .is_extra(audio.is_extra())
                        .extra(extra)
                        .path(audio.getPath())
                        .size(audio.getSize())
                        .data(loadAsResource(audio).getContentAsByteArray())
                        .transcriptionAdded(audio.isTranscriptionAdded())
                        .transcriptionRaw(audio.getTranscriptionRaw())
                        .transcriptionCorrected(audio.getTranscriptionCorrected())
                        .transcriptionStatus(audio.getTranscriptionStatus())
                        .transcriptionModel(audio.getTranscriptionModel())
                        .transcriptionLanguage(audio.getTranscriptionLanguage())
                        .transcriptionDuration(audio.getTranscriptionDuration())
                        .segmentsJson(audio.getSegmentsJson())
                        .build();
                responses.add(claimAudioResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        return responses;
    }
    @Override
    public List<ClaimAudioResponse> getAudiosBySuggestion(Suggestion suggestion) {
        List<ClaimAudio> list = repository.findBySuggestion(suggestion);
        List<ClaimAudioResponse> responses = new ArrayList<>();
        for (ClaimAudio audio : list) {
            ClaimAudioResponse claimAudioResponse;
            try {
                ExtraContentResponse extra = null;
                if(audio.getExtraContent() instanceof ExtraContent){
                    extra = modelMapper.map(audio.getExtraContent(),ExtraContentResponse.class);
                }
                claimAudioResponse = ClaimAudioResponse
                        .builder()
                        .id(audio.getId())
                        .name(audio.getName())
                        .is_extra(audio.is_extra())
                        .extra(extra)
                        .path(audio.getPath())
                        .size(audio.getSize())
                        .data(loadAsResource(audio).getContentAsByteArray())
                        .transcriptionAdded(audio.isTranscriptionAdded())
                        .transcriptionRaw(audio.getTranscriptionRaw())
                        .transcriptionCorrected(audio.getTranscriptionCorrected())
                        .transcriptionStatus(audio.getTranscriptionStatus())
                        .transcriptionModel(audio.getTranscriptionModel())
                        .transcriptionLanguage(audio.getTranscriptionLanguage())
                        .transcriptionDuration(audio.getTranscriptionDuration())
                        .segmentsJson(audio.getSegmentsJson())
                        .build();
                responses.add(claimAudioResponse);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
        System.out.println(responses.size()+">>>");
        return responses;
    }

   
    private ClaimAudio storeOneFile(MultipartFile file, Claim claim,@Nullable ExtraContent extraContent) {
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
                .extraContent(extraContent)
                .is_extra(extraContent != null)
                .name(fileName)
                .size(file.getSize())
                .path(targetLocation.toAbsolutePath().toFile().getAbsolutePath())
                .build();
        return repository.save(media);
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
