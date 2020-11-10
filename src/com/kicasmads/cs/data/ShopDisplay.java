package com.kicasmads.cs.data;

import com.kicasmads.cs.Utils;
import net.minecraft.server.v1_16_R2.NBTTagCompound;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.*;
import org.bukkit.inventory.EntityEquipment;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.metadata.MetadataValue;
import org.bukkit.util.EulerAngle;

import java.util.UUID;

public class ShopDisplay {
    private UUID glassCaseAS, sellItemEntity, buyItemEntity;

    private DisplayType displayType;
    private final Shop shop;
    private final Location location;


    public ShopDisplay(Location location, Shop shop) {
        this(location, shop, DisplayType.ITEM_ONLY);
    }

    public ShopDisplay(Location location, Shop shop, DisplayType display) {
        this.displayType = display != null ? display : DisplayType.ITEM_ONLY;
        this.shop = shop;
        this.location = location;

        createShopDisplay();
    }

    public ShopDisplay(NBTTagCompound tag, Location location, Shop shop) {

        this.displayType = DisplayType.values()[tag.getInt("type")];
        this.sellItemEntity = uuidFromString(tag.getString("sellItem"));
        this.buyItemEntity = uuidFromString(tag.getString("buyItem"));
        this.glassCaseAS = uuidFromString(tag.getString("glassCase"));

        this.shop = shop;
        this.location = location;
    }

    public String getDisplayString() {
        return Utils.formattedName(this.displayType);
    }

    ;

    UUID uuidFromString(String s) {
        if (s.equals("none") || s.equals("null")) {
            return null;
        } else {
            return UUID.fromString(s);
        }
    }

    public void createShopDisplay() {
        removeShopDisplay();
        switch (displayType) {
            case ITEM_ONLY:
                createDisplayItem();
                break;
            case ON_WALL:
                createDisplayWall();
                break;
            case LARGE_BLOCK:
                createDisplayBlock();
                break;
            case DISPLAY_CASE:
                createDisplayBlock(new ItemStack(Material.GLASS), false, true);
            case ITEM_FIXED:
                createDisplayFixed();
                break;
            case OFF:
                removeShopDisplay();
                break;
        }
    }


    private void createDisplayItem() { // Create The ITEM_ONLY display (Item Entity)
        ItemStack buyItem = shop.getBuyItem();
        ItemStack sellItem = shop.getSellItem().clone();

        buyItem.setAmount(shop.getBuyAmount());
        sellItem.setAmount(shop.getSellAmount());
        switch (shop.getType()) {
            case BUY:
                displayItem(location.clone().add(0.5, 0.875, 0.5), buyItem, true);
                break;

            case SELL:
                displayItem(location.clone().add(0.5, 0.875, 0.5), sellItem, false);
                break;

            case BARTER: {
                // Display it so that the buy item is on the left and the sell item is on the right
                switch (((WallSign) shop.getSignLocation().getBlock().getBlockData()).getFacing()) {
                    case NORTH:
                        displayItem(location.clone().add(0.75, 0.875, 0.5), buyItem, true);
                        displayItem(location.clone().add(0.25, 0.875, 0.5), sellItem, false);
                        break;

                    case SOUTH:
                        displayItem(location.clone().add(0.25, 0.875, 0.5), buyItem, true);
                        displayItem(location.clone().add(0.75, 0.875, 0.5), sellItem, false);
                        break;

                    case EAST:
                        displayItem(location.clone().add(0.5, 0.875, 0.25), buyItem, true);
                        displayItem(location.clone().add(0.5, 0.875, 0.75), sellItem, false);
                        break;

                    case WEST:
                        displayItem(location.clone().add(0.5, 0.875, 0.75), buyItem, true);
                        displayItem(location.clone().add(0.5, 0.875, 0.25), sellItem, false);
                        break;
                }
            }
        }
    }

    private void displayItem(Location location, ItemStack stack, boolean isBuy) {
        // Summon a persistent, non-pickup-able item
        switch (shop.getType()) {
            case SELL:
                sellItemEntity = Utils.summonStaticItem(location, stack).getUniqueId();
                break;
            case BUY:
                buyItemEntity = Utils.summonStaticItem(location, stack).getUniqueId();
                break;
            case BARTER:
                if (isBuy)
                    buyItemEntity = Utils.summonStaticItem(location, stack).getUniqueId();
                else
                    sellItemEntity = Utils.summonStaticItem(location, stack).getUniqueId();
        }
    }

    private void createDisplayWall() { // Create the ON_WALL display (Item Frame Entity)
        ItemStack sellStack = shop.getSellItem();
        ItemStack buyStack = shop.getBuyItem();
        ItemFrame itemFrame = location.getWorld().spawn(location.clone().add(0, 1, 0), ItemFrame.class);
        itemFrame.setFacingDirection(((WallSign) shop.getSignLocation().getBlock().getBlockData()).getFacing(), true);
        itemFrame.setVisible(false);
        itemFrame.setFixed(true);


        switch (shop.getType()) {
            case SELL:
                itemFrame.setItem(sellStack);
                sellItemEntity = itemFrame.getUniqueId();

                break;
            case BUY:
                itemFrame.setItem(buyStack);
                buyItemEntity = itemFrame.getUniqueId();
                break;
        }
    }

