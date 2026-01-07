package com.github.mbto.eatlog.service;

import com.github.mbto.eatlog.common.custommodel.InternalAccount;
import com.github.mbto.eatlog.common.dto.auth.YandexAuth;
import com.github.mbto.eatlog.repository.AccountRepository;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

@Profile("dev")
@Service
@Slf4j
@AllArgsConstructor
public class DevLoginService {
    private AccountRepository accountRepository;

    public InternalAccount findByYandexId(
            YandexAuth yandexAuth,
            String ip
    ) {
        return accountRepository.findByYandexId(yandexAuth, ip);
    }
}
