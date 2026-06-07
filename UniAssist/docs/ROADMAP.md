# UniAssist — Development Roadmap

## Phase 1 — Environment Setup (Week 1–2) ✅
- Python 3, MySQL, VS Code, Postman installed
- Django project created and connected to MySQL
- uniassist_db database created
- GitHub repository initialized

## Phase 2 — Backend Core APIs (Week 3–5)
- Custom User model (role: student / tutor / admin)
- Student and Tutor models
- OTP email verification (expires 10 min)
- JWT Registration and Login APIs
- Tutor domain selection (academic / skill / both)
- Tutor document upload APIs
- Student browse and filter tutor APIs
- Booking request APIs (create, accept, reject)

## Phase 3 — Backend Advanced APIs (Week 6–7)
- Course create and enrollment APIs
- Session scheduling and tracking APIs
- Payment APIs (eSewa integration)
- Payout calculation and release APIs
- Review and rating APIs (post-session only)
- Lateness and no-show report APIs
- Reschedule request APIs
- Admin dashboard and verification APIs
- FCM notification APIs

## Phase 4 — Android Screens & Navigation (Week 8–9)
- Android project setup (Java, MVVM, Retrofit)
- Splash, Login, Register, OTP screens
- Student Dashboard, Tutor Listing, Tutor Profile
- Booking screen, User Profile screen

## Phase 5 — Android Advanced Screens (Week 10–11)
- Course List and Course Detail screens
- Payment screen (eSewa SDK)
- Live Class screen (Jitsi Meet SDK)
- Notifications screen
- Review screen
- Tutor Dashboard and Payout Breakdown

## Phase 6 — Third-Party Integrations (Week 11–12)
- Jitsi Meet SDK (Android)
- Firebase Cloud Messaging (FCM)
- eSewa Payment SDK (Android)
- Django SMTP email (OTP)

## Phase 7 — Testing & Submission (Week 12–13)
- Full flow testing (Student → Book → Pay → Class → Review)
- Tutor flow testing
- Admin panel testing
- Bug fixes
- Project report and ER diagram
- Demo presentation
- Django backend deployment (PythonAnywhere or Railway)