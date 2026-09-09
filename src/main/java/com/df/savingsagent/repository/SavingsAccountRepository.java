package com.df.savingsagent.repository;

import com.df.savingsagent.domain.SavingsAccount;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SavingsAccountRepository extends JpaRepository<SavingsAccount, String> {
    List<SavingsAccount> findByCustomerCustomerId(String customerId);

    @Query("""
            select a from SavingsAccount a
            where lower(a.accountAlias) = lower(:alias)
               or lower(a.accountName) = lower(:alias)
               or a.accountNumber = :alias
            """)
    List<SavingsAccount> findByAliasOrNameOrNumber(@Param("alias") String alias);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from SavingsAccount a where a.accountNumber = :accountNumber")
    Optional<SavingsAccount> findLockedByAccountNumber(@Param("accountNumber") String accountNumber);
}
