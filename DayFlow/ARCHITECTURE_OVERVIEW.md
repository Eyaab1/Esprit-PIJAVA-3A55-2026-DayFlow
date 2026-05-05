# 🏗️ Coach Availability Calendar - Architecture Overview

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     DayFlow Application                          │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Presentation Layer (UI)                     │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │                                                           │   │
│  │  FindCoachController                                     │   │
│  │  ├─ "Voir disponibilités" button                        │   │
│  │  └─ openCoachCalendar(coach)                            │   │
│  │                                                           │   │
│  │  CalendarCoachController                                │   │
│  │  ├─ Monthly calendar view                               │   │
│  │  ├─ Time slot display                                   │   │
│  │  ├─ Slot selection                                      │   │
│  │  └─ Reservation handling                                │   │
│  │                                                           │   │
│  │  FXML Files:                                             │   │
│  │  ├─ calendar_coach.fxml                                 │   │
│  │  └─ google_calendar_sync.fxml                           │   │
│  │                                                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Business Logic Layer                        │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │                                                           │   │
│  │  DisponibiliteService                                   │   │
│  │  ├─ getDisponibilitesByCoach(coachId)                   │   │
│  │  ├─ getAvailableSlots(coachId)                          │   │
│  │  ├─ isSlotAvailable(coachId, date, time)               │   │
│  │  ├─ createDisponibilite(disponibilite)                 │   │
│  │  ├─ updateStatus(id, status)                           │   │
│  │  └─ deleteDisponibilite(id)                            │   │
│  │                                                           │   │
│  │  SessionService                                          │   │
│  │  ├─ reserverSession(...)                                │   │
│  │  └─ updateSessionStatus(...)                            │   │
│  │                                                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Data Access Layer (Repository)             │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │                                                           │   │
│  │  DisponibiliteRepository                                │   │
│  │  ├─ getDisponibilitesByCoach(coachId)                   │   │
│  │  ├─ getAvailableSlots(coachId)                          │   │
│  │  ├─ getAvailableSlotsByDate(coachId, date)             │   │
│  │  ├─ createDisponibilite(disponibilite)                 │   │
│  │  ├─ updateDisponibiliteStatus(id, status)              │   │
│  │  ├─ deleteDisponibilite(id)                            │   │
│  │  └─ isSlotAvailable(coachId, date, time)               │   │
│  │                                                           │   │
│  │  SessionRepository                                       │   │
│  │  ├─ createSession(session)                              │   │
│  │  └─ updateSession(session)                              │   │
│  │                                                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Data Model Layer                            │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │                                                           │   │
│  │  Disponibilite                                           │   │
│  │  ├─ id: int                                              │   │
│  │  ├─ coachId: int                                         │   │
│  │  ├─ date: LocalDate                                      │   │
│  │  ├─ heure_debut: LocalTime                              │   │
│  │  ├─ heure_fin: LocalTime                                │   │
│  │  ├─ statut: String                                       │   │
│  │  ├─ created_at: LocalDateTime                           │   │
│  │  └─ updated_at: LocalDateTime                           │   │
│  │                                                           │   │
│  │  Session                                                 │   │
│  │  ├─ id: int                                              │   │
│  │  ├─ coachId: int                                         │   │
│  │  ├─ userId: int                                          │   │
│  │  ├─ date: LocalDate                                      │   │
│  │  ├─ heure_debut: LocalTime                              │   │
│  │  ├─ heure_fin: LocalTime                                │   │
│  │  └─ statut: String                                       │   │
│  │                                                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                            ↓                                      │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │              Database Layer (PostgreSQL)                │   │
│  ├──────────────────────────────────────────────────────────┤   │
│  │                                                           │   │
│  │  disponibilite table                                     │   │
│  │  ├─ id (SERIAL PRIMARY KEY)                             │   │
│  │  ├─ coach_id (INT FK → user.id)                         │   │
│  │  ├─ date (DATE)                                          │   │
│  │  ├─ heure_debut (TIME)                                  │   │
│  │  ├─ heure_fin (TIME)                                    │   │
│  │  ├─ statut (VARCHAR)                                     │   │
│  │  ├─ created_at (TIMESTAMP)                              │   │
│  │  └─ updated_at (TIMESTAMP)                              │   │
│  │                                                           │   │
│  │  session table                                           │   │
│  │  ├─ id (INT PRIMARY KEY)                                │   │
│  │  ├─ coach_id (INT FK → user.id)                         │   │
│  │  ├─ user_id (INT FK → user.id)                          │   │
│  │  ├─ date (DATE)                                          │   │
│  │  ├─ heure_debut (TIME)                                  │   │
│  │  ├─ heure_fin (TIME)                                    │   │
│  │  └─ statut (VARCHAR)                                     │   │
│  │                                                           │   │
│  │  user table                                              │   │
│  │  ├─ id (INT PRIMARY KEY)                                │   │
│  │  ├─ firstName (VARCHAR)                                  │   │
│  │  ├─ lastName (VARCHAR)                                   │   │
│  │  └─ ... (other fields)                                   │   │
│  │                                                           │   │
│  └──────────────────────────────────────────────────────────┘   │
│                                                                   │
└─────────────────────────────────────────────────────────────────┘
```

---

## Data Flow Diagram

```
User Action: Click "Voir disponibilités"
        ↓
