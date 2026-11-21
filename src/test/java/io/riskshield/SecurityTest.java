package io.riskshield;

import io.riskshield.adapter.*;
import io.riskshield.application.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.simple.JdbcClient;

@WebMvcTest(controllers=RiskController.class,properties={"risk.users.engine=test-only-password-not-for-deployment","risk.users.analyst=test-only-password-not-for-deployment","risk.users.auditor=test-only-password-not-for-deployment","risk.users.admin-a=test-only-password-not-for-deployment","risk.users.admin-b=test-only-password-not-for-deployment"})
@Import({SecurityConfiguration.class,ProblemHandler.class})
class SecurityTest {
    @Autowired MockMvc mvc;
    @MockitoBean DecisionService decisions; @MockitoBean FeedbackService feedback; @MockitoBean ConfigurationService configs; @MockitoBean JdbcClient db;
    @MockitoBean EvaluationRunService runs;
    @Test void unauthenticatedIs401() throws Exception { mvc.perform(post("/risk/evaluations").contentType("application/json").content("{}")).andExpect(status().isUnauthorized()); }
    @Test void engineCannotManageRules() throws Exception { mvc.perform(post("/risk/admin/configurations").with(user("engine").roles("ENGINE")).contentType("application/json").content("{}")).andExpect(status().isForbidden()); }
    @Test void analystCannotIntakeAndAuditorCannotLabel() throws Exception {
        mvc.perform(post("/risk/evaluations").with(user("analyst").roles("ANALYST")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
        mvc.perform(post("/risk/feedback").with(user("auditor").roles("AUDITOR")).contentType("application/json").content("{}")).andExpect(status().isForbidden());
    }
    @Test void unknownPropertiesAndInvalidPayloadAre400() throws Exception {
        mvc.perform(post("/risk/evaluations").with(user("engine").roles("ENGINE")).contentType("application/json").content("{\"label\":\"FRAUD\"}")).andExpect(status().isBadRequest());
    }
    @Test void realBasicCredentialsUseConfiguredEncoder() throws Exception {
        mvc.perform(get("/risk/reviews").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("analyst","test-only-password-not-for-deployment"))).andExpect(status().isOk());
        mvc.perform(get("/risk/reviews").with(org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic("analyst","wrong-password"))).andExpect(status().isUnauthorized());
    }
    @Test void oversizedAuthenticatedRequestIs413() throws Exception {
        mvc.perform(post("/risk/evaluations").with(user("engine").roles("ENGINE")).contentType("application/json").content("x".repeat(32769))).andExpect(status().isPayloadTooLarge());
    }
}
