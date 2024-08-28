package com.sicmagroup.gpr.api.config.user;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.modelmapper.ModelMapper;
import org.springframework.data.crossstore.ChangeSetPersister.NotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.sicmagroup.gpr.api.auth.AuthenticationResponse;
import com.sicmagroup.gpr.api.chat.message.NewMessageRequest;
import com.sicmagroup.gpr.domain.dto.ApiResponseDto;
import com.sicmagroup.gpr.domain.dto.ErrorResponse;
import com.sicmagroup.gpr.domain.dto.PosteDto;
import com.sicmagroup.gpr.domain.dto.ServicePointDto;
import com.sicmagroup.gpr.domain.dto.UserDto;
import com.sicmagroup.gpr.domain.dto.claimResponse.PosteResponse;
import com.sicmagroup.gpr.domain.dto.claimResponse.ServicePointResponse;
import com.sicmagroup.gpr.domain.enumeration.ChatStatus;
import com.sicmagroup.gpr.domain.enumeration.Role;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.Poste;
import com.sicmagroup.gpr.domain.model.ServicePoint;
import com.sicmagroup.gpr.domain.model.User;
import com.sicmagroup.gpr.domain.model.chat.Chat;
import com.sicmagroup.gpr.repository.UserRepository;
import com.sicmagroup.gpr.repository.chat.ChatRepository;
import com.sicmagroup.gpr.repository.chat.MessageRepository;
import com.sicmagroup.gpr.service.auth.AuthenticationException;
import com.sicmagroup.gpr.service.auth.AuthenticationServiceImpl;
import com.sicmagroup.gpr.service.chat.ChatServiceImpl;
import com.sicmagroup.gpr.service.chat.message.MessageServiceImp;
import com.sicmagroup.gpr.service.claim.ClaimServiceImpl;
import com.sicmagroup.gpr.service.setting.SettingServiceImpl;
import com.sicmagroup.gpr.utils.Utils;

import jakarta.annotation.security.RolesAllowed;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestParam;

@RestController
@CrossOrigin

@RequestMapping("/api/v1/config/user")
@RequiredArgsConstructor
@RolesAllowed("H12")
public class UserController {

    private final AuthenticationServiceImpl authenticationServiceImpl;
    private final ModelMapper modelMapper;
    private final SettingServiceImpl settingServiceImpl;

