package com.kushal.uniassist;

import android.Manifest;
import android.app.AlertDialog;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.bumptech.glide.Glide;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.AvailabilityRequest;
import com.kushal.uniassist.models.AvailabilitySlot;
import com.kushal.uniassist.models.SkillRequest;
import com.kushal.uniassist.models.SkillResponse;
import com.kushal.uniassist.models.SubjectRequest;
import com.kushal.uniassist.models.SubjectResponse;
import com.kushal.uniassist.models.TutorProfileRequest;
import com.kushal.uniassist.models.TutorResponse;
import com.kushal.uniassist.models.TutorDocument;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TutorEditProfileActivity extends AppCompatActivity {

    private static final int REQUEST_IMAGE_PICK = 100;
    private static final int REQUEST_IMAGE_PERMISSION = 101;
    private static final int REQUEST_DOC_PICK = 102;

    private ImageView ivBack, ivProfile;
    private View rlPhoto;
    private EditText etBio, etPricing;
    private Chip chipDomain;
    private ChipGroup chipGroupSubjects, chipGroupSkills;
    private LinearLayout llAvailabilityList, llSubjectsSection, llSkillsSection, llDocumentList;
    private Button btnAddSubject, btnAddSkill, btnAddAvailability, btnSave, btnUploadDocument;
    private ProgressBar progressBar;
    private Spinner spinnerDocType;
    private String[] docTypeValues = {"certificate", "marksheet", "citizenship", "portfolio", "skill_certificate"};
    private String[] docTypeDisplay = {"Certificate", "Marksheet", "Citizenship", "Portfolio", "Skill Certificate"};

    private ApiService apiService;
    private SessionManager sessionManager;
    private TutorResponse currentProfile;
    private Uri selectedImageUri;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tutor_edit_profile);

        // Status Bar Color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        }

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        ivBack = findViewById(R.id.ivBack);
        ivProfile = findViewById(R.id.ivProfile);
        rlPhoto = findViewById(R.id.rlPhoto);
        etBio = findViewById(R.id.etBio);
        etPricing = findViewById(R.id.etPricing);
        chipDomain = findViewById(R.id.chipDomain);
        chipGroupSubjects = findViewById(R.id.chipGroupSubjects);
        chipGroupSkills = findViewById(R.id.chipGroupSkills);
        llAvailabilityList = findViewById(R.id.llAvailabilityList);
        llSubjectsSection = findViewById(R.id.llSubjectsSection);
        llSkillsSection = findViewById(R.id.llSkillsSection);
        btnAddSubject = findViewById(R.id.btnAddSubject);
        btnAddSkill = findViewById(R.id.btnAddSkill);
        btnAddAvailability = findViewById(R.id.btnAddAvailability);
        btnSave = findViewById(R.id.btnSave);
        progressBar = findViewById(R.id.progressBar);
        spinnerDocType = findViewById(R.id.spinnerDocType);
        btnUploadDocument = findViewById(R.id.btnUploadDocument);
        llDocumentList = findViewById(R.id.llDocumentList);

        if (spinnerDocType != null) {
            spinnerDocType.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, docTypeDisplay));
        }

        ivBack.setOnClickListener(v -> finish());
        rlPhoto.setOnClickListener(v -> checkPermissionsAndPickImage());
        btnAddSubject.setOnClickListener(v -> showAddSubjectDialog());
        btnAddSkill.setOnClickListener(v -> showAddSkillDialog());
        btnAddAvailability.setOnClickListener(v -> showAddAvailabilityDialog());
        btnSave.setOnClickListener(v -> saveProfile());
        if (btnUploadDocument != null) {
            btnUploadDocument.setOnClickListener(v -> openDocPicker());
        }

        loadProfile();
        loadDocuments();
    }

    private void loadProfile() {
        showLoading(true);
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.getMyTutorProfile(authHeader).enqueue(new Callback<ApiResponse<TutorResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<TutorResponse>> call, Response<ApiResponse<TutorResponse>> response) {
                showLoading(false);
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    currentProfile = response.body().getData();
                    populateData();
                } else {
                    Toast.makeText(TutorEditProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<TutorResponse>> call, Throwable t) {
                showLoading(false);
                Toast.makeText(TutorEditProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void populateData() {
        etBio.setText(currentProfile.getBio());
        etPricing.setText(currentProfile.getPricingPerSession());
        chipDomain.setText(currentProfile.getDomain());

        String domain = currentProfile.getDomain().toLowerCase();
        llSubjectsSection.setVisibility(domain.contains("academic") || domain.contains("both") ? View.VISIBLE : View.GONE);
        llSkillsSection.setVisibility(domain.contains("skill") || domain.contains("both") ? View.VISIBLE : View.GONE);

        Glide.with(this)
                .load(currentProfile.getProfilePhotoUrl())
                .placeholder(R.drawable.ic_tutor_placeholder)
                .circleCrop()
                .into(ivProfile);

        updateChips();
        updateAvailabilityList();
    }

    private void updateChips() {
        chipGroupSubjects.removeAllViews();
        for (com.kushal.uniassist.models.SubjectResponse subject : currentProfile.getSubjects()) {
            Chip chip = new Chip(this);
            chip.setText(subject.getName());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> removeSubject(subject.getId()));
            chipGroupSubjects.addView(chip);
        }

        chipGroupSkills.removeAllViews();
        for (com.kushal.uniassist.models.SkillResponse skill : currentProfile.getSkills()) {
            Chip chip = new Chip(this);
            chip.setText(skill.getName());
            chip.setCloseIconVisible(true);
            chip.setOnCloseIconClickListener(v -> removeSkill(skill.getId()));
            chipGroupSkills.addView(chip);
        }
    }

    private void updateAvailabilityList() {
        llAvailabilityList.removeAllViews();
        List<AvailabilitySlot> slots = currentProfile.getAvailabilitySlots();
        if (slots != null) {
            for (AvailabilitySlot slot : slots) {
                View view = LayoutInflater.from(this).inflate(R.layout.item_availability_tutor_edit, llAvailabilityList, false);
                TextView tvSlot = view.findViewById(R.id.tvSlotInfo);
                ImageView ivDelete = view.findViewById(R.id.ivDelete);

                tvSlot.setText(slot.getDayOfWeek() + ": " + slot.getStartTime() + " - " + slot.getEndTime());
                ivDelete.setOnClickListener(v -> removeAvailability(slot.getId()));
                llAvailabilityList.addView(view);
            }
        }
    }

    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        intent.setType("image/*");
        startActivityForResult(intent, REQUEST_IMAGE_PICK);
    }

    private void checkPermissionsAndPickImage() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                        REQUEST_IMAGE_PERMISSION);
                return;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        REQUEST_IMAGE_PERMISSION);
                return;
            }
        }
        openImagePicker();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_IMAGE_PERMISSION) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openImagePicker();
            } else {
                Toast.makeText(this, "Permission needed to select photo", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_PICK && resultCode == RESULT_OK && data != null) {
            selectedImageUri = data.getData();
            Log.d("TutorEdit", "Image selected: " + selectedImageUri);
            Glide.with(this).load(selectedImageUri).circleCrop().into(ivProfile);
            Toast.makeText(this, "Photo selected. Will be uploaded on save.", Toast.LENGTH_SHORT).show();
        } else if (requestCode == REQUEST_DOC_PICK && resultCode == RESULT_OK && data != null) {
            Uri docUri = data.getData();
            uploadDocument(docUri);
        }
    }

    private void saveProfile() {
        String bio = etBio.getText().toString().trim();
        String pricing = etPricing.getText().toString().trim();

        if (bio.isEmpty() || pricing.isEmpty()) {
            Toast.makeText(this, "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);
        String authHeader = "Bearer " + sessionManager.getAccessToken();

        if (selectedImageUri != null) {
            try {
                Bitmap bitmap = MediaStore.Images.Media.getBitmap(getContentResolver(), selectedImageUri);
                ByteArrayOutputStream bos = new ByteArrayOutputStream();
                bitmap.compress(Bitmap.CompressFormat.JPEG, 70, bos);
                RequestBody requestFile = RequestBody.create(MediaType.parse("image/jpeg"), bos.toByteArray());
                MultipartBody.Part photo = MultipartBody.Part.createFormData("profile_photo", "photo.jpg", requestFile);

                RequestBody bioBody = RequestBody.create(MediaType.parse("text/plain"), bio);
                RequestBody pricingBody = RequestBody.create(MediaType.parse("text/plain"), pricing);

                apiService.setupTutorProfileWithPhoto(authHeader, bioBody, pricingBody, photo).enqueue(new Callback<ApiResponse<TutorResponse>>() {
                    @Override
                    public void onResponse(Call<ApiResponse<TutorResponse>> call, Response<ApiResponse<TutorResponse>> response) {
                        handleSaveResponse(response);
                    }

                    @Override
                    public void onFailure(Call<ApiResponse<TutorResponse>> call, Throwable t) {
                        showLoading(false);
                        Toast.makeText(TutorEditProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (IOException e) {
                showLoading(false);
                e.printStackTrace();
            }
        } else {
            TutorProfileRequest request = new TutorProfileRequest(bio, pricing);
            apiService.setupTutorProfile(authHeader, request).enqueue(new Callback<ApiResponse<TutorResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<TutorResponse>> call, Response<ApiResponse<TutorResponse>> response) {
                    handleSaveResponse(response);
                }

                @Override
                public void onFailure(Call<ApiResponse<TutorResponse>> call, Throwable t) {
                    showLoading(false);
                    Toast.makeText(TutorEditProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void handleSaveResponse(Response<ApiResponse<TutorResponse>> response) {
        showLoading(false);
        if (response.isSuccessful()) {
            Toast.makeText(TutorEditProfileActivity.this, "Profile saved successfully", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            try {
                String error = response.errorBody().string();
                Log.e("TutorEdit", "Save error: " + error);
                Toast.makeText(TutorEditProfileActivity.this, "Save failed: " + error, Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Toast.makeText(TutorEditProfileActivity.this, "Save failed", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showAddSubjectDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Subject");
        final EditText input = new EditText(this);
        input.setHint("e.g. Mathematics");
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) addSubject(name);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addSubject(String name) {
        showLoading(true);
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.addSubject(authHeader, new SubjectRequest(name)).enqueue(new Callback<ApiResponse<SubjectResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SubjectResponse>> call, Response<ApiResponse<SubjectResponse>> response) {
                if (response.isSuccessful()) {
                    loadProfile();
                } else {
                    showLoading(false);
                    Toast.makeText(TutorEditProfileActivity.this, "Failed to add", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SubjectResponse>> call, Throwable t) {
                showLoading(false);
            }
        });
    }

    private void removeSubject(int id) {
        showLoading(true);
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.removeSubject(authHeader, id).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    loadProfile();
                } else {
                    showLoading(false);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                showLoading(false);
            }
        });
    }

    private void showAddSkillDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Add Skill");
        final EditText input = new EditText(this);
        input.setHint("e.g. Photoshop");
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String name = input.getText().toString().trim();
            if (!name.isEmpty()) addSkill(name);
        });
        builder.setNegativeButton("Cancel", null);
        builder.show();
    }

    private void addSkill(String name) {
        showLoading(true);
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.addSkill(authHeader, new SkillRequest(name)).enqueue(new Callback<ApiResponse<SkillResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<SkillResponse>> call, Response<ApiResponse<SkillResponse>> response) {
                if (response.isSuccessful()) {
                    loadProfile();
                } else {
                    showLoading(false);
                    Toast.makeText(TutorEditProfileActivity.this, "Failed to add", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<SkillResponse>> call, Throwable t) {
                showLoading(false);
            }
        });
    }

    private void removeSkill(int id) {
        showLoading(true);
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.removeSkill(authHeader, id).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    loadProfile();
                } else {
                    showLoading(false);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                showLoading(false);
            }
        });
    }

    private void showAddAvailabilityDialog() {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_add_availability, null);
        Spinner spinnerDay = view.findViewById(R.id.spinnerDay);
        TextView tvStartTime = view.findViewById(R.id.tvStartTime);
        TextView tvEndTime = view.findViewById(R.id.tvEndTime);

        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        spinnerDay.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, days));

        tvStartTime.setOnClickListener(v -> showTimePicker(tvStartTime));
        tvEndTime.setOnClickListener(v -> showTimePicker(tvEndTime));

        new AlertDialog.Builder(this)
                .setTitle("Add Availability Slot")
                .setView(view)
                .setPositiveButton("Add", (dialog, which) -> {
                    String day = spinnerDay.getSelectedItem().toString();
                    String start = tvStartTime.getText().toString();
                    String end = tvEndTime.getText().toString();
                    
                    if (!start.equals("Start Time") && !end.equals("End Time")) {
                        // Ensure HH:mm:ss
                        if (start.length() == 5) start += ":00";
                        if (end.length() == 5) end += ":00";
                        addAvailability(day, start, end);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showTimePicker(TextView textView) {
        Calendar c = Calendar.getInstance();
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            textView.setText(String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute));
        }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), true).show();
    }

    private void addAvailability(String day, String start, String end) {
        Log.d("TutorEdit", "Adding availability...");
        Log.d("TutorEdit", "Day: " + day);
        Log.d("TutorEdit", "Start: " + start);
        Log.d("TutorEdit", "End: " + end);

        showLoading(true);
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.addAvailability(authHeader, new AvailabilityRequest(day, start, end)).enqueue(new Callback<ApiResponse<AvailabilitySlot>>() {
            @Override
            public void onResponse(Call<ApiResponse<AvailabilitySlot>> call, Response<ApiResponse<AvailabilitySlot>> response) {
                if (response.isSuccessful()) {
                    loadProfile();
                } else {
                    showLoading(false);
                    try {
                        String error = response.errorBody().string();
                        Log.e("TutorEdit", "Availability error: " + error);
                        Toast.makeText(TutorEditProfileActivity.this, "Error: " + error, Toast.LENGTH_LONG).show();
                    } catch (Exception e) {
                        Toast.makeText(TutorEditProfileActivity.this, "Failed to add slot", Toast.LENGTH_SHORT).show();
                    }
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<AvailabilitySlot>> call, Throwable t) {
                showLoading(false);
                Log.e("TutorEdit", "Add availability failure: " + t.getMessage());
            }
        });
    }

    private void removeAvailability(int id) {
        showLoading(true);
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.removeAvailability(authHeader, id).enqueue(new Callback<ApiResponse<Object>>() {
            @Override
            public void onResponse(Call<ApiResponse<Object>> call, Response<ApiResponse<Object>> response) {
                if (response.isSuccessful()) {
                    loadProfile();
                } else {
                    showLoading(false);
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<Object>> call, Throwable t) {
                showLoading(false);
            }
        });
    }

    private void showLoading(boolean loading) {
        progressBar.setVisibility(loading ? View.VISIBLE : View.GONE);
        btnSave.setEnabled(!loading);
    }

    private void loadDocuments() {
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.getTutorDocuments(authHeader).enqueue(new Callback<ApiResponse<List<TutorDocument>>>() {
            @Override
            public void onResponse(Call<ApiResponse<List<TutorDocument>>> call, Response<ApiResponse<List<TutorDocument>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    updateDocumentList(response.body().getData());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<List<TutorDocument>>> call, Throwable t) {}
        });
    }

    private void updateDocumentList(List<TutorDocument> docs) {
        if (llDocumentList == null) return;
        llDocumentList.removeAllViews();
        for (TutorDocument doc : docs) {
            View view = LayoutInflater.from(this).inflate(R.layout.item_document_tutor_edit, llDocumentList, false);
            TextView tvType = view.findViewById(R.id.tvDocType);
            TextView tvDate = view.findViewById(R.id.tvUploadDate);
            ImageView ivIcon = view.findViewById(R.id.ivDocIcon);

            String typeDisplay = doc.getDocType();
            // Map code to display name
            for (int i = 0; i < docTypeValues.length; i++) {
                if (docTypeValues[i].equals(doc.getDocType())) {
                    typeDisplay = docTypeDisplay[i];
                    break;
                }
            }

            tvType.setText(typeDisplay);
            tvDate.setText("Uploaded on: " + doc.getUploadedAt());
            
            llDocumentList.addView(view);
        }
    }

    private void openDocPicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        String[] mimetypes = {"image/*", "application/pdf"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimetypes);
        startActivityForResult(intent, REQUEST_DOC_PICK);
    }

    private void uploadDocument(Uri docUri) {
        showLoading(true);
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        int selectedIndex = spinnerDocType.getSelectedItemPosition();
        String selectedDocType = docTypeValues[selectedIndex];

        try {
            InputStream is = getContentResolver().openInputStream(docUri);
            byte[] bytes = new byte[is.available()];
            is.read(bytes);
            is.close();

            String mimeType = getContentResolver().getType(docUri);
            if (mimeType == null) mimeType = "application/pdf";
            String ext = mimeType.contains("image") ? ".jpg" : ".pdf";

            RequestBody requestFile = RequestBody.create(MediaType.parse(mimeType), bytes);
            MultipartBody.Part filePart = MultipartBody.Part.createFormData("file_path", "doc" + ext, requestFile);
            RequestBody docTypeBody = RequestBody.create(MediaType.parse("text/plain"), selectedDocType);

            apiService.uploadTutorDocument(authHeader, docTypeBody, filePart).enqueue(new Callback<ApiResponse<TutorDocument>>() {
                @Override
                public void onResponse(Call<ApiResponse<TutorDocument>> call, Response<ApiResponse<TutorDocument>> response) {
                    showLoading(false);
                    if (response.isSuccessful()) {
                        Toast.makeText(TutorEditProfileActivity.this, "Document uploaded", Toast.LENGTH_SHORT).show();
                        loadDocuments();
                    } else {
                        Toast.makeText(TutorEditProfileActivity.this, "Upload failed", Toast.LENGTH_SHORT).show();
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<TutorDocument>> call, Throwable t) {
                    showLoading(false);
                    Toast.makeText(TutorEditProfileActivity.this, "Network error", Toast.LENGTH_SHORT).show();
                }
            });
        } catch (Exception e) {
            showLoading(false);
            e.printStackTrace();
            Toast.makeText(this, "Error processing file", Toast.LENGTH_SHORT).show();
        }
    }
}
