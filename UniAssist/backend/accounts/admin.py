from django.contrib import admin
from django.contrib.auth.admin import UserAdmin as BaseUserAdmin

from .models import (
    User,
    Student,
    Tutor,
    TutorDocument,
    TutorAvailability,
    Subject,
    Skill,
)


@admin.register(User)
class UserAdmin(BaseUserAdmin):
    list_display  = ('email', 'full_name', 'role', 'is_active', 'is_staff', 'created_at')
    list_filter   = ('role', 'is_active', 'is_staff')
    search_fields = ('email', 'full_name')
    ordering      = ('-created_at',)

    fieldsets = (
        (None,           {'fields': ('email', 'password')}),
        ('Personal Info',{'fields': ('full_name', 'role')}),
        ('Permissions',  {'fields': ('is_active', 'is_staff', 'is_superuser', 'groups', 'user_permissions')}),
        ('Timestamps',   {'fields': ('created_at',)}),
    )
    readonly_fields = ('created_at',)

    add_fieldsets = (
        (None, {
            'classes': ('wide',),
            'fields':  ('email', 'full_name', 'role', 'password1', 'password2'),
        }),
    )


@admin.register(Student)
class StudentAdmin(admin.ModelAdmin):
    list_display  = ('user', 'grade_or_university', 'is_suspended', 'warning_count')
    list_filter   = ('is_suspended',)
    search_fields = ('user__email', 'user__full_name')


@admin.register(Tutor)
class TutorAdmin(admin.ModelAdmin):
    list_display  = ('user', 'domain', 'pricing_per_session', 'is_verified', 'is_suspended')
    list_filter   = ('domain', 'is_verified', 'is_suspended')
    search_fields = ('user__email', 'user__full_name')


@admin.register(TutorDocument)
class TutorDocumentAdmin(admin.ModelAdmin):
    list_display  = ('tutor', 'doc_type', 'uploaded_at')
    list_filter   = ('doc_type',)


@admin.register(TutorAvailability)
class TutorAvailabilityAdmin(admin.ModelAdmin):
    list_display = ('tutor', 'day_of_week', 'start_time', 'end_time')


@admin.register(Subject)
class SubjectAdmin(admin.ModelAdmin):
    list_display = ('name', 'tutor')
    search_fields = ('name',)


@admin.register(Skill)
class SkillAdmin(admin.ModelAdmin):
    list_display = ('name', 'tutor')
    search_fields = ('name',)
