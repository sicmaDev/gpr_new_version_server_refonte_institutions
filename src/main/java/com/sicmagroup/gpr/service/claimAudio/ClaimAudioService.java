package com.sicmagroup.gpr.service.claimAudio;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.api.claimAudio.ClaimAudioResponse;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.ClaimAudio;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;

public interface ClaimAudioService {
    
    List<ClaimAudio> store(MultipartFile[] files, Claim claim);

    List<ClaimAudio> store(MultipartFile[] files, Suggestion suggestion);

    Resource loadAsResource(Long mediaId);

    ClaimAudio getAudio(Long id) throws FileNotFoundException;

    List<ClaimAudioResponse> getAudioByClaim(Claim claim);

    List<ClaimAudio> getAudiosBySuggestion(Suggestion suggestion);
}
