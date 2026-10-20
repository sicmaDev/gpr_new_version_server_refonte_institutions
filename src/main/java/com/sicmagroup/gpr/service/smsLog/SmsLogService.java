package com.sicmagroup.gpr.service.smsLog;

import java.util.List;
import java.util.Optional;

import com.sicmagroup.gpr.domain.dto.SmsLogDto;
import com.sicmagroup.gpr.domain.model.SmsLog;

public interface SmsLogService {

    /** Les SMS archivés, du plus récent au plus ancien. */
    List<SmsLogDto> getAll();

    Optional<SmsLogDto> getById(Long id);

    SmsLog save(SmsLog smsLog);
}
