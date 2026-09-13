"""
WSGI config for UniAssist project.
"""

import os
from django.core.wsgi import get_wsgi_application

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'uniassist.settings')
application = get_wsgi_application()

def _ensure_admin():
    try:
        from accounts.models import User
        email = os.getenv('DJANGO_SUPERUSER_EMAIL', 'admin@uniassist.com')
        pwd = os.getenv('DJANGO_SUPERUSER_PASSWORD', 'Admin@12345')
        if not User.objects.filter(email=email).exists():
            User.objects.create_superuser(email=email, full_name='UniAssist Admin', password=pwd)
        else:
            u = User.objects.get(email=email)
            u.set_password(pwd)
            u.is_active = True
            u.is_superuser = True
            u.is_staff = True
            u.save()
    except Exception:
        pass

_ensure_admin()
