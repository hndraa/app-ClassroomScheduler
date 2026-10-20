# Rainbow Team – Classroom Scheduler

SOFTWARE | SCHEDULING SYSTEM | JAVA · MAVEN · CLI

A Java-based classroom scheduling application designed to manage course schedules, assign classrooms, and validate scheduling conflicts. The application supports static and dynamic scheduling, room-capacity validation, and CSV-based schedule storage through a command-line interface.

## 01. Project Overview

### Project Description

Rainbow Team – Classroom Scheduler is a command-line application developed to simplify classroom scheduling by organizing courses, lecturers, student groups, classrooms, and time slots.

### Objectives

- Manage course schedules across multiple classrooms and weekdays.
- Validate scheduling conflicts involving classrooms, lecturers, and student groups.
- Ensure student enrollment does not exceed classroom capacity.
- Support predefined schedules and user-defined scheduling data.
- Store and display schedules using CSV files.

### Problem Statement

Manual classroom scheduling can result in overlapping activities, room-capacity violations, and difficulties maintaining existing schedules when new activities are added.

### Solution

The application combines graph coloring and backtracking-based constraint search to support schedule generation and conflict validation. It provides static and dynamic scheduling modes, automatically generates activity IDs, validates classroom capacity, and preserves existing schedules when new entries fail validation.

## 02. Tech Stack

| Category | Technology |
|---|---|
| Programming Language | Java 21 |
| Build Tool | Apache Maven |
| User Interface | Command-Line Interface (CLI) |
| Data Storage | CSV |
| Testing Framework | JUnit 5 |
| Scheduling Algorithm | Graph Coloring and Backtracking Constraint Search |
| Development Environment | Visual Studio Code |

## 03. Screenshots / Results

This section presents the application's main workflow and output.

### 3.1 Main Menu

The main menu provides access to static scheduling, dynamic scheduling, saved schedule viewing, and application exit.

<img width="290" height="127" alt="image" src="https://github.com/user-attachments/assets/adf3c2d8-ec66-4967-8132-25d108861144" />

<img width="844" height="381" alt="image" src="https://github.com/user-attachments/assets/4b2ee47b-abfb-4a79-9ebe-0a2526053d9f" />



### 3.2 Dynamic Scheduling

Users select a weekday, choose available classrooms, and enter course details, including time slots, course names, lecturers, student groups, and student counts. Activity IDs are generated automatically.

<img width="545" height="545" alt="image" src="https://github.com/user-attachments/assets/527d3a8e-8b9e-41d2-9876-70230772c769" />


### 3.3 Room Capacity Validation

The application checks student enrollment against the selected classroom's capacity. If the number exceeds the limit, the application displays a warning and requests a valid student count.

<img width="569" height="171" alt="image" src="https://github.com/user-attachments/assets/11910b6f-02ec-4056-9549-da48370c266c" />


### 3.4 Scheduling Conflict Validation

The application detects overlapping activities involving the same classroom, lecturer, or student group. Invalid submissions are rejected, and the previously saved schedule remains unchanged.

`Classroom Conflict`
![Main Menu]<img width="247" height="207" alt="image" src="https://github.com/user-attachments/assets/7b163a1c-4ce6-422a-a93d-85d7af77f6fb" />

`Lecturer Conflict`

<img width="384" height="276" alt="image" src="https://github.com/user-attachments/assets/4d6bb53d-ac67-4b87-bb9a-5f52d7b7a789" />

`Student Group Conflict`

<img width="370" height="273" alt="image" src="https://github.com/user-attachments/assets/d0949721-b2ca-4d66-b179-d061b909feb0" />


### 3.5 Saved Schedule

Saved activities are displayed in a structured table, grouped by weekday and ordered by classroom followed by start time.

<img width="621" height="546" alt="image" src="https://github.com/user-attachments/assets/06e2bd29-ade4-4b66-a485-05f82c77c1c6" />


### 3.6 CSV Schedule Storage

The application stores shared schedule data in `data/schedule.csv`. Static mode also generates an export at `output/schedule-statis.csv`.

<img width="1118" height="183" alt="image" src="https://github.com/user-attachments/assets/a34e9436-6954-4703-93c4-e88859a5ebd9" />


## 04. Installation & Usage

### Prerequisites

Install the following software before running the application:

- Java Development Kit (JDK) 21
- Apache Maven
- Visual Studio Code or another Java-compatible IDE

Verify the installations:

```bash
java -version
mvn -version
```

### Project Structure

```text
rainbow-team-scheduler/
├── data/
│   └── schedule.csv
├── output/
│   └── schedule-statis.csv
├── src/
│   ├── main/
│   │   └── java/
│   │       └── com/
│   │           └── rainbowteam/
│   │               └── scheduler/
│   │                   ├── Main.java
│   │                   ├── Statis.java
│   │                   ├── Dinamis.java
│   │                   ├── ScheduleRunner.java
│   │                   ├── model/
│   │                   ├── graph/
│   │                   ├── service/
│   │                   └── io/
│   └── test/
│       └── java/
├── pom.xml
└── README.md
```

### Installation

**Step 1 — Clone the repository**

Replace the URL with your actual GitHub repository URL.

```bash
git clone [YOUR_GITHUB_REPOSITORY_URL]
```

**Step 2 — Navigate to the project directory**

```bash
cd rainbow-team-scheduler
```

**Step 3 — Build the project and run tests**

```bash
mvn clean test
```

### Running the Application

Start the application using Maven:

```bash
mvn exec:java
```

### Main Menu Options

| Option | Description |
|---|---|
| `1` | Run static scheduling using predefined sample data |
| `2` | Create a dynamic schedule using user-provided data |
| `3` | View saved schedules |
| `0` | Exit the application |

### Data Storage

- `data/schedule.csv` — stores the shared schedule data used by the application.
- `output/schedule-statis.csv` — stores the exported schedule generated by static mode.

Existing schedules are preserved when new activities fail validation. Successful additions are stored in the shared schedule file.

---
