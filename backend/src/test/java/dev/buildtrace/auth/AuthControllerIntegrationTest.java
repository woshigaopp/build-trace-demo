package dev.buildtrace.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:h2:mem:auth-controller-test;MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE",
    "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void registersLogsInAndProtectsAccountProjects() throws Exception {
        String emailA = "a-" + UUID.randomUUID() + "@example.com";
        String emailB = "b-" + UUID.randomUUID() + "@example.com";
        String tokenA = register(emailA);
        String tokenB = register(emailB);

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email").value(emailA));

        String projectResponse = mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + tokenA)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"Private app\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(projectResponse).path("id").asText();

        mockMvc.perform(get("/api/projects/{id}", projectId)
                .header("Authorization", "Bearer " + tokenB))
            .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/projects/{id}", projectId)
                .header("Authorization", "Bearer " + tokenA))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Private app"));

        mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(emailA, "wrong-password")))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.message").value("邮箱或密码错误"));

        mockMvc.perform(get("/api/projects"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void authenticatedSseGenerationCompletesAcrossAsyncSecurityDispatch() throws Exception {
        String token = register("sse-" + UUID.randomUUID() + "@example.com");
        String projectResponse = mockMvc.perform(post("/api/projects")
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\":\"SSE app\"}"))
            .andExpect(status().isOk())
            .andReturn().getResponse().getContentAsString();
        String projectId = objectMapper.readTree(projectResponse).path("id").asText();

        MvcResult stream = mockMvc.perform(post("/api/projects/{id}/generate", projectId)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .accept(MediaType.TEXT_EVENT_STREAM)
                .content("{\"prompt\":\"build a todo list\"}"))
            .andExpect(request().asyncStarted())
            .andReturn();

        mockMvc.perform(asyncDispatch(stream))
            .andExpect(status().isOk())
            .andExpect(content().string(org.hamcrest.Matchers.containsString("event:completed")));
    }

    private String register(String email) throws Exception {
        String response = mockMvc.perform(post("/api/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(credentials(email, "password-123")))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.user.email").value(email))
            .andReturn().getResponse().getContentAsString();
        JsonNode payload = objectMapper.readTree(response);
        return payload.path("token").asText();
    }

    private String credentials(String email, String password) throws Exception {
        return objectMapper.writeValueAsString(new AuthController.Credentials(email, password));
    }
}
