package com.payflow.service;

import com.payflow.dto.TransferRequest;
import com.payflow.model.Account;
import com.payflow.model.User;
import com.payflow.repository.AccountRepository;
import com.payflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Testcontainers
class ConcurrencyTest {

    @Autowired
    private TransactionService transactionService;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private UserRepository userRepository;

    private Account sourceAccount;
    private Account destAccount;

    @BeforeEach
    void setUp() {
        User user = userRepository.save(new User("conc-" + UUID.randomUUID() + "@test.com", "hash", "Test"));

        sourceAccount = new Account(user.getId(), "Source", "USD");
        sourceAccount.setBalance(BigDecimal.valueOf(1000));
        sourceAccount = accountRepository.save(sourceAccount);

        destAccount = new Account(user.getId(), "Destination", "USD");
        destAccount.setBalance(BigDecimal.ZERO);
        destAccount = accountRepository.save(destAccount);
    }

    @Test
    void concurrentTransfersProduceCorrectBalance() throws Exception {
        int threadCount = 10;
        BigDecimal transferAmount = BigDecimal.TEN;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(1);

        List<Future<?>> futures = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            String key = UUID.randomUUID().toString();
            futures.add(executor.submit(() -> {
                try {
                    latch.await();
                    transactionService.transfer(new TransferRequest(
                            sourceAccount.getId(), destAccount.getId(),
                            transferAmount, "Concurrent transfer", key));
                } catch (Exception e) {
                    // Some may fail due to optimistic lock exhaustion — that's expected
                }
                return null;
            }));
        }

        latch.countDown(); // Start all threads simultaneously
        executor.shutdown();
        executor.awaitTermination(30, TimeUnit.SECONDS);

        // Wait for all futures
        for (Future<?> f : futures) {
            try { f.get(); } catch (ExecutionException ignored) {}
        }

        Account updatedSource = accountRepository.findById(sourceAccount.getId()).orElseThrow();
        Account updatedDest = accountRepository.findById(destAccount.getId()).orElseThrow();

        // Source + Dest must equal original total (conservation of money)
        BigDecimal total = updatedSource.getBalance().add(updatedDest.getBalance());
        assertThat(total).isEqualByComparingTo(BigDecimal.valueOf(1000));

        // At least some transfers succeeded
        assertThat(updatedDest.getBalance()).isGreaterThan(BigDecimal.ZERO);
    }
}
