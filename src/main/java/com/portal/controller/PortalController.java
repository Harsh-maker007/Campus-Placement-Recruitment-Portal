package com.portal.controller;

import com.portal.dao.PortalDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.nio.charset.StandardCharsets;
import java.util.stream.Collectors;

@Controller
public class PortalController {

    private static final Set<String> APPLICATION_STATUSES = Set.of("APPLIED", "SCREENING", "INTERVIEW", "OFFER", "REJECTED");

    @Autowired
    private PortalDAO portalDAO;

    @GetMapping("/")
    public String index(@RequestParam(defaultValue = "") String studentSearch,
            @RequestParam(defaultValue = "") String jobSearch, Model model) {
        List<Map<String, Object>> allStudents = portalDAO.getAllStudents();
        List<Map<String, Object>> allJobs = portalDAO.getAllJobs();
        List<Map<String, Object>> applications = portalDAO.getAllApplications();
        List<Map<String, Object>> students = filterRows(allStudents, studentSearch, "name", "email", "skills");
        List<Map<String, Object>> jobs = filterRows(allJobs, jobSearch, "company_name", "title", "required_skills");

        model.addAttribute("students", students);
        model.addAttribute("jobs", jobs);
        model.addAttribute("applications", applications);
        model.addAttribute("applicationStatuses", APPLICATION_STATUSES);
        model.addAttribute("studentSearch", studentSearch);
        model.addAttribute("jobSearch", jobSearch);
        model.addAttribute("stats", buildStats(allStudents, allJobs, applications));
        model.addAttribute("recommendations", buildRecommendations(allStudents, allJobs, applications));
        return "index";
    }

    @PostMapping("/add-student")
    public String addStudent(@RequestParam String name, @RequestParam String email, @RequestParam double gpa, @RequestParam String skills) {
        if (isBlank(name) || isBlank(skills) || !isValidEmail(email) || !isValidGpa(gpa)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid student details");
        }
        portalDAO.addStudent(name, email, gpa, skills);
        return "redirect:/";
    }

    @PostMapping("/add-job")
    public String addJob(@RequestParam String companyName, @RequestParam String title, @RequestParam double minGpa, @RequestParam String requiredSkills) {
        if (isBlank(companyName) || isBlank(title) || isBlank(requiredSkills) || !isValidGpa(minGpa)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid job details");
        }
        portalDAO.addJob(companyName, title, minGpa, requiredSkills);
        return "redirect:/";
    }

