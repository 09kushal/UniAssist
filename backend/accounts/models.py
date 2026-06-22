"""
UniAssist — accounts/models.py

Implements:
  • User         — custom AbstractBaseUser with role field (student/tutor/admin)
  • Student      — OneToOne extension of User
  • Tutor        — OneToOne extension of User
  • TutorDocument
  • TutorAvailability
  • Subject      (Academic domain)
  • Skill        (Skill-Based domain)

All models follow DATABASE_SCHEMA.md exactly (3NF, snake_case, explicit FKs).
"""

from django.contrib.auth.models import (
    AbstractBaseUser,
    BaseUserManager,
    PermissionsMixin,
)
from django.db import models


# ─── User Manager ─────────────────────────────────────────────────────────────

class UserManager(BaseUserManager):
    """Custom manager for the UniAssist User model (email-based auth)."""

    def create_user(self, email, full_name, password=None, role='student', **extra_fields):
        if not email:
            raise ValueError('Email address is required.')
        email = self.normalize_email(email)
        user = self.model(email=email, full_name=full_name, role=role, **extra_fields)
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, full_name, password=None, **extra_fields):
        extra_fields.setdefault('is_staff', True)
        extra_fields.setdefault('is_superuser', True)
        return self.create_user(
            email=email,
            full_name=full_name,
            password=password,
            role='admin',
            **extra_fields,
        )


# ─── User ─────────────────────────────────────────────────────────────────────

class User(AbstractBaseUser, PermissionsMixin):
    """
    Central User model.
    Schema ref: DATABASE_SCHEMA.md → User
    Roles: student | tutor | admin
    """

    class Role(models.TextChoices):
        STUDENT = 'student', 'Student'
        TUTOR   = 'tutor',   'Tutor'
        ADMIN   = 'admin',   'Admin'

    full_name  = models.CharField(max_length=255)
    email      = models.EmailField(unique=True)
    device_token = models.CharField(max_length=255, blank=True, null=True)
    # password is handled by AbstractBaseUser
    role       = models.CharField(
        max_length=10,
        choices=Role.choices,
        default=Role.STUDENT,
    )
    is_active  = models.BooleanField(default=False)  # Activated after OTP verification
    is_staff   = models.BooleanField(default=False)   # Required by Django admin
    created_at = models.DateTimeField(auto_now_add=True)

    objects = UserManager()

    USERNAME_FIELD  = 'email'
    REQUIRED_FIELDS = ['full_name']

    class Meta:
        db_table = 'user'
        verbose_name = 'User'
        verbose_name_plural = 'Users'

    def __str__(self):
        return f'{self.full_name} <{self.email}> [{self.role}]'

    # ── Convenience role checks ──────────────────────────────────────────────
    @property
    def is_student(self):
        return self.role == self.Role.STUDENT

    @property
    def is_tutor(self):
        return self.role == self.Role.TUTOR

    @property
    def is_admin_user(self):
        return self.role == self.Role.ADMIN


# ─── Student ──────────────────────────────────────────────────────────────────

class Student(models.Model):
    """
    Student profile — OneToOne extension of User.
    Schema ref: DATABASE_SCHEMA.md → Student
    """

    user                  = models.OneToOneField(
        User,
        on_delete=models.CASCADE,
        related_name='student_profile',
    )
    grade_or_university   = models.CharField(max_length=255, blank=True, default='')
    subjects_of_interest  = models.CharField(max_length=500, blank=True, default='')
    profile_photo         = models.ImageField(
        upload_to='students/photos/',
        blank=True,
        null=True,
    )
    is_suspended          = models.BooleanField(default=False)
    warning_count         = models.IntegerField(default=0)

    class Meta:
        db_table = 'student'
        verbose_name = 'Student'
        verbose_name_plural = 'Students'

    def __str__(self):
        return f'Student: {self.user.full_name}'


# ─── Tutor ────────────────────────────────────────────────────────────────────

class Tutor(models.Model):
    """
    Tutor profile — OneToOne extension of User.
    Schema ref: DATABASE_SCHEMA.md → Tutor
    Domain choices: academic | skill | both  (dual-domain platform)
    """

    class Domain(models.TextChoices):
        ACADEMIC = 'academic', 'Academic'
        SKILL    = 'skill',    'Skill-Based'
        BOTH     = 'both',     'Both'

    user                  = models.OneToOneField(
        User,
        on_delete=models.CASCADE,
        related_name='tutor_profile',
    )
    domain                = models.CharField(
        max_length=10,
        choices=Domain.choices,
        default=Domain.ACADEMIC,
    )
    bio                   = models.TextField(blank=True, default='')
    pricing_per_session   = models.DecimalField(
        max_digits=10,
        decimal_places=2,
        default=0.00,
        help_text='Price in NPR (Nepalese Rupee)',
    )
    is_verified           = models.BooleanField(default=False)
    is_suspended          = models.BooleanField(default=False)
    warning_count         = models.IntegerField(default=0)
    no_show_count         = models.IntegerField(default=0)
    punctuality_score     = models.DecimalField(
        max_digits=5,
        decimal_places=2,
        default=100.00,
    )
    total_sessions_done   = models.IntegerField(default=0)
    profile_photo         = models.ImageField(
        upload_to='tutors/photos/',
        blank=True,
        null=True,
    )

    class Meta:
        db_table = 'tutor'
        verbose_name = 'Tutor'
        verbose_name_plural = 'Tutors'

    def __str__(self):
        return f'Tutor: {self.user.full_name} ({self.domain})'


# ─── TutorDocument ────────────────────────────────────────────────────────────

class TutorDocument(models.Model):
    """
    Documents submitted by tutor for admin verification.
    Schema ref: DATABASE_SCHEMA.md → TutorDocument
    Allowed file types: JPG, PNG, PDF only (enforced in serializer).
    """

    class DocType(models.TextChoices):
        PROFILE_PHOTO      = 'profile_photo',      'Profile Photo'
        CERTIFICATE        = 'certificate',        'Certificate'
        MARKSHEET          = 'marksheet',          'Marksheet'
        CITIZENSHIP        = 'citizenship',        'Citizenship'
        PORTFOLIO          = 'portfolio',          'Portfolio'
        SKILL_CERTIFICATE  = 'skill_certificate',  'Skill Certificate'

    tutor       = models.ForeignKey(
        Tutor,
        on_delete=models.CASCADE,
        related_name='documents',
    )
    doc_type    = models.CharField(max_length=20, choices=DocType.choices)
    file_path   = models.FileField(upload_to='tutors/documents/')
    uploaded_at = models.DateTimeField(auto_now_add=True)

    class Meta:
        db_table = 'tutor_document'
        verbose_name = 'Tutor Document'
        verbose_name_plural = 'Tutor Documents'

    def __str__(self):
        return f'{self.doc_type} — {self.tutor.user.full_name}'


# ─── TutorAvailability ────────────────────────────────────────────────────────

class TutorAvailability(models.Model):
    """
    Weekly availability slots set by a tutor.
    Schema ref: DATABASE_SCHEMA.md → TutorAvailability
    """

    class DayOfWeek(models.TextChoices):
        MON = 'Mon', 'Monday'
        TUE = 'Tue', 'Tuesday'
        WED = 'Wed', 'Wednesday'
        THU = 'Thu', 'Thursday'
        FRI = 'Fri', 'Friday'
        SAT = 'Sat', 'Saturday'
        SUN = 'Sun', 'Sunday'

    tutor        = models.ForeignKey(
        Tutor,
        on_delete=models.CASCADE,
        related_name='availability_slots',
    )
    day_of_week  = models.CharField(max_length=3, choices=DayOfWeek.choices)
    start_time   = models.TimeField()
    end_time     = models.TimeField()

    class Meta:
        db_table = 'tutor_availability'
        verbose_name = 'Tutor Availability'
        verbose_name_plural = 'Tutor Availabilities'

    def __str__(self):
        return (
            f'{self.tutor.user.full_name} — '
            f'{self.day_of_week} {self.start_time}–{self.end_time}'
        )


# ─── Subject (Academic Domain) ────────────────────────────────────────────────

class Subject(models.Model):
    """
    Academic subjects taught by a tutor.
    Schema ref: DATABASE_SCHEMA.md → Subject
    """

    tutor = models.ForeignKey(
        Tutor,
        on_delete=models.CASCADE,
        related_name='subjects',
    )
    name  = models.CharField(max_length=255)

    class Meta:
        db_table = 'subject'
        verbose_name = 'Subject'
        verbose_name_plural = 'Subjects'

    def __str__(self):
        return f'{self.name} (by {self.tutor.user.full_name})'


# ─── Skill (Skill-Based Domain) ───────────────────────────────────────────────

class Skill(models.Model):
    """
    Skills taught by a tutor (Skill-Based domain).
    Schema ref: DATABASE_SCHEMA.md → Skill
    """

    tutor = models.ForeignKey(
        Tutor,
        on_delete=models.CASCADE,
        related_name='skills',
    )
    name  = models.CharField(max_length=255)

    class Meta:
        db_table = 'skill'
        verbose_name = 'Skill'
        verbose_name_plural = 'Skills'

    def __str__(self):
        return f'{self.name} (by {self.tutor.user.full_name})'


# ─── OTP Record ───────────────────────────────────────────────────────────────

class OTPRecord(models.Model):
    """
    Stores a single active OTP per email address.
    Used for: registration verification, password reset.

    Rules (from API_RULES.md & CONTEXT.md):
      - OTP expires in exactly 10 minutes.
      - One OTP per email at a time — previous record is deleted before a new
        one is created (enforced at the service layer in views.py).
    """

    email      = models.EmailField(unique=True)   # One OTP per email at a time
    otp_code   = models.CharField(max_length=6)
    created_at = models.DateTimeField(auto_now_add=True)
    expires_at = models.DateTimeField()           # Set to created_at + 10 minutes

    class Meta:
        db_table     = 'otp_record'
        verbose_name = 'OTP Record'
        verbose_name_plural = 'OTP Records'

    def __str__(self):
        return f'OTP for {self.email} (expires {self.expires_at})'

    def is_valid(self):
        """Return True if the OTP has not yet expired."""
        from django.utils import timezone
        return timezone.now() <= self.expires_at
