import re

with open('backend/tutors/serializers.py', 'r') as f:
    content = f.read()

# Add TutorDocument to imports
content = content.replace(
    'from accounts.models import Tutor, Subject, Skill, TutorAvailability',
    'from accounts.models import Tutor, Subject, Skill, TutorAvailability, TutorDocument'
)

# Add shared document validator
validator_code = """

def validate_document_file(file):
    ext = os.path.splitext(file.name)[1].lower()
    if ext not in ['.jpg', '.jpeg', '.png', '.pdf']:
        raise serializers.ValidationError('Only JPG, PNG, and PDF documents are accepted.')
    if file.size > MAX_PHOTO_SIZE_BYTES:
        raise serializers.ValidationError(f'Document size must not exceed {MAX_PHOTO_SIZE_MB} MB.')
    return file
"""
content = content.replace(
    '# ─── Tutor Profile Setup Serializer ───────────────────────────────────────────',
    validator_code + '\n\n# ─── Tutor Profile Setup Serializer ───────────────────────────────────────────'
)

# Add TutorDocument serializers
doc_serializers = """
# ─── Tutor Document Serializers ───────────────────────────────────────────────

class TutorDocumentSerializer(serializers.ModelSerializer):
    file_url = serializers.SerializerMethodField()

    class Meta:
        model = TutorDocument
        fields = ['id', 'doc_type', 'file_path', 'uploaded_at', 'file_url']

    def get_file_url(self, obj):
        request = self.context.get('request')
        if obj.file_path and request:
            return request.build_absolute_uri(obj.file_path.url)
        return None

class UploadTutorDocumentSerializer(serializers.ModelSerializer):
    class Meta:
        model = TutorDocument
        fields = ['doc_type', 'file_path']

    def validate_file_path(self, file):
        return validate_document_file(file)

"""
content = content.replace(
    '# ─── Add Availability Slot Input Serializer ───────────────────────────────────',
    doc_serializers + '\n# ─── Add Availability Slot Input Serializer ───────────────────────────────────'
)

with open('backend/tutors/serializers.py', 'w') as f:
    f.write(content)