FindCoachController.openCoachCalendar(coach)
        ↓
Load calendar_coach.fxml
        ↓
CalendarCoachController.initialize()
        ↓
DisponibiliteService.getDisponibilitesByCoach(coachId)
        ↓
DisponibiliteRepository.getDisponibilitesByCoach(coachId)
        ↓
Database Query: SELECT * FROM disponibilite WHERE coach_id = ?
        ↓
Return List<Disponibilite>
        ↓
CalendarCoachController.displayCalendarDays()
        ↓
Display calendar with available slots (green)
        ↓
User clicks on time slot
        ↓
CalendarCoachController.selectTimeSlot(slot)
        ↓
User clicks "Réserver session"
        ↓
CalendarCoachController.performReservation()
        ↓
SessionService.reserverSession(...)
        ↓
SessionRepository.createSession(session)
        ↓
Database Insert: INSERT INTO session (...)
        ↓
DisponibiliteRepository.updateDisponibiliteStatus(id, 'reserve')
        ↓
Database Update: UPDATE disponibilite SET statut = 'reserve'
        ↓
Show confirmation message
        ↓
Refresh calendar display
```

---

## Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    FindCoachController                       │
│  (Displays list of coaches with "Voir disponibilités" btn)  │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Click button
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                  CalendarCoachController                     │
│  (Displays calendar with available time slots)              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Needs data
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                  DisponibiliteService                        │
│  (Business logic for availability management)               │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ Needs data
                         ↓
┌─────────────────────────────────────────────────────────────┐
│               DisponibiliteRepository                        │
│  (Database operations for disponibilite table)              │
└────────────────────────┬────────────────────────────────────┘
                         │
                         │ SQL queries
                         ↓
┌─────────────────────────────────────────────────────────────┐
│                   PostgreSQL Database                        │
│  (Stores disponibilite and session data)                    │
└─────────────────────────────────────────────────────────────┘
```

---

## Database Schema Diagram

```
┌──────────────────────────────────────────────────────────────┐
│                         user                                  │
├──────────────────────────────────────────────────────────────┤
│ id (PK)                                                       │
│ firstName                                                     │
│ lastName                                                      │
│ email                                                         │
│ ... (other fields)                                            │
└──────────────────────────────────────────────────────────────┘
                            ↑
                            │ FK
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
        ↓                                       ↓
┌──────────────────────────────────────┐  ┌──────────────────────────────────────┐
│         disponibilite                │  │           session                    │
├──────────────────────────────────────┤  ├──────────────────────────────────────┤
│ id (PK)                              │  │ id (PK)                              │
│ coach_id (FK → user.id)              │  │ coach_id (FK → user.id)              │
│ date                                 │  │ user_id (FK → user.id)               │
│ heure_debut                          │  │ date                                 │
│ heure_fin                            │  │ heure_debut                          │
│ statut (disponible/reserve/annulea)  │  │ heure_fin                            │
│ created_at                           │  │ statut (planifiee/completee/annulee) │
│ updated_at                           │  │ created_at                           │
│                                      │  │ updated_at                           │
│ Indexes:                             │  │                                      │
│ - idx_coach_id                       │  │ Indexes:                             │
│ - idx_date                           │  │ - idx_coach_id                       │
│ - idx_coach_date                     │  │ - idx_user_id                        │
│ - idx_statut                         │  │ - idx_date                           │
│ - idx_disponible_slots               │  │                                      │
│                                      │  │ Constraints:                         │
│ Constraints:                         │  │ - UNIQUE (coach_id, user_id, date)   │
│ - UNIQUE (coach_id, date, time)      │  │                                      │
│ - CHECK (heure_debut < heure_fin)    │  │                                      │
└──────────────────────────────────────┘  └──────────────────────────────────────┘
```

