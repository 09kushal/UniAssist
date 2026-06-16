from django.test import TestCase
from django.urls import reverse
from rest_framework import status
from rest_framework.test import APIClient
from accounts.models import User, Student, Tutor, Subject, Skill
from booking.models import Booking
from payments.models import Payment, Payout
import uuid
import hashlib
import hmac
import base64
import json
from datetime import timedelta
from django.utils import timezone

class PaymentTests(TestCase):
    def setUp(self):
        self.client = APIClient()
        
        # Create users
        self.student_user = User.objects.create_user(
            email='student@test.com',
            password='password123',
            full_name='Student User',
            role=User.Role.STUDENT,
            is_active=True
        )
        self.student = Student.objects.create(
            user=self.student_user
        )
        
        self.tutor_user = User.objects.create_user(
            email='tutor@test.com',
            password='password123',
            full_name='Tutor User',
            role=User.Role.TUTOR,
            is_active=True
        )
        self.tutor = Tutor.objects.create(
            user=self.tutor_user,
            pricing_per_session=1000
        )
        
        self.admin_user = User.objects.create_superuser(
            email='admin@test.com',
            password='password123',
            full_name='Admin User'
        )

        # Create booking
        self.booking = Booking.objects.create(
            student=self.student,
            tutor=self.tutor,
            subject_or_skill='Math',
            booking_status=Booking.BookingStatus.ACCEPTED,
            proposed_date=timezone.now().date(),
            proposed_start_time=timezone.now().time(),
            proposed_end_time=(timezone.now() + timedelta(hours=1)).time()
        )

    def test_initiate_payment(self):
        self.client.force_authenticate(user=self.student_user)
        url = reverse('payments:initiate')
        response = self.client.post(url, {'booking_id': self.booking.id})
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(response.data['success'])
        
        payment = Payment.objects.get(booking=self.booking)
        self.assertEqual(payment.payment_status, Payment.PaymentStatus.PENDING)
        self.assertEqual(payment.amount, 1000)

    def test_esewa_callback_success(self):
        # First initiate payment
        payment = Payment.objects.create(
            booking=self.booking,
            amount=self.tutor.pricing_per_session,
            payment_status=Payment.PaymentStatus.PENDING
        )
        
        transaction_uuid = f"UA-{payment.id}-ABCDEF12"
        product_code = "EPAYTEST"
        total_amount = "1000.00"
        
        message = f"total_amount={total_amount},transaction_uuid={transaction_uuid},product_code={product_code}"
        signature = base64.b64encode(hmac.new(
            b'8gBm/:&EnhH.1/q',
            message.encode('utf-8'),
            hashlib.sha256
        ).digest()).decode('utf-8')
        
        callback_data = {
            "status": "COMPLETE",
            "transaction_uuid": transaction_uuid,
            "transaction_code": "0001ZDFE",
            "total_amount": total_amount,
            "product_code": product_code,
            "signed_field_names": "total_amount,transaction_uuid,product_code",
            "signature": signature
        }
        
        url = reverse('payments:callback')
        response = self.client.post(url, callback_data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        payment.refresh_from_db()
        self.assertEqual(payment.payment_status, Payment.PaymentStatus.COMPLETED)
        
        self.booking.refresh_from_db()
        self.assertTrue(self.booking.officially_scheduled)

    def test_payment_status(self):
        payment = Payment.objects.create(
            booking=self.booking,
            amount=self.tutor.pricing_per_session,
            payment_status=Payment.PaymentStatus.COMPLETED
        )
        
        self.client.force_authenticate(user=self.student_user)
        url = reverse('payments:status', args=[self.booking.id])
        response = self.client.get(url)
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['data']['payment_status'], Payment.PaymentStatus.COMPLETED)

    def test_admin_payout_release(self):
        payment = Payment.objects.create(
            booking=self.booking,
            amount=self.tutor.pricing_per_session,
            payment_status=Payment.PaymentStatus.COMPLETED
        )
        
        self.client.force_authenticate(user=self.admin_user)
        url = reverse('payments:admin-payout-release', args=[self.booking.id])
        response = self.client.post(url, {'fine_percentage': 0.1}) # 10% fine
        
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        self.assertTrue(response.data['success'])
        
        payout = Payout.objects.get(booking=self.booking)
        self.assertEqual(payout.tutor_base_share, 700.00) # 70% of 1000
        self.assertEqual(payout.commission_amount, 300.00) # 30% of 1000
        self.assertEqual(payout.fine_amount, 70.00) # 10% of 700
        self.assertEqual(payout.tutor_final_payout, 630.00) # 700 - 70

    def test_tutor_payout_history(self):
        payment = Payment.objects.create(
            booking=self.booking,
            amount=self.tutor.pricing_per_session,
            payment_status=Payment.PaymentStatus.COMPLETED
        )
        Payout.objects.create(
            tutor=self.tutor,
            booking=self.booking,
            total_paid=1000,
            commission_amount=300,
            tutor_base_share=700,
            fine_amount=0,
            student_refund=0,
            tutor_final_payout=700,
            payout_status=Payout.PayoutStatus.RELEASED
        )
        
        self.client.force_authenticate(user=self.tutor_user)
        url = reverse('payments:tutor-payouts')
        response = self.client.get(url)
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(len(response.data['data']['results']), 1)
