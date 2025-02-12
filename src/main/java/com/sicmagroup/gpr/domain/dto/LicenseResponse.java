package com.sicmagroup.gpr.domain.dto;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Set;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import jakarta.persistence.Transient;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)  // Ajouter cette annotation ici
public class LicenseResponse {

    private int id;
    private String serial;
    private String type;
    @JsonProperty("duration_in_days")
    private Long durationInDays;
    @JsonProperty("number_of_computers")
    private Long numberOfComputers;
    @JsonProperty("number_of_used")
    private Long numberOfUsed;
    // // private Set<Client> clients;
    // @JsonProperty("created_at")
    // private LocalDateTime created_at= LocalDateTime.now();
    // @JsonProperty("updated_at")
    // private LocalDateTime updated_at= LocalDateTime.now();

    @JsonProperty("createdAt")
    private LocalDateTime createdAt= LocalDateTime.now();
    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt= LocalDateTime.now();

    @Transient
    private transient boolean etat;
    @Transient
    private transient String message;

    
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
    // public Set<Client> getClients() {
    //     return clients;
    // }
    // public void setClients(Set<Client> clients) {
    //     this.clients = clients;
    // }

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
