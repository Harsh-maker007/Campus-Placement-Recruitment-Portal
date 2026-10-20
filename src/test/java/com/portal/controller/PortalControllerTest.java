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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
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
        List<Map<String, Object>> students = List.of(
                Map.of("name", "Ada", "email", "ada@example.com", "gpa", 8.5, "skills", "Java,SQL"),
                Map.of("name", "Bob", "email", "bob@example.com", "gpa", 7.2, "skills", "React,HTML")
        );
        List<Map<String, Object>> jobs = List.of(
                Map.of("company_name", "Acme", "title", "Java Engineer", "min_gpa", 8.0, "required_skills", "Java,SQL"),
                Map.of("company_name", "Beta", "title", "Frontend Engineer", "min_gpa", 6.5, "required_skills", "React,HTML")
        );
        when(portalDAO.getAllStudents()).thenReturn(students);
        when(portalDAO.getAllJobs()).thenReturn(jobs);
        when(portalDAO.getAllApplications()).thenReturn(List.of());

        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"))
                .andExpect(model().attribute("students", students))
                .andExpect(model().attribute("jobs", jobs))
                .andExpect(model().attributeExists("stats"))
                .andExpect(model().attributeExists("recommendations"))
                .andExpect(model().attribute("applications", List.of()));

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

    @Test
    void applyCreatesApplicationWhenStudentAndJobExist() throws Exception {
        when(portalDAO.studentExists(4)).thenReturn(true);
        when(portalDAO.jobExists(9)).thenReturn(true);
        when(portalDAO.applicationExists(4, 9)).thenReturn(false);

        mockMvc.perform(post("/apply").param("studentId", "4").param("jobId", "9"))
                .andExpect(status().is3xxRedirection())
                .andExpect(view().name("redirect:/"));

        verify(portalDAO).addApplication(4, 9);
    }

    @Test
    void applyRejectsDuplicateApplication() throws Exception {
        when(portalDAO.studentExists(4)).thenReturn(true);
        when(portalDAO.jobExists(9)).thenReturn(true);
        when(portalDAO.applicationExists(4, 9)).thenReturn(true);

        mockMvc.perform(post("/apply").param("studentId", "4").param("jobId", "9"))
                .andExpect(status().isConflict());
    }

    @Test
    void updateApplicationAcceptsOnlyKnownPipelineStages() throws Exception {
        mockMvc.perform(post("/update-application")
                        .param("applicationId", "3")
                        .param("status", "interview"))
                .andExpect(status().is3xxRedirection());

        verify(portalDAO).updateApplicationStatus(3, "INTERVIEW");

        mockMvc.perform(post("/update-application")
                        .param("applicationId", "3")
                        .param("status", "hired"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void exportReturnsCsvWithEscapedValuesAndDownloadHeader() throws Exception {
        when(portalDAO.getAllStudents()).thenReturn(List.of(
                Map.of("id", 1, "name", "Ada, Lovelace", "email", "ada@example.com", "gpa", 8.5, "skills", "Java")));

        mockMvc.perform(get("/export").param("type", "students"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition", "attachment; filename=placement-students.csv"))
                .andExpect(content().string("\"id\",\"name\",\"email\",\"gpa\",\"skills\"\r\n"
                        + "\"1\",\"Ada, Lovelace\",\"ada@example.com\",\"8.5\",\"Java\""));
    }

    @Test
    void exportRejectsUnknownDataType() throws Exception {
        mockMvc.perform(get("/export").param("type", "secrets"))
                .andExpect(status().isBadRequest());
    }
}