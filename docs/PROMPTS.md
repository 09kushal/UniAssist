# UniAssist — Agent Prompts Library

## How to use
Copy the prompt for the current phase and paste into Antigravity.
Always start every prompt with the "Read these files" instruction.

phase1 
Read the following files completely before doing anything:
- docs/CONTEXT.md
- docs/DATABASE_SCHEMA.md
- docs/API_RULES.md

Task:
Set up the Django backend project inside the backend/ folder.
Connect it to MySQL database named uniassist_db
with user root and password uniassist123.
Create a custom User model with role field (student/tutor/admin).
Create Student and Tutor models exactly as defined in
docs/DATABASE_SCHEMA.md.
Install all required packages and run migrations.

Requirements:
1. Custom User model extending AbstractBaseUser
2. Role field: ENUM student / tutor / admin
3. Student model linked to User with all fields from DATABASE_SCHEMA.md
4. Tutor model linked to User with all fields from DATABASE_SCHEMA.md
5. MySQL database connection in settings.py
6. JWT authentication configured in settings.py
7. CORS headers configured
8. All apps registered in INSTALLED_APPS
9. Follow snake_case naming for all models and fields
10. Follow response format defined in API_RULES.md

Generate these files in order:
1. backend/uniassist/settings.py (full file)
2. backend/accounts/models.py (full file)
3. backend/accounts/admin.py (register all models)
4. requirements.txt (all packages needed)
5. Migration command to run after
---

## Phase 2 — Authentication Module
Read the following files completely before doing anything:
- docs/CONTEXT.md
- docs/DATABASE_SCHEMA.md
- docs/API_RULES.md

Task:
Build the complete Authentication Module for UniAssist.

Context:
- Django + DRF backend inside backend/ folder
- MySQL database: uniassist_db
- Custom User model already exists with role field (student/tutor/admin)
- Python 3.14, Django 5.x, mysqlclient 2.2.8

Requirements:

1. OTP MODEL
   - Create OTP model with fields:
     email, otp_code, created_at, expires_at
   - OTP expires in exactly 10 minutes
   - One OTP per email at a time

2. STUDENT REGISTRATION API
   POST /api/auth/register/student/
   - Fields: full_name, email, password
   - Validate email is unique
   - Hash password
   - Send 6-digit OTP to email via Django SMTP
   - Return success message

3. TUTOR REGISTRATION API
   POST /api/auth/register/tutor/
   - Fields: full_name, email, password, domain (academic/skill/both)
   - Validate email is unique
   - Hash password
   - Send 6-digit OTP to email via Django SMTP
   - Return success message

4. OTP VERIFICATION API
   POST /api/auth/verify-otp/
   - Fields: email, otp_code
   - Check OTP matches and is not expired
   - If valid: activate user (is_active = True)
   - If expired: return error "OTP expired"
   - If wrong: return error "Invalid OTP"

5. LOGIN API
   POST /api/auth/login/
   - Fields: email, password
   - Validate credentials
   - Check user is active (OTP verified)
   - Return JWT access token + refresh token + role

6. LOGOUT API
   POST /api/auth/logout/
   - Blacklist JWT refresh token
   - Return success message

7. PASSWORD RESET - REQUEST
   POST /api/auth/password-reset/request/
   - Fields: email
   - Send OTP to email
   - Return success message

8. PASSWORD RESET - CONFIRM
   POST /api/auth/password-reset/confirm/
   - Fields: email, otp_code, new_password
   - Verify OTP
   - Update password
   - Return success message

GMAIL SMTP SETTINGS to add in settings.py:
EMAIL_BACKEND = 'django.core.mail.backends.smtp.EmailBackend'
EMAIL_HOST = 'smtp.gmail.com'
EMAIL_PORT = 587
EMAIL_USE_TLS = True
EMAIL_HOST_USER = 'your Gmail address'
EMAIL_HOST_PASSWORD = 'your Gmail app password'

Follow exact API response format from API_RULES.md:
Success: {"success": true, "message": "...", "data": {...}}
Error: {"success": false, "message": "..."}

