package com.sicmagroup.gpr.service.media;

import java.io.FileNotFoundException;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.InboxMessage;
import com.sicmagroup.gpr.domain.model.Media;
import com.sicmagroup.gpr.domain.model.Suggestion;

public interface MediaService {

    List<Media> store(MultipartFile[] files, Claim claim);

    List<Media> store(MultipartFile[] files, Suggestion suggestion);

    List<Media> storeFromString(List<String> files, Claim claim);


    Media storeFileWhatsapp(InboxMessage message, String mimeType);

    Stream<Path> loadAll(Long claimId);

    Path load(Long mediaId);

    Resource loadAsResource(Long mediaId);

    Resource loadAsResource(String path);

    Media getFile(Long id) throws FileNotFoundException;
    Media getFileByPath(String path) throws FileNotFoundException;

    List<Media> getFileByClaim(Claim claim);

    List<Media> getFilesBySuggestion(Suggestion suggestion);
    Boolean attachFileToClaim(Claim claim, List<InboxMessage> messages);
    Boolean attachFileToClaim(Suggestion suggestion, List<InboxMessage> messages);

}
