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

/**
 * Per-app tunnel routing mode.
 *
 * NONE      – traffic goes directly through the VPN (NetGuard default behaviour).
 * WIREGUARD – traffic is routed through the WireGuard tunnel managed by WireGuardManager.
 * SOCKS5    – traffic is forwarded through the SOCKS5 proxy managed by Socks5Manager.
 */
public enum TunnelMode {
    NONE(0),
    WIREGUARD(1),
    SOCKS5(2);

    public final int value;

    TunnelMode(int value) {
        this.value = value;
    }

    public static TunnelMode fromValue(int value) {
        for (TunnelMode m : values())
            if (m.value == value)
                return m;
        return NONE;
    }
}
