package com.df.savingsagent.dto;

import java.util.List;

public record BeneficiaryInquiryResponse(String transferType, List<BeneficiaryDto> beneficiaries) {
}
