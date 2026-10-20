# Campus Placement & Recruitment Portal

A Spring Boot, Thymeleaf, JDBC, and H2 application for managing campus recruiting from student discovery through offer tracking.

## Features

- Student profiles with GPA and comma-separated skills.
- Recruiter role postings with minimum GPA and required skills.
- Ranked student-to-role recommendations based on GPA eligibility and skill overlap.
- Application creation from a recommendation, with duplicate applications prevented per student and role.
- Application pipeline stages: Applied, Screening, Interview, Offer, and Rejected.
- Dashboard metrics for students, open roles, average GPA, match-ready students, applications, and offers.
- Searchable student and job directories.
- CSV exports for students, roles, and applications.
- H2 schema initialization and a responsive dashboard layout.

## Run Locally

Requires Java 17 or newer and Maven.

```bash
mvn spring-boot:run
```

Open `http://localhost:8080`.

Run the tests with:

```bash
mvn test
```

The H2 console is available at `/h2-console` when the application is running. The default development database is configured in `src/main/resources/application.properties`.
