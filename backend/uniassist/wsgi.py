"""
WSGI config for UniAssist project.
"""

import os
from django.core.wsgi import get_wsgi_application

os.environ.setdefault('DJANGO_SETTINGS_MODULE', 'uniassist.settings')
application = get_wsgi_application()
