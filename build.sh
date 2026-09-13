#!/usr/bin/env bash
# Exit on error
set -o errexit

echo "📦 Installing backend Python dependencies..."
pip install -r backend/requirements.txt

echo "🎨 Collecting static files with WhiteNoise..."
python backend/manage.py collectstatic --no-input

echo "🗄️ Running database migrations..."
python backend/manage.py migrate
