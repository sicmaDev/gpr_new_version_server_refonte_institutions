package com.sicmagroup.gpr.service.log;

import java.util.List;

import com.sicmagroup.gpr.api.config.log.LogRequest;
import com.sicmagroup.gpr.domain.model.Log;

public interface LogService {

    List<Log> getAllLogs();
    Log saveLog(Log log);
    List<Log> getExportLogs();

}
