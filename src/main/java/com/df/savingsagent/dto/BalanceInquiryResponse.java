package com.df.savingsagent.dto;

import java.util.List;

public record BalanceInquiryResponse(String customerId, List<AccountBalanceDto> accounts) {
}
