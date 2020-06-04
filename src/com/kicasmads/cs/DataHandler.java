package com.kicasmads.cs;

import net.minecraft.server.v1_15_R1.NBTBase;
import net.minecraft.server.v1_15_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagList;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class DataHandler {

	private final List<Shop> shops;
	private final HashMap<Location, Shop> shopLocations;
	private final HashMap<Location, ShopBuilder> builderCache;

	public DataHandler() {
		shops = new ArrayList<>();
		shopLocations = new HashMap<>();
		builderCache = new HashMap<>();
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

			for (NBTBase nbtBase : shopList)
				shops.add(new Shop((NBTTagCompound) nbtBase));
		}
	}

	public void cacheBuilder(Location sign, ShopBuilder builder) {
		builderCache.put(sign, builder);
	}

	public ShopBuilder getCachedBuilder(Location sign, Player player) {
		ShopBuilder builder = builderCache.get(sign);
		if(builder.isOwner(player)) return builder;
		return null;
	}

	public void removeCachedBuilder(Location sign) {
		builderCache.remove(sign);
	}

	public void addShop(Shop shop) {
		shops.add(shop);
	}
}
