package com.portal.controller;

import com.portal.dao.PortalDAO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Controller
public class PortalController {

    @Autowired
    private PortalDAO portalDAO;

    @GetMapping("/")
    public String index(Model model) {
        model.addAttribute("students", portalDAO.getAllStudents());
        model.addAttribute("jobs", portalDAO.getAllJobs());
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
