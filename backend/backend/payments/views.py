
class DemoPaymentCompleteView(APIView):
    """
    POST /api/payments/demo-complete/
    Auth: Student JWT required.
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        from decimal import Decimal
        booking_id = request.data.get('booking_id')

        if not booking_id:
            return error_response(
                message="booking_id required",
                status=status.HTTP_400_BAD_REQUEST
            )

        try:
            booking = Booking.objects.get(
                id=booking_id,
                student__user=request.user
            )

            # Only accepted bookings can be paid
            if booking.booking_status != Booking.BookingStatus.ACCEPTED:
                return error_response(
                    message=f"Booking is not in accepted status. Current: {booking.booking_status}",
                    status=status.HTTP_400_BAD_REQUEST
                )

            # Create or update payment
            payment, created = Payment.objects.get_or_create(
                booking=booking,
                defaults={
                    'amount': booking.tutor.pricing_per_session,
                    'payment_status': Payment.PaymentStatus.COMPLETED,
                    'esewa_ref_id': f'DEMO-{uuid.uuid4().hex[:6].upper()}',
                    'paid_at': timezone.now()
                }
            )

            if not created:
                payment.payment_status = Payment.PaymentStatus.COMPLETED
                payment.esewa_ref_id = f'DEMO-{uuid.uuid4().hex[:6].upper()}'
                payment.paid_at = timezone.now()
                payment.save()

            # Mark officially scheduled
            booking.officially_scheduled = True
            booking.save()

            # Create Pending Payout
            create_pending_payout(booking, payment.amount)

            # Create Session for demo
            try:
                scheduled_dt = datetime.datetime.combine(
                    booking.proposed_date,
                    booking.proposed_start_time
                )
                if timezone.is_naive(scheduled_dt):
                    scheduled_dt = timezone.make_aware(scheduled_dt)

                Session.objects.get_or_create(
                    booking=booking,
                    defaults={
                        'scheduled_at': scheduled_dt,
                        'officially_scheduled': True,
                        'session_status': Session.SessionStatus.PENDING
                    }
                )
            except Exception:
                pass

            from notifications.services import notify_payment_confirmed
            notify_payment_confirmed(booking)

            return success_response(
                message="Payment marked complete. Session scheduled and Payout pending.",
                data={
                    "booking_id": booking.id,
                    "officially_scheduled": True,
                    "payment_status": "completed"
                }
            )

        except Booking.DoesNotExist:
            return error_response(
                message="Booking not found",
                status=status.HTTP_404_NOT_FOUND
            )
        except Exception as e:
            return error_response(
                message=str(e),
                status=status.HTTP_500_INTERNAL_SERVER_ERROR
            )
