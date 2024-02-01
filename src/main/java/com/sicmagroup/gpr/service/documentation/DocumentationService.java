package com.sicmagroup.gpr.service.documentation;

import java.io.FileNotFoundException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import com.sicmagroup.gpr.domain.model.Documentation;
import com.sicmagroup.gpr.domain.model.User;

public interface DocumentationService {

    List<Documentation> stores(MultipartFile[] files, String[] libelle, User user);

    Documentation getDocumentation(Long id) throws FileNotFoundException;

    Resource loadAsResource(Long docId);

    void deleteDocumentation(Long id) throws FileNotFoundException;

    List<Documentation> list();

}
