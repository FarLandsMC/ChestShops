package com.kicasmads.cs.data;

import com.mojang.authlib.GameProfile;
import org.bukkit.Bukkit;
import org.bukkit.craftbukkit.v1_17_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import java.util.*;

public class ProfileUpdater {
    private final Set<UUID> updated;
    private final int rateLimit;
    private long lastMinute;
    private int requests;

    private static final long MINUTES_TO_MILLIS = 1000L * 60L;

    public ProfileUpdater(int maxRequestsPerMin) {
        this.updated = new HashSet<>();
        this.rateLimit = maxRequestsPerMin;
        this.lastMinute = currentMinute();
        this.requests = 0;
    }

    public GameProfile maybeUpdate(GameProfile profile) {
        updateRequestCount();

        Player player = Bukkit.getPlayer(profile.getId());
        if (player != null) {
            updated.add(profile.getId());
            return ((CraftPlayer) player).getProfile();
        }

        if (updated.contains(profile.getId()))
            return profile;

        if (requests < rateLimit) {
            updated.add(profile.getId());
            requests ++;
            return new GameProfile(profile.getId(), null);
        }

        return profile;
    }

    private void updateRequestCount() {
        long currentMinute = currentMinute();
        if (currentMinute != lastMinute) {
            lastMinute = currentMinute;
            requests = 0;
        }
    }

    private static long currentMinute() {
        return System.currentTimeMillis() / MINUTES_TO_MILLIS;
    }
}
