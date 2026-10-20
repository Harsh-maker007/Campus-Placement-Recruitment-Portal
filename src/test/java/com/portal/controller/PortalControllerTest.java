package com.portal.controller;

import com.portal.dao.PortalDAO;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@ExtendWith(MockitoExtension.class)
class PortalControllerTest {

    @Mock
    private PortalDAO portalDAO;

    @InjectMocks
    private PortalController portalController;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(portalController).build();
    }

    @Test
    void indexAddsStudentsAndJobsToModel() throws Exception {
        List<Map<String, Object>> students = List.of(Map.of("name", "Ada"));
        List<Map<String, Object>> jobs = List.of(Map.of("title", "Engineer"));
        when(portalDAO.getAllStudents()).thenReturn(students);
        when(portalDAO.getAllJobs()).thenReturn(jobs);

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("students", students))
                .andExpect(model().attribute("jobs", jobs));

        verify(portalDAO).getAllStudents();
        verify(portalDAO).getAllJobs();
    }

    @Test
    void addStudentStoresSubmissionAndRedirects() throws Exception {
        mockMvc.perform(post("/add-student")
                        .param("name", "Ada Lovelace")
                        .param("email", "ada@example.com")
                        .param("gpa", "3.8")
                        .param("skills", "Java,SQL"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));

        verify(portalDAO).addStudent("Ada Lovelace", "ada@example.com", 3.8, "Java,SQL");
    }

    @Test
    void addJobStoresSubmissionAndRedirects() throws Exception {
        mockMvc.perform(post("/add-job")
                        .param("companyName", "Analytical Engines")
                        .param("title", "Engineer")
                        .param("minGpa", "3.5")
                        .param("requiredSkills", "Java"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));

        verify(portalDAO).addJob("Analytical Engines", "Engineer", 3.5, "Java");
    }

    @Test
    void addStudentRejectsMissingRequiredParameter() throws Exception {
        mockMvc.perform(post("/add-student")
                        .param("name", "Ada Lovelace")
                        .param("email", "ada@example.com")
                        .param("gpa", "3.8"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(portalDAO);
    }

    @Test
    void addStudentRejectsInvalidEmailAndGpa() throws Exception {
        mockMvc.perform(post("/add-student")
                        .param("name", "Ada Lovelace")
                        .param("email", "not-an-email")
                        .param("gpa", "11")
                        .param("skills", "Java"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(portalDAO);
    }

    @Test
    void addJobRejectsBlankCompanyAndOutOfRangeGpa() throws Exception {
        mockMvc.perform(post("/add-job")
                        .param("companyName", "   ")
                        .param("title", "Engineer")
                        .param("minGpa", "-1")
                        .param("requiredSkills", "Java"))
                .andExpect(status().isBadRequest());

        verifyNoInteractions(portalDAO);
    }
}