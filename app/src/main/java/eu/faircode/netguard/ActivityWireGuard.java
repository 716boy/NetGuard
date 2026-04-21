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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * UI for importing and managing the WireGuard tunnel configuration.
 *
 * Users can paste a .conf directly into the text field or pick a file from
 * storage. The config is validated (must contain [Interface] and [Peer])
 * before being saved via WireGuardManager.
 */
public class ActivityWireGuard extends AppCompatActivity {
    private static final String TAG = "NetGuard.WireGuardUI";

    private EditText etConfig;
    private TextView tvWgStatus;
    private ImageView ivWgStatusIcon;
    private LinearLayout llWgPeerInfo;
    private TextView tvWgPeer;

    private final ActivityResultLauncher<String[]> filePicker =
            registerForActivityResult(new ActivityResultContracts.OpenDocument(), uri -> {
                if (uri != null) readConfigFromUri(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wireguard);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(R.string.title_wireguard);
        }

        etConfig       = findViewById(R.id.etWgConfig);
        tvWgStatus     = findViewById(R.id.tvWgStatus);
        ivWgStatusIcon = findViewById(R.id.ivWgStatusIcon);
        llWgPeerInfo   = findViewById(R.id.llWgPeerInfo);
        tvWgPeer       = findViewById(R.id.tvWgPeer);

        Button btnImport = findViewById(R.id.btnWgImport);
        Button btnSave   = findViewById(R.id.btnWgSave);
        Button btnClear  = findViewById(R.id.btnWgClear);

        WireGuardManager wg = WireGuardManager.getInstance(this);
        String existing = wg.loadConfig();
        if (!existing.isEmpty()) {
            etConfig.setText(existing);
        }
        updateStatus(wg);

        btnImport.setOnClickListener(v ->
                filePicker.launch(new String[]{"*/*"}));

        btnSave.setOnClickListener(v -> {
            String conf = etConfig.getText().toString().trim();
            if (!isValidConfig(conf)) {
                Toast.makeText(this, R.string.msg_wg_invalid_config, Toast.LENGTH_LONG).show();
                return;
            }
            wg.saveConfig(conf);
            updateStatus(wg);
            Toast.makeText(this, R.string.msg_wg_saved, Toast.LENGTH_SHORT).show();
            ServiceSinkhole.reload("wireguard config", this, false);
        });

        btnClear.setOnClickListener(v -> {
            wg.saveConfig("");
            etConfig.setText("");
            updateStatus(wg);
            ServiceSinkhole.reload("wireguard config", this, false);
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

    private void readConfigFromUri(Uri uri) {
        try (InputStream is = getContentResolver().openInputStream(uri);
             BufferedReader br = new BufferedReader(new InputStreamReader(is))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null)
                sb.append(line).append('\n');
            etConfig.setText(sb.toString());
        } catch (IOException e) {
            Log.e(TAG, "Error reading config file", e);
            Toast.makeText(this, R.string.msg_wg_read_error, Toast.LENGTH_LONG).show();
        }
    }

    private boolean isValidConfig(String conf) {
        return !TextUtils.isEmpty(conf)
                && conf.contains("[Interface]")
                && conf.contains("[Peer]");
    }

    private void updateStatus(WireGuardManager wg) {
        if (wg.hasConfig()) {
            String peer = wg.getPeerEndpoint();
            tvWgStatus.setText(getString(R.string.msg_wg_configured,
                    peer != null ? peer : getString(R.string.msg_wg_peer_unknown)));
            ivWgStatusIcon.setImageResource(R.drawable.twotone_info_outline_24);

            // Show peer info row
            if (peer != null) {
                llWgPeerInfo.setVisibility(View.VISIBLE);
                tvWgPeer.setText(peer);
            } else {
                llWgPeerInfo.setVisibility(View.GONE);
            }
        } else {
            tvWgStatus.setText(R.string.msg_wg_not_configured);
            ivWgStatusIcon.setImageResource(R.drawable.twotone_info_outline_24);
            llWgPeerInfo.setVisibility(View.GONE);
        }
    }
}