    @GetMapping("/list")
    public ResponseEntity<ApiResponseDto> list() {
     
        List<User> allUsers = authenticationServiceImpl.all();
        ApiResponseDto apiResponseDto = ApiResponseDto.builder()
                .status(true)
                .content(allUsers.stream().map(this::convertToDto).collect(Collectors.toList()))
                .build();
        return ResponseEntity.ok(apiResponseDto);  

    }
    @DeleteMapping("/disabled/{id}/{isDisabled}")
    public ResponseEntity<ApiResponseDto> disabledUser(@PathVariable(name = "id", required = true) Long id,@PathVariable(name = "isDisabled", required = true) boolean isDisabled) {
     
        ApiResponseDto apiResponseDto;
        User user;
        try {
            if(isDisabled){
                user = authenticationServiceImpl.deleteTempUser(id);
                
            }else{
                user = authenticationServiceImpl.enabledUser(id);
            }
    
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(user))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
          
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("User not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }
    @GetMapping("/list/{deleted}")
    public ResponseEntity<ApiResponseDto> getAll(@PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        if (deleted) {
            List<User> allUsers = authenticationServiceImpl.getAllDeleted();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allUsers.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } else {
            List<User> allUsers = authenticationServiceImpl.getAll();
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(allUsers.stream().map(this::convertToDto).collect(Collectors.toList()))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        }

    }

    @GetMapping("/{id}/{deleted}")
    public ResponseEntity<ApiResponseDto> get(@PathVariable(name = "id", required = true) Long id,
            @PathVariable(name = "deleted", required = false) boolean deleted) {
        ApiResponseDto apiResponseDto;
        User user;
        try {
            user = authenticationServiceImpl.getDeletedById(id, deleted);
            System.out.println(id);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(convertToDto(user))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {
            e.printStackTrace();
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("User not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

    }

    @PostMapping("/register")
    public ResponseEntity<AuthenticationResponse> register(@RequestBody RegisterRequest request) {
        if (request.getAdditionalRole() == "") {
            request.setAdditionalRole(Role.MOLDUE.name());
        }
        try {
            return ResponseEntity.ok(authenticationServiceImpl.register(request));
        } catch (AuthenticationException e) {
            ApiResponseDto apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("Invalid email").message("This email already exist").build())
                    .build();
            e.printStackTrace();
            AuthenticationResponse authenticationResponse = AuthenticationResponse
                    .builder()
                    .response(apiResponseDto)
                    .build();

            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(authenticationResponse);
        }
    }

    @PutMapping("/{id}/update")
    public ResponseEntity<ApiResponseDto> updateServicePoint(@PathVariable(name = "id") Long id,
            @RequestBody RegisterRequest request) {
        ApiResponseDto apiResponseDto;
        if (id != request.getId()) {
            throw new IllegalArgumentException("Les ID ne correspondent pas");
        } else {
            User user;
            try {
                user = authenticationServiceImpl.updateUser(id, request);
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(convertToDto(user))
                        .build();
                return ResponseEntity.ok(apiResponseDto);

            } catch (Exception e) {
                e.printStackTrace();
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().title("NOT FOUND").message("User not found").build())
                        .build();
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
            }

        }

    }

    @DeleteMapping("/{id}/delete_temp")
    public ResponseEntity<ApiResponseDto> deleteTempUser(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        try {
            User user = authenticationServiceImpl.deleteTempUser(id);
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(true)
                    .content(convertToDto(user))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (NotFoundException e) {

            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Service Point not found").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }
    }

    @DeleteMapping("/{id}/delete")
    public ResponseEntity<ApiResponseDto> deleteUser(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto;
        User user = new User();

        try {
            user = authenticationServiceImpl.getById(id);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("NOT FOUND").message("Utilisateur introuvable").build())
                    .build();
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(apiResponseDto);
        }

        if (user.getClaimsCollect().isEmpty()) {
            try {
                authenticationServiceImpl.deleteUser(user);
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(true)
                        .content(true)
                        .build();

            } catch (Exception e) {
                apiResponseDto = ApiResponseDto
                        .builder()
                        .status(false)
                        .content(ErrorResponse.builder().title("Something wrong").message(e.getMessage()).build())
                        .build();
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
            }

            return ResponseEntity.ok(apiResponseDto);
        } else {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("Opération impossible")
                            .message("L'utilisateur intervient dans une ou plusieurs réclamations").build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }

    }

    /**
     * Config receiver email list
     * 
     * @return list of user selected to receive mail
     */
    @GetMapping(value = "/email_receiver/list")
    public ResponseEntity<ApiResponseDto> emailReceiverList() {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();

        List<User> usersReceiver = authenticationServiceImpl.getEmailReceivers();
        apiResponseDto = ApiResponseDto.builder()
                .status(true)
                .content(usersReceiver.stream().map(this::convertToDto).collect(Collectors.toList()))
                .build();

        return ResponseEntity.ok(apiResponseDto);
    }

    @PostMapping(value = "/email_receiver/change")
    public ResponseEntity<ApiResponseDto> addEmailReceiver(@RequestBody AddEmailReceiver request) {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        try {
            User usersReceiver = authenticationServiceImpl.addEmailReceivers(request);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content(this.convertToDto(usersReceiver))
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("Something wrong").message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }

    }

    @DeleteMapping(value = "/email_receiver/{id}/delete")
    public ResponseEntity<ApiResponseDto> removeEmailReceiver(@PathVariable(name = "id") Long id) {
        ApiResponseDto apiResponseDto = ApiResponseDto.builder().build();
        try {
            authenticationServiceImpl.removeEmailReceiver(id);
            apiResponseDto = ApiResponseDto.builder()
                    .status(true)
                    .content("Utilisateur retiré de la liste")
                    .build();
            return ResponseEntity.ok(apiResponseDto);
        } catch (Exception e) {
            apiResponseDto = ApiResponseDto
                    .builder()
                    .status(false)
                    .content(ErrorResponse.builder().title("Something wrong").message(e.getMessage()).build())
                    .build();
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(apiResponseDto);
        }
    }

    // @GetMapping(value = "/path")
    // public String getMethodName() {
    //     User user;
    //     try {
    //         user = authenticationServiceImpl.getById(2L);
    //         Utils.sendSms(Arrays.asList(user),"Essaie sms backend", settingServiceImpl);
           
    //     } catch (Exception ex) {
    //         // TODO Auto-generated catch block
    //         ex.printStackTrace();
    //     }

    //     return new String();
    // }

    private UserDto convertToDto(User user) {
        UserDto userDto = modelMapper.map(user, UserDto.class);
        userDto.setPosteDto(convertToResponse(user.getPoste()));
        userDto.setServicePointDto(convertToResponse(user.getServicePoint()));
        return userDto;
    }

    private PosteResponse convertToResponse(Poste poste1) {
        PosteResponse posteResponse = modelMapper.map(poste1, PosteResponse.class);
        return posteResponse;
    }

    private ServicePointResponse convertToResponse(ServicePoint servicepoint1) {
        ServicePointResponse servicePointResponse = modelMapper.map(servicepoint1, ServicePointResponse.class);
        return servicePointResponse;
    }

    private User convertFromDtoToEntity(UserDto userDto) {
        User user = modelMapper.map(userDto, User.class);

        if (user.getId() != null) {
            // ServicePoint oldServicePoint = serviceImpl.getById(servicePointDto.getId());

        } else {

        }
        return user;
    }
}