---

## Class Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    Disponibilite                             │
├─────────────────────────────────────────────────────────────┤
│ - id: int                                                    │
│ - coachId: int                                               │
│ - date: LocalDate                                            │
│ - heure_debut: LocalTime                                     │
│ - heure_fin: LocalTime                                       │
│ - statut: String                                             │
│ - created_at: LocalDateTime                                  │
│ - updated_at: LocalDateTime                                  │
├─────────────────────────────────────────────────────────────┤
│ + getId(): int                                               │
│ + getCoachId(): int                                          │
│ + getDate(): LocalDate                                       │
│ + getHeure_debut(): LocalTime                                │
│ + getHeure_fin(): LocalTime                                  │
│ + getStatut(): String                                        │
│ + setStatut(String): void                                    │
│ + ... (other getters/setters)                                │
└─────────────────────────────────────────────────────────────┘
                            ↑
                            │ uses
                            │
        ┌───────────────────┴───────────────────┐
        │                                       │
        ↓                                       ↓
┌──────────────────────────────────────┐  ┌──────────────────────────────────────┐
│  DisponibiliteRepository             │  │  DisponibiliteService                │
├──────────────────────────────────────┤  ├──────────────────────────────────────┤
│ - dbConnexion: DbConnexion           │  │ - repository: DisponibiliteRepository│
├──────────────────────────────────────┤  ├──────────────────────────────────────┤
│ + getDisponibilitesByCoach()         │  │ + getDisponibilitesByCoach()         │
│ + getAvailableSlots()                │  │ + getAvailableSlots()                │
│ + getAvailableSlotsByDate()          │  │ + getAvailableSlotsByDate()          │
│ + createDisponibilite()              │  │ + createDisponibilite()              │
│ + updateDisponibiliteStatus()        │  │ + updateDisponibiliteStatus()        │
│ + deleteDisponibilite()              │  │ + deleteDisponibilite()              │
│ + isSlotAvailable()                  │  │ + isSlotAvailable()                  │
│ + getAvailableSlotsCount()           │  │ + getAvailableSlotsCount()           │
└──────────────────────────────────────┘  └──────────────────────────────────────┘
                                                        ↑
                                                        │ uses
                                                        │
                                            ┌───────────┴───────────┐
                                            │                       │
                                            ↓                       ↓
                                    ┌──────────────────┐  ┌──────────────────┐
                                    │ SessionService   │  │ SessionRepository│
                                    ├──────────────────┤  ├──────────────────┤
                                    │ + reserverSession│  │ + createSession()│
                                    │ + updateSession()│  │ + updateSession()│
                                    └──────────────────┘  └──────────────────┘
```

---

## Sequence Diagram: Reservation Flow

```
User          FindCoachController    CalendarCoachController    DisponibiliteService    Database
  │                  │                         │                        │                  │
  │ Click button     │                         │                        │                  │
  ├─────────────────→│                         │                        │                  │
  │                  │ Load FXML               │                        │                  │
  │                  ├────────────────────────→│                        │                  │
  │                  │                         │ initialize()           │                  │
  │                  │                         ├───────────────────────→│                  │
  │                  │                         │                        │ Query DB         │
  │                  │                         │                        ├─────────────────→│
  │                  │                         │                        │ Return slots     │
  │                  │                         │                        │←─────────────────┤
  │                  │                         │←───────────────────────┤                  │
  │                  │ Display calendar        │                        │                  │
  │                  │←────────────────────────┤                        │                  │
  │ See calendar     │                         │                        │                  │
  │←─────────────────┤                         │                        │                  │
  │                  │                         │                        │                  │
  │ Click slot       │                         │                        │                  │
  ├─────────────────→│                         │                        │                  │
  │                  │ selectTimeSlot()        │                        │                  │
  │                  ├────────────────────────→│                        │                  │
  │                  │                         │ Highlight slot        │                  │
  │                  │                         │                        │                  │
  │ Click Reserve    │                         │                        │                  │
  ├─────────────────→│                         │                        │                  │
  │                  │ performReservation()    │                        │                  │
  │                  ├────────────────────────→│                        │                  │
  │                  │                         │ reserverSession()      │                  │
  │                  │                         ├───────────────────────→│                  │
  │                  │                         │                        │ Create session   │
  │                  │                         │                        ├─────────────────→│
  │                  │                         │                        │ Update status    │
  │                  │                         │                        ├─────────────────→│
  │                  │                         │                        │ Success          │
  │                  │                         │                        │←─────────────────┤
  │                  │                         │←───────────────────────┤                  │
  │                  │ Show confirmation       │                        │                  │
  │                  │←────────────────────────┤                        │                  │
  │ Reservation OK   │                         │                        │                  │
  │←─────────────────┤                         │                        │                  │
