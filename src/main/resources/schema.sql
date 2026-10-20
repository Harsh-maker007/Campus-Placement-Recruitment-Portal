CREATE TABLE IF NOT EXISTS students (
    id INT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100),
    email VARCHAR(100),
    gpa DOUBLE,
    skills VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS jobs (
    id INT AUTO_INCREMENT PRIMARY KEY,
    company_name VARCHAR(100),
    title VARCHAR(100),
    min_gpa DOUBLE,
    required_skills VARCHAR(255)
);

CREATE TABLE IF NOT EXISTS applications (
    id INT AUTO_INCREMENT PRIMARY KEY,
    student_id INT NOT NULL,
    job_id INT NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'APPLIED',
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_student_job UNIQUE (student_id, job_id),
    CONSTRAINT fk_application_student FOREIGN KEY (student_id) REFERENCES students(id),
    CONSTRAINT fk_application_job FOREIGN KEY (job_id) REFERENCES jobs(id)
);
