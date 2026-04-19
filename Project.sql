SELECT DATABASE();
DROP TABLE IF EXISTS AssignedTo;
DROP TABLE IF EXISTS Task;
DROP TABLE IF EXISTS Project;
DROP TABLE IF EXISTS Supervisor;
DROP TABLE IF EXISTS Employee;
DROP TABLE IF EXISTS Center;
DROP TABLE IF EXISTS Department;


CREATE TABLE Department (
    departmentID INT PRIMARY KEY,
    departmentName VARCHAR(100) NOT NULL
);

CREATE TABLE Center (
    centerID INT PRIMARY KEY,
    centerName VARCHAR(100) NOT NULL,
    country VARCHAR(100),
    budget DECIMAL(12,2)
);

CREATE TABLE Employee (
    employeeID INT PRIMARY KEY,
    firstName VARCHAR(100) NOT NULL,
    lastName VARCHAR(100) NOT NULL,
    gender VARCHAR(20),
    startDate DATE,
    annualSalary DECIMAL(12,2),
    centerID INT,
    departmentID INT,
    FOREIGN KEY (centerID) REFERENCES Center(centerID),
    FOREIGN KEY (departmentID) REFERENCES Department(departmentID)
);

CREATE TABLE Supervisor (
    supervisorID INT PRIMARY KEY,
    title VARCHAR(100),
    FOREIGN KEY (supervisorID) REFERENCES Employee(employeeID)
);

CREATE TABLE Project (
    projectID INT PRIMARY KEY,
    projectName VARCHAR(150) NOT NULL,
    startDate DATE,
    deadline DATE,
    active BOOLEAN,
    supervisorID INT,
    FOREIGN KEY (supervisorID) REFERENCES Supervisor(supervisorID)
);

CREATE TABLE Task (
    taskID INT,
    taskName VARCHAR(150) NOT NULL,
    status VARCHAR(50),
    deadline DATE,
    projectID INT,
    PRIMARY KEY (projectID, taskID),
    FOREIGN KEY (projectID) REFERENCES Project(projectID)
);

CREATE TABLE AssignedTo (
    employeeID INT,
    projectID INT,
    role VARCHAR(100),
    hours INT,
    PRIMARY KEY (employeeID, projectID),
    FOREIGN KEY (employeeID) REFERENCES Employee(employeeID),
    FOREIGN KEY (projectID) REFERENCES Project(projectID)
);

SHOW TABLES;