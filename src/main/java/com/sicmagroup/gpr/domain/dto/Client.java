package com.sicmagroup.gpr.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class Client {
    
    // public Client() {
    // }
    // public Client(int id, String fullname, String company, String email, String activationRequest,
    //         LocalDateTime createdAt, LocalDateTime updatedAt) {
    //     this.id = id;
    //     this.fullname = fullname;
    //     this.company = company;
    //     this.email = email;
    //     this.activationRequest = activationRequest;
    //     this.createdAt = createdAt;
    //     this.updatedAt = updatedAt;
    // }
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getFullname() {
        return fullname;
    }
    public void setFullname(String fullname) {
        this.fullname = fullname;
    }
    public String getCompany() {
        return company;
    }
    public void setCompany(String company) {
        this.company = company;
    }
    public String getEmail() {
        return email;
    }
    public void setEmail(String email) {
        this.email = email;
    }
    public String getActivationRequest() {
        return activationRequest;
    }
    public void setActivationRequest(String activationRequest) {
        this.activationRequest = activationRequest;
    }
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    private int id;
    private String fullname;
    private String company;
    private String email;
    private String activationRequest;
    private LocalDateTime createdAt= LocalDateTime.now();
    private LocalDateTime updatedAt= LocalDateTime.now();
}
