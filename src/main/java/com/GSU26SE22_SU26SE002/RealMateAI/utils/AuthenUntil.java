package com.GSU26SE22_SU26SE002.RealMateAI.utils;

import com.GSU26SE22_SU26SE002.RealMateAI.model.Account;
import com.GSU26SE22_SU26SE002.RealMateAI.repositories.AccountRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;


@Component
public class AuthenUntil {
    @Autowired
    private AccountRepository accountRepository;

    public Account getCurrentUSer(){
        String username = SecurityContextHolder.getContext().getAuthentication().getName();


        return accountRepository.findByUserName(username).orElse(null);
    }
}