Generate these files:
1. backend/accounts/models.py (add OTP model)
2. backend/accounts/serializers.py (full file)
3. backend/accounts/views.py (full file)
4. backend/accounts/urls.py (full file)
5. backend/uniassist/urls.py (include accounts urls)
6. backend/uniassist/settings.py (add email config)
7. Migration command to run after

---

## Phase 3 — Tutor Discovery Module
Read the following files completely before doing anything:
- docs/CONTEXT.md
- docs/DATABASE_SCHEMA.md
- docs/API_RULES.md
now that i have completed phase 1 and phase2 .build Phase 3 — Tutor Discovery Module.

Task:
Build the complete Tutor Discovery Module for UniAssist.
Create a new Django app called 'tutors' inside backend/ folder.

Context:
- Phase 2 (Authentication) is already complete
- accounts app already has User, Student, Tutor, Subject, Skill,
  TutorAvailability, TutorDocument models
- Do NOT recreate existing models — import them from accounts app
- Do NOT add pymysql anywhere — mysqlclient 2.2.8 is already installed
- __init__.py must remain empty

Requirements:

1. TUTOR PROFILE SETUP API
   PATCH /api/tutors/profile/setup/
   Auth: Tutor JWT required
   Fields: bio, pricing_per_session, profile_photo (image upload)
   - Update tutor bio and pricing
   - Upload profile photo (JPG/PNG only, max 5MB)
   - Return updated tutor profile

2. ADD SUBJECT API (Academic domain tutors only)
   POST /api/tutors/subjects/add/
   Auth: Tutor JWT required
   Fields: name
   - Only allowed if tutor domain is academic or both
   - Return added subject

3. REMOVE SUBJECT API
   DELETE /api/tutors/subjects/<id>/remove/
   Auth: Tutor JWT required
   - Only tutor who owns subject can delete it

4. ADD SKILL API (Skill-based domain tutors only)
   POST /api/tutors/skills/add/
   Auth: Tutor JWT required
   Fields: name
   - Only allowed if tutor domain is skill or both
   - Return added skill

5. REMOVE SKILL API
   DELETE /api/tutors/skills/<id>/remove/
   Auth: Tutor JWT required
   - Only tutor who owns skill can delete it

6. ADD AVAILABILITY SLOT API
   POST /api/tutors/availability/add/
   Auth: Tutor JWT required
   Fields: day_of_week (Mon/Tue/Wed/Thu/Fri/Sat/Sun),
           start_time, end_time
   - Validate no overlapping slots on same day
   - Return added slot

7. REMOVE AVAILABILITY SLOT API
   DELETE /api/tutors/availability/<id>/remove/
   Auth: Tutor JWT required

8. TUTOR LISTING API
   GET /api/tutors/list/
   Auth: Not required (public)
   Query params for filtering:
   - domain (academic/skill/both)
   - subject (subject name search)
   - skill (skill name search)
   - min_rating (minimum average rating)
   - available_day (Mon/Tue/Wed/Thu/Fri/Sat/Sun)
   - page, page_size (pagination)
   Rules:
   - Only show is_verified=True tutors
   - Show: full_name, domain, bio, pricing_per_session,
     profile_photo, punctuality_score, total_sessions_done,
     average_rating, subjects list, skills list,
     availability slots, verified badge
   - Order by rating descending by default

9. TUTOR PROFILE DETAIL API
   GET /api/tutors/<id>/profile/
   Auth: Not required (public)
   - Full tutor profile
   - All subjects and skills
   - All availability slots
   - Average rating and review count
   - total_sessions_done, punctuality_score
   - Verified badge status

10. MY PROFILE API (for logged in tutor)
    GET /api/tutors/my-profile/
    Auth: Tutor JWT required
    - Full own profile with all details

Follow exact API response format from API_RULES.md.
All list endpoints must be paginated.
File uploads: JPG, PNG only — validate in serializer.
Role-based permissions: only tutors can edit their own profile.

