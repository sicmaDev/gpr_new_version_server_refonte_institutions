package com.sicmagroup.gpr.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

import jakarta.persistence.Transient;

@Data
@AllArgsConstructor
@NoArgsConstructor

public class LicenseResponse {
    
    // public License() {
    // }
    
    // public License(int id, String serial, String type, Long durationInDays, Long numberOfComputers, Long numberOfUsed,
    //         Set<Client> clients, LocalDateTime createdAt, LocalDateTime updatedAt) {
    //     this.id = id;
    //     this.serial = serial;
    //     this.type = type;
    //     this.durationInDays = durationInDays;
    //     this.numberOfComputers = numberOfComputers;
    //     this.numberOfUsed = numberOfUsed;
    //     this.clients = clients;
    //     this.createdAt = createdAt;
    //     this.updatedAt = updatedAt;
    // }

    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public String getSerial() {
        return serial;
    }
    public void setSerial(String serial) {
        this.serial = serial;
    }
    public String getType() {
        return type;
    }
    public void setType(String type) {
        this.type = type;
    }
    public Long getDurationInDays() {
        return durationInDays;
    }
    public void setDurationInDays(Long durationInDays) {
        this.durationInDays = durationInDays;
    }
    public Long getNumberOfComputers() {
        return numberOfComputers;
    }
    public void setNumberOfComputers(Long numberOfComputers) {
        this.numberOfComputers = numberOfComputers;
    }
    public Long getNumberOfUsed() {
        return numberOfUsed;
    }
    public void setNumberOfUsed(Long numberOfUsed) {
        this.numberOfUsed = numberOfUsed;
    }
    public Set<Client> getClients() {
        return clients;
    }
    public void setClients(Set<Client> clients) {
        this.clients = clients;
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
    private String serial;
    private String type;
    private Long durationInDays;
    private Long numberOfComputers;
    private Long numberOfUsed;
    private Set<Client> clients;
    private LocalDateTime createdAt= LocalDateTime.now();
    private LocalDateTime updatedAt= LocalDateTime.now();

    @Transient
    private transient boolean etat;
    @Transient
    private transient String message;

    public String getMessage(){
        return message;
    }

    public void setMessage(String message){
        this.message = message;
    }

    public boolean getEtat(){
        return this.etat;
    }

    public void setEtat(boolean etat){
        this.etat = etat;
    }


    
}
