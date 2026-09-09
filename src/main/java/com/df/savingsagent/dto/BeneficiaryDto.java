package com.df.savingsagent.dto;

public record BeneficiaryDto(
        String customerId,
        String beneficiaryName,
        String accountNumber,
        String bankCode,
        String bankName,
        String branchCode,
        String currency,
        String customerStatus,
        String accountStatus,
        boolean savedBeneficiary
) {
}