Generate these files:
1. backend/tutors/__init__.py (empty)
2. backend/tutors/models.py (empty — models already in accounts)
3. backend/tutors/serializers.py (full file)
4. backend/tutors/views.py (full file)
5. backend/tutors/urls.py (full file)
6. backend/uniassist/urls.py (add tutors urls)
7. backend/uniassist/settings.py (add tutors app + media files config)
8. Migration command if any new models added

---

## Phase 4 — Booking System
Read the following files completely before doing anything:
- docs/CONTEXT.md
- docs/DATABASE_SCHEMA.md
- docs/API_RULES.md
- docs/PROGRESS.md

Check PROGRESS.md first to understand what is already built.
Update PROGRESS.md after completing each task.

Current status:
- Phase 1 ✅ Complete
- Phase 2 ✅ Complete — accounts app has User, Student, Tutor,
  Subject, Skill, TutorAvailability, TutorDocument models
- Phase 3 ✅ Complete — tutors app has all discovery APIs working
- mysqlclient 2.2.8 is the MySQL driver — do NOT add pymysql
- __init__.py must stay empty

Task:
Build the complete Booking System Module for UniAssist.
Create a new Django app called 'booking' inside backend/ folder.

Requirements:

1. BOOKING REQUEST API
   POST /api/booking/request/
   Auth: Student JWT required
   Fields: tutor_id, subject_or_skill, proposed_date,
           proposed_start_time, proposed_end_time, message
   - booking_status must start as pending
   - Only students can create bookings
   - Tutor must be verified (is_verified=True)
   - Return full booking details

2. TUTOR ACCEPT/REJECT BOOKING API
   PATCH /api/booking/<id>/respond/
   Auth: Tutor JWT required
   Fields: action (accept / reject), rejection_reason (optional)
   - Only the tutor who received the booking can respond
   - booking_status → accepted or rejected
   - Return updated booking

3. BOOKING STATUS API
   GET /api/booking/<id>/status/
   Auth: Student or Tutor JWT required
   - Only the student or tutor involved can view
   - Return booking details with current status

4. STUDENT BOOKING HISTORY API
   GET /api/booking/my-bookings/
   Auth: Student JWT required
   - All bookings for the logged in student
   - Paginated with page and page_size
   - Filter by status (pending/accepted/rejected)

5. TUTOR BOOKING HISTORY API
   GET /api/booking/my-requests/
   Auth: Tutor JWT required
   - All booking requests received by the logged in tutor
   - Paginated with page and page_size
   - Filter by status (pending/accepted/rejected)

6. CANCEL BOOKING API
   PATCH /api/booking/<id>/cancel/
   Auth: Student JWT required
   - Only student can cancel
   - Only allowed if booking_status is pending
   - booking_status → cancelled

Booking Flow (STRICT ORDER — never skip):
1. Student sends request → booking_status = pending
2. Tutor accepts or rejects → booking_status = accepted | rejected
3. Student pays via eSewa → payment_status = completed (Phase 5)
4. officially_scheduled = TRUE only after payment (Phase 5)
5. Sessions via Jitsi Meet (Phase 9)

Important rules:
- officially_scheduled must default to False
- Never set officially_scheduled = True in this phase
  (that happens in Phase 5 after payment)
- Follow exact API response format from API_RULES.md
- All list endpoints must be paginated
- Never return raw Django errors

Generate these files:
1. backend/booking/__init__.py (empty)
2. backend/booking/models.py (Booking model only)
3. backend/booking/serializers.py (full file)
4. backend/booking/views.py (full file)
5. backend/booking/urls.py (full file)
6. backend/uniassist/urls.py (add booking urls)
7. backend/uniassist/settings.py (add booking to INSTALLED_APPS)
8. Migration command to run after
9. Update docs/PROGRESS.md marking Phase 4 tasks as complete

---

## Phase 5 — Payment Module
Read the following files completely before doing anything:
- docs/CONTEXT.md
- docs/DATABASE_SCHEMA.md
- docs/API_RULES.md
- docs/PROGRESS.md

Check PROGRESS.md first to understand what is already built.
Update PROGRESS.md after completing each task.

