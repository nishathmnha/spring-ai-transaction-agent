package com.df.savingsagent.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;

public record ValidateAmountRequest(
        @NotBlank String amount,
        @JsonProperty("account_number") @NotBlank String accountNumber,
        @JsonProperty("receiver_bank_code") @NotBlank String receiverBankCode,
        @JsonProperty("receiver_bank_name") @NotBlank String receiverBankName
) {
}