```

---

## Technology Stack

```
┌─────────────────────────────────────────────────────────────┐
│                    Frontend Layer                            │
├─────────────────────────────────────────────────────────────┤
│ • JavaFX 21 - UI Framework                                  │
│ • FXML - UI Markup Language                                 │
│ • CSS - Styling                                             │
│ • Java 23 - Programming Language                            │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Business Logic Layer                       │
├─────────────────────────────────────────────────────────────┤
│ • Java 23 - Programming Language                            │
│ • Service Pattern - Business Logic                          │
│ • Repository Pattern - Data Access                          │
│ • Model Classes - Data Representation                       │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Data Access Layer                          │
├─────────────────────────────────────────────────────────────┤
│ • JDBC - Database Connectivity                              │
│ • PreparedStatements - SQL Execution                        │
│ • Connection Pooling - Performance                          │
│ • DbConnexion Singleton - Connection Management             │
└─────────────────────────────────────────────────────────────┘
                            ↓
┌─────────────────────────────────────────────────────────────┐
│                   Database Layer                             │
├─────────────────────────────────────────────────────────────┤
│ • PostgreSQL 12+ - Relational Database                      │
│ • SQL - Query Language                                      │
│ • Indexes - Performance Optimization                        │
│ • Constraints - Data Integrity                              │
└─────────────────────────────────────────────────────────────┘
```

---

## Deployment Architecture

```
┌─────────────────────────────────────────────────────────────┐
│                    Development Machine                       │
├─────────────────────────────────────────────────────────────┤
│                                                               │
│  ┌──────────────────────────────────────────────────────┐   │
│  │              DayFlow Application                     │   │
│  │  (Maven Project - Java 23 + JavaFX 21)              │   │
│  └──────────────────────────────────────────────────────┘   │
│                            ↓                                  │
│  ┌──────────────────────────────────────────────────────┐   │
│  │         PostgreSQL Database (localhost:5432)        │   │
│  │  • Database: pidev_db                               │   │
│  │  • User: postgres                                   │   │
│  │  • Tables: disponibilite, session, user, ...        │   │
│  └──────────────────────────────────────────────────────┘   │
│                                                               │
└─────────────────────────────────────────────────────────────┘
```

---

## File Structure

```
DayFlow/
├── src/main/java/
│   ├── model/coaching_session/
│   │   └── Disponibilite.java
│   ├── repository/coaching_session/
│   │   └── DisponibiliteRepository.java
│   ├── services/coaching_session_module/
│   │   └── DisponibiliteService.java
│   ├── controllers/
│   │   ├── CalendarCoachController.java
│   │   ├── GoogleCalendarSyncController.java
│   │   ├── CalendarSyncStatusController.java
│   │   └── userdashboard/
│   │       └── FindCoachController.java (modified)
│   └── utils/
│       ├── DbConnexion.java
│       └── DatabaseMigration.java
├── src/main/resources/
│   └── user/coaching_session/
│       ├── calendar_coach.fxml
│       ├── google_calendar_sync.fxml
│       └── google_calendar_sync.css
├── database/migrations/
│   ├── create_disponibilite_table.sql
│   └── insert_sample_disponibilite_data.sql
└── Documentation/
    ├── CALENDAR_INTEGRATION_FINAL_SUMMARY.md
    ├── QUICK_TEST_GUIDE.md
    ├── IMPLEMENTATION_STATUS.md
    └── ARCHITECTURE_OVERVIEW.md (this file)
```

---

**Last Updated**: May 5, 2026  
**Architecture Version**: 1.0  
**Status**: ✅ Complete
