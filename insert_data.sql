USE company_db;

SET FOREIGN_KEY_CHECKS = 0;

DELETE FROM AssignedTo;
DELETE FROM Task;
DELETE FROM Project;
DELETE FROM Supervisor;
DELETE FROM Employee;
DELETE FROM Center;
DELETE FROM Department;

SET FOREIGN_KEY_CHECKS = 1;

LOAD DATA LOCAL INFILE 'put your path to the dataset (department.csv) in the project here'
INTO TABLE Department
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(departmentID, departmentName);

LOAD DATA LOCAL INFILE 'put your path to the dataset (center.csv) in the project here'
INTO TABLE Center
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(centerID, centerName, country, budget);

LOAD DATA LOCAL INFILE 'put your path to the dataset (employee.csv) in the project here'
INTO TABLE Employee
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(employeeID, firstName, lastName, gender, startDate, annualSalary, @departmentID, @centerID)
SET centerID = @centerID,
    departmentID = @departmentID;

LOAD DATA LOCAL INFILE 'put your path to the dataset (supervisor.csv) in the project here'
INTO TABLE Supervisor
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(supervisorID, title);

LOAD DATA LOCAL INFILE 'put your path to the dataset (project.csv) in the project here'
INTO TABLE Project
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(projectID, projectName, startDate, deadline, active, supervisorID);

LOAD DATA LOCAL INFILE 'put your path to the dataset (task.csv) in the project here'
INTO TABLE Task
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(projectID, taskID, taskName, status, deadline);

LOAD DATA LOCAL INFILE 'put your path to the dataset (assignedTo.csv) in the project here'
INTO TABLE AssignedTo
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(employeeID, projectID, hours, role);

SELECT COUNT(*) AS dept_count FROM Department;
SELECT COUNT(*) AS center_count FROM Center;
SELECT COUNT(*) AS employee_count FROM Employee;
SELECT COUNT(*) AS supervisor_count FROM Supervisor;
SELECT COUNT(*) AS project_count FROM Project;
SELECT COUNT(*) AS task_count FROM Task;
SELECT COUNT(*) AS assignedto_count FROM AssignedTo;
