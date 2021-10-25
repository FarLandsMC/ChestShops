package com.kicasmads.cs.data;

import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class SkullCache {

    public static final Map<UUID, String> CACHE = new ConcurrentHashMap<>();

    /**
     * Update a player's information
     * @param uuid The UUID of the player to update
     * @return The new value after updating
     */
    public static String update(@Nonnull UUID uuid) {
        String n = getHeadProperty(uuid);
        CACHE.put(uuid, n);
        return n;

    }

    public static String get(UUID uuid) {
        if (CACHE.containsKey(uuid)) {
            return CACHE.get(uuid);
        } else {
            return update(uuid);
        }
    }

    private static String getHeadProperty(UUID uuid) {
        try {
            URL url = new URL("https://sessionserver.mojang.com/session/minecraft/profile/" + uuid);
            InputStreamReader reader = new InputStreamReader(url.openStream());
            return new JsonParser().parse(reader).getAsJsonObject().get("properties").getAsJsonArray().get(0).getAsJsonObject().get("value").getAsString();
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static ItemStack getHead(OfflinePlayer profile) {
        return getHead(profile.getUniqueId());
    }

    public static ItemStack getHead(UUID uuid) {
        String texture = get(uuid);
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        if (texture == null || texture.isEmpty()) {
            return head;
        }
        ItemMeta headMeta = head.getItemMeta();
        GameProfile profile = new GameProfile(uuid, null);
        profile.getProperties().put("textures", new Property("textures", texture));
        Field profileField = null;
        try {
            profileField = headMeta.getClass().getDeclaredField("profile");
        } catch (NoSuchFieldException | SecurityException e) {
            e.printStackTrace();
        }
        profileField.setAccessible(true);
        try {
            profileField.set(headMeta, profile);
        } catch (IllegalArgumentException | IllegalAccessException e) {
            e.printStackTrace();
        }
        head.setItemMeta(headMeta);
        return head;
    }

}
