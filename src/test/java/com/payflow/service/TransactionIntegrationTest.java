package com.payflow.service;

import com.payflow.dto.*;
import com.payflow.model.Account;
import com.payflow.model.User;
import com.payflow.repository.AccountRepository;
import com.payflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
@TestPropertySource(properties = {
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration,org.springframework.boot.autoconfigure.kafka.KafkaAutoConfiguration",
        "spring.datasource.url=jdbc:tc:postgresql:16:///testdb",
        "spring.datasource.driver-class-name=org.testcontainers.jdbc.ContainerDatabaseDriver",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        "spring.flyway.enabled=false"
})
class TransactionIntegrationTest {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;

    @MockBean
    private BalanceCacheService balanceCacheService;
    @MockBean
    private RateLimitService rateLimitService;
    @MockBean
    private com.payflow.event.TransactionEventPublisher transactionEventPublisher;

    private Account sourceAccount;
    private Account destAccount;
    private Long userId;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("integ-" + UUID.randomUUID() + "@test.com", "hash", "Test User"));
        userId = user.getId();

        sourceAccount = new Account(userId, "Source", "USD");
        sourceAccount.setBalance(BigDecimal.valueOf(1000));
        sourceAccount = accountRepository.save(sourceAccount);

        destAccount = new Account(userId, "Destination", "USD");
        destAccount.setBalance(BigDecimal.valueOf(500));
        destAccount = accountRepository.save(destAccount);
    }

    @Test
    void fullTransferFlow() {
        String key = UUID.randomUUID().toString();
        TransferRequest request = new TransferRequest(
                sourceAccount.getId(), destAccount.getId(),
                BigDecimal.valueOf(200), "Integration test", key);

        TransferResponse response = transactionService.transfer(request, userId);

        assertThat(response.getCorrelationId()).isNotNull();
        assertThat(response.getDebit().getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(response.getCredit().getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(700));

        // Verify account balances
        Account updatedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account updatedDest = accountRepository.findById(destAccount.getId()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(800));
        assertThat(updatedDest.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(700));
    }

    @Test
    void depositAndWithdrawFlow() {
        String depositKey = UUID.randomUUID().toString();
        TransactionResponse deposit = transactionService.deposit(
                new DepositRequest(sourceAccount.getId(), BigDecimal.valueOf(300), "Deposit", depositKey), userId);

        assertThat(deposit.getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(1300));

        String withdrawKey = UUID.randomUUID().toString();
        TransactionResponse withdraw = transactionService.withdraw(
                new WithdrawRequest(sourceAccount.getId(), BigDecimal.valueOf(100), "Withdraw", withdrawKey), userId);

        assertThat(withdraw.getBalanceAfter()).isEqualByComparingTo(BigDecimal.valueOf(1200));
    }

    @Test
    void idempotentTransferReplay() {
        String key = UUID.randomUUID().toString();
        TransferRequest request = new TransferRequest(
                sourceAccount.getId(), destAccount.getId(),
                BigDecimal.valueOf(100), "Idem test", key);

        TransferResponse first = transactionService.transfer(request, userId);
        TransferResponse second = transactionService.transfer(request, userId);

        assertThat(first.getCorrelationId()).isEqualTo(second.getCorrelationId());

        // Balance only changed once
        Account updatedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        assertThat(updatedSource.getBalance()).isEqualByComparingTo(BigDecimal.valueOf(900));
    }
}
