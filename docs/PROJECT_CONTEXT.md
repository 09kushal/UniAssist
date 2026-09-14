# UniAssist — Project Context

## 1. PROJECT OVERVIEW
**App Name:** UniAssist  
**Purpose:** An Online Mentorship and Learning Platform connecting students with verified tutors across two distinct domains (Academic Learning and Skill-Based Learning). It is a dual-domain learning ecosystem, not just a simple tutoring app.  
**Target Market:** Students looking for academic help (C, Java, Python, Math, Science, etc.) and skill-based learning (Photoshop, UI/UX, Web Dev, IELTS, etc.).  
**Tech Stack:**
- **Backend:** Django + Django REST Framework, MySQL (fully normalized, 3NF minimum), JWT Authentication, Firebase Cloud Messaging (FCM), SMTP via Gmail, Django media files.
- **Android:** Java, XML Layouts, Retrofit, RecyclerView, MVVM pattern.
- **Integrations:** Jitsi Meet API (Video Classes), eSewa Payment Gateway (Payments).

**User Roles and Permissions:**
- **Student:** Can register, browse tutors, book sessions, pay (via eSewa), join live video classes, and review tutors.
- **Tutor:** Can register, choose a domain (academic/skill/both), upload verification documents, set availability slots, accept/reject bookings, and conduct sessions.
- **Admin:** Verifies tutors, manages users, handles reports (lateness/complaints), and releases payouts.

---

## 2. BACKEND STRUCTURE
The backend is split into modular Django apps:
- **`accounts/`** — `models`, `views`, `urls` (Handles User, Student, Tutor, OTP, Auth, Profiles)
- **`booking/`** — `models`, `views`, `urls` (Handles Booking requests, Session management, Accept/Reject logic)
- **`payments/`** — `models`, `views`, `urls` (Handles eSewa initiation, callback, Payouts, Commission/Fine logic)
- **`reviews/`** — `models`, `views`, `urls` (Handles Student reviews and Tutor ratings)
- **`reports/`** — `models`, `views`, `urls` (Handles LatenessReports, RescheduleRequests, Admin actions)
- **`notifications/`** — `models`, `views`, `urls` (Handles Firebase Cloud Messaging and Notification tracking)
- **`tutors/`** — `models`, `views`, `urls` (Handles Tutor specific models like Subject, Skill, TutorDocument, TutorAvailability, listing APIs)

---

## 3. COMPLETE API ENDPOINTS LIST

### Authentication & Profiles (`accounts/`)
- `POST /api/auth/login/` (No Auth) — Body: `{email, password}` | Response: `{access, refresh, user}`
- `POST /api/auth/register/student/` (No Auth) — Body: Student details | Response: `{message, user}`
- `POST /api/auth/register/tutor/` (No Auth) — Body: Tutor details | Response: `{message, user}`
- `POST /api/auth/verify-otp/` (No Auth) — Body: `{email, otp}` | Response: `{message}`
- `POST /api/auth/password-reset/request/` (No Auth) — Body: `{email}`
- `POST /api/auth/password-reset/confirm/` (No Auth) — Body: `{email, otp, new_password}`
- `GET /api/auth/student/profile/` (Auth: Student) | Response: StudentProfile
- `PATCH /api/auth/student/profile/setup/` (Auth: Student) — Body: Profile fields/photo | Response: StudentProfile

### Tutor Discovery & Management (`tutors/`)
- `GET /api/tutors/list/` (Auth: Yes) — Query: `?domain=&page=` | Response: Paginated Tutors
- `GET /api/tutors/{id}/profile/` (Auth: Yes) | Response: TutorProfile
- `GET /api/tutors/my-profile/` (Auth: Tutor) | Response: TutorProfile
- `PATCH /api/tutors/profile/setup/` (Auth: Tutor) — Body: Profile fields | Response: TutorProfile
- `POST /api/tutors/subjects/add/` (Auth: Tutor) — Body: `{name}` | Response: Subject
- `DELETE /api/tutors/subjects/{id}/remove/` (Auth: Tutor)
- `POST /api/tutors/skills/add/` (Auth: Tutor) — Body: `{name}` | Response: Skill
- `DELETE /api/tutors/skills/{id}/remove/` (Auth: Tutor)
- `POST /api/tutors/availability/add/` (Auth: Tutor) — Body: `{day_of_week, start_time, end_time}` | Response: Slot
- `DELETE /api/tutors/availability/{id}/remove/` (Auth: Tutor)

