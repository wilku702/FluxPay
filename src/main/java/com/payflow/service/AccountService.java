package com.payflow.service;

import com.payflow.dto.AccountResponse;
import com.payflow.dto.CreateAccountRequest;
import com.payflow.dto.UpdateAccountStatusRequest;
import com.payflow.exception.AccountNotFoundException;
import com.payflow.model.Account;
import com.payflow.model.AccountStatus;
import com.payflow.repository.AccountRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final BalanceCacheService balanceCacheService;

    @Transactional
    public AccountResponse create(Long userId, CreateAccountRequest request) {
        Account account = new Account(userId, request.getAccountName(), request.getCurrency());
        account = accountRepository.save(account);
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public AccountResponse getById(Long id, Long userId) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        if (!account.getUserId().equals(userId)) {
            throw new AccountNotFoundException(id);
        }
        balanceCacheService.put(account.getId(), account.getBalance());
        return AccountResponse.from(account);
    }

    @Transactional(readOnly = true)
    public List<AccountResponse> getByUserId(Long userId) {
        return accountRepository.findByUserId(userId).stream()
                .map(AccountResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public AccountResponse updateStatus(Long id, Long userId, UpdateAccountStatusRequest request) {
        Account account = accountRepository.findById(id)
                .orElseThrow(() -> new AccountNotFoundException(id));
        if (!account.getUserId().equals(userId)) {
            throw new AccountNotFoundException(id);
        }
        validateStatusTransition(account.getStatus(), request.getStatus());
        account.setStatus(request.getStatus());
        account = accountRepository.save(account);
        balanceCacheService.evict(account.getId());
        return AccountResponse.from(account);
    }

    private void validateStatusTransition(AccountStatus current, AccountStatus target) {
        if (current == AccountStatus.CLOSED) {
            throw new IllegalArgumentException("Cannot change status of a closed account");
        }
        if (current == target) {
            throw new IllegalArgumentException("Account is already " + current);
        }
    }
}
