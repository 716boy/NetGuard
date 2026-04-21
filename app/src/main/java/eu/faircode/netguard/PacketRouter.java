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
import android.util.Log;

/**
 * Decides which tunnel path an allowed packet takes and ensures the
 * required tunnel is running.
 *
 * Routing decision (evaluated in order):
 *   1. uid has TunnelMode.WIREGUARD → WireGuard SOCKS5 endpoint
 *   2. uid has TunnelMode.SOCKS5    → external SOCKS5 proxy endpoint
 *   3. otherwise                    → direct (NetGuard default)
 *
 * The actual redirect is performed by isAddressAllowed() in ServiceSinkhole
 * which reads mapUidTunnel.  PacketRouter's role here is lifecycle management:
 * starting/stopping WireGuard when the VPN service starts/stops, and
 * providing a single place to add future tunnel types.
 */
public class PacketRouter {
    private static final String TAG = "NetGuard.PacketRouter";

    private final Context context;
    private final WireGuardManager wgManager;
    private final Socks5Manager s5Manager;

    public PacketRouter(Context context) {
        this.context = context.getApplicationContext();
        this.wgManager = WireGuardManager.getInstance(context);
        this.s5Manager = Socks5Manager.getInstance(context);
    }

    // -------------------------------------------------------------------------
    // Lifecycle
    // -------------------------------------------------------------------------

    /**
     * Called when the VPN tunnel starts.  Brings up any configured tunnels.
     */
    public void start() {
        Log.i(TAG, "PacketRouter start");

        // Start WireGuard if a config is present
        if (wgManager.hasConfig()) {
            Log.i(TAG, "Starting WireGuard tunnel");
            wgManager.start();
        } else {
            Log.i(TAG, "No WireGuard config — skipping WireGuard start");
        }

        // Log SOCKS5 state (the proxy process is external; we just verify it)
        if (s5Manager.isEnabled()) {
            Log.i(TAG, "SOCKS5 proxy configured at " + s5Manager.getEndpoint());
        }
    }

    /**
     * Called when the VPN tunnel stops.  Tears down managed tunnels.
     */
    public void stop() {
        Log.i(TAG, "PacketRouter stop");
        wgManager.stop();
    }

    // -------------------------------------------------------------------------
    // Status helpers (used by UI)
    // -------------------------------------------------------------------------

    public boolean isWireGuardRunning() {
        return wgManager.isRunning();
    }

    public boolean isSocks5Enabled() {
        return s5Manager.isEnabled();
    }

    /**
     * Returns a human-readable description of the tunnel mode for a given uid.
     */
    public static String describeTunnelMode(TunnelMode mode) {
        switch (mode) {
            case WIREGUARD: return "WireGuard";
            case SOCKS5:    return "SOCKS5";
            default:        return "None";
        }
    }
}
