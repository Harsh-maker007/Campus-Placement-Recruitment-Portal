package com.portal;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:h2:mem:portal-smoke;DB_CLOSE_DELAY=-1;MODE=MySQL",
        "spring.sql.init.mode=always"
})
class PortalApplicationSmokeTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void dashboardStartsWithApplicationSchemaAndRenders() throws Exception {
        MvcResult result = mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andReturn();

        assertTrue(result.getResponse().getContentType().startsWith(MediaType.TEXT_HTML_VALUE));
        assertTrue(result.getResponse().getContentAsString().contains("Application pipeline"));
        assertTrue(result.getResponse().getContentAsString().contains("Recommended matches"));
    }

        @Test
        void candidateCanMoveFromProfileToOfferAndCsvExport() throws Exception {
        mockMvc.perform(post("/add-student")
                .param("name", "Ada Lovelace")
                .param("email", "ada@example.com")
                .param("gpa", "8.8")
                .param("skills", "Java, SQL"))
            .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/add-job")
                .param("companyName", "Analytical Engines")
                .param("title", "Java Engineer")
                .param("minGpa", "8.0")
                .param("requiredSkills", "Java,SQL"))
            .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/apply").param("studentId", "1").param("jobId", "1"))
            .andExpect(status().is3xxRedirection());
        mockMvc.perform(post("/update-application")
                .param("applicationId", "1")
                .param("status", "OFFER"))
            .andExpect(status().is3xxRedirection());

        MvcResult dashboard = mockMvc.perform(get("/"))
            .andExpect(status().isOk())
            .andReturn();
        String page = dashboard.getResponse().getContentAsString();
        assertTrue(page.contains("Ada Lovelace"));
        assertTrue(page.contains("Java Engineer"));
        assertTrue(page.contains("OFFER"));

        MvcResult export = mockMvc.perform(get("/export").param("type", "applications"))
            .andExpect(status().isOk())
            .andReturn();
        assertTrue(export.getResponse().getContentAsString().contains("Analytical Engines"));
        assertTrue(export.getResponse().getContentAsString().contains("OFFER"));
        }
}