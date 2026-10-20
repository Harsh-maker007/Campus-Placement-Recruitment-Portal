package com.portal.dao;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PortalDAOTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private PortalDAO portalDAO;

    @Test
    void addStudentExecutesInsertWithProvidedValues() {
        portalDAO.addStudent("Ada Lovelace", "ada@example.com", 3.8, "Java,SQL");

        verify(jdbcTemplate).update(
                "INSERT INTO students (name, email, gpa, skills) VALUES (?, ?, ?, ?)",
                "Ada Lovelace", "ada@example.com", 3.8, "Java,SQL");
    }

    @Test
    void getAllStudentsReturnsQueryResults() {
        List<Map<String, Object>> students = List.of(Map.of("name", "Ada Lovelace"));
        when(jdbcTemplate.queryForList("SELECT * FROM students")).thenReturn(students);

        assertSame(students, portalDAO.getAllStudents());
    }

    @Test
    void addJobExecutesInsertWithProvidedValues() {
        portalDAO.addJob("Analytical Engines", "Engineer", 3.5, "Java");

        verify(jdbcTemplate).update(
                "INSERT INTO jobs (company_name, title, min_gpa, required_skills) VALUES (?, ?, ?, ?)",
                "Analytical Engines", "Engineer", 3.5, "Java");
    }

    @Test
    void getAllJobsReturnsQueryResults() {
        List<Map<String, Object>> jobs = List.of(Map.of("title", "Engineer"));
        when(jdbcTemplate.queryForList("SELECT * FROM jobs")).thenReturn(jobs);

        assertSame(jobs, portalDAO.getAllJobs());
    }
}