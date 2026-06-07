# UniAssist — UI Guidelines (Android)

## Theme
- Style         : Dark navy + white + teal
- Primary color : #0B1F3A  (dark navy)
- Accent color  : #14B8A6  (teal)
- Background    : #F8F9FA  (light screens)
- Text primary  : #0B1F3A
- Text secondary: #6B7280

## Component Rules
- Buttons  : Rounded corners 12dp, teal fill, white text
- Cards    : 16dp corner radius, subtle shadow
- Inputs   : Outlined style, teal focus border
- Icons    : Material Design Icons only

## Typography
- Headings : Bold, 20–24sp
- Body     : Regular, 14–16sp
- Captions : 12sp, secondary color

## Android Screens (Build Order)
1.  Splash Screen
2.  Login Screen
3.  Register Screen (Student flow)
4.  Register Screen (Tutor flow)
5.  OTP Verification Screen
6.  Profile Setup Screen
7.  Student Dashboard
8.  Tutor Listing Screen (domain/category filters)
9.  Tutor Profile Screen
10. Booking Request Screen
11. Payment Screen (eSewa)
12. Live Class Screen (Jitsi Meet)
13. Mini Course List Screen
14. Course Detail Screen
15. Lateness Report Form
16. Reschedule Request Form
17. Notifications Screen
18. Student Profile Screen
19. Tutor Dashboard
20. Payout Breakdown Screen
21. Admin Dashboard
22. Tutor Verification Panel
23. Reports & Complaints Panel
24. Payout Release Panel

## MVVM Pattern (Mandatory)
- Every screen has: Activity/Fragment → ViewModel → Repository → Retrofit API
- No business logic inside Activity or Fragment
- Use LiveData for all UI state updates
- Use Retrofit for all API calls

## General Rules
- No hardcoded strings — use strings.xml
- No hardcoded colors — use colors.xml
- No hardcoded dimensions — use dimens.xml
- Java only — no Kotlin
- RecyclerView for all lists