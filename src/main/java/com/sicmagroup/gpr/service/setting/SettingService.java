package com.sicmagroup.gpr.service.setting;

import java.util.HashMap;
import java.util.List;

import com.sicmagroup.gpr.api.config.setting.AddSettingRequest;
import com.sicmagroup.gpr.api.config.setting.UpdateSettingRequest;
import com.sicmagroup.gpr.domain.enumeration.ConfigExportEnum;
import com.sicmagroup.gpr.domain.model.Setting;

public interface SettingService {
    
    List<Setting> getAll();
    Setting getbySlug(String slug) throws Exception;
    Setting save(AddSettingRequest request);    
    Setting update(UpdateSettingRequest request) throws Exception;
    void updateLicence();
    
}
