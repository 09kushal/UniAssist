import os
import firebase_admin
from firebase_admin import credentials, messaging
from django.conf import settings
import logging

logger = logging.getLogger(__name__)

# Initialize Firebase App
try:
    if not firebase_admin._apps:
        cred_path = os.path.join(settings.BASE_DIR, 'firebase_credentials.json')
        if os.path.exists(cred_path):
            cred = credentials.Certificate(cred_path)
            firebase_admin.initialize_app(cred)
            logger.info("Firebase Admin initialized successfully.")
        else:
            logger.warning(f"Firebase credentials not found at {cred_path}")
except Exception as e:
    logger.error(f"Error initializing Firebase Admin: {e}")

def send_fcm_notification(device_token, title, body):
    if not firebase_admin._apps:
        logger.warning("Firebase not initialized. Cannot send push notification.")
        return False
        
    if not device_token:
        logger.info("No device token provided. Skipping push notification.")
        return False

    try:
        message = messaging.Message(
            notification=messaging.Notification(
                title=title,
                body=body,
            ),
            token=device_token,
        )
        response = messaging.send(message)
        logger.info(f"Successfully sent FCM message: {response}")
        return True
    except Exception as e:
        logger.error(f"Error sending FCM message: {e}")
        return False
