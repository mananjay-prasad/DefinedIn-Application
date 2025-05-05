package com.example.defindin;

import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    // UI Components
    private EditText EdtTxt;
    private ImageView BtnSend;
    private RecyclerView recyclerView;
    private TextView HeadingTxt;
    private TextView wordLimitTxt;
    private SeekBar seekBar;
    private Spinner toneSpinner;

    // Chat components
    private List<MessageModel> messageModelList;
    private MessageAdapter messageAdapter;

    // API Config
    private static final String API_URL = "https://define-i05a.onrender.com/api/define";
    private static final int TIMEOUT = 30;
    private int wordLimit = 10;
    private String lang = "eng";
    private String tone = "neutral"; // Default tone

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Apply window insets for proper UI display
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        initializeUI();
        setupRecyclerView();
        setupSeekBar();
        setupToneSpinner();
        setupSendButton();
    }

    private void initializeUI() {
        recyclerView = findViewById(R.id.recyclerView);
        EdtTxt = findViewById(R.id.EdtTxt);
        BtnSend = findViewById(R.id.BtnSend);
        HeadingTxt = findViewById(R.id.HeadingTxt);
        wordLimitTxt = findViewById(R.id.wordLimitTxt);
        seekBar = findViewById(R.id.seekBar);
        toneSpinner = findViewById(R.id.toneSpinner);

        wordLimitTxt.setText("Word limit: " + wordLimit);
    }

    private void setupRecyclerView() {
        messageModelList = new ArrayList<>();
        messageAdapter = new MessageAdapter(messageModelList);
        recyclerView.setAdapter(messageAdapter);

        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);
    }

    private void setupSeekBar() {
        seekBar.setProgress(wordLimit);
        seekBar.setMax(100);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (progress == 0) {
                    progress = 1;
                    seekBar.setProgress(progress);
                }
                wordLimit = progress;
                wordLimitTxt.setText("Word limit: " + wordLimit);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }

    private void setupToneSpinner() {
        // Define tone options programmatically instead of from resources
        String[] toneOptions = {"neutral", "formal", "informal", "humorous", "serious"};

        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this, android.R.layout.simple_spinner_item, toneOptions);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        toneSpinner.setAdapter(adapter);

        // Set default selection to "neutral" (first item)
        toneSpinner.setSelection(0);

        toneSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                tone = parent.getItemAtPosition(position).toString();
                Log.d("DefinedIn", "Selected tone: " + tone);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                tone = "neutral"; // Default to neutral if nothing selected
            }
        });
    }

    private void setupSendButton() {
        BtnSend.setOnClickListener(v -> {
            String word = EdtTxt.getText().toString().trim();

            if (word.isEmpty()) {
                EdtTxt.setError("Please enter a word");
                EdtTxt.requestFocus();
            } else {
                // Hide keyboard
                hideKeyboard();

                addToChat(word, MessageModel.SEND_BY_ME);
                callDefinitionAPI(word);
                HeadingTxt.setVisibility(View.GONE);
            }
        });
    }

    private void hideKeyboard() {
        // Get input method manager
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getSystemService(android.content.Context.INPUT_METHOD_SERVICE);

        // Find the currently focused view
        View view = getCurrentFocus();

        // If no view currently has focus, create a new one to grab a window token
        if (view == null) {
            view = new View(this);
        }

        // Hide the keyboard
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    private void addToChat(String message, String sender) {
        runOnUiThread(() -> {
            messageModelList.add(new MessageModel(message, sender));
            messageAdapter.notifyDataSetChanged();
            recyclerView.smoothScrollToPosition(messageModelList.size());
            EdtTxt.setText("");
        });
    }

    private void addResponse(String response) {
        addToChat(response, MessageModel.SEND_BY_BOT);
    }

    private void callDefinitionAPI(String word) {
        // Show loading message
        final int loadingMessageIndex = messageModelList.size(); // Save position of loading message
        addToChat("Looking up definition for \"" + word + "\"...", MessageModel.SEND_BY_BOT);

        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(TIMEOUT, TimeUnit.SECONDS)
                .writeTimeout(TIMEOUT, TimeUnit.SECONDS)
                .build();

        // Ensure parameters are properly formatted
        String encodedWord = Uri.encode(word);
        wordLimit = Math.max(1, Math.min(wordLimit, 250));

        // Add tone parameter to the API call
        String url = API_URL + "?word=" + encodedWord + "&length=" + wordLimit + "&lang=" + lang + "&tone=" + tone;
        Log.d("DefinedIn", "Requesting: " + url);

        Request request = new Request.Builder()
                .url(url)
                .header("Accept", "application/json")
                .get()
                .build();

        runOnUiThread(() -> {
            BtnSend.setVisibility(View.GONE);
            seekBar.setVisibility(View.GONE);
            EdtTxt.setVisibility(View.GONE);
            toneSpinner.setEnabled(false); // Disable spinner during API call instead of hiding
        });

        client.newCall(request).enqueue(new Callback() {

            @Override
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> {
                    BtnSend.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);
                    EdtTxt.setVisibility(View.VISIBLE);
                    toneSpinner.setEnabled(true); // Re-enable spinner after API response
                });

                Log.e("DefinedIn", "API request failed", e);
                // Update the loading message with error instead of adding new message
                updateResponseMessage(loadingMessageIndex, "Failed to load definition: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                runOnUiThread(() -> {
                    BtnSend.setVisibility(View.VISIBLE);
                    seekBar.setVisibility(View.VISIBLE);
                    EdtTxt.setVisibility(View.VISIBLE);
                    toneSpinner.setEnabled(true); // Re-enable spinner after API response
                });

                String responseBody = response.body().string();

                if (response.isSuccessful()) {
                    try {
                        JSONObject json = new JSONObject(responseBody);
                        if (json.has("result")) {
                            // Replace loading message with the actual result
                            updateResponseMessage(loadingMessageIndex, json.getString("result"));
                        } else {
                            // Replace loading message with "not found" message
                            updateResponseMessage(loadingMessageIndex, "No definition found for \"" + word + "\"");
                        }
                    } catch (JSONException e) {
                        Log.e("DefinedIn", "JSON parsing error", e);
                        updateResponseMessage(loadingMessageIndex, "Unable to parse definition data");
                    }
                } else {
                    // Replace loading message with appropriate error
                    String errorMessage = getErrorMessage(response, word);
                    updateResponseMessage(loadingMessageIndex, errorMessage);
                }
            }
        });
    }

    // New method to update an existing message instead of adding a new one
    private void updateResponseMessage(int index, String newMessage) {
        runOnUiThread(() -> {
            if (index < messageModelList.size()) {
                messageModelList.get(index).setMessage(newMessage);
                messageAdapter.notifyItemChanged(index);
            }
        });
    }

    // Helper method to get appropriate error message
    private String getErrorMessage(Response response, String word) {
        switch (response.code()) {
            case 400:
                return "Invalid request. Please check the word and try again.";
            case 404:
                return "The word \"" + word + "\" was not found in the dictionary.";
            case 429:
                return "Too many requests. Please try again later.";
            case 503:
                return "Dictionary service is currently unavailable. Please try again later.";
            default:
                return "Error " + response.code() + ": " + response.message();
        }
    }
}