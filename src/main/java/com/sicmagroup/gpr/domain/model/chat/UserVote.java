package com.sicmagroup.gpr.domain.model.chat;

import java.util.Objects;

import com.sicmagroup.gpr.domain.enumeration.VoteType;
import com.sicmagroup.gpr.domain.model.User;

import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
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
@Table(name = "gps_user_vote")
public class UserVote {
      @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private User user;

    @ManyToOne
    private Vote vote;

    @Enumerated(EnumType.STRING)
    private VoteType voteType;

    @Override
    public boolean equals(Object obj){
      if(obj == null || obj.getClass() != getClass())
        return false;
      if(this == obj)
        return true;
      
      UserVote userVote = (UserVote) obj;
      return Objects.equals(userVote.getId(), this.id);
    }
}
