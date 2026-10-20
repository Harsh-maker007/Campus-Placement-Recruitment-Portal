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
