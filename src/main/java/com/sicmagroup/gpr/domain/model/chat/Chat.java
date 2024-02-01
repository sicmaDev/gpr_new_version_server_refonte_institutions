package com.sicmagroup.gpr.domain.model.chat;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;

import com.sicmagroup.gpr.domain.enumeration.ChatStatus;
import com.sicmagroup.gpr.domain.model.Claim;
import com.sicmagroup.gpr.domain.model.User;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;

@Data 
@AllArgsConstructor 
@NoArgsConstructor
@Builder
@Entity
@Table(name = "gps_chat")
public class Chat {
    @Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;


    @ManyToOne
    private User createdBy;

    @Enumerated(EnumType.STRING)
    private ChatStatus status;

    @ManyToMany(mappedBy = "chatsMember", fetch = FetchType.EAGER)
    private List<User> members;

    @ManyToMany(mappedBy = "chatsGuest", fetch = FetchType.EAGER)
    private List<User> guests;

    @OneToMany(fetch = FetchType.EAGER)
    private List<Vote> vote;

    @OneToMany(mappedBy = "chat", fetch = FetchType.EAGER)
    private List<Message> messages;

    @OneToOne(mappedBy = "session")
    private Claim claim;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Override
    public boolean equals(Object obj){
        if(obj == null || obj.getClass() != getClass())
            return false;
        if(obj == this)
            return true;

        Chat chat = (Chat) obj;

        return Objects.equals(chat.getId(), id);
    }
}
