package com.kushal.uniassist;

import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.ViewOutlineProvider;
import android.view.Window;
import android.view.WindowManager;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.PaginatedResponse;
import com.kushal.uniassist.models.BookingResponse;
import com.kushal.uniassist.models.TutorResponse;
import com.kushal.uniassist.models.UnreadCountResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class StudentDashboardActivity extends AppCompatActivity {

    private static final int NOTIF_REQUEST_CODE = 200;
    private TextView tvGreeting, tvStudentName, tvBookingCount, tvSessionsDone, tvPendingCount, tvNoBookings, tvNotifBadge, tvSectionTitle, tvStudentInitial;
    private TextView btnAcademic, btnSkill, tvViewAll, tvSeeAll;
    private EditText etSearch;
    private ImageView ivSearchBtn, ivStudentPhoto, ivWhatsApp, ivInstagram, ivAppLogo;
    private LinearLayout llFacebook, llWhatsApp, llInstagram;
    private android.widget.ImageButton btnNotifications;
    private androidx.core.widget.NestedScrollView nestedScrollView;
    private CardView cardHowSearch, cardHowBook, cardHowPay, cardHowJoin, cardContactAdmin, cardReportProblem;
    private RecyclerView rvFeaturedTutors, rvRecentBookings;
    private FeaturedTutorAdapter featuredTutorAdapter;
    private BookingAdapter bookingAdapter;
    private SessionManager sessionManager;
    private ApiService apiService;
    private String currentDomain = "academic";

    private Handler autoScrollHandler = new Handler(Looper.getMainLooper());
    private Runnable autoScrollRunnable;
    private int autoScrollIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_student_dashboard);

        // Edge-to-edge & Status Bar Fix
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            window.setStatusBarColor(ContextCompat.getColor(this, R.color.primary));
        }
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(android.R.id.content), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        sessionManager = new SessionManager(this);
        apiService = ApiClient.getClient().create(ApiService.class);

        nestedScrollView = findViewById(R.id.nestedScrollView);
        tvGreeting = findViewById(R.id.tvGreeting);
        tvStudentName = findViewById(R.id.tvStudentName);
        tvStudentInitial = findViewById(R.id.tvStudentInitial);
        ivStudentPhoto = findViewById(R.id.ivStudentPhoto);
        ivAppLogo = findViewById(R.id.ivAppLogo);
        if (ivAppLogo != null) {
            ivAppLogo.setBackgroundColor(android.graphics.Color.TRANSPARENT);
        }
        tvBookingCount = findViewById(R.id.tvBookingCount);
        tvSessionsDone = findViewById(R.id.tvSessionsDone);
        tvPendingCount = findViewById(R.id.tvPendingCount);
        tvNotifBadge = findViewById(R.id.tvNotifBadge);
        btnNotifications = findViewById(R.id.btnNotifications);
        
        btnAcademic = findViewById(R.id.btnAcademic);
        btnSkill = findViewById(R.id.btnSkill);
        etSearch = findViewById(R.id.etSearch);
        ivSearchBtn = findViewById(R.id.ivSearchBtn);
        tvSectionTitle = findViewById(R.id.tvSectionTitle);
        tvViewAll = findViewById(R.id.tvViewAll);
        tvSeeAll = findViewById(R.id.tvSeeAll);
        
        rvFeaturedTutors = findViewById(R.id.rvFeaturedTutors);
        rvRecentBookings = findViewById(R.id.rvRecentBookings);
        tvNoBookings = findViewById(R.id.tvNoBookings);

        cardHowSearch = findViewById(R.id.cardHowSearch);
        cardHowBook = findViewById(R.id.cardHowBook);
        cardHowPay = findViewById(R.id.cardHowPay);
        cardHowJoin = findViewById(R.id.cardHowJoin);
        
        cardContactAdmin = findViewById(R.id.cardContactAdmin);
        llFacebook = findViewById(R.id.llFacebook);
        llWhatsApp = findViewById(R.id.llWhatsApp);
        llInstagram = findViewById(R.id.llInstagram);
        cardReportProblem = findViewById(R.id.cardReportProblem);
        CardView cardAboutUs = findViewById(R.id.cardAboutUs);
        if (cardAboutUs != null) {
            cardAboutUs.setOnClickListener(v -> showAboutDialog());
        }

        setupRecentBookings();
        setupGreeting();
        setupClickListeners();
        setupBottomNav();
        loadDashboardData();
    }

    private void setupBottomNav() {
        com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                nestedScrollView.smoothScrollTo(0, 0);
                return true;
            } else if (id == R.id.nav_categories) {
                openTutorList("");
                return true;
            } else if (id == R.id.nav_bookings) {
                startActivity(new Intent(this, MyBookingsActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, EditProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void startAutoScroll() {
        if (autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
        autoScrollRunnable = new Runnable() {
            @Override
            public void run() {
                if (featuredTutorAdapter != null && featuredTutorAdapter.getItemCount() > 0) {
                    autoScrollIndex++;
                    if (autoScrollIndex >= featuredTutorAdapter.getItemCount()) {
                        autoScrollIndex = 0;
                    }
                    rvFeaturedTutors.smoothScrollToPosition(autoScrollIndex);
                }
                autoScrollHandler.postDelayed(this, 3000);
            }
        };
        autoScrollHandler.postDelayed(autoScrollRunnable, 3000);
    }

    private void setupRecentBookings() {
        rvRecentBookings.setLayoutManager(new LinearLayoutManager(this));
        bookingAdapter = new BookingAdapter(booking -> {
            Toast.makeText(this, "Booking: " + booking.getSubjectOrSkill(), Toast.LENGTH_SHORT).show();
        });
        rvRecentBookings.setAdapter(bookingAdapter);
    }

    private void setupGreeting() {
        String name = sessionManager.getFullName();
        if (name == null) name = "Student";
        
        Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        String greeting;
        if (hour < 12) greeting = "Good Morning";
        else if (hour < 17) greeting = "Good Afternoon";
        else greeting = "Good Evening";
        
        tvGreeting.setText(greeting + ",");
        tvStudentName.setText(name);

        String photoUrl = sessionManager.getProfilePhoto();
        if (name != null && !name.isEmpty()) {
            tvStudentInitial.setText(String.valueOf(name.charAt(0)).toUpperCase());
        }
        
        if (photoUrl != null && !photoUrl.isEmpty()) {
            tvStudentInitial.setVisibility(View.GONE);
            Glide.with(this)
                .load(photoUrl)
                .circleCrop()
                .placeholder(R.drawable.bg_teal_circle)
                .into(ivStudentPhoto);
        } else {
            tvStudentInitial.setVisibility(View.VISIBLE);
        }
    }

    private void setupClickListeners() {
        btnAcademic.setOnClickListener(v -> {
            currentDomain = "academic";
            btnAcademic.setBackgroundResource(R.drawable.bg_toggle_selected);
            btnAcademic.setTextColor(Color.WHITE);
            btnSkill.setBackground(null);
            btnSkill.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvSectionTitle.setText("Academic Excellence");
            loadFeaturedTutors("academic");
        });

        btnSkill.setOnClickListener(v -> {
            currentDomain = "skill";
            btnSkill.setBackgroundResource(R.drawable.bg_toggle_selected);
            btnSkill.setTextColor(Color.WHITE);
            btnAcademic.setBackground(null);
            btnAcademic.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
            tvSectionTitle.setText("Skills Excellence");
            loadFeaturedTutors("skill");
        });

        ivSearchBtn.setOnClickListener(v -> {
            String query = etSearch.getText().toString().trim();
            Intent intent = new Intent(this, TutorListActivity.class);
            intent.putExtra("search_query", query);
            intent.putExtra("domain", currentDomain);
            startActivity(intent);
        });

        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                String query = etSearch.getText().toString().trim();
                Intent intent = new Intent(this, TutorListActivity.class);
                intent.putExtra("search_query", query);
                intent.putExtra("domain", currentDomain);
                startActivity(intent);
                return true;
            }
            return false;
        });

        etSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (featuredTutorAdapter != null) {
                    featuredTutorAdapter.getFilter().filter(s);
                }
            }
            @Override
            public void afterTextChanged(Editable s) {}
        });

        btnNotifications.setOnClickListener(v -> {
            startActivityForResult(new Intent(this, NotificationsActivity.class), NOTIF_REQUEST_CODE);
        });

        tvViewAll.setOnClickListener(v -> openTutorList(currentDomain));
        tvSeeAll.setOnClickListener(v -> startActivity(new Intent(this, MyBookingsActivity.class)));
        
        cardHowSearch.setOnClickListener(v -> {
            etSearch.requestFocus();
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(etSearch, InputMethodManager.SHOW_IMPLICIT);
            }
        });

        cardHowBook.setOnClickListener(v -> startActivity(new Intent(this, TutorListActivity.class)));

        cardHowPay.setOnClickListener(v -> startActivity(new Intent(this, MyBookingsActivity.class)));

        cardHowJoin.setOnClickListener(v -> {
            String token = "Bearer " + sessionManager.getAccessToken();
            apiService.getMyBookings(token, 1).enqueue(new Callback<ApiResponse<PaginatedResponse<BookingResponse>>>() {
                @Override
                public void onResponse(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Response<ApiResponse<PaginatedResponse<BookingResponse>>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        List<BookingResponse> bookings = response.body().getData().getResults();
                        BookingResponse scheduledBooking = null;
                        for (BookingResponse b : bookings) {
                            if (b.getBookingStatus().equalsIgnoreCase("accepted") && b.isOfficiallyScheduled()) {
                                scheduledBooking = b;
                                break;
                            }
                        }

                        if (scheduledBooking != null) {
                            // Set pending review flag
                            sessionManager.setPendingReviewBookingId(scheduledBooking.getId());

                            Intent intent = new Intent(StudentDashboardActivity.this, JoinSessionActivity.class);
                            intent.putExtra("booking_id", scheduledBooking.getId());
                            intent.putExtra("user_full_name", sessionManager.getFullName());
                            intent.putExtra("user_role", "student");
                            startActivity(intent);
                        } else {
                            boolean hasAccepted = false;
                            for (BookingResponse b : bookings) {
                                if (b.getBookingStatus().equalsIgnoreCase("accepted")) {
                                    hasAccepted = true;
                                    break;
                                }
                            }

                            if (hasAccepted) {
                                new AlertDialog.Builder(StudentDashboardActivity.this)
                                        .setTitle("Payment Required")
                                        .setMessage("Please complete the payment to join your session.")
                                        .setPositiveButton("Go to Bookings", (d, w) -> startActivity(new Intent(StudentDashboardActivity.this, MyBookingsActivity.class)))
                                        .setNegativeButton("Cancel", null)
                                        .show();
                            } else {
                                Toast.makeText(StudentDashboardActivity.this, "No scheduled sessions yet.\nBook a tutor first!", Toast.LENGTH_LONG).show();
                            }
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Throwable t) {
                    Toast.makeText(StudentDashboardActivity.this, "Please go to My Bookings to join your session", Toast.LENGTH_LONG).show();
                }
            });
        });

        cardContactAdmin.setOnClickListener(v -> Toast.makeText(this, "Contact: admin@uniassist.com", Toast.LENGTH_LONG).show());
        
        llFacebook.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://facebook.com/uniassist"));
            startActivity(intent);
        });

        llWhatsApp.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://wa.me/977XXXXXXXXXX"));
            startActivity(intent);
        });

        llInstagram.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse("https://instagram.com/uniassist"));
            startActivity(intent);
        });

        cardReportProblem.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_SENDTO);
            intent.setData(Uri.parse("mailto:"));
            intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"support@uniassist.com"});
            intent.putExtra(Intent.EXTRA_SUBJECT, "Problem Report - UniAssist App");
            startActivity(Intent.createChooser(intent, "Report Problem"));
        });
    }

    private void openTutorList(String domain) {
        Intent intent = new Intent(this, TutorListActivity.class);
        intent.putExtra("domain", domain);
        startActivity(intent);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (autoScrollRunnable != null) {
            autoScrollHandler.removeCallbacks(autoScrollRunnable);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadNotificationCount();
        loadDashboardData();
        startAutoScroll();
        checkPendingReview();
    }

    private void checkPendingReview() {
        int bookingId = sessionManager.getPendingReviewBookingId();
        if (bookingId != -1) {
            // Clear flag immediately
            sessionManager.clearPendingReview();

            // Check if already reviewed
            String authHeader = "Bearer " + sessionManager.getAccessToken();
            apiService.checkReview(authHeader, bookingId).enqueue(new Callback<ApiResponse<com.kushal.uniassist.models.ReviewCheckResponse>>() {
                @Override
                public void onResponse(Call<ApiResponse<com.kushal.uniassist.models.ReviewCheckResponse>> call, Response<ApiResponse<com.kushal.uniassist.models.ReviewCheckResponse>> response) {
                    if (response.isSuccessful() && response.body() != null && response.body().isSuccess()) {
                        if (!response.body().getData().isHasReviewed()) {
                            // Show review screen
                            Intent intent = new Intent(StudentDashboardActivity.this, SubmitReviewActivity.class);
                            intent.putExtra("booking_id", bookingId);
                            startActivity(intent);
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<com.kushal.uniassist.models.ReviewCheckResponse>> call, Throwable t) {
                    Log.e("Dashboard", "Review check failed", t);
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == NOTIF_REQUEST_CODE) {
            loadNotificationCount();
        }
    }

    private void loadNotificationCount() {
        String token = "Bearer " + sessionManager.getAccessToken();
        apiService.getUnreadCount(token).enqueue(new Callback<ApiResponse<UnreadCountResponse>>() {
            @Override
            public void onResponse(Call<ApiResponse<UnreadCountResponse>> call, Response<ApiResponse<UnreadCountResponse>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    updateNotifBadge(response.body().getData().getUnreadCount());
                }
            }

            @Override
            public void onFailure(Call<ApiResponse<UnreadCountResponse>> call, Throwable t) {
                Log.e("Dashboard", "Notif count failed");
            }
        });
    }

    private void updateNotifBadge(int count) {
        if (count > 0) {
            tvNotifBadge.setVisibility(View.VISIBLE);
            tvNotifBadge.setText(count > 99 ? "99+" : String.valueOf(count));
        } else {
            tvNotifBadge.setVisibility(View.GONE);
        }
    }

    private void loadDashboardData() {
        loadFeaturedTutors(currentDomain);
        loadBookingStats();
    }

    private void loadFeaturedTutors(String domain) {
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.getTutorList(authHeader, domain, 1).enqueue(new Callback<ApiResponse<PaginatedResponse<TutorResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginatedResponse<TutorResponse>>> call, Response<ApiResponse<PaginatedResponse<TutorResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    setupFeaturedTutors(response.body().getData().getResults());
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<PaginatedResponse<TutorResponse>>> call, Throwable t) {}
        });
    }

    private void loadBookingStats() {
        String authHeader = "Bearer " + sessionManager.getAccessToken();
        apiService.getMyBookings(authHeader, 1).enqueue(new Callback<ApiResponse<PaginatedResponse<BookingResponse>>>() {
            @Override
            public void onResponse(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Response<ApiResponse<PaginatedResponse<BookingResponse>>> response) {
                if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                    List<BookingResponse> bookings = response.body().getData().getResults();
                    tvBookingCount.setText(String.valueOf(bookings.size()));
                    
                    int done = 0;
                    int pending = 0;
                    for (BookingResponse b : bookings) {
                        if ("completed".equalsIgnoreCase(b.getBookingStatus())) done++;
                        else if ("pending".equalsIgnoreCase(b.getBookingStatus())) pending++;
                    }
                    tvSessionsDone.setText(String.valueOf(done));
                    tvPendingCount.setText(String.valueOf(pending));

                    if (bookings.isEmpty()) {
                        tvNoBookings.setVisibility(View.VISIBLE);
                        rvRecentBookings.setVisibility(View.GONE);
                    } else {
                        tvNoBookings.setVisibility(View.GONE);
                        rvRecentBookings.setVisibility(View.VISIBLE);
                        List<BookingResponse> recent = bookings.size() > 5 ? bookings.subList(0, 5) : bookings;
                        bookingAdapter.updateList(recent);
                    }
                }
            }
            @Override
            public void onFailure(Call<ApiResponse<PaginatedResponse<BookingResponse>>> call, Throwable t) {}
        });
    }

    private void setupFeaturedTutors(List<TutorResponse> tutors) {
        rvFeaturedTutors.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        featuredTutorAdapter = new FeaturedTutorAdapter(tutors, tutor -> {
            Intent intent = new Intent(this, TutorProfileActivity.class);
            intent.putExtra("tutor_id", tutor.getId());
            startActivity(intent);
        });
        rvFeaturedTutors.setAdapter(featuredTutorAdapter);
    }

    private void showAboutDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("About UniAssist")
                .setMessage("UniAssist is Nepal's premier online mentorship and learning platform connecting students with verified tutors across academic and skill-based domains.\n\nLearn • Connect • Succeed\n\nVersion 1.0.0\nMade in Nepal 🇳🇵")
                .setPositiveButton("Close", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
