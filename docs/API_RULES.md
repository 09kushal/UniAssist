# UniAssist — API Rules & Standards

## Response Format

### Success
{
    "success": true,
    "message": "Descriptive message",
    "data": { }
}

### Error
{
    "success": false,
    "message": "Error description",
    "errors": { }
}

## Authentication
- JWT Bearer token in Authorization header
- Format: Authorization: Bearer <access_token>
- Role-based permissions enforced on every protected endpoint
- Roles: student / tutor / admin

## HTTP Methods
- GET    : retrieve data
- POST   : create new resource
- PUT    : full update
- PATCH  : partial update
- DELETE : remove resource

## Status Codes
- 200 : OK
- 201 : Created
- 400 : Bad Request
- 401 : Unauthorized
- 403 : Forbidden
- 404 : Not Found
- 500 : Internal Server Error

## Pagination
- All list endpoints must be paginated
- page_number and page_size as query params

## OTP Rules
- OTP expires in exactly 10 minutes
- OTP sent to email via Django SMTP (Gmail)
- Used for: registration, password reset

## Booking & Payment Flow (EXACT ORDER)
1. Student sends booking request → booking_status = pending
2. Tutor accepts or rejects → booking_status = accepted | rejected
3. Student pays via eSewa → payment_status = completed
4. officially_scheduled = TRUE only after payment_status = completed
5. Sessions conducted via Jitsi Meet
6. Admin marks course complete → releases payout

## Admin Dashboard Summary API
GET /api/admin/dashboard/summary/
Auth: Admin JWT required
Response:
{
    "success": true,
    "data": {
        "total_students": 120,
        "total_tutors": 45,
        "active_bookings": 18,
        "total_platform_earnings": 45000.00
    }
}

## File Upload Rules
- Accepted types: JPG, PNG, PDF only
- Enforce validation in DRF serializer
- Store via Django media files (ImageField / FileField)

## General Rules
- Never return raw Django errors to client
- Always wrap responses in the standard envelope above
- Never expose passwords or tokens in responses
- Validate all inputs server-side