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

LOAD DATA LOCAL INFILE 'C:\Users\giahu\Documents\Workspace\CSDS-341-Final-Project\dataset\dataset\department.csv'
INTO TABLE Department
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(deptID, deptName);

LOAD DATA LOCAL INFILE 'C:/full/path/to/project/data/centers.csv'
INTO TABLE Center
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(centerID, centerName, country, budget);

LOAD DATA LOCAL INFILE 'C:\Users\giahu\Documents\Workspace\CSDS-341-Final-Project\dataset\dataset\center.csv'
INTO TABLE Employee
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(employeeID, firstName, lastName, gender, startDate, annualSalary, centerID, deptID);

LOAD DATA LOCAL INFILE 'C:\Users\giahu\Documents\Workspace\CSDS-341-Final-Project\dataset\dataset\supervisor.csv'
INTO TABLE Supervisor
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(employeeID, title);

LOAD DATA LOCAL INFILE 'C:\Users\giahu\Documents\Workspace\CSDS-341-Final-Project\dataset\dataset\project.csv'
INTO TABLE Project
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(projectID, projectName, startDate, deadline, active, supervisorID);

LOAD DATA LOCAL INFILE 'C:\Users\giahu\Documents\Workspace\CSDS-341-Final-Project\dataset\dataset\task.csv'
INTO TABLE Task
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(taskID, taskName, status, deadline, projectID);

LOAD DATA LOCAL INFILE 'C:\Users\giahu\Documents\Workspace\CSDS-341-Final-Project\dataset\dataset\assignedTo.csv'
INTO TABLE AssignedTo
FIELDS TERMINATED BY ','
ENCLOSED BY '"'
LINES TERMINATED BY '\n'
IGNORE 1 ROWS
(employeeID, projectID, role, hours);

SELECT COUNT(*) AS dept_count FROM Department;
SELECT COUNT(*) AS center_count FROM Center;
SELECT COUNT(*) AS employee_count FROM Employee;
SELECT COUNT(*) AS supervisor_count FROM Supervisor;
SELECT COUNT(*) AS project_count FROM Project;
SELECT COUNT(*) AS task_count FROM Task;
SELECT COUNT(*) AS assignedto_count FROM AssignedTo;