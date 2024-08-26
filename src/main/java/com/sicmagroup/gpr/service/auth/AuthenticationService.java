package com.sicmagroup.gpr.service.auth;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.ResponseEntity;

import com.sicmagroup.gpr.api.auth.AuthenticationRequest;
import com.sicmagroup.gpr.api.auth.AuthenticationResponse;
import com.sicmagroup.gpr.api.auth.UpdatePwdRequest;
import com.sicmagroup.gpr.api.auth.UpdateRequest;
import com.sicmagroup.gpr.api.config.user.AddEmailReceiver;
import com.sicmagroup.gpr.api.config.user.ForgetPasswordRequest;
import com.sicmagroup.gpr.api.config.user.RegisterRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;

public interface AuthenticationService {

    public AuthenticationResponse register(RegisterRequest request) throws AuthenticationException ;

    public AuthenticationResponse authenticate(AuthenticationRequest request) ;

    public ResponseEntity<ApiResponseDto> getAuthData();
    
    public ResponseEntity<ApiResponseDto> forgetPassword(ForgetPasswordRequest request);

    public List<User> getAll();

    public List<User> getAllDeleted();

    public User getById(Long id) throws NotFoundException;

    public User getDeletedById(Long id, boolean deleted) throws NotFoundException;

    public User updateUser(Long id, RegisterRequest userDto) throws NotFoundException;

    public User deleteTempUser(Long id) throws NotFoundException;

    public void deleteUser(User user) throws Exception;

    public List<User> getUsersByRoles(List<Role> roles);

    public User getByEmail(String email) throws Exception;

    public void updateAccountUser(UpdateRequest request) throws Exception;

    public void updateAccountPwdUser(UpdatePwdRequest request) throws Exception;

    public HashMap<String, Object> getDashboard();

    public List<User> getEmailReceivers();

    public User addEmailReceivers(AddEmailReceiver choosedIds)  throws Exception;

    public void removeEmailReceiver(Long id) throws Exception;

    public List<User> getEmailReceiversForNotif(ServicePoint servicePointIndexe);
}
