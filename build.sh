#!/usr/bin/env bash
# Exit on error
set -o errexit

echo "📦 Installing backend Python dependencies..."
pip install -r backend/requirements.txt

echo "🎨 Collecting static files with WhiteNoise..."
python backend/manage.py collectstatic --no-input

echo "🗄️ Running database migrations..."
python backend/manage.py migrate

echo "👤 Ensuring Django Superuser exists..."
python backend/manage.py shell -c "
from accounts.models import User
import os
email = os.getenv('DJANGO_SUPERUSER_EMAIL', 'admin@uniassist.com')
pwd = os.getenv('DJANGO_SUPERUSER_PASSWORD', 'Admin@12345')
if not User.objects.filter(email=email).exists():
    User.objects.create_superuser(email=email, full_name='UniAssist Admin', password=pwd)
    print('Created superuser admin@uniassist.com successfully.')
else:
    u = User.objects.get(email=email)
    u.set_password(pwd)
    u.is_active = True
    u.is_superuser = True
    u.is_staff = True
    u.save()
    print('Updated superuser admin@uniassist.com credentials.')
"
