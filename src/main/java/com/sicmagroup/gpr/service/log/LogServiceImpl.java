package com.sicmagroup.gpr.service.log;

import java.util.List;

import org.springframework.stereotype.Service;

import com.sicmagroup.gpr.api.config.log.LogRequest;
import com.sicmagroup.gpr.domain.model.Log;
import com.sicmagroup.gpr.repository.LogRepository;

import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class LogServiceImpl implements LogService {
    private final LogRepository repository;

    @Override
    public List<Log> getAllLogs() {
        return repository.findAll();
    }

    @Override
    public Log saveLog(Log log) {
       
        return repository.save(log);
    }
}
