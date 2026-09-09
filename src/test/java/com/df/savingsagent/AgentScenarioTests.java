package com.df.savingsagent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.df.savingsagent.domain.SavingsAccount;
import com.df.savingsagent.repository.FundTransferRepository;
import com.df.savingsagent.repository.SavingsAccountRepository;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class AgentScenarioTests {
    private static final String AUTH = "Bearer demo-token";

    @Autowired
    MockMvc mvc;

    @Autowired
    SavingsAccountRepository accountRepository;

    @Autowired
    FundTransferRepository transferRepository;

    @Test
    void validTransferRequiresConfirmationThenMovesMoney() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-VALID","message":"Transfer LKR 10 from my salary account to Varuni."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.confirmationRequired").value(true))
                .andExpect(jsonPath("$.toolTrace[0].tool").value("sourceAccountBalanceInquiry"))
                .andExpect(jsonPath("$.toolTrace[1].tool").value("savedBeneficiaryInquiry"))
                .andExpect(jsonPath("$.toolTrace[2].tool").value("validateTransferAmount"));

        assertThat(transferRepository.count()).isZero();

        mvc.perform(post("/api/agent/conversations/CONV-VALID/confirm")
                        .header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("COMPLETED"))
                .andExpect(jsonPath("$.transaction.status").value("COMPLETED"))
                .andExpect(jsonPath("$.toolTrace[0].tool").value("executeFundTransfer"));

        assertBalance("001020020001", "990.00");
        assertBalance("001020020974", "510.00");
    }

    @Test
    void missingInformationRequestsClarificationAndDoesNotTransfer() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-MISSING","message":"Transfer money to Varuni."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("NEEDS_CLARIFICATION"));

        assertThat(transferRepository.count()).isZero();
    }

    @Test
    void invalidSourceAccountStopsBeforeExecution() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-BAD-SOURCE","message":"Transfer LKR 10 from my unknown account to Varuni."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.toolTrace[2].tool").value("validateTransferAmount"));

        assertThat(transferRepository.count()).isZero();
    }

    @Test
    void invalidDestinationStopsBeforeExecution() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-BAD-DEST","message":"Transfer LKR 10 from my salary account to Unknown Person."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATION_FAILED"));

        assertThat(transferRepository.count()).isZero();
    }

    @Test
    void inactiveSourceStatusesStopTransfer() throws Exception {
        for (String alias : new String[]{"dormant account", "frozen account", "closed account", "restricted account"}) {
            mvc.perform(post("/api/agent/chat")
                            .header("Authorization", AUTH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"conversationId":"CONV-%s","message":"Transfer LKR 10 from my %s to Varuni."}
                                    """.formatted(alias.replace(" ", "-"), alias)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VALIDATION_FAILED"));
        }
        assertThat(transferRepository.count()).isZero();
    }

    @Test
    void inactiveDestinationStatusesStopTransfer() throws Exception {
        for (String beneficiary : new String[]{"Dormant Beneficiary", "Frozen Beneficiary", "Closed Beneficiary", "Restricted Beneficiary"}) {
            mvc.perform(post("/api/agent/chat")
                            .header("Authorization", AUTH)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                    {"conversationId":"CONV-%s","message":"Transfer LKR 10 from my salary account to %s."}
                                    """.formatted(beneficiary.replace(" ", "-"), beneficiary)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.status").value("VALIDATION_FAILED"));
        }
        assertThat(transferRepository.count()).isZero();
    }

    @Test
    void insufficientBalanceAndLimitExceededStopTransfer() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-INSUFFICIENT","message":"Transfer LKR 2,000 from my salary account to Varuni."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATION_FAILED"));

        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-LIMIT","message":"Transfer LKR 100 from my salary account to Varuni."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("VALIDATION_FAILED"));

        assertThat(transferRepository.count()).isZero();
    }

    @Test
    void rejectionClearsPendingAndDoesNotChangeBalances() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-REJECT","message":"Transfer LKR 10 from my salary account to Varuni."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"));

        mvc.perform(post("/api/agent/conversations/CONV-REJECT/reject").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("REJECTED"));

        assertThat(transferRepository.count()).isZero();
        assertBalance("001020020001", "1000.00");
    }

    @Test
    void changedAmountInvalidatesPreviousPendingTransfer() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-CHANGE","message":"Transfer LKR 10 from my salary account to Varuni."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.transfer.amount").value(10.00));

        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-CHANGE","message":"Change it to LKR 20."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"))
                .andExpect(jsonPath("$.transfer.amount").value(20.00));
    }

    @Test
    void duplicateUuidDebitsOnlyOnce() throws Exception {
        String body = """
                {
                  "account_number": "001020020974",
                  "amount": 10,
                  "initiator_mobile_no": "",
                  "narration": "",
                  "receiver_bank": "6995",
                  "receiver_branch": null,
                  "receiver_name": "Varuni",
                  "uuid": "fixed-duplicate-uuid",
                  "version": "V2"
                }
                """;
        mvc.perform(post("/api/savings/transfer").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/api/savings/transfer").header("Authorization", AUTH).contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        assertThat(transferRepository.count()).isEqualTo(1);
        assertBalance("001020020001", "990.00");
    }

    @Test
    void multiTurnReferenceAndNonTransferQuestionBehaveAsExpected() throws Exception {
        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-MULTI","message":"What is the balance of my salary account?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolTrace[0].tool").value("sourceAccountBalanceInquiry"));

        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-MULTI","message":"Transfer LKR 10 from that account to Varuni."}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("AWAITING_CONFIRMATION"));

        mvc.perform(post("/api/agent/chat")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"conversationId":"CONV-NONTRANSFER","message":"What does available balance mean?"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.toolTrace").isEmpty());
    }

    @Test
    void postmanContractsAreExposed() throws Exception {
        mvc.perform(get("/api/savings/savings/balance").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accounts[0].accountAlias").exists());

        mvc.perform(get("/api/savings/beneficiary/get/DFP").header("Authorization", AUTH))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.beneficiaries[0].beneficiaryName").value("Varuni"));

        mvc.perform(post("/api/savings/transfers/validate/amount")
                        .header("Authorization", AUTH)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"amount":"100.00","account_number":"001020020974","receiver_bank_code":"6995","receiver_bank_name":"Dialog Finance PLC"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.valid").value(false))
                .andExpect(jsonPath("$.violations[0].code").value("TRANSACTION_LIMIT_EXCEEDED"));
    }

    private void assertBalance(String accountNumber, String expected) {
        SavingsAccount account = accountRepository.findById(accountNumber).orElseThrow();
        assertThat(account.getAvailableBalance()).isEqualByComparingTo(new BigDecimal(expected));
    }
}
