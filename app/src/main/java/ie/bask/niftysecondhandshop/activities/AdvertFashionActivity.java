package ie.bask.niftysecondhandshop.activities;

import android.app.ProgressDialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.RadioGroup;
import android.widget.Spinner;

import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.michaelmuenzer.android.scrollablennumberpicker.ScrollableNumberPicker;

import java.util.UUID;

import ie.bask.niftysecondhandshop.R;
import ie.bask.niftysecondhandshop.models.AdvertFashion;

public class AdvertFashionActivity extends Base {

    private RadioGroup productType;
    private Spinner clothingSizeSpinner;
    private ScrollableNumberPicker shoeSize;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_advert_fashion);

        // Initialising widgets
        advertImage = findViewById(R.id.advertImage);
        productTitle = findViewById(R.id.productTitle);
        clothingSizeSpinner = findViewById(R.id.clothingSizeSpinner);
        snp_horizontal = findViewById(R.id.snp_horizontal);
        priceManual = findViewById(R.id.priceManual);
        productType = findViewById(R.id.typeRadioGroup);
        shoeSize = findViewById(R.id.snp_shoeSizes);
        autoCompleteCounty = findViewById(R.id.autoCompleteCounty);
        productDetails = findViewById(R.id.productDetails);
        submitButton = findViewById(R.id.submitButton);
        progressDialog = new ProgressDialog(this);

        // Set the max input length of title to 25 characters
        InputFilter[] filter = new InputFilter[1];
        filter[0] = new InputFilter.LengthFilter(25);
        productTitle.setFilters(filter);

        // Set the max length of price to 3
        filter[0] = new InputFilter.LengthFilter(3);
        priceManual.setFilters(filter);

        // Set max input length of autoCompleteCounty to 9 chars
        filter[0] = new InputFilter.LengthFilter(9);
        autoCompleteCounty.setFilters(filter);

        filter[0] = new InputFilter.LengthFilter(50);
        productDetails.setFilters(filter);

        // Check clothing by default
        productType.check(R.id.clothing_radioButton);

        // If shoes radio button is clicked, hide clothing sizes
        productType.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (checkedId == R.id.clothing_radioButton) {
                    clothingSizeSpinner.setVisibility(View.VISIBLE);
                    shoeSize.setVisibility(View.GONE);
                } else {
                    clothingSizeSpinner.setVisibility(View.GONE);
                    shoeSize.setVisibility(View.VISIBLE);
                }
            }
        });

        // Populate string-array with clothing sizes to the spinner
        ArrayAdapter<String> myAdapter = new ArrayAdapter<>(AdvertFashionActivity.this,
                R.layout.spinner_item, getResources().getStringArray(R.array.clothingSizes));
        myAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        clothingSizeSpinner.setAdapter(myAdapter);

        // Load string-array from resources to give suggestions
        // to the user when they start typing
        ArrayAdapter<String> arrayAdapterCounties = new ArrayAdapter<>(AdvertFashionActivity.this, android.R.layout.simple_dropdown_item_1line,
                getResources().getStringArray(R.array.counties));
        autoCompleteCounty.setAdapter(arrayAdapterCounties);
        // Show suggestions after 1 symbol is typed
        autoCompleteCounty.setThreshold(1);

        // Getting the reference of Users node
        databaseUsers = FirebaseDatabase.getInstance().getReference("users");

        // Store current user id
        String currentUserId = firebaseAuth.getCurrentUser().getUid();

        // Get value of county for current user from databaseUsers
        databaseUsers.child(currentUserId).addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                try {
                    String county = (String) dataSnapshot.child("county").getValue();
                    autoCompleteCounty.setText(county);
                }
                // Catch NullPointerException if the user is logged in with Google
                // therefore meaning there will be no value for county
                catch (NullPointerException nullPointer) {
                    autoCompleteCounty.setText(null);
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.v("MyLogs", "Error reading data");
            }
        });

        permissionCheck();
        takePhoto();

        // Initialise Firebase Storage
        storage = FirebaseStorage.getInstance();
        storageReference = storage.getReference();

        // Initialising Google API client
        mGoogleSignInClient = createGoogleClient();
    }


    public void submitButtonPressed(View view) {
        // Get input from widgets
        final String title = productTitle.getText().toString().trim();
        final double price;
        final String location = autoCompleteCounty.getText().toString().trim();
        final String details = productDetails.getText().toString().trim();
        int radioId = productType.getCheckedRadioButtonId();
        final String type;
        final String size;

        // Use the number picker if manual price is empty, default value of np is 0
        if (priceManual.getText().toString().isEmpty()) {
            price = (double) snp_horizontal.getValue();
        } else {
            price = Double.parseDouble(priceManual.getText().toString().trim());
        }

        // Set value of type for each radio button clicked
        if (radioId == R.id.clothing_radioButton) {
            type = "Clothing";
        } else {
            type = "Shoes";
        }

        // Set size = value from spinner if checked radio button is Clothing
        if (radioId == R.id.clothing_radioButton) {
            size = clothingSizeSpinner.getSelectedItem().toString();
        }
        // Set size = value from EditText if checked radio button is Shoes
        else {
            size = String.valueOf(shoeSize.getValue());
        }

        // Get the URI of captured image
        String imageUri = getImageUri(bitmap);

        // Check if the user entered an existing county
        autoCompleteCounty.setValidator(new AutoCompleteTextView.Validator() {
            @Override
            public boolean isValid(CharSequence text) {
                for (int j = 0; j < getResources().getStringArray(R.array.counties).length; j++) {
                    String currentElement = getResources().getStringArray(R.array.counties)[j];
                    if (location.equals(currentElement)) {
                        Log.v("MyLogs", "FOUND COUNTY IN ARRAY!");
                        return true;
                    }
                }
                return false;
            }

            @Override
            public CharSequence fixText(CharSequence invalidText) {
                return null;
            }
        });
        autoCompleteCounty.performValidation();


        // Check if there are empty fields and set errors to alert the user
        if (TextUtils.isEmpty(title)) {
            productTitle.setError("Title is required!");
            productTitle.requestFocus();
        } else if (TextUtils.isEmpty(location) || !autoCompleteCounty.getValidator().isValid(location)) {
            autoCompleteCounty.setError("Empty or invalid county!");
            autoCompleteCounty.requestFocus();
        } else if (TextUtils.isEmpty(details)) {
            productDetails.setError("Details field is required!");
            productDetails.requestFocus();
        }
        // If none of the fields are empty
        else {
            // Start progress dialog
            progressDialog.setMessage("Uploading advert...");
            progressDialog.show();

            // Get reference to Firebase Storage and put photo into images folder
            final StorageReference ref = storageReference.child("images/" + UUID.randomUUID().toString());
            ref.putFile(Uri.parse(imageUri)).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                    // Close progress dialog
                    progressDialog.dismiss();

                    String downloadURL = String.valueOf(taskSnapshot.getDownloadUrl());

                    // Create a new advert with the data
                    newAdvertFashion(new AdvertFashion(downloadURL, title, price, type, size, location, details));
                    Log.v("MyLogs", "Submit pressed! Data: 1) Title: " + title + " (2) Price: " + price + " (3) Type: " + type + " (4) Size: " + size +
                            " (5) Location: " + location + " (6) Details: " + details);
                }
            });
        }
    }

    /**
     * Receive captured image from camera and store it as Bitmap
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CAMERA_PIC_REQUEST && resultCode == RESULT_OK) {
            bitmap = (Bitmap) data.getExtras().get("data");
            advertImage.setImageBitmap(bitmap);
        } else {
            startActivity(new Intent(this, MainActivity.class));
        }
    }

}
