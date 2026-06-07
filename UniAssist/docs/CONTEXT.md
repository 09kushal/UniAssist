# UniAssist — Project Context

## Project Identity
UniAssist is an Online Mentorship and Learning Platform.
It is a dual-domain learning ecosystem — NOT a simple tutoring app.
It connects students with verified tutors across two distinct domains:

1. Academic Learning — C, Java, Python, DBMS, OS, Networking, Math,
   Science, school and university subjects.
2. Skill-Based Learning — Photoshop, Graphic Design, UI/UX, Web Development,
   Video Editing, Digital Marketing, Handwriting, IELTS, Language Learning, etc.

Dual-domain architecture is a CORE design requirement.
Every module, API, database schema, and UI screen must reflect both domains.

## Technology Stack (NON-NEGOTIABLE)
- Android Frontend : Java, XML Layouts, Retrofit, RecyclerView, MVVM
- Backend          : Django + Django REST Framework
- Authentication   : JWT (JSON Web Tokens) + role-based access control
- Database         : MySQL (fully normalized, 3NF minimum)
- Video Classes    : Jitsi Meet API
- Notifications    : Firebase Cloud Messaging (FCM)
- Payments         : eSewa Payment Gateway (sandbox product_code = EPAYTEST)
- Email / OTP      : Django SMTP via Gmail
- File Storage     : Django media files (ImageField / FileField)

## User Roles
- Student  : register, browse tutors, book sessions, pay, join live class, review
- Tutor    : register, choose domain, upload docs, set availability, accept bookings, conduct sessions
- Admin    : verify tutors, manage users, handle reports, release payouts

## Folder Structure
UniAssist/
├── backend/
│   ├── accounts/
│   ├── booking/
│   ├── sessions/
│   ├── payments/
│   ├── reviews/
│   ├── notifications/
│   └── manage.py
├── android/
├── docs/
├── requirements.txt
└── README.md

## Naming Conventions
- Python / Database : snake_case
- Java (Android)   : camelCase
- No hardcoded strings — use constants and resource files

## Hard Constraints
- NO Stripe, PayPal, or any non-eSewa gateway
- NO Zoom, Google Meet, or non-Jitsi video solution
- NO Kotlin — Android is Java only
- NO PostgreSQL or SQLite — MySQL only
- NO in-app messaging between student and tutor
- NO automatic refund system
- NO automatic suspension or fine system
- NO mid-course cancellation
- NO social login or phone number login
- All pricing in NPR (Nepalese Rupee)
- eSewa sandbox product_code = EPAYTEST
- OTP expires in exactly 10 minutes
- Supported file types: JPG, PNG, PDF only
- 30% platform commission always deducted first
- Fine always calculated from tutor's 70% share only
- Fine amount always equals student refund amount
- Tutor payout held until admin manually releases

## General Coding Rules
- Clean architecture, strict separation of concerns
- Modular DRF serializers and viewsets
- Android MVVM: ViewModel + LiveData + Repository pattern
- All APIs must have full documented inputs and outputs
- Django migrations must be incremental and reversible
- File uploads: JPG, PNG, PDF only — enforce in serializer validation
- Always treat this as a dual-domain platform — never academic only