package com.example.co_cart;

import android.app.DatePickerDialog;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.example.co_cart.models.User;
import com.example.co_cart.repository.FirebaseHelper;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.textfield.TextInputEditText;

import java.util.Calendar;

public class ProfileActivity extends AppCompatActivity {

    private ImageView ivProfilePhoto;
    private FloatingActionButton btnPickPhoto;
    private TextInputEditText etFirstName, etMiddleName, etLastName;
    private Spinner spinnerCountryCode;
    private TextInputEditText etMobile, etAge, etDob;
    private ChipGroup chipGroupGender;
    private Chip chipMale, chipFemale, chipOther;
    private MaterialButton btnSaveProfile;
    private ProgressBar progressBar;

    private FirebaseHelper firebaseHelper;
    private User currentUser;
    private String selectedPhotoUriString = "";

    private static final String[] COUNTRY_CODES = new String[]{
            "🇮🇳 India (+91)",
            "🇺🇸 USA (+1)",
            "🇬🇧 UK (+44)",
            "🇨🇦 Canada (+1)",
            "🇦🇺 Australia (+61)",
            "🇩🇪 Germany (+49)",
            "🇫🇷 France (+33)",
            "🇯🇵 Japan (+81)",
            "🇦🇪 UAE (+971)",
            "🇸🇬 Singapore (+65)",
            "🇳🇵 Nepal (+977)",
            "🇧🇩 Bangladesh (+880)",
            "🇵🇰 Pakistan (+92)",
            "🇱🇰 Sri Lanka (+94)",
            "🇿🇦 South Africa (+27)",
            "🇧🇷 Brazil (+55)"
    };

    private final ActivityResultLauncher<String> photoPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetContent(),
            uri -> {
                if (uri != null) {
                    selectedPhotoUriString = uri.toString();
                    ivProfilePhoto.setImageURI(uri);
                    Toast.makeText(ProfileActivity.this, "Profile photo selected!", Toast.LENGTH_SHORT).show();
                }
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        FirebaseHelper.init(this);
        firebaseHelper = FirebaseHelper.getInstance();

        ivProfilePhoto = findViewById(R.id.ivProfilePhoto);
        btnPickPhoto = findViewById(R.id.btnPickPhoto);
        etFirstName = findViewById(R.id.etFirstName);
        etMiddleName = findViewById(R.id.etMiddleName);
        etLastName = findViewById(R.id.etLastName);
        spinnerCountryCode = findViewById(R.id.spinnerCountryCode);
        etMobile = findViewById(R.id.etMobile);
        etAge = findViewById(R.id.etAge);
        etDob = findViewById(R.id.etDob);
        chipGroupGender = findViewById(R.id.chipGroupGender);
        chipMale = findViewById(R.id.chipMale);
        chipFemale = findViewById(R.id.chipFemale);
        chipOther = findViewById(R.id.chipOther);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        progressBar = findViewById(R.id.progressBar);

        ArrayAdapter<String> countryAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, COUNTRY_CODES);
        countryAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerCountryCode.setAdapter(countryAdapter);

        btnPickPhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        ivProfilePhoto.setOnClickListener(v -> photoPickerLauncher.launch("image/*"));
        etDob.setOnClickListener(v -> showDatePickerDialog());
        btnSaveProfile.setOnClickListener(v -> saveProfile());