Current status:
- Phase 1 ✅ Complete
- Phase 2 ✅ Complete — accounts app: User, Student, Tutor,
  Subject, Skill, TutorAvailability, TutorDocument models
- Phase 3 ✅ Complete — tutors app: all discovery APIs
- Phase 4 ✅ Complete — booking app: Booking model,
  request/respond/cancel/history APIs
- mysqlclient 2.2.8 is the MySQL driver — do NOT add pymysql
- __init__.py must stay empty

Task:
Build the complete Payment Module for UniAssist.
Create a new Django app called 'payments' inside backend/ folder.

Requirements:

1. ESEWA PAYMENT INITIATION API
   POST /api/payments/initiate/
   Auth: Student JWT required
   Fields: booking_id
   - Only accepted bookings can be paid
   - Generate eSewa payment form data
   - Amount = booking tutor's pricing_per_session
   - Return eSewa payment parameters to frontend

2. ESEWA PAYMENT CALLBACK API
   POST /api/payments/callback/
   No auth (eSewa calls this directly)
   - Verify payment signature from eSewa
   - On success: payment_status = completed
   - On success: officially_scheduled = TRUE on the booking
   - On failure: payment_status = failed
   - Return appropriate response

3. PAYMENT STATUS API
   GET /api/payments/<booking_id>/status/
   Auth: Student or Tutor JWT required
   - Only involved parties can check
   - Return payment details and status

4. PAYOUT CALCULATION RULES (STRICT):
   - Platform takes 30% commission
   - Tutor receives 70% of session price
   - Fines are deducted from tutor's 70% only
   - Fine amount: 20% of tutor's 70% per offence
   - Net payout = tutor 70% - all fines

5. ADMIN PAYOUT RELEASE API
   POST /api/payments/admin/payout/<booking_id>/release/
   Auth: Admin JWT required
   - Calculate final payout using above formula
   - Create Payout record
   - Mark payout as released
   - Return payout breakdown:
     {
       "session_price": 1000,
       "platform_commission": 300,
       "tutor_share": 700,
       "total_fines": 0,
       "net_payout": 700
     }

6. PAYOUT HISTORY API
   GET /api/payments/tutor/payouts/
   Auth: Tutor JWT required
   - All payouts for logged in tutor
   - Paginated

eSewa Integration (Nepal):
- Use eSewa test environment
- Merchant code: EPAYTEST
- Test URL: https://rc-epay.esewa.com.np/api/epay/main/v2/form
- Success URL: http://127.0.0.1:8000/api/payments/callback/
- Failure URL: http://127.0.0.1:8000/api/payments/failed/
- Generate HMAC SHA256 signature

Follow exact API response format from API_RULES.md.
All list endpoints paginated.
Never expose raw errors.
Never return passwords or tokens.

Generate these files:
1. backend/payments/__init__.py (empty)
2. backend/payments/models.py (Payment and Payout models)
3. backend/payments/serializers.py (full file)
4. backend/payments/views.py (full file)
5. backend/payments/urls.py (full file)
6. backend/uniassist/urls.py (add payments urls)
7. backend/uniassist/settings.py (add payments to INSTALLED_APPS)
8. Migration command to run after
9. Update docs/PROGRESS.md marking Phase 5 tasks complete

---

## Phase 6 — Reviews & Reports
Read the following files completely before doing anything:
- docs/CONTEXT.md
- docs/DATABASE_SCHEMA.md
- docs/API_RULES.md

Current project status:
- Phase 1 (Environment Setup) ✅ Complete
- Phase 2 (Authentication) ✅ Complete
- Phase 3 (Tutor Discovery) ✅ Complete
- Phase 4 (Booking System) ✅ Complete
- Phase 5 (Payment Module) ✅ Complete
- Phase 6 (Review & Rating) ← Build this now

