package com.example.chatgptforandroid;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Context;
import android.net.ConnectivityManager;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    RecyclerView recyclerView;
    TextView welcomeTextView;
    EditText messagingEditText;
    ImageButton sendButton;
    List<Message> messageList;
    MessageAdapter messageAdapter;
    Context context;

    // Posting to a Server (OpenAI's Server) using OkHttp
    public static final MediaType JSON = MediaType.get("application/json; charset=utf-8");
    OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        messageList = new ArrayList<>();

        recyclerView = findViewById(R.id.recycler_view);
        welcomeTextView = findViewById(R.id.welcome_text);
        messagingEditText = findViewById(R.id.messaging_edit_text);
        sendButton = findViewById(R.id.send_button);

        //Prompting a Toast if Internet is Not Connected
        if(!isConnected()) {
            Toast.makeText(this, "\t\t\t\t\t\t\t No Internet Connectivity.\nPlease turn your Internet Connection on.", Toast.LENGTH_LONG).show();
        }

        // Setting-up the RecyclerView
        messageAdapter = new MessageAdapter(messageList);
        recyclerView.setAdapter(messageAdapter);
        LinearLayoutManager llm = new LinearLayoutManager(this);
        llm.setStackFromEnd(true);
        recyclerView.setLayoutManager(llm);

        // On-Click-Listener for Send Button
        sendButton.setOnClickListener((view) -> {
            welcomeTextView.setVisibility(View.GONE);

            String usersText = messagingEditText.getText().toString().trim();
            addToChat(usersText, Message.SENT_BY_USER);
            messagingEditText.setText("");
            callAPI(usersText);
        });
    }

    void addToChat(String message, String sentBy) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                messageList.add(new Message(message, sentBy));
                messageAdapter.notifyDataSetChanged();
                recyclerView.smoothScrollToPosition(messageAdapter.getItemCount());
            }
        });
    }

    void addResponse(String response) {
        messageList.remove(messageList.size()-1); // Removes the prompt that ChatGPT is 'Typing...'
        addToChat(response, Message.SENT_BY_CHATGPT);
    }

    // OpenAI's API Link: https://platform.openai.com/docs/api-reference/completions
    void callAPI(String usersText) {
        // Adds a Prompt that ChatGPT is 'Typing' while a Response is being is generated.
        messageList.add(new Message("Typing...", Message.SENT_BY_CHATGPT));

        // Setting-up OkHttp
        JSONObject jsonBody = new JSONObject();
        try {
            jsonBody.put("model", "text-davinci-003");
            jsonBody.put("prompt", usersText);
            jsonBody.put("max_tokens", 4000); // Newer models by OpenAI support a context length (prompt + max_tokens) of 4096 tokens.
            jsonBody.put("temperature", 0);
        } catch (JSONException e) {
            throw new RuntimeException(e);
        }

        RequestBody body = RequestBody.create(jsonBody.toString(), JSON);
        Request request = new Request.Builder()
                .url("https://api.openai.com/v1/completions")
                .header("Authorization", "Bearer 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")
                .post(body)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                addResponse("Failed to generate a Response due to " + e.getMessage() + "\n\nTry asking again OR check your Connection!");
            }

            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                if(response.isSuccessful()) {
                    JSONObject jsonObject = null;
                    try {
                        jsonObject = new JSONObject(response.body().string());
                        JSONArray jsonArray = jsonObject.getJSONArray("choices");
                        String result = jsonArray.getJSONObject(0).getString("text");
                        addResponse(result.trim());
                    } catch (JSONException e) {
                        throw new RuntimeException(e);
                    }
                } else {
                    addResponse("Failed to generate a Response due to " + response.body().string() + "\n\nTry asking again Or check your Connection!");
                }
            }
        });
    }

    // Checking if Internet is Connected
    private boolean isConnected() {
        ConnectivityManager cm = (ConnectivityManager) getApplicationContext().getSystemService(context.CONNECTIVITY_SERVICE);
        return cm.getActiveNetworkInfo()!=null && cm.getActiveNetworkInfo().isConnectedOrConnecting();
    }
}