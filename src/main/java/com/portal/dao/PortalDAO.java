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

    public List<Map<String, Object>> getAllApplications() {
        return jdbcTemplate.queryForList("""
                SELECT a.id, a.student_id, a.job_id, a.status, a.applied_at,
                       s.name AS student_name, s.email AS student_email,
                       j.company_name, j.title
                FROM applications a
                JOIN students s ON s.id = a.student_id
                JOIN jobs j ON j.id = a.job_id
                ORDER BY a.applied_at DESC, a.id DESC
                """);
    }

    public boolean studentExists(int studentId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM students WHERE id = ?)", Boolean.class, studentId));
    }

    public boolean jobExists(int jobId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM jobs WHERE id = ?)", Boolean.class, jobId));
    }

    public boolean applicationExists(int studentId, int jobId) {
        return Boolean.TRUE.equals(jdbcTemplate.queryForObject(
                "SELECT EXISTS(SELECT 1 FROM applications WHERE student_id = ? AND job_id = ?)",
                Boolean.class, studentId, jobId));
    }

    public void addApplication(int studentId, int jobId) {
        jdbcTemplate.update("INSERT INTO applications (student_id, job_id) VALUES (?, ?)", studentId, jobId);
    }

    public void updateApplicationStatus(int applicationId, String status) {
        jdbcTemplate.update("UPDATE applications SET status = ? WHERE id = ?", status, applicationId);
    }
}