### Booking System (`booking/`)
- `POST /api/booking/request/` (Auth: Student) — Body: `{tutor_id, subject_or_skill, proposed_date, proposed_start_time, proposed_end_time, message}` | Response: Booking
- `GET /api/booking/my-bookings/` (Auth: Student) — Query: `?page=` | Response: Paginated Bookings
- `GET /api/booking/my-requests/` (Auth: Tutor) — Query: `?page=&status=` | Response: Paginated Booking Requests
- `PATCH /api/booking/{booking_id}/respond/` (Auth: Tutor) — Body: `{action, rejection_reason}` | Response: Booking

### Payments (`payments/`)
- `POST /api/payments/initiate/` (Auth: Student) — Body: `{booking_id}` | Response: PaymentDetails
- `POST /api/payments/demo-complete/` (Auth: Student) — Body: `{booking_id}` | Response: `{success}` (Workaround for Sandbox)

### Notifications (`notifications/`)
- `GET /api/notifications/` (Auth: Yes) — Query: `?page=` | Response: Paginated Notifications
- `PATCH /api/notifications/{id}/read/` (Auth: Yes) | Response: Notification
- `PATCH /api/notifications/read-all/` (Auth: Yes)
- `GET /api/notifications/unread-count/` (Auth: Yes) | Response: `{unread_count}`

---

## 4. DATABASE MODELS
*All tables adhere to 3NF minimum.*

- **User**: `id`, `full_name`, `email`, `password`, `role`, `is_active`, `created_at`
- **Student**: `id`, `user` (FK), `grade_or_university`, `subjects_of_interest`, `profile_photo`, `is_suspended`, `warning_count`
- **Tutor**: `id`, `user` (FK), `domain`, `bio`, `pricing_per_session`, `is_verified`, `is_suspended`, `warning_count`, `no_show_count`, `punctuality_score`, `total_sessions_done`, `profile_photo`
- **TutorDocument**: `id`, `tutor` (FK), `doc_type`, `file_path`, `uploaded_at`
- **TutorAvailability**: `id`, `tutor` (FK), `day_of_week`, `start_time`, `end_time`
- **Subject**: `id`, `tutor` (FK), `name`
- **Skill**: `id`, `tutor` (FK), `name`
- **Booking**: `id`, `student` (FK), `tutor` (FK), `selected_slot` (FK), `booking_status`, `officially_scheduled`, `created_at`. *Constraint: officially_scheduled is True ONLY after payment.*
- **Session**: `id`, `booking` (FK), `scheduled_at`, `tutor_start_time`, `session_status`, `officially_scheduled`
- **Course**: `id`, `tutor` (FK), `title`, `description`, `domain`, `price`, `created_at`
- **Enrollment**: `id`, `student` (FK), `course` (FK), `enrolled_at`, `progress`
- **Payment**: `id`, `booking` (FK), `amount`, `payment_status`, `esewa_ref_id`, `paid_at`
- **Payout**: `id`, `tutor` (FK), `booking` (FK), `total_paid`, `commission_amount`, `tutor_base_share`, `fine_percentage`, `fine_amount`, `fine_reason`, `fine_imposed_by` (FK), `student_refund`, `tutor_final_payout`, `payout_status`, `released_by` (FK), `released_at`
- **LatenessReport**: `id`, `session` (FK), `reported_by` (FK), `reported_against` (FK), `reporter_role`, `delay_range`, `description`, `admin_action`, `created_at`
- **RescheduleRequest**: `id`, `booking` (FK), `requested_by` (FK), `reason`, `status`, `created_at`
- **Review**: `id`, `booking` (FK), `student` (FK), `tutor` (FK), `rating`, `review_text`, `knowledge_rating`, `teaching_rating`, `communication_rating`, `punctuality_rating`, `created_at`
- **Notification**: `id`, `user` (FK), `title`, `message`, `is_read`, `created_at`

---