    @PostMapping("/apply")
    public String apply(@RequestParam int studentId, @RequestParam int jobId) {
        if (!portalDAO.studentExists(studentId) || !portalDAO.jobExists(jobId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Student or job posting was not found");
        }
        if (portalDAO.applicationExists(studentId, jobId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "This student has already applied to this role");
        }
        portalDAO.addApplication(studentId, jobId);
        return "redirect:/";
    }

    @PostMapping("/update-application")
    public String updateApplication(@RequestParam int applicationId, @RequestParam String status) {
        String normalizedStatus = status.toUpperCase(Locale.ROOT);
        if (!APPLICATION_STATUSES.contains(normalizedStatus)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid application status");
        }
        portalDAO.updateApplicationStatus(applicationId, normalizedStatus);
        return "redirect:/";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> export(@RequestParam String type) {
        List<String> lines = switch (type) {
            case "students" -> exportRows(portalDAO.getAllStudents(), List.of("id", "name", "email", "gpa", "skills"));
            case "jobs" -> exportRows(portalDAO.getAllJobs(), List.of("id", "company_name", "title", "min_gpa", "required_skills"));
            case "applications" -> exportRows(portalDAO.getAllApplications(),
                    List.of("id", "student_name", "student_email", "company_name", "title", "status", "applied_at"));
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown export type");
        };
        byte[] content = String.join("\r\n", lines).getBytes(StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=placement-" + type + ".csv")
                .contentType(MediaType.parseMediaType("text/csv;charset=UTF-8"))
                .body(content);
    }

    private Map<String, Object> buildStats(List<Map<String, Object>> students, List<Map<String, Object>> jobs,
            List<Map<String, Object>> applications) {
        Map<String, Object> stats = new LinkedHashMap<>();

        double avgGpa = students.stream()
                .mapToDouble(student -> toDouble(student, "gpa", "GPA"))
                .average()
                .orElse(0.0);

        long eligibleStudents = students.stream()
                .filter(student -> studentMatchesAnyJob(student, jobs))
                .count();

        Set<String> skillSet = jobs.stream()
                .flatMap(job -> skillTokens(String.valueOf(value(job, "required_skills", "REQUIRED_SKILLS"))).stream())
                .collect(Collectors.toSet());

        stats.put("totalStudents", students.size());
        stats.put("totalJobs", jobs.size());
        stats.put("avgGpa", Math.round(avgGpa * 100.0) / 100.0);
        stats.put("eligibleStudents", eligibleStudents);
        stats.put("totalApplications", applications.size());
        stats.put("offers", applications.stream().filter(application -> "OFFER".equalsIgnoreCase(text(application, "status"))).count());
        stats.put("topSkills", String.join(", ", skillSet.stream().sorted().limit(5).toList()));
        return stats;
    }

    private List<Map<String, Object>> buildRecommendations(List<Map<String, Object>> students, List<Map<String, Object>> jobs,
            List<Map<String, Object>> applications) {
        List<Map<String, Object>> recommendations = new ArrayList<>();

        for (Map<String, Object> student : students) {
            String studentName = String.valueOf(value(student, "name", "NAME"));
                int studentId = toInt(student, "id", "ID");
            double studentGpa = toDouble(student, "gpa", "GPA");
            Set<String> studentSkills = skillTokens(String.valueOf(value(student, "skills", "SKILLS")));
                Set<Integer> appliedJobIds = applications.stream()
                    .filter(application -> toInt(application, "student_id", "STUDENT_ID") == studentId)
                    .map(application -> toInt(application, "job_id", "JOB_ID"))
                    .collect(Collectors.toSet());

            List<Map<String, Object>> matches = new ArrayList<>();
            for (Map<String, Object> job : jobs) {
                String companyName = String.valueOf(value(job, "company_name", "COMPANY_NAME"));
                String title = String.valueOf(value(job, "title", "TITLE"));
                int jobId = toInt(job, "id", "ID");
                double minGpa = toDouble(job, "min_gpa", "MIN_GPA");
                Set<String> requiredSkills = skillTokens(String.valueOf(value(job, "required_skills", "REQUIRED_SKILLS")));

                int overlap = (int) studentSkills.stream().filter(requiredSkills::contains).count();
                int skillScore = requiredSkills.isEmpty() ? 0 : (int) Math.round(overlap * 60.0 / requiredSkills.size());
                int matchScore = (studentGpa >= minGpa ? 40 : 0) + skillScore;

                if (matchScore > 0) {
                    Map<String, Object> match = new LinkedHashMap<>();
                    match.put("jobId", jobId);
                    match.put("companyName", companyName);
                    match.put("title", title);
                    match.put("score", matchScore);
                    match.put("applied", appliedJobIds.contains(jobId));
                    matches.add(match);
                }
            }

            matches.sort(Comparator.comparingInt(m -> -(Integer) m.get("score")));
            List<String> topTitles = matches.stream().limit(3).map(item -> String.valueOf(item.get("companyName")) + " - " + item.get("title")).toList();

            Map<String, Object> recommendation = new LinkedHashMap<>();
            recommendation.put("studentName", studentName);
            recommendation.put("studentId", studentId);
            recommendation.put("studentGpa", Math.round(studentGpa * 100.0) / 100.0);
            recommendation.put("openingCount", matches.size());
            recommendation.put("matches", matches);
            recommendation.put("topTitles", topTitles);
            recommendations.add(recommendation);
        }

        return recommendations;
    }

    private boolean studentMatchesAnyJob(Map<String, Object> student, List<Map<String, Object>> jobs) {
        double studentGpa = toDouble(student, "gpa", "GPA");
        Set<String> studentSkills = skillTokens(String.valueOf(value(student, "skills", "SKILLS")));

        for (Map<String, Object> job : jobs) {
            double minGpa = toDouble(job, "min_gpa", "MIN_GPA");
            Set<String> requiredSkills = skillTokens(String.valueOf(value(job, "required_skills", "REQUIRED_SKILLS")));
            if (studentGpa >= minGpa && !studentSkills.stream().filter(requiredSkills::contains).findAny().isEmpty()) {
                return true;
            }
        }
        return false;
    }

    private List<Map<String, Object>> filterRows(List<Map<String, Object>> rows, String query, String... fields) {
        if (isBlank(query)) {
            return rows;
        }
        String normalizedQuery = query.trim().toLowerCase(Locale.ROOT);
        return rows.stream().filter(row -> java.util.Arrays.stream(fields)
                .map(field -> text(row, field))
                .anyMatch(value -> value.toLowerCase(Locale.ROOT).contains(normalizedQuery)))
                .toList();
    }

    private List<String> exportRows(List<Map<String, Object>> rows, List<String> fields) {
        List<String> lines = new ArrayList<>();
        lines.add(fields.stream().map(this::csvEscape).collect(Collectors.joining(",")));
        for (Map<String, Object> row : rows) {
            lines.add(fields.stream().map(field -> csvEscape(text(row, field))).collect(Collectors.joining(",")));
        }
        return lines;
    }

    private String csvEscape(String value) {
        String escaped = value.replace("\"", "\"\"");
        return "\"" + escaped + "\"";
    }

    private String text(Map<String, Object> row, String key) {
        return String.valueOf(value(row, key, key.toUpperCase(Locale.ROOT)));
    }

    private int toInt(Map<String, Object> row, String... keys) {
        Object raw = value(row, keys);
        if (raw instanceof Number number) {
            return number.intValue();
        }
        try {
            return Integer.parseInt(String.valueOf(raw));
        } catch (NumberFormatException ex) {
            return -1;
        }
    }

    private Object value(Map<String, Object> row, String... keys) {
        for (String key : keys) {
            if (row.containsKey(key)) {
                return row.get(key);
            }
        }
        return "";
    }

    private double toDouble(Map<String, Object> row, String... keys) {
        Object raw = value(row, keys);
        if (raw == null) {
            return 0.0;
        }
        if (raw instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(raw.toString());
        } catch (NumberFormatException ex) {
            return 0.0;
        }
    }

    private Set<String> skillTokens(String rawSkills) {
        if (isBlank(rawSkills)) {
            return Set.of();
        }
        return java.util.Arrays.stream(rawSkills.split(","))
                .map(part -> part.trim())
                .filter(part -> !part.isEmpty())
                .map(part -> part.toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private boolean isValidEmail(String email) {
        return !isBlank(email) && email.matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    }

    private boolean isValidGpa(double gpa) {
        return Double.isFinite(gpa) && gpa >= 0.0 && gpa <= 10.0;
    }
}
