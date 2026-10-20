package com.portal.dao;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Map;

@Repository
public class PortalDAO {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    public void addStudent(String name, String email, double gpa, String skills) {
        jdbcTemplate.update("INSERT INTO students (name, email, gpa, skills) VALUES (?, ?, ?, ?)", name, email, gpa, skills);
    }

    public List<Map<String, Object>> getAllStudents() {
        return jdbcTemplate.queryForList("SELECT * FROM students");
    }

    public void addJob(String companyName, String title, double minGpa, String requiredSkills) {
        jdbcTemplate.update("INSERT INTO jobs (company_name, title, min_gpa, required_skills) VALUES (?, ?, ?, ?)", companyName, title, minGpa, requiredSkills);
    }

    public List<Map<String, Object>> getAllJobs() {
        return jdbcTemplate.queryForList("SELECT * FROM jobs");
    }
}