## 5. ANDROID PROJECT STRUCTURE
**Activities (`com/kushal/uniassist/`)**:
- `SplashActivity.java`: App entry, checks session.
- `LoginActivity.java`: Authenticates user.
- `RegisterActivity.java`: Student registration.
- `TutorRegisterActivity.java`: Tutor registration.
- `OtpVerifyActivity.java`: Verifies OTP during signup/reset.
- `ForgotPasswordActivity.java`: Password reset flow.
- `MainActivity.java`: Host container / fallback.
- `StudentDashboardActivity.java`: Main dashboard for students.
- `TutorDashboardActivity.java`: Main dashboard for tutors.
- `EditProfileActivity.java`: Edits Student profile.
- `TutorEditProfileActivity.java`: Edits Tutor profile/settings.
- `TutorListActivity.java`: Browses tutors.
- `TutorProfileActivity.java`: Detailed tutor view.
- `BookingRequestActivity.java`: Initiates booking with a tutor.
- `MyBookingsActivity.java`: Student's booking history.
- `TutorBookingsActivity.java`: Tutor's incoming requests & bookings.
- `EsewaPaymentActivity.java`: Payment flow via eSewa.
- `JoinSessionActivity.java`: Launches Jitsi Meet.
- `NotificationsActivity.java`: Displays notifications.
- `PayoutActivity.java` & `TutorReportsActivity.java`: Stubs/WIP for tutor financials and reports.

**Adapters (`com/kushal/uniassist/`)**:
- `AvailabilityAdapter.java`: Renders tutor's availability slots.
- `BookingAdapter.java`: General booking list adapter.
- `BookingDetailAdapter.java`: Renders detailed student booking cards (handles cancel, pay, join).
- `FeaturedTutorAdapter.java`: Renders featured tutors on student dashboard.
- `NotificationAdapter.java`: Renders notifications list.
- `TutorAdapter.java`: Renders lists of tutors.
- `TutorBookingAdapter.java`: Renders tutor's side of booking requests (accept, reject, join).

**Network (`com/kushal/uniassist/network/`)**:
- `ApiClient.java`: Configures Retrofit and logging. Intercepts to add Bearer token.
- `ApiService.java`: Defines all REST endpoints, methods, parameters.

**Models (`com/kushal/uniassist/models/`)**:
- POJOs mapping to backend JSON requests/responses (e.g., `ApiResponse`, `LoginResponse`, `BookingResponse`, etc.)

**Utilities (`com/kushal/uniassist/`)**:
- `SessionManager.java`: Wrapper around SharedPreferences for tokens and user details.
- `UniAssistApp.java`: Application class.

---

## 6. ANDROID SCREENS LIST
- **Splash Screen**: `SplashActivity.java` | `activity_splash.xml` | Shows logo | Navigates to Login/Dashboard.
- **Login**: `LoginActivity.java` | `activity_login.xml` | Shows login form | API: `/login/` | Navigates to Dashboards or Registration.
- **Student Register**: `RegisterActivity.java` | `activity_register.xml` | Form | API: `/register/student/` | Navigates to OTP.
- **Tutor Register**: `TutorRegisterActivity.java` | `activity_tutor_register.xml` | Form | API: `/register/tutor/` | Navigates to OTP.
- **OTP Verify**: `OtpVerifyActivity.java` | `activity_otp_verify.xml` | OTP input | API: `/verify-otp/` | Navigates to Login.
- **Student Dashboard**: `StudentDashboardActivity.java` | `activity_student_dashboard.xml` | Home feed | API: `/tutors/list/` | Navigates to Tutor List, Profile, My Bookings.
- **Tutor Dashboard**: `TutorDashboardActivity.java` | `activity_tutor_dashboard.xml` | Tutor home | Navigates to Profile setup, Requests, Earnings.
- **Tutor List**: `TutorListActivity.java` | `activity_tutor_list.xml` | List + Filter | API: `/tutors/list/` | Navigates to Tutor Profile.
- **Tutor Profile**: `TutorProfileActivity.java` | `activity_tutor_profile.xml` | Tutor details & slots | API: `/{id}/profile/` | Navigates to Booking Request.
- **Booking Request**: `BookingRequestActivity.java` | `activity_booking_request.xml` | Booking form | API: `/booking/request/` | Navigates to My Bookings.
- **My Bookings (Student)**: `MyBookingsActivity.java` | `activity_my_bookings.xml` | List of bookings | API: `/booking/my-bookings/` | Calls Pay or Join.
- **Tutor Bookings**: `TutorBookingsActivity.java` | `activity_tutor_bookings.xml` | List of requests | API: `/booking/my-requests/`, `/respond/` | Calls Join.
- **Payment**: `EsewaPaymentActivity.java` | `activity_esewa_payment.xml` | eSewa Web UI | API: `/payments/initiate/` | Navigates back.
- **Join Session**: `JoinSessionActivity.java` | `activity_join_session.xml` | Jitsi Meet call screen | Uses Jitsi SDK (No API call).