    private void createDisplayBlock(ItemStack item, boolean smallStand, boolean displayCase) { // Create the glass case for CASE - Glass only (Armour Stand Entity)
        ArmorStand armorStand = (ArmorStand) location.getWorld().spawnEntity(location.clone()
                        .subtract(0, .5, 0)
                        .add(.5, smallStand ? .75 : 0, .5),
                EntityType.ARMOR_STAND);
        armorStand.setInvisible(true);
        armorStand.setInvulnerable(true);
        armorStand.setMarker(true); // Make it so that it can be clicked through
        armorStand.setSmall(smallStand);
        armorStand.getEquipment().setHelmet(item);
        if (displayCase) {
            glassCaseAS = armorStand.getUniqueId();
        } else {
            if (shop.getType() == ShopType.BUY) {
                buyItemEntity = armorStand.getUniqueId();
            } else {
                sellItemEntity = armorStand.getUniqueId();
            }
        }
    }

    private void createDisplayBlock() {
        createDisplayBlock(shop.getType() == ShopType.BUY ? shop.getBuyItem() : shop.getSellItem(), false, false);
    }

    private void createDisplayBlock(boolean smallStand) {
        createDisplayBlock(shop.getType() == ShopType.BUY ? shop.getBuyItem() : shop.getSellItem(), smallStand, false);
    }

    private void createDisplayFixed() { // Create the fixed item for ITEM_FIXED and CASE displays (Armour Stand Entity)
        ItemStack item = shop.getType() == ShopType.BUY ? shop.getBuyItem() : shop.getSellItem();
        ArmorStand armorStand;
        if (!item.getType().isBlock()) {
            Location loc = location.clone().subtract(0, .5, 0);
            loc.setPitch(0);
            switch (((WallSign) shop.getSignLocation().getBlock().getBlockData()).getFacing()) {
                case NORTH, SOUTH -> {
                    loc.add(.14, 0, 1.0625);
                    loc.setYaw(180);
                    armorStand = (ArmorStand) location.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                }
                default -> {
                    loc.add(1.0625, 0, .85);
                    loc.setYaw(90);
                    armorStand = (ArmorStand) location.getWorld().spawnEntity(loc, EntityType.ARMOR_STAND);
                }
            }
            armorStand.setRightArmPose(new EulerAngle(-Math.PI / 2.0f, 0, 0));
            armorStand.setInvisible(true);
            armorStand.setInvulnerable(true);
            armorStand.setMarker(true); // Make it so that it can be clicked through
            armorStand.getEquipment().setItemInMainHand(item);
            if (shop.getType() == ShopType.SELL) {
                sellItemEntity = armorStand.getUniqueId();
            } else {
                buyItemEntity = armorStand.getUniqueId();
            }
        } else {
            createDisplayBlock(true);
        }
    }


    public void removeShopDisplay() {
        try {
            if (sellItemEntity != null) {
                Bukkit.getEntity(sellItemEntity).remove();
                sellItemEntity = null;
            }
            if (buyItemEntity != null) {
                Bukkit.getEntity(buyItemEntity).remove();
                buyItemEntity = null;
            }
            if (glassCaseAS != null) {
                Bukkit.getEntity(glassCaseAS).remove();
                glassCaseAS = null;
            }
        } catch (Exception ignored) {
        }
    }

    public boolean isDisplaying(Item item) {
        return item.getUniqueId().equals(buyItemEntity) || item.getUniqueId().equals(sellItemEntity);
    }

    public boolean isShown() {
        return ((displayType == DisplayType.DISPLAY_CASE && glassCaseAS != null) || displayType == DisplayType.ITEM_FIXED) && (sellItemEntity != null || buyItemEntity != null) || // Display is CASE or ITEM_FIXED
                (displayType == DisplayType.ITEM_ONLY && (sellItemEntity != null || buyItemEntity != null)) || // Display is ITEM_ONLY
                (displayType == DisplayType.ON_WALL && sellItemEntity != null); // Display is ON_WALL(item frame)
    }

    public DisplayType cycleDisplayType() {
        int current = displayType.ordinal();
        displayType = current + 1 < DisplayType.values().length ? DisplayType.values()[current + 1] : DisplayType.values()[0];
        if (shop.getType().equals(ShopType.BARTER)) {
            displayType = DisplayType.ITEM_ONLY;
        }
        if (!(shop.getBuyItem().getType().isBlock() ||
                shop.getSellItem().getType().isBlock()) &&
                displayType.equals(DisplayType.LARGE_BLOCK)) {
            cycleDisplayType();
        }
        createShopDisplay();
        return displayType;
    }

    public NBTTagCompound toNBT() {
        NBTTagCompound tag = new NBTTagCompound();
        tag.setInt("type", displayType.ordinal());
        tag.setString("sellItem", sellItemEntity != null ? sellItemEntity.toString() : "null");
        tag.setString("buyItem", buyItemEntity == null ? "none" : buyItemEntity.toString());
        tag.setString("glassCase", glassCaseAS == null ? "none" : glassCaseAS.toString());


        return tag;
    }

}
