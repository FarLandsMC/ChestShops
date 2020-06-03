package com.kicasmads.cs;

import net.minecraft.server.v1_15_R1.NBTCompressedStreamTools;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import net.minecraft.server.v1_15_R1.NBTTagList;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;

public class DataHandler {

	HashMap<UUID, List<Shop>> shops = new HashMap<>();

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

		for(List<Shop> shopsList : shops.values()) {
			for(Shop shop : shopsList) {
				NBTTagCompound shopNbt = shop.toNbt();
				shopList.add(shopNbt);
			}
		}

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

			for (int i = 0; i < shopList.size(); i++) {
				Shop s = new Shop((NBTTagCompound) shopList.get(i));
				if (shops.containsKey(s.getOwner())) shops.get(s.getOwner()).add(s);
				else {
					List<Shop> playerShops = new ArrayList<>();
					playerShops.add(s);
					shops.put(s.getOwner(), playerShops);
				}
			}
		}
	}
}
