# DayFlow — Personal Development & Coaching Platform (JavaFX Desktop Version)

## Overview
DayFlow is a desktop-based personal development and coaching platform developed using JavaFX.  
The application helps users set goals, build routines, track progress, and engage with communities through posts and chatrooms. It also includes a coaching workflow and a reclamation management system.

This project was developed as part of the **PIDEV Program** at Esprit School of Engineering during the Academic Year **2025–2026**.

---

# Features

## Authentication & User Management
- User registration and login
- Secure authentication system
- Role-based access:
  - User
  - Coach
  - Admin
- User profile management
- Profile picture support
- Account settings and editing

---

## Goals & Routines Module
- Create and manage personal goals
- Define routines linked to goals
- Track daily progress
- Goal completion statistics
- Community goals participation
- Productivity-focused workflow

---

## Community & Social Features

### Posts System
- Create and publish posts
- Rich text content support
- Scheduled publishing
- Tags and categorization
- Like and save functionality
- Comment system
- Edit and delete posts
- Community feed interface

### AI Content Moderation
- Toxicity analysis using Perspective API
- Detection of offensive or harmful language
- Automatic moderation warnings
- Moderation alerts and logs
- Safe community interactions

---

## Chatrooms & Messaging
- Goal-based public chatrooms
- Private conversations between users
- Real-time messaging system
- Community interaction features
- User communication management

---

## Coaching Module
- Send coaching requests
- Coach session scheduling
- Session management
- Coach-user interaction workflow
- Coaching status tracking

---

## Reclamation Module
- Submit reclamations/complaints
- Admin response system
- Reclamation tracking
- Moderation and management tools

---

## Admin Dashboard
- User management
- Platform statistics
- Moderation logs
- Community management tools
- Administrative controls

---

# Tech Stack

## Frontend
- JavaFX
- FXML
- CSS Styling
- Scene Builder
- Java Event Handling

---

## Backend & Core Technologies
- Java
- JDBC
- PostgreSQL / MySQL Database
- REST APIs
- HTTP Requests
- JSON Processing

---

## External APIs & Services
- Perspective API (toxicity moderation)
- Mail services / notifications
- Real-time communication services

---

# Architecture

The application follows a modular layered architecture:

```text
UI Layer (JavaFX / FXML)
        ↓
Controller Layer
        ↓
Service Layer
        ↓
DAO / Repository Layer
        ↓
Database
```

---

# Main Modules

## User Module
Handles:
- Authentication
- Authorization
- Profiles
- User roles

---

## Goals & Routines Module
Handles:
- Goal lifecycle
- Routine tracking
- Progress management

---

## Posts Module
Handles:
- Posts
- Comments
- Likes
- Saved posts
- Moderation system

---

## Chatroom Module
Handles:
- Public chatrooms
- Private messaging
- Community interactions

---

## Coaching Module
Handles:
- Coaching requests
- Session management
- Coach interactions

---

## Reclamation Module
Handles:
- Complaint submission
- Admin processing
- User support workflow

---

## Admin Module
Handles:
- Statistics
- Moderation
- Platform administration

---

# Desktop Application Features

## JavaFX UI
- Modern desktop interface
- Responsive layouts
- Interactive dashboards
- Styled components using CSS
- Navigation between scenes

---

## Database Integration
- JDBC connectivity
- CRUD operations
- Entity management
- Persistent data storage

---

## Real-Time & Dynamic Features
- Dynamic post updates
- Notifications
- Interactive chat experience
- Live moderation feedback

---

# Project Structure

```text
src/
│
├── controllers/
├── entities/
├── services/
├── utils/
├── gui/
├── css/
├── resources/
└── main/
```

---

# Contributors
- Eya Hwess
- Eya Abdellaoui
- Roua Taboubi
- Shaima Barouni
- Ranym Zaghbib
- Mariem Ayari

---

# Academic Context

This project was developed at Esprit School of Engineering – Tunisia as part of:
- PIDEV — 3rd Year Engineering Program
- Academic Year 2025–2026

The objective of the project is to design and develop a complete multi-module software solution using modern development technologies and software engineering practices.

---

# Getting Started

## Prerequisites
Before running the application, make sure you have installed:
- Java JDK 17+ (recommended)
- JavaFX SDK
- IntelliJ IDEA / NetBeans / Eclipse
- Scene Builder
- PostgreSQL or MySQL
- Maven or Gradle (if used)

---

# Installation

## 1. Clone the Repository

```bash
git clone <repo-url>
cd DayFlow-JavaFX
```

---

## 2. Configure the Database

Create a database and update the database configuration inside the project.

Example:

```java
private static final String URL = "jdbc:mysql://localhost:3306/dayflow";
private static final String USER = "root";
private static final String PASSWORD = "";
```

---

## 3. Import the Project

Open the project in:
- IntelliJ IDEA
- NetBeans
- Eclipse

---

## 4. Configure JavaFX

Add JavaFX SDK libraries and VM options.

Example VM options:

```bash
--module-path "PATH_TO_FX" --add-modules javafx.controls,javafx.fxml
```

---

## 5. Install Dependencies

If using Maven:

```bash
mvn clean install
```

If using Gradle:

```bash
gradle build
```

---

# Running the Application

## Using IntelliJ IDEA
Run the main JavaFX application file:

```text
Main.java
```

---

## Using Maven

```bash
mvn javafx:run
```

---

# Database Migration

Execute the SQL script included in the project:

```text
dayflow.sql
```

This script creates:
- Tables
- Relationships
- Constraints
- Initial data

---

# AI Moderation Workflow

The application integrates the Perspective API to analyze:
- Posts
- Comments
- User-generated content

If toxic or offensive language is detected:
- The user receives warnings
- The content may be flagged
- Admin moderation logs are updated

---

# Security Features
- Password hashing
- Session management
- Role-based authorization
- Input validation
- Protection against invalid requests

---

# Future Improvements
- Mobile application version
- AI productivity assistant
- Video coaching sessions
- Advanced analytics dashboard
- Gamification system
- Achievement badges
- Smart recommendation system

---

# Screens & Modules

The desktop application includes:
- Authentication screens
- Dashboard interfaces
- Goal management pages
- Social feed system
- Chat interfaces
- Coaching management screens
- Admin panels

---

# Acknowledgments
Special thanks to:
- Esprit School of Engineering
- Supervising professors
- Academic staff of the PIDEV program
- Team members and contributors

---

# Repository

```text
# Projet-PI-2026
```

---

# License
This project was developed for academic and educational purposes as part of the PIDEV engineering curriculum.
