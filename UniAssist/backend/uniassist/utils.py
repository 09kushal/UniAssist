"""
UniAssist — Utility helpers & custom DRF exception handler.

All API responses MUST follow the envelope defined in API_RULES.md:
  Success → { "success": true,  "message": "...", "data": { } }
  Error   → { "success": false, "message": "...", "errors": { } }
"""

from rest_framework.views import exception_handler
from rest_framework.response import Response


def custom_exception_handler(exc, context):
    """
    Wrap DRF exceptions in the UniAssist standard response envelope.
    Never returns raw Django error tracebacks to the client.
    """
    response = exception_handler(exc, context)

    if response is not None:
        error_detail = response.data
        # Flatten single-key or list error structures for readability
        message = 'An error occurred.'
        if isinstance(error_detail, dict):
            if 'detail' in error_detail:
                message = str(error_detail['detail'])
            else:
                # Use first field error as the message
                first_key = next(iter(error_detail))
                first_val = error_detail[first_key]
                if isinstance(first_val, list):
                    message = str(first_val[0])
                else:
                    message = str(first_val)
        elif isinstance(error_detail, list) and error_detail:
            message = str(error_detail[0])

        response.data = {
            'success': False,
            'message': message,
            'errors': error_detail,
        }

    return response


def success_response(data=None, message='Success', status=200):
    """Helper to build a standard success envelope."""
    from rest_framework import status as drf_status
    payload = {
        'success': True,
        'message': message,
        'data': data if data is not None else {},
    }
    return Response(payload, status=status)


def error_response(message='Error', errors=None, status=400):
    """Helper to build a standard error envelope."""
    payload = {
        'success': False,
        'message': message,
        'errors': errors if errors is not None else {},
    }
    return Response(payload, status=status)
