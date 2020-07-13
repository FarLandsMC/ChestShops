package com.kicasmads.cs.data;

import com.kicasmads.cs.ChestShops;

import net.minecraft.server.v1_16_R1.NBTBase;
import net.minecraft.server.v1_16_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_16_R1.NBTTagCompound;
import net.minecraft.server.v1_16_R1.NBTTagList;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
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

	public DataHandler() {
		shops = new ArrayList<>();
		shopLocations = new HashMap<>();
		builderCache = new HashMap<>();
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
		shop.removeDisplayItems();
	}

	public Shop getShop(Location location) {
		if (location == null)
			return null;
		return shopLocations.get(location);
	}

	public List<Shop> getShops(Player player) {
		return shops.stream().filter(shop -> shop.isOwner(player)).collect(Collectors.toList());
	}

	public List<Shop> getAllShops() {
		return shops;
	}

	public static void initNbt(String file) {
		File f = new File(ChestShops.getInstance().getDataFolder(), file);

		if(!f.exists()) {
			try {
				if(!f.createNewFile()) throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the file system?");
				NBTCompressedStreamTools.a(new NBTTagCompound(), new FileOutputStream(f));
			} catch (IOException e) {
				throw new RuntimeException("Failed to create " + file + ". Did you give the process access to the file system?", e);
			}
		}
	}

	public void save(String file) {
		initNbt(file);
		File f = new File(ChestShops.getInstance().getDataFolder(), file);

		NBTTagList shopList = new NBTTagList();

		for(Shop shop : shops)
			shopList.add(shop.toNbt());

		NBTTagCompound root = new NBTTagCompound();
		root.set("shops", shopList);

		try {
			NBTCompressedStreamTools.a(root, new FileOutputStream(f));
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to write " + file + ". Did you give the process access to the file system?", e);
		}
	}

	public void load(String file) {
		initNbt(file);
		File f = new File(ChestShops.getInstance().getDataFolder().getAbsolutePath(), file);

		NBTTagCompound root;

		try {
			root = NBTCompressedStreamTools.a(new FileInputStream(f));
		}
		catch (IOException e) {
			throw new RuntimeException("Failed to read " + file + ". Did you give the process access to the file system?", e);
		}

		if(root.hasKey("shops")) {
			NBTTagList shopList = root.getList("shops", 10);

			for (NBTBase nbtBase : shopList) {
				Shop s = new Shop((NBTTagCompound) nbtBase);
				shops.add(s);
				shopLocations.put(s.getSignLocation(), s);
				shopLocations.put(s.getChestLocation(), s);
			}
		}
	}

	public void cacheBuilder(Location sign, ShopBuilder builder) {
		builderCache.put(sign, builder);
		Bukkit.getScheduler().runTaskLater(ChestShops.getInstance(), () -> {
			if (builderCache.remove(sign) != null && builder.getOwner().isOnline())
				builder.getOwner().sendMessage(ChatColor.RED + "You did not create your shop fast enough! You will need to remake it.");
		}, 2 * 60 * 20);
	}
}
