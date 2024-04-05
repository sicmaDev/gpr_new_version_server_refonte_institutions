package com.sicmagroup.gpr.api.claim.botClaim;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;


@RestController
@RequestMapping("/api/v1/bot/claim")
@RequiredArgsConstructor
public class BotClaimController {
    @PostMapping("/save")
    public String postSaveBotClaim(@RequestBody String entity) {
        
        
        return entity;
    }

    @PostMapping("/save/form")
    public String postSaveBotClaimFromForm(@RequestBody String entity) {
        
        
        return entity;
    }
    
}
