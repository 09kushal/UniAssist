# UniAssist — Database Schema

All tables: 3NF minimum, explicit FK with ON DELETE/ON UPDATE, snake_case.

## Core Entities
User, Student, Tutor, TutorCategory, Subject, Skill,
TutorDocument, TutorAvailability, Booking, Session,
Course, Enrollment, Payment, Payout, LatenessReport,
RescheduleRequest, Review, Notification

---

## User
- id (PK)
- full_name VARCHAR
- email VARCHAR UNIQUE
- password VARCHAR (hashed)
- role ENUM(student, tutor, admin)
- is_active BOOLEAN DEFAULT TRUE
- created_at DATETIME

## Student
- id (PK)
- user FK → User
- grade_or_university VARCHAR
- subjects_of_interest VARCHAR
- profile_photo VARCHAR
- is_suspended BOOLEAN DEFAULT FALSE
- warning_count INT DEFAULT 0

## Tutor
- id (PK)
- user FK → User
- domain ENUM(academic, skill, both)
- bio TEXT
- pricing_per_session DECIMAL(10,2)
- is_verified BOOLEAN DEFAULT FALSE
- is_suspended BOOLEAN DEFAULT FALSE
- warning_count INT DEFAULT 0
- no_show_count INT DEFAULT 0
- punctuality_score DECIMAL(5,2) DEFAULT 100.00
- total_sessions_done INT DEFAULT 0
- profile_photo VARCHAR

## TutorDocument
- id (PK)
- tutor FK → Tutor
- doc_type ENUM(profile_photo, certificate, marksheet,
                citizenship, portfolio, skill_certificate)
- file_path VARCHAR
- uploaded_at DATETIME

## TutorAvailability
- id (PK)
- tutor FK → Tutor
- day_of_week ENUM(Mon,Tue,Wed,Thu,Fri,Sat,Sun)
- start_time TIME
- end_time TIME

## Subject (Academic domain)
- id (PK)
- tutor FK → Tutor
- name VARCHAR

## Skill (Skill-Based domain)
- id (PK)
- tutor FK → Tutor
- name VARCHAR

## Booking
- id (PK)
- student FK → Student
- tutor FK → Tutor
- selected_slot FK → TutorAvailability
- booking_status ENUM(pending, accepted, rejected, completed)
- officially_scheduled BOOLEAN DEFAULT FALSE
- created_at DATETIME

## Session
- id (PK)
- booking FK → Booking
- scheduled_at DATETIME
- tutor_start_time DATETIME NULL
- session_status ENUM(pending, completed, cancelled)
- officially_scheduled BOOLEAN DEFAULT FALSE

## Course
- id (PK)
- tutor FK → Tutor
- title VARCHAR
- description TEXT
- domain ENUM(academic, skill)
- price DECIMAL(10,2)
- created_at DATETIME

## Enrollment
- id (PK)
- student FK → Student
- course FK → Course
- enrolled_at DATETIME
- progress INT DEFAULT 0

## Payment
- id (PK)
- booking FK → Booking
- amount DECIMAL(10,2)
- payment_status ENUM(pending, completed, failed)
- esewa_ref_id VARCHAR NULL
- paid_at DATETIME NULL

## Payout
- id (PK)
- tutor FK → Tutor
- booking FK → Booking
- total_paid DECIMAL(10,2)
- commission_amount DECIMAL(10,2)
- tutor_base_share DECIMAL(10,2)
- fine_percentage DECIMAL(5,2) DEFAULT 0
- fine_amount DECIMAL(10,2) DEFAULT 0
- fine_reason VARCHAR NULL
- fine_imposed_by FK → User(Admin) NULL
- student_refund DECIMAL(10,2) DEFAULT 0
- tutor_final_payout DECIMAL(10,2)
- payout_status ENUM(held, released, fined)
- released_by FK → User(Admin) NULL
- released_at DATETIME NULL

## LatenessReport
- id (PK)
- session FK → Session
- reported_by FK → User
- reported_against FK → User
- reporter_role ENUM(student, tutor)
- delay_range ENUM(5_15min, 15_30min, 30_plus, no_show)
- description TEXT
- admin_action ENUM(pending, no_action, warning, fined, suspended, removed)
- created_at DATETIME

## RescheduleRequest
- id (PK)
- booking FK → Booking
- requested_by FK → Student
- reason TEXT
- status ENUM(pending, approved, rejected)
- created_at DATETIME

## Review
- id (PK)
- booking FK → Booking
- student FK → Student
- tutor FK → Tutor
- rating INT (1–5)
- review_text TEXT
- knowledge_rating INT NULL
- teaching_rating INT NULL
- communication_rating INT NULL
- punctuality_rating INT NULL
- created_at DATETIME

## Notification
- id (PK)
- user FK → User
- title VARCHAR
- message TEXT
- is_read BOOLEAN DEFAULT FALSE
- created_at DATETIME

## Commission & Fine Formula (EXACT)
Given: Student pays NPR 1000, Admin sets fine = 10%
Platform commission = 1000 * 0.30 = NPR 300
Tutor base share    = 1000 * 0.70 = NPR 700
Fine amount         = 700  * 0.10 = NPR 70
Tutor final payout  = 700  - 70   = NPR 630
Student refund      = NPR 70
Admin commission    = NPR 300 (unchanged)