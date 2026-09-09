package com.df.savingsagent.repository;

import com.df.savingsagent.domain.FundTransfer;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface FundTransferRepository extends JpaRepository<FundTransfer, Long> {
    Optional<FundTransfer> findByUuid(String uuid);
}
