package eu.faircode.netguard;

/*
    This file is part of NetGuard.

    NetGuard is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    NetGuard is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with NetGuard.  If not, see <http://www.gnu.org/licenses/>.
*/

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.view.MenuItem;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Dedicated UI for configuring the external SOCKS5 proxy.
 *
 * Replaces the flat EditTextPreference entries in Advanced Options with a
 * focused screen that includes a live reachability test.
 */
public class ActivitySocks5 extends AppCompatActivity {

    private CheckBox  cbEnabled;
    private EditText  etAddr;
    private EditText  etPort;
    private EditText  etUsername;
    private EditText  etPassword;
    private TextView  tvStatus;
    private ImageView ivStatusIcon;

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_socks5);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_socks5);
        }

        cbEnabled  = findViewById(R.id.cbSocks5Enabled);
        etAddr     = findViewById(R.id.etSocks5Addr);
        etPort     = findViewById(R.id.etSocks5Port);
        etUsername = findViewById(R.id.etSocks5Username);
        etPassword = findViewById(R.id.etSocks5Password);
        tvStatus   = findViewById(R.id.tvSocks5Status);
        ivStatusIcon = findViewById(R.id.ivSocks5StatusIcon);

        Button btnTest = findViewById(R.id.btnSocks5Test);
        Button btnSave = findViewById(R.id.btnSocks5Save);

        // Load current values from SharedPreferences
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        cbEnabled.setChecked(prefs.getBoolean(Socks5Manager.PREF_ENABLED, false));
        etAddr.setText(prefs.getString(Socks5Manager.PREF_ADDR, Socks5Manager.DEFAULT_ADDR));
        etPort.setText(prefs.getString(Socks5Manager.PREF_PORT, String.valueOf(Socks5Manager.DEFAULT_PORT)));
        etUsername.setText(prefs.getString(Socks5Manager.PREF_USERNAME, ""));
        etPassword.setText(prefs.getString(Socks5Manager.PREF_PASSWORD, ""));

        updateStatusIdle();

        btnTest.setOnClickListener(v -> runReachabilityTest());

        btnSave.setOnClickListener(v -> {
            if (!validateInputs()) return;
            savePreferences();
            Toast.makeText(this, R.string.msg_socks5_saved, Toast.LENGTH_SHORT).show();
            ServiceSinkhole.reload("socks5 config", this, false);
        });
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdownNow();
    }

    private boolean validateInputs() {
        String addr = etAddr.getText().toString().trim();
        String port = etPort.getText().toString().trim();

        if (TextUtils.isEmpty(addr)) {
            etAddr.setError(getString(R.string.msg_socks5_addr_required));
            return false;
        }
        if (TextUtils.isEmpty(port)) {
            etPort.setError(getString(R.string.msg_socks5_port_required));
            return false;
        }
        try {
            int p = Integer.parseInt(port);
            if (p < 1 || p > 65535) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            etPort.setError(getString(R.string.msg_socks5_port_invalid));
            return false;
        }
        return true;
    }

    private void savePreferences() {
        PreferenceManager.getDefaultSharedPreferences(this)
                .edit()
                .putBoolean(Socks5Manager.PREF_ENABLED, cbEnabled.isChecked())
                .putString(Socks5Manager.PREF_ADDR, etAddr.getText().toString().trim())
                .putString(Socks5Manager.PREF_PORT, etPort.getText().toString().trim())
                .putString(Socks5Manager.PREF_USERNAME, etUsername.getText().toString().trim())
                .putString(Socks5Manager.PREF_PASSWORD, etPassword.getText().toString())
                .apply();
    }

    private void runReachabilityTest() {
        if (!validateInputs()) return;

        // Save current values so Socks5Manager reads them
        savePreferences();

        tvStatus.setText(R.string.msg_socks5_testing);
        ivStatusIcon.setImageResource(R.drawable.twotone_info_outline_24);

        Socks5Manager s5 = Socks5Manager.getInstance(this);
        executor.execute(() -> {
            boolean reachable = s5.isReachable();
            mainHandler.post(() -> {
                if (reachable) {
                    tvStatus.setText(getString(R.string.msg_socks5_reachable,
                            s5.getAddr(), s5.getPort()));
                    ivStatusIcon.setImageResource(R.drawable.twotone_info_outline_24);
                } else {
                    tvStatus.setText(getString(R.string.msg_socks5_unreachable,
                            s5.getAddr(), s5.getPort()));
                    ivStatusIcon.setImageResource(R.drawable.twotone_info_outline_24);
                }
            });
        });
    }

    private void updateStatusIdle() {
        Socks5Manager s5 = Socks5Manager.getInstance(this);
        if (s5.isEnabled()) {
            tvStatus.setText(getString(R.string.msg_socks5_configured,
                    s5.getAddr(), s5.getPort()));
        } else {
            tvStatus.setText(R.string.msg_socks5_disabled);
        }
        ivStatusIcon.setImageResource(R.drawable.twotone_info_outline_24);
    }
}