---

## 7. KEY CONFIGURATIONS
- **BASE_URL**: `https://yin-elongated-studio.ngrok-free.dev/`
- **SharedPreferences Name**: `"UniAssistSession"`
- **SharedPreferences Keys**: `access_token`, `refresh_token`, `role`, `full_name`, `email`, `profile_photo`.
- **SessionManager Methods**: `saveSession()`, `getAccessToken()`, `getRefreshToken()`, `getRole()`, `getFullName()`, `getEmail()`, `getProfilePhoto()`, `isLoggedIn()`, `clearSession()`.
- **Firebase Setup**: Credentials located at `backend/firebase_credentials.json`.
- **eSewa Configuration**: Sandbox product code = `EPAYTEST`. All pricing in NPR.

---

## 8. BUSINESS LOGIC
- **Commission Formula**: 30% platform commission always deducted first.  
  *Example:* Student pays 1000 NPR → Admin gets 300 NPR, Tutor Base Share is 700 NPR.
- **Fine Formula**: Calculated from the Tutor's 70% share only. The fine amount always equals the student refund amount.  
  *Example (10% fine):* Tutor Base (700) * 10% = 70 NPR Fine. Tutor Final Payout = 630 NPR. Student Refund = 70 NPR.
- **Booking Flow**: 
  1. Student requests → `pending`. 
  2. Tutor responds → `accepted` or `rejected`. 
  3. Student pays (eSewa) → `completed` payment, `officially_scheduled=True`. 
  4. Session takes place via Jitsi Meet.
- **OTP Expiry**: Expires in exactly 10 minutes.

---

## 9. KNOWN ISSUES & WORKAROUNDS
- **eSewa Sandbox Unavailable**: A demo payment endpoint (`/api/payments/demo-complete/`) has been created to simulate a successful payment and bypass eSewa gateway issues.
- **MySQL Dual Installation Fix**: `mysqlclient` version 2.2.8 is correctly installed. Do NOT add or import `pymysql` anywhere in the project, as it conflicts with `mysqlclient`.
- **SSL Certificate Fix for Gmail SMTP**: Requires proper CA certificate configurations or bypassing strict verification in local dev environments if standard TLS handshake fails.

---

## 10. PENDING FEATURES
- **Complete**: Environment Setup, Authentication, Tutor Discovery, Booking System, Review & Rating Module.
- **Partially Done**: Payment Module (APIs ready, eSewa workaround active), Reports & Admin (APIs ready, missing some UI integrations), Android App (Basic screens complete, specific UI polishes pending).
- **Not Started**: Notifications full E2E testing in Android, full App E2E integration testing, backend production deployment (PythonAnywhere/Railway).

---

## 11. DEMO INSTRUCTIONS
- **Start Server (Backend)**:
  ```bash
  cd ~/UniAssist/backend
  source venv/bin/activate
  python manage.py runserver
  ```
- **Connect Phone**: Update `BASE_URL` in `ApiClient.java` to match your local IP or `ngrok` URL. Run via Android Studio onto the connected device.
- **Demo Flow**:
  1. Register/Login as a Tutor -> Setup profile/availability.
  2. Register/Login as a Student -> Browse tutors -> Book a slot.
  3. Switch to Tutor -> Accept booking.
  4. Switch to Student -> Proceed to Pay (use Demo Payment button).
  5. Both Users -> Click "Join Session" to enter Jitsi Meet.
- **Test Credentials**: Use standard registered user emails to bypass OTP for quick access (if configured in dev settings).

---

## 12. FILE LOCATIONS
- **Backend Directory**: `~/UniAssist/backend/`
- **Android Directory**: `~/UniAssist/android/`
- **Documentation Directory**: `~/UniAssist/docs/`
- **Firebase Credentials**: `~/UniAssist/backend/firebase_credentials.json`
- **.env File**: `~/UniAssist/backend/.env`