CRITICAL RULES — READ BEFORE WRITING ANY CODE:
- Do NOT add pymysql anywhere — mysqlclient 2.2.8 is the only MySQL driver
- __init__.py files must always remain empty
- Do NOT recreate models that already exist in accounts app
- Import existing models from accounts app
- Follow exact API response format from API_RULES.md
- All pricing in NPR (Nepalese Rupee)
- MySQL database: uniassist_db, user: root, password: uniassist123

Existing apps already built:
- accounts/ — User, Student, Tutor, Subject, Skill, 
               TutorAvailability, TutorDocument models
- tutors/   — Tutor discovery and profile APIs
- booking/  — Booking request, accept/reject, status APIs
- payments/ — eSewa payment, payout calculation APIs

Task:
Build the complete Review & Rating Module for UniAssist.
Create a new Django app called 'reviews' inside backend/ folder.

Requirements:

1. REVIEW MODEL
   Create Review model in reviews/models.py with fields:
   - id (PK)
   - booking FK → Booking (OneToOne — one review per booking)
   - student FK → Student
   - tutor FK → Tutor
   - rating INT (1–5, required)
   - review_text TEXT (required)
   - knowledge_rating INT NULL (1–5, optional)
   - teaching_rating INT NULL (1–5, optional)
   - communication_rating INT NULL (1–5, optional)
   - punctuality_rating INT NULL (1–5, optional)
   - created_at DATETIME auto

2. SUBMIT REVIEW API
   POST /api/reviews/submit/
   Auth: Student JWT required
   Fields: booking_id, rating, review_text,
           knowledge_rating (optional),
           teaching_rating (optional),
           communication_rating (optional),
           punctuality_rating (optional)
   Rules:
   - Only student who made the booking can review
   - Booking must have booking_status = completed
   - One review per booking (duplicate prevention)
   - Rating must be between 1 and 5
   - Sub-ratings if provided must be between 1 and 5
   - After saving review, update tutor's average rating
     on Tutor model automatically
   Response:
   {
     "success": true,
     "message": "Review submitted successfully.",
     "data": { review details }
   }

3. GET TUTOR REVIEWS API
   GET /api/reviews/tutor/<tutor_id>/
   Auth: Not required (public)
   - List all reviews for a tutor
   - Show: student full_name, rating, review_text,
     sub-ratings, created_at
   - Order by created_at descending (newest first)
   - Paginated (page, page_size query params)
   Response:
   {
     "success": true,
     "message": "Reviews retrieved successfully.",
     "data": {
       "average_rating": 4.5,
       "total_reviews": 10,
       "results": [ list of reviews ]
     }
   }

4. GET MY REVIEWS API (for logged in student)
   GET /api/reviews/my-reviews/
   Auth: Student JWT required
   - List all reviews submitted by this student
   - Show booking details alongside review
   - Paginated

5. CHECK IF REVIEWED API
   GET /api/reviews/check/<booking_id>/
   Auth: Student JWT required
   - Returns whether student has already reviewed this booking
   - Used by Android app to show/hide review button
   Response:
   {
     "success": true,
     "data": {
       "has_reviewed": true/false,
       "review": { review details if exists, null if not }
     }
   }

6. AVERAGE RATING CALCULATION
   - After every review submission, recalculate tutor average rating
   - Average = sum of all ratings / total reviews for that tutor
   - Update Tutor.punctuality_score based on punctuality_rating average
   - This must happen automatically on every review save

VALIDATION RULES:
- Booking must belong to the requesting student
- Booking status must be 'completed' before review allowed
- One review per booking — return error if already reviewed
- Rating 1-5 only — reject anything outside this range
- Sub-ratings are optional but if provided must be 1-5

Generate these files:
1. backend/reviews/__init__.py (empty)
2. backend/reviews/models.py (Review model)
3. backend/reviews/serializers.py (full file)
4. backend/reviews/views.py (full file)
5. backend/reviews/urls.py (full file)
6. backend/uniassist/urls.py (add reviews urls)
7. backend/uniassist/settings.py (add reviews to INSTALLED_APPS)
8. Migration command to run after

After generating all files, show me the migration 
command to run and list all API endpoints created.

---

## Phase 7 — Notifications
[coming soon]