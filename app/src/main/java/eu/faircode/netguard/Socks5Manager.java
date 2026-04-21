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

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.Socket;

/**
 * Manages the external SOCKS5 proxy used for per-app SOCKS5 routing.
 *
 * The proxy itself (e.g. usque/WARP, xray-core, or any SOCKS5 server) is
 * expected to run as a separate process.  This class tracks the configured
 * endpoint and provides a health-check so PacketRouter can verify the proxy
 * is reachable before routing traffic through it.
 *
 * Preference keys:
 *   socks5_enabled  – master switch (reuses NetGuard's existing key)
 *   socks5_addr     – proxy host (default 127.0.0.1)
 *   socks5_port     – proxy port (default 40000)
 *   socks5_username – optional auth username
 *   socks5_password – optional auth password
 */
public class Socks5Manager {
    private static final String TAG = "NetGuard.Socks5";

    public static final String PREF_ENABLED  = "socks5_enabled";
    public static final String PREF_ADDR     = "socks5_addr";
    public static final String PREF_PORT     = "socks5_port";
    public static final String PREF_USERNAME = "socks5_username";
    public static final String PREF_PASSWORD = "socks5_password";

    public static final String DEFAULT_ADDR = "127.0.0.1";
    public static final int    DEFAULT_PORT = 40000;

    private static final int HEALTH_CHECK_TIMEOUT_MS = 2000;

    private static Socks5Manager instance;
    private final Context context;

    private Socks5Manager(Context context) {
        this.context = context.getApplicationContext();
    }

    public static synchronized Socks5Manager getInstance(Context context) {
        if (instance == null)
            instance = new Socks5Manager(context);
        return instance;
    }

    // -------------------------------------------------------------------------
    // Config
    // -------------------------------------------------------------------------

    public boolean isEnabled() {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getBoolean(PREF_ENABLED, false);
    }

    public InetSocketAddress getEndpoint() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        String addr = prefs.getString(PREF_ADDR, DEFAULT_ADDR);
        int port;
        try {
            port = Integer.parseInt(prefs.getString(PREF_PORT, String.valueOf(DEFAULT_PORT)));
        } catch (NumberFormatException e) {
            port = DEFAULT_PORT;
        }
        return new InetSocketAddress(addr, port);
    }

    public String getAddr() {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_ADDR, DEFAULT_ADDR);
    }

    public int getPort() {
        try {
            return Integer.parseInt(
                    PreferenceManager.getDefaultSharedPreferences(context)
                            .getString(PREF_PORT, String.valueOf(DEFAULT_PORT)));
        } catch (NumberFormatException e) {
            return DEFAULT_PORT;
        }
    }

    public String getUsername() {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_USERNAME, "");
    }

    public String getPassword() {
        return PreferenceManager.getDefaultSharedPreferences(context)
                .getString(PREF_PASSWORD, "");
    }

    // -------------------------------------------------------------------------
    // Health check
    // -------------------------------------------------------------------------

    /**
     * Returns true if a TCP connection to the configured SOCKS5 endpoint
     * can be established within HEALTH_CHECK_TIMEOUT_MS.
     * Must not be called on the main thread.
     */
    public boolean isReachable() {
        InetSocketAddress ep = getEndpoint();
        try (Socket s = new Socket()) {
            s.connect(ep, HEALTH_CHECK_TIMEOUT_MS);
            Log.d(TAG, "SOCKS5 proxy reachable at " + ep);
            return true;
        } catch (IOException e) {
            Log.w(TAG, "SOCKS5 proxy not reachable at " + ep + ": " + e.getMessage());
            return false;
        }
    }
}
