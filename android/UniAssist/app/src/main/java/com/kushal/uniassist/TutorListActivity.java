package com.kushal.uniassist;

import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.chip.ChipGroup;
import com.kushal.uniassist.models.ApiResponse;
import com.kushal.uniassist.models.PaginatedResponse;
import com.kushal.uniassist.models.TutorResponse;
import com.kushal.uniassist.network.ApiClient;
import com.kushal.uniassist.network.ApiService;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class TutorListActivity extends AppCompatActivity {

    private static final String TAG = "TutorList";
    private static final int PAGE_SIZE = 10;
    private RecyclerView rvTutors;
    private TutorAdapter adapter;
    private List<TutorResponse> allTutors = new ArrayList<>();
    private List<TutorResponse> filteredTutors = new ArrayList<>();
    private ProgressBar progressBar;
    private TextView tvEmpty;
    private ChipGroup chipGroupFilters;
    private EditText etSearch;
    private ImageView ivBack;
    private SessionManager sessionManager;
    private TextView tvResultCount;
    private SwipeRefreshLayout swipeRefresh;
    private LinearLayout emptyStateLayout;
    
    private int currentPage = 1;
    private int totalPages = 1;
    private boolean isLoading = false;
    private boolean isLastPage = false;
    private String currentDomain = "";
    private String searchQuery = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            setContentView(R.layout.activity_tutor_list);

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

            if (sessionManager == null) {
                sessionManager = new SessionManager(this);
            }
            
            rvTutors = findViewById(R.id.rvTutors);
            progressBar = findViewById(R.id.progressBar);
            tvEmpty = findViewById(R.id.tvEmpty);
            chipGroupFilters = findViewById(R.id.chipGroupFilters);
            etSearch = findViewById(R.id.etSearch);
            ivBack = findViewById(R.id.ivBack);
            tvResultCount = findViewById(R.id.tvResultCount);
            swipeRefresh = findViewById(R.id.swipeRefresh);
            emptyStateLayout = findViewById(R.id.emptyStateLayout);

            if (ivBack != null) ivBack.setOnClickListener(v -> finish());
            
            if (swipeRefresh != null) {
                swipeRefresh.setOnRefreshListener(() -> loadTutors(true));
            }

            String domainExtra = getIntent().getStringExtra("domain");
            if (domainExtra != null) {
                currentDomain = domainExtra;
            }
            
            String queryExtra = getIntent().getStringExtra("search_query");
            if (queryExtra != null && !queryExtra.isEmpty()) {
                searchQuery = queryExtra.toLowerCase().trim();
                if (etSearch != null) etSearch.setText(queryExtra);
            }

            adapter = new TutorAdapter(new TutorAdapter.OnTutorClickListener() {
                @Override
                public void onTutorClick(TutorResponse tutor) {
                    Intent intent = new Intent(TutorListActivity.this, TutorProfileActivity.class);
                    intent.putExtra("tutor_id", tutor.getId());
                    intent.putExtra("tutor_name", tutor.getFullName());
                    intent.putExtra("tutor_price", tutor.getPricingPerSession());
                    intent.putExtra("tutor_domain", tutor.getDomain());
                    startActivity(intent);
                }

                @Override
                public void onBookNowClick(TutorResponse tutor) {
                    Intent intent = new Intent(TutorListActivity.this, TutorProfileActivity.class);
                    intent.putExtra("tutor_id", tutor.getId());
                    intent.putExtra("tutor_name", tutor.getFullName());
                    intent.putExtra("tutor_price", tutor.getPricingPerSession());
                    intent.putExtra("tutor_domain", tutor.getDomain());
                    startActivity(intent);
                }
            });

            if (rvTutors != null) {
                rvTutors.setLayoutManager(new LinearLayoutManager(this));
                rvTutors.setAdapter(adapter);

                rvTutors.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    @Override
                    public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                        super.onScrolled(recyclerView, dx, dy);
                        if (dy > 0) {
                            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                            if (layoutManager != null) {
                                int visibleItemCount = layoutManager.getChildCount();
                                int totalItemCount = layoutManager.getItemCount();
                                int firstVisibleItem = layoutManager.findFirstVisibleItemPosition();

                                if (!isLoading && currentPage < totalPages && (visibleItemCount + firstVisibleItem) >= totalItemCount - 2) {
                                    currentPage++;
                                    loadTutors(false);
                                }
                            }
                        }
                    }
                });
            }

            if (chipGroupFilters != null) {
                chipGroupFilters.setOnCheckedStateChangeListener((group, checkedIds) -> {
                    if (checkedIds.isEmpty()) return;
                    int checkedId = checkedIds.get(0);
                    if (checkedId == R.id.chipAll) {
                        currentDomain = "";
                    } else if (checkedId == R.id.chipAcademic) {
                        currentDomain = "academic";
                    } else if (checkedId == R.id.chipSkill) {
                        currentDomain = "skill";
                    }
                    loadTutors(true);
                });
            }

            if (etSearch != null) {
                etSearch.addTextChangedListener(new TextWatcher() {
                    @Override
                    public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                    @Override
                    public void onTextChanged(CharSequence s, int start, int before, int count) {
                        searchQuery = s.toString().toLowerCase().trim();
                        filterLocally();
                    }
                    @Override
                    public void afterTextChanged(Editable s) {}
                });
            }

            loadTutors(true);
        } catch (Exception e) {
            Log.e(TAG, "onCreate error: " + e.getMessage());
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            finish();
        }
    }

    private void filterLocally() {
        if (searchQuery.isEmpty()) {
            filteredTutors.clear();
            filteredTutors.addAll(allTutors);
        } else {
            filteredTutors.clear();
            List<TutorResponse> filtered = allTutors.stream()
                    .filter(t -> (t.getFullName() != null && t.getFullName().toLowerCase().contains(searchQuery)) || 
                            (t.getSubjects() != null && t.getSubjects().stream().anyMatch(s -> s.getName() != null && s.getName().toLowerCase().contains(searchQuery))) ||
                            (t.getSkills() != null && t.getSkills().stream().anyMatch(s -> s.getName() != null && s.getName().toLowerCase().contains(searchQuery))))
                    .collect(Collectors.toList());
            filteredTutors.addAll(filtered);
        }
        if (adapter != null) adapter.updateList(filteredTutors);
        
        if (tvResultCount != null) {
            tvResultCount.setText("Showing " + filteredTutors.size() + " tutor" + (filteredTutors.size() != 1 ? "s" : ""));
        }
        
        boolean isEmpty = filteredTutors.isEmpty();
        if (emptyStateLayout != null) emptyStateLayout.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        if (rvTutors != null) rvTutors.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
    }

    private void loadTutors(boolean refresh) {
        if (sessionManager.getAccessToken() == null) {
            redirectToLogin();
            return;
        }

        if (refresh) {
            currentPage = 1;
            totalPages = 1;
        }

        isLoading = true;
        if (progressBar != null) progressBar.setVisibility(View.VISIBLE);
        if (refresh && tvEmpty != null) {
            tvEmpty.setVisibility(View.GONE);
        }

        try {
            ApiService apiService = ApiClient.getClient().create(ApiService.class);
            String authHeader = "Bearer " + sessionManager.getAccessToken();
            
            String domainParam = currentDomain.isEmpty() ? null : currentDomain;
            Log.d("TutorDebug", "Loading tutors... domain=" + domainParam + ", page=" + currentPage);

            apiService.getTutorList(authHeader, domainParam, currentPage, null, null, null).enqueue(new Callback<ApiResponse<PaginatedResponse<TutorResponse>>>() {
                @Override
                public void onResponse(Call<ApiResponse<PaginatedResponse<TutorResponse>>> call, Response<ApiResponse<PaginatedResponse<TutorResponse>>> response) {
                    isLoading = false;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);

                    if (response.isSuccessful() && response.body() != null && response.body().getData() != null) {
                        PaginatedResponse<TutorResponse> data = response.body().getData();
                        
                        int totalCount = data.getCount();
                        totalPages = (int) Math.ceil((double) totalCount / PAGE_SIZE);
                        
                        Log.d("TutorDebug", "Total count: " + totalCount);
                        Log.d("TutorDebug", "Total pages: " + totalPages);
                        Log.d("TutorDebug", "Results: " + data.getResults().size());

                        List<TutorResponse> tutors = data.getResults();
                        
                        if (refresh) {
                            allTutors.clear();
                        }
                        
                        if (tutors != null && !tutors.isEmpty()) {
                            allTutors.addAll(tutors);
                            if (emptyStateLayout != null) emptyStateLayout.setVisibility(View.GONE);
                            if (rvTutors != null) rvTutors.setVisibility(View.VISIBLE);
                            filterLocally();
                        } else {
                            if (refresh) {
                                if (emptyStateLayout != null) {
                                    emptyStateLayout.setVisibility(View.VISIBLE);
                                    if (tvEmpty != null) tvEmpty.setText("No tutors found");
                                }
                                if (rvTutors != null) rvTutors.setVisibility(View.GONE);
                                if (tvResultCount != null) tvResultCount.setText("Showing 0 tutors");
                            }
                        }

                        if (data.getNext() == null) {
                            totalPages = currentPage;
                            Log.d("TutorDebug", "No more pages");
                        }

                    } else if (response.code() == 401) {
                        redirectToLogin();
                    } else {
                        if (response.code() == 404 && currentPage > 1) {
                            Log.d("TutorDebug", "No more pages (404)");
                            return;
                        }
                        if (currentPage == 1) {
                            Toast.makeText(TutorListActivity.this, "Failed to load tutors", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onFailure(Call<ApiResponse<PaginatedResponse<TutorResponse>>> call, Throwable t) {
                    isLoading = false;
                    if (progressBar != null) progressBar.setVisibility(View.GONE);
                    if (swipeRefresh != null) swipeRefresh.setRefreshing(false);
                    if (currentPage == 1) {
                        Toast.makeText(TutorListActivity.this, "Network error: " + t.getMessage(), Toast.LENGTH_LONG).show();
                    }
                }
            });
        } catch (Exception e) {
            isLoading = false;
            if (progressBar != null) progressBar.setVisibility(View.GONE);
            Log.e(TAG, "Catch error: " + e.getMessage());
        }
    }

    private void redirectToLogin() {
        if (sessionManager != null) sessionManager.clearSession();
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
