package com.hinglish.cipherkeyboard;

import android.os.Bundle;
import android.provider.Settings;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // No layout inflation / resource binding — keeps build fast and error-free
        Toast.makeText(
                this,
                "Hinglish Cipher Keyboard configuration complete!\n" +
                        "Steps to enable:\n" +
                        "1. Go to Settings > System > Languages & Input > On-screen keyboard\n" +
                        "2. Enable 'Hinglish Cipher Keyboard'\n" +
                        "3. Switch to it from any text field's keyboard picker",
                Toast.LENGTH_LONG
        ).show();

        // Directly open system input method settings so user can enable it immediately
        try {
            startActivity(new android.content.Intent(Settings.ACTION_INPUT_METHOD_SETTINGS));
        } catch (Exception e) {
            // Fail silently if settings screen unavailable on device
        }

        finish();
    }
}
