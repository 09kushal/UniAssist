import os
import django

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'uniassist.settings')
django.setup()

from rest_framework.test import APIClient
from accounts.models import User, Tutor, Student, TutorAvailability
from booking.models import Booking, Session
import datetime
from django.utils import timezone

client = APIClient()

tutor_user = User.objects.filter(role='TUTOR').first()
student_user = User.objects.filter(role='STUDENT').first()
tutor_profile = tutor_user.tutor_profile
student_profile = student_user.student_profile

slot, _ = TutorAvailability.objects.get_or_create(
    tutor=tutor_profile, 
    day_of_week='Mon', 
    start_time=datetime.time(10, 0), 
    end_time=datetime.time(11, 0)
)

print("\n=== PART 3: JOIN TOKEN ===")
paid_booking = Booking.objects.create(
    student=student_profile,
    tutor=tutor_profile,
    selected_slot=slot,
    subject_or_skill="Physics",
    proposed_date=timezone.now().date(),
    proposed_start_time=datetime.time(10, 0),
    proposed_end_time=datetime.time(11, 0),
    booking_status='scheduled',
    officially_scheduled=True
)

Session.objects.create(
    booking=paid_booking,
    scheduled_at=timezone.now() + datetime.timedelta(minutes=5),
    duration_minutes=60,
    status='SCHEDULED'
)

client.force_authenticate(user=student_user)
response = client.post(f'/api/booking/{paid_booking.id}/join-token/')
print(f"Status: {response.status_code}")
print(f"Body: {response.content.decode()}")

