package com.kicasmads.cs.data;

import com.kicasmads.cs.ChestShops;

import net.kyori.adventure.nbt.*;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DataHandler {
	private final List<Shop> shops;
	private final Map<Location, Shop> shopLocations;
	private final Map<Location, ShopBuilder> builderCache;
	private final ProfileUpdater profileUpdater;

	public DataHandler() {
		shops = new ArrayList<>();
		shopLocations = new HashMap<>();
		builderCache = new HashMap<>();
		profileUpdater = new ProfileUpdater(5);
	}

	public ProfileUpdater getProfileUpdater() {
		return profileUpdater;
	}

	public ShopBuilder getCachedBuilder(Location sign) {
		return builderCache.get(sign);
	}

	public void removeCachedBuilder(Location sign) {
		builderCache.remove(sign);
	}

	public void addShop(Shop shop, Location chestLocation, Location signLocation) {
		shops.add(shop);
		shopLocations.put(chestLocation, shop);
		shopLocations.put(signLocation, shop);
	}

	public void removeShop(Shop shop) {
		shops.remove(shop);
		shopLocations.remove(shop.getChestLocation());
		shopLocations.remove(shop.getSignLocation());
		shop.getDisplay().removeShopDisplay();
	}

	public Shop getShop(Location location) {
		if (location == null)
			return null;
		return shopLocations.get(location);
	}

	public List<Shop> getShops(Player player) {
		return shops.stream().filter(shop -> shop.isOwner(player)).collect(Collectors.toList());
	}

	public List<Shop> getShops(UUID uuid) {
		return shops.stream().filter(shop -> shop.getOwner().equals(uuid)).collect(Collectors.toList());
	}

	public List<Shop> getAllShops() {
		return shops;
	}

	public static void initNbt(String file) {
		File f = new File(ChestShops.getInstance().getDataFolder(), file);

		if (!f.exists()) {
			try {
				if (!f.createNewFile())
					throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the file system?");
				BinaryTagIO.writer().write(CompoundBinaryTag.empty(), new FileOutputStream(f));
			} catch (IOException e) {
				throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the file system?", e);
			}
		}
	}

	public void save(String fileName) {
		initNbt(fileName);
		File file = new File(ChestShops.getInstance().getDataFolder(), fileName);

		ListBinaryTag shopList = ListBinaryTag.empty();

		for (Shop shop : shops) {
			shopList = shopList.add(shop.toNbt());
		}

		CompoundBinaryTag root = CompoundBinaryTag.builder().put("shops", shopList).build();

		try {
			BinaryTagIO.writer().write(root, new FileOutputStream(file));
		} catch (IOException e) {
			throw new RuntimeException("Failed to write " + fileName + ". Did you give the process access to the file system?", e);
		}
	}

	public void load(String fileName) {
		initNbt(fileName);
		File file = new File(ChestShops.getInstance().getDataFolder().getAbsolutePath(), fileName);

		CompoundBinaryTag root;

		try {
			root = BinaryTagIO.reader().read(new FileInputStream(file));
		} catch (IOException e) {
			throw new RuntimeException("Failed to read " + fileName + ". Did you give the process access to the file system?", e);
		}

		if (root.keySet().contains("shops")) {
			ListBinaryTag shopList = root.getList("shops");

			for (BinaryTag binaryTag : shopList) {
				CompoundBinaryTag tag = (CompoundBinaryTag) binaryTag;
				Shop shop;
				try {
					shop = new Shop(tag);
				} catch (Throwable ex) {
					ChestShops.error("Failed to load shop: " + ex);
					ex.printStackTrace();
					continue;
				}
				if(shop.removeIfInvalidChest(false)){ // Remove if the block where the chest for the shop should be isn't a chest
					continue;
				}
				shops.add(shop);
				shopLocations.put(shop.getSignLocation(), shop);
				shopLocations.put(shop.getChestLocation(), shop);
			}
			shops.stream().map(Shop::getOwner).distinct().forEach(SkullCache::update); // Update skull cache for all owners on update
		}
	}

	public void cacheBuilder(Location sign, ShopBuilder builder) {
		builderCache.put(sign, builder);
		Bukkit.getScheduler().runTaskLater(ChestShops.getInstance(), () -> {
			if (builderCache.remove(sign) != null && builder.getOwner().isOnline())
				builder.getOwner().sendMessage(ChatColor.RED + "You did not create your shop fast enough! You will need to remake it.");
		}, 2 * 60 * 20);
	}

	public Collection<OfflinePlayer> getAllOwners() {
		return getAllShops().stream().map(Shop::getOwnerOfflinePlayer).distinct().collect(Collectors.toList());
	}
}