        loadUserProfile();
    }

    private void showDatePickerDialog() {
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, (view, selectedYear, selectedMonth, selectedDay) -> {
            String dobText = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
            etDob.setText(dobText);
        }, year, month, day);

        datePickerDialog.show();
    }

    private void loadUserProfile() {
        setLoading(true);
        String userId = firebaseHelper.getCurrentUserId();
        firebaseHelper.getUserProfile(userId, new FirebaseHelper.Callback<User>() {
            @Override
            public void onSuccess(User user) {
                setLoading(false);
                currentUser = user;

                // Populate Name fields
                if (user.getFirstName() != null && !user.getFirstName().isEmpty()) {
                    etFirstName.setText(user.getFirstName());
                    etMiddleName.setText(user.getMiddleName());
                    etLastName.setText(user.getLastName());
                } else if (user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
                    String[] parts = user.getDisplayName().split("\\s+");
                    if (parts.length == 1) {
                        etFirstName.setText(parts[0]);
                    } else if (parts.length == 2) {
                        etFirstName.setText(parts[0]);
                        etLastName.setText(parts[1]);
                    } else if (parts.length >= 3) {
                        etFirstName.setText(parts[0]);
                        etMiddleName.setText(parts[1]);
                        etLastName.setText(parts[2]);
                    }
                }

                // Populate Country Code Spinner
                setCountryCodeSelection(user.getCountryCode());
                etMobile.setText(user.getMobileNumber());
                etAge.setText(user.getAge());
                etDob.setText(user.getDob());

                // Populate Photo if available
                if (user.getProfilePhotoUrl() != null && !user.getProfilePhotoUrl().isEmpty()) {
                    try {
                        selectedPhotoUriString = user.getProfilePhotoUrl();
                        ivProfilePhoto.setImageURI(Uri.parse(selectedPhotoUriString));
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }

                // Populate Gender
                String gender = user.getGender();
                if ("Male 👨".equalsIgnoreCase(gender) || "Male".equalsIgnoreCase(gender)) {
                    chipMale.setChecked(true);
                } else if ("Female 👩".equalsIgnoreCase(gender) || "Female".equalsIgnoreCase(gender)) {
                    chipFemale.setChecked(true);
                } else if ("Other ⚧".equalsIgnoreCase(gender) || "Other".equalsIgnoreCase(gender)) {
                    chipOther.setChecked(true);
                }
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(ProfileActivity.this, "Failed to load profile", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveProfile() {
        String firstName = etFirstName.getText() != null ? etFirstName.getText().toString().trim() : "";
        String middleName = etMiddleName.getText() != null ? etMiddleName.getText().toString().trim() : "";
        String lastName = etLastName.getText() != null ? etLastName.getText().toString().trim() : "";
        String countryCode = getSelectedCountryCode();
        String mobile = etMobile.getText() != null ? etMobile.getText().toString().trim() : "";
        String age = etAge.getText() != null ? etAge.getText().toString().trim() : "";
        String dob = etDob.getText() != null ? etDob.getText().toString().trim() : "";
        String gender = getSelectedGender();

        if (TextUtils.isEmpty(firstName)) {
            etFirstName.setError("First Name is required");
            return;
        }

        if (TextUtils.isEmpty(lastName)) {
            etLastName.setError("Last Name is required");
            return;
        }

        // Validate 10-digit mobile number containing only digits
        if (TextUtils.isEmpty(mobile)) {
            etMobile.setError("Mobile number is required");
            return;
        }

        if (mobile.length() != 10 || !mobile.matches("[0-9]{10}")) {
            etMobile.setError("Mobile number must be exactly 10 digits (0-9)");
            return;
        }

        if (currentUser == null) {
            currentUser = new User(firebaseHelper.getCurrentUserId(), "", firstName + " " + lastName, "");
        }

        currentUser.setFirstName(firstName);
        currentUser.setMiddleName(middleName);
        currentUser.setLastName(lastName);
        currentUser.setDisplayName(firstName + (middleName.isEmpty() ? "" : " " + middleName) + " " + lastName);
        currentUser.setCountryCode(countryCode);
        currentUser.setMobileNumber(mobile);
        currentUser.setAge(age);
        currentUser.setGender(gender);
        currentUser.setDob(dob);
        currentUser.setProfilePhotoUrl(selectedPhotoUriString);

        setLoading(true);
        firebaseHelper.updateUserProfile(currentUser, new FirebaseHelper.Callback<Boolean>() {
            @Override
            public void onSuccess(Boolean result) {
                setLoading(false);
                Toast.makeText(ProfileActivity.this, "Profile updated successfully!", Toast.LENGTH_SHORT).show();
                finish();
            }

            @Override
            public void onError(String errorMessage) {
                setLoading(false);
                Toast.makeText(ProfileActivity.this, "Failed to update profile: " + errorMessage, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private String getSelectedCountryCode() {
        if (spinnerCountryCode != null && spinnerCountryCode.getSelectedItem() != null) {
            String selected = spinnerCountryCode.getSelectedItem().toString();
            int start = selected.indexOf("(");
            int end = selected.indexOf(")");
            if (start != -1 && end != -1) {
                return selected.substring(start + 1, end).trim();
            }
        }
        return "+91";
    }

    private void setCountryCodeSelection(String code) {
        if (code == null || code.isEmpty()) code = "+91";
        for (int i = 0; i < COUNTRY_CODES.length; i++) {
            if (COUNTRY_CODES[i].contains("(" + code + ")")) {
                spinnerCountryCode.setSelection(i);
                return;
            }
        }
        spinnerCountryCode.setSelection(0);
    }

    private String getSelectedGender() {
        int checkedId = chipGroupGender.getCheckedChipId();
        if (checkedId == R.id.chipMale) {
            return "Male 👨";
        } else if (checkedId == R.id.chipFemale) {
            return "Female 👩";
        } else if (checkedId == R.id.chipOther) {
            return "Other ⚧";
        }
        return "";
    }

    private void setLoading(boolean loading) {
        if (loading) {
            progressBar.setVisibility(View.VISIBLE);
            btnSaveProfile.setEnabled(false);
        } else {
            progressBar.setVisibility(View.GONE);
            btnSaveProfile.setEnabled(true);
        }
    }
}
