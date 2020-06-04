package com.kicasmads.cs;

import net.minecraft.server.v1_15_R1.EntityItem;
import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_15_R1.entity.CraftItem;
import org.bukkit.craftbukkit.v1_15_R1.inventory.CraftItemStack;
import org.bukkit.entity.Item;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.UUID;

public class Utils {
    /**
     * Un-formats the given name according to how it was formatted in the formattedName method of this class, and get
     * the enumeration value corresponding to that name. If an enumeration constant could not be found with the
     * unformatted name, then null is returned.
     *
     * @param name  the formatted name.
     * @param clazz the enum class.
     * @param <E>   the enum type.
     * @return the enumeration constant corresponding to the given formatted name in the given class, or null if no such
     * constant could be found.
     */
    @SuppressWarnings("unchecked")
    public static <E extends Enum<E>> E valueOfFormattedName(String name, Class<E> clazz) {
        String elementName = name.replaceAll("-", "_").toUpperCase();
        try {
            Method method = clazz.getMethod("valueOf", String.class);
            return (E) method.invoke(null, elementName);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException ex) {
            return null;
        }
    }

    /**
     * Converts the given enumeration element's name (which should be all capitalized with underscores) and replaces the
     * underscores with hyphens and converts the string to lower case.
     *
     * @param e the enumeration element.
     * @return the formatted name of the given element as defined above.
     */
    public static String formattedName(Enum e) {
        return e.name().replaceAll("_", "-").toLowerCase();
    }

    public static ItemStack itemStackFromNBT(NBTTagCompound nbt) {
        return nbt == null || nbt.isEmpty() ? null : CraftItemStack.asBukkitCopy(net.minecraft.server.v1_15_R1.ItemStack.a(nbt));
    }

    public static NBTTagCompound itemStackToNBT(ItemStack stack) {
        NBTTagCompound nbt = new NBTTagCompound();
        if(stack != null)
            CraftItemStack.asNMSCopy(stack).save(nbt);
        return nbt;
    }

    public static Location locationFromNBT(NBTTagCompound nbt) {
        return new Location(Bukkit.getWorld(UUID.fromString(nbt.getString("world"))), nbt.getInt("x"), nbt.getInt("y"), nbt.getInt("z"));
    }

    public static NBTTagCompound locationToNBT(Location location) {
        NBTTagCompound nbt = new NBTTagCompound();
        nbt.setString("world", location.getWorld().getUID().toString());
        nbt.setInt("x", location.getBlockX());
        nbt.setInt("y", location.getBlockY());
        nbt.setInt("z", location.getBlockZ());
        return nbt;
    }

    public static Item summonStaticItem(Location location, ItemStack item) {
        Item itemEntity = (Item) location.getWorld().dropItem(location, item);
        itemEntity.setVelocity(new Vector());
        itemEntity.setGravity(false);
        ((EntityItem) ((CraftItem) itemEntity).getHandle()).age = -32768;
        itemEntity.setPickupDelay(32767);
        return itemEntity;
    }

    public static int firstInsertableStack(Inventory inv, ItemStack stack) {
        int firstStack = inv.first(stack.getType());

        if(firstStack == -1) return -1;
        if(inv.getItem(firstStack).getAmount() != stack.getMaxStackSize()) return firstStack;

        ItemStack[] invContents = inv.getStorageContents();

        for(int i = firstStack; i < invContents.length; i++) {
            ItemStack currStack = invContents[i];
            if(currStack != null && currStack.getType().equals(stack.getType()) && currStack.getAmount() != currStack.getMaxStackSize()) return i;
        }
        return -1;
    }

    public static String getItemName(ItemStack stack) {
        return toProperCase(stack.getType().name().replaceAll("_", " "));
    }

    public static String toProperCase(String str) {
        StringBuilder builder = new StringBuilder(str.length());
        boolean nextUpperCase = true;

        for(char c : str.toCharArray()) {
            if(Character.isSpaceChar(c)) {
                nextUpperCase = true;
            }
            else if(nextUpperCase) {
                c = Character.toUpperCase(c);
                nextUpperCase = false;
            } else {
                c = Character.toLowerCase(c);
            }

            builder.append(c);
        }

        return builder.toString();
    }
}
