#!/usr/bin/env bash
set -o errexit

cd backend
python manage.py migrate --no-input

python manage.py shell -c "
from accounts.models import User
import os
email = os.getenv('DJANGO_SUPERUSER_EMAIL', 'admin@uniassist.com')
pwd = os.getenv('DJANGO_SUPERUSER_PASSWORD', 'Admin@12345')
try:
    if not User.objects.filter(email=email).exists():
        User.objects.create_superuser(email=email, full_name='UniAssist Admin', password=pwd)
        print('Created superuser ' + email)
    else:
        u = User.objects.get(email=email)
        u.set_password(pwd)
        u.is_superuser = True
        u.is_staff = True
        u.save()
        print('Verified superuser ' + email)
except Exception as e:
    print('Superuser setup note:', e)
"

exec gunicorn uniassist.wsgi:application --bind 0.0.0.0:$PORT
