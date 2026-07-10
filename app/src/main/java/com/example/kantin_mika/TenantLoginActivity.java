package com.example.kantin_mika;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

public class TenantLoginActivity extends AppCompatActivity {
    private boolean isPasswordVisible = false;
    private String URL_LOGIN = "http://192.168.101.7/pmob/api_uas/login_tenant.php";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_tenant_login);

        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        EditText etUsername = findViewById(R.id.etUsername);
        EditText etPassword = findViewById(R.id.etPassword);
        ImageView btnTogglePassword = findViewById(R.id.btnTogglePassword);

        btnTogglePassword.setOnClickListener(v -> {
            isPasswordVisible = !isPasswordVisible;
            if (isPasswordVisible) {
                etPassword.setInputType(InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye_off);
            } else {
                etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                btnTogglePassword.setImageResource(R.drawable.ic_eye);
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        findViewById(R.id.btnLogin).setOnClickListener(v -> {
            String username = etUsername.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "Harap isi username dan password", Toast.LENGTH_SHORT).show();
                return;
            }

            performLogin(username, password);
        });
    }

    private void performLogin(String username, String password) {
        new Thread(() -> {
            String responseBody = "";
            try {
                URL url = new URL(URL_LOGIN);
                HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                conn.setRequestMethod("POST");
                conn.setDoOutput(true);
                conn.setConnectTimeout(5000);
                conn.setReadTimeout(5000);

                String data = "username=" + URLEncoder.encode(username, "UTF-8") +
                             "&password=" + URLEncoder.encode(password, "UTF-8");

                OutputStream os = conn.getOutputStream();
                os.write(data.getBytes("UTF-8"));
                os.flush();
                os.close();
                
                InputStream is;
                int status = conn.getResponseCode();
                if (status >= 400) is = conn.getErrorStream();
                else is = conn.getInputStream();

                BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                StringBuilder sb = new StringBuilder();
                String line;
                while ((line = reader.readLine()) != null) sb.append(line);
                reader.close();
                responseBody = sb.toString();

                Log.d("LOGIN_DEBUG", "Raw Response: " + responseBody);

                JSONObject res = new JSONObject(responseBody);
                String apiStatus = res.getString("status");
                String message = res.getString("message");

                runOnUiThread(() -> {
                    if ("success".equals(apiStatus)) {
                        try {
                            JSONObject tenantData = res.getJSONObject("data");
                            getSharedPreferences("TenantPref", MODE_PRIVATE).edit()
                                    .putInt("id_tenant", tenantData.getInt("id_tenant"))
                                    .putString("username", tenantData.getString("username"))
                                    .putString("nama_tenant", tenantData.getString("nama_tenant"))
                                    .apply();

                            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                            startActivity(new Intent(TenantLoginActivity.this, TenantHomeActivity.class));
                            finish();
                        } catch (Exception e) {
                            Toast.makeText(this, "Gagal memproses data login", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                    }
                });

            } catch (Exception e) {
                e.printStackTrace();
                final String errorMsg = responseBody.isEmpty() ? e.getMessage() : responseBody;
                runOnUiThread(() -> {
                    // Fallback admin tetap ada
                    if ("admin".equals(username) && "admin".equals(password)) {
                        getSharedPreferences("TenantPref", MODE_PRIVATE).edit()
                                .putInt("id_tenant", 1)
                                .putString("username", "admin")
                                .apply();
                        startActivity(new Intent(TenantLoginActivity.this, TenantHomeActivity.class));
                        finish();
                    } else {
                        // Tampilkan isi error asli agar bisa didebug
                        Toast.makeText(this, "Server Error: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
            }
        }).start();
    }
}
