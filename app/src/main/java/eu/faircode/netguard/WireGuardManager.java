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

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.net.InetSocketAddress;

/**
 * Manages WireGuard tunnel configuration and lifecycle.
 *
 * The tunnel is implemented as a SOCKS5-over-WireGuard proxy: wireguard-go
 * (or a compatible userspace implementation) is expected to expose a local
 * SOCKS5 listener whose address is stored in preferences as
 * "wireguard_socks5_addr" / "wireguard_socks5_port".  PacketRouter reads
 * those values when routing packets for apps whose tunnel_mode == WIREGUARD.
 *
 * Config is stored as a plain WireGuard .conf string in SharedPreferences
 * under the key "wireguard_config".  A future iteration can replace this
 * with wireguard-android's Tunnel API.
 */
public class WireGuardManager {
    private static final String TAG = "NetGuard.WireGuard";

    // Preference keys
    public static final String PREF_WG_CONFIG       = "wireguard_config";
    public static final String PREF_WG_ENABLED      = "wireguard_enabled";
    public static final String PREF_WG_SOCKS5_ADDR  = "wireguard_socks5_addr";
    public static final String PREF_WG_SOCKS5_PORT  = "wireguard_socks5_port";

    // Default local SOCKS5 endpoint exposed by the WireGuard userspace process
    public static final String DEFAULT_SOCKS5_ADDR = "127.0.0.1";
    public static final int    DEFAULT_SOCKS5_PORT = 40001;

    private static WireGuardManager instance;
    private final Context context;
    private volatile boolean running = false;

    private WireGuardManager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized WireGuardManager getInstance(Context context) {
        if (instance == null)
            instance = new WireGuardManager(context);
        return instance;
    }

    // -------------------------------------------------------------------------
    // Config helpers
    // -------------------------------------------------------------------------

    public void saveConfig(String wgConf) {
        PreferenceManager.getDefaultSharedPreferences(context)
                .edit()
                .putString(PREF_WG_CONFIG, wgConf)
                .apply();
        Log.i(TAG, "WireGuard config saved (" + wgConf.length() + " chars)");
    }

    public String loadConfig() {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_WG_CONFIG, "");
    }

    public boolean hasConfig() {
        return !loadConfig().isEmpty();
    }

    /**
     * Returns the endpoint (host:port) from the [Peer] section of the stored config,
     * or null if not found / config is empty.
     */
    public String getPeerEndpoint() {
        String conf = loadConfig();
        if (conf.isEmpty()) return null;
        try (BufferedReader br = new BufferedReader(new StringReader(conf))) {
            String line;
            while ((line = br.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("Endpoint")) {
                    String[] parts = line.split("=", 2);
                    if (parts.length == 2)
                        return parts[1].trim();
                }
            }
        } catch (IOException ignored) {
        }
        return null;
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Start the WireGuard tunnel.
     *
     * In this implementation the tunnel is expected to be managed externally
     * (e.g. wireguard-android or a bundled wireguard-go binary) and to expose
     * a local SOCKS5 proxy.  This method records the intended state so that
     * PacketRouter can route traffic correctly.
     *
     * Replace the body of this method with actual tunnel bring-up when
     * integrating wireguard-android's Tunnel API.
     */
    public synchronized void start() {
        if (running) return;
        if (!hasConfig()) {
            Log.w(TAG, "No WireGuard config — tunnel not started");
            return;
        }
        Log.i(TAG, "WireGuard tunnel start requested (peer=" + getPeerEndpoint() + ")");
        // TODO: invoke wireguard-android Tunnel.up() or start wireguard-go binary here
        running = true;
    }

    public synchronized void stop() {
        if (!running) return;
        Log.i(TAG, "WireGuard tunnel stop requested");
        // TODO: invoke wireguard-android Tunnel.down() or stop wireguard-go binary here
        running = false;
    }

    public boolean isRunning() {
        return running;
    }

    // -------------------------------------------------------------------------
    // Routing helpers used by PacketRouter
    // -------------------------------------------------------------------------

    /**
     * Returns the local SOCKS5 address through which WireGuard-routed traffic
     * should be forwarded.
     */
    public InetSocketAddress getSocks5Endpoint() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String addr = prefs.getString(PREF_WG_SOCKS5_ADDR, DEFAULT_SOCKS5_ADDR);
        int port = prefs.getInt(PREF_WG_SOCKS5_PORT, DEFAULT_SOCKS5_PORT);
        return new InetSocketAddress(addr, port);
    }
}
