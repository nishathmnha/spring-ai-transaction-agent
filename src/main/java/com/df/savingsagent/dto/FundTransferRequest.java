package com.df.savingsagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record FundTransferRequest(
        @JsonProperty("account_number") @NotBlank String accountNumber,
        @NotNull BigDecimal amount,
        @JsonProperty("initiator_mobile_no") String initiatorMobileNo,
        String narration,
        @JsonProperty("receiver_bank") @NotBlank String receiverBank,
        @JsonProperty("receiver_branch") String receiverBranch,
        @JsonProperty("receiver_name") @NotBlank String receiverName,
        @NotBlank String uuid,
        @NotBlank String version
) {
}
