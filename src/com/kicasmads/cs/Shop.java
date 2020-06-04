package com.kicasmads.cs;

import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Shop {
    private final ShopType type;
    private final UUID owner;
    private final Location sign;
    private final Location chest;
    private final ItemStack buyItem;
    private final ItemStack sellItem;
    private final int sellAmount;
    private final int buyAmount;

    public Shop(ShopType type, UUID owner, Location sign, Location chest, ItemStack buyItem, ItemStack sellItem,
                int buyAmount, int sellAmount) {
        this.type = type;
        this.owner = owner;
        this.sign = sign;
        this.chest = chest;
        this.buyItem = buyItem;
        this.sellItem = sellItem;
        this.buyAmount = buyAmount;
        this.sellAmount = sellAmount;
    }

    public Shop(NBTTagCompound tag) {
        type = ShopType.values()[tag.getInt("type")];
        owner = UUID.fromString(tag.getString("owner"));
        sign = Utils.locationFromNBT(tag.getCompound("signLocation"));
        chest = Utils.locationFromNBT(tag.getCompound("chestLocation"));
        buyItem = Utils.itemStackFromNBT(tag.getCompound("buyItem"));
        sellItem = Utils.itemStackFromNBT(tag.getCompound("sellItem"));
        buyAmount = tag.getInt("buyAmount");
        sellAmount = tag.getInt("sellAmount");
    }

    public final ShopType getType() {
        return type;
    }

    public UUID getOwner() {
        return owner;
    }

    public void tryTransaction(Player player) {
        Chest shopChest = (Chest) chest.getBlock();
        Inventory inv = shopChest.getBlockInventory();

        // Make sure shop can pay out
        if(!inv.containsAtLeast(sellItem, sellAmount)) {
            player.chat("This shop is out of stock. Come back later.");
            return;
        }

        if(inv.firstEmpty() == -1) {
            player.sendMessage(ChatColor.RED + "Chest is full and cannot accept any more transactions");
            return;
        }

        Inventory playerInv = player.getInventory();

        // Make sure player can pay
        if(!playerInv.containsAtLeast(buyItem, buyAmount)) {
            player.chat("You need " + buyItem.getAmount() + " " + buyItem.toString() + " in order to buy this.");
            return;
        }

        removePlayerCost(playerInv);

        givePlayerItems(player);
    }

    private void removeSellItems() {
        Chest shopChest = (Chest) chest.getBlock();
        int numRemoved = 0;

        while(numRemoved < sellAmount) {
            // Get first instance of sell item
            ItemStack stack = shopChest.getBlockInventory().getItem(shopChest.getBlockInventory().first(sellItem));
            numRemoved += stack.getAmount();
            // Set the stack to either how many we have left to remove or 0
            stack.setAmount(Math.max(stack.getAmount() - (sellAmount - numRemoved), 0));
            // Remove the stack if it has an amount of 0
            if(stack.getAmount() == 0) shopChest.getBlockInventory().remove(stack);
        }
    }

    private void removePlayerCost(Inventory playerInv) {
        int numRemoved = 0;

        while(numRemoved < buyAmount) {
            ItemStack stack = playerInv.getItem(playerInv.first(buyItem));
            numRemoved += stack.getAmount();

            stack.setAmount(Math.min(stack.getAmount() - (buyAmount - numRemoved), 0));
            if(stack.getAmount() == 0) playerInv.remove(stack);
        }
    }

    private void givePlayerItems(Player player) {
        Inventory playerInv = player.getInventory();
        int numGiven = 0;

        // Loop until we have no more left to give or we can't insert any more items
        while(numGiven < sellAmount && (playerInv.firstEmpty() == -1 && playerInv.first(sellItem) == -1)) {
            // See if we can insert into an already existing stack
            if(playerInv.first(sellItem) == -1) {
                ItemStack[] contents = playerInv.getStorageContents();
                ItemStack giveItem = sellItem.clone();
                // Set the stack size to either the max stack or the amount we have left to give
                giveItem.setAmount(Math.min(sellAmount - numGiven, sellItem.getMaxStackSize()));
                numGiven += giveItem.getAmount();
                // Insert the new stack into the player's inventory
                contents[playerInv.firstEmpty()] = giveItem;
            } else {
                ItemStack stack = playerInv.getItem(playerInv.first(sellItem));
                int ogAmonut = stack.getAmount();

                // Set the stack to either the amount we have left + the current amount or the max size
                stack.setAmount(Math.min(stack.getAmount() + (sellAmount - numGiven), sellItem.getMaxStackSize()));
                // Only count items we added, not ones that were already there
                numGiven += stack.getAmount() - ogAmonut;
            }

        }

        // Drop any extra items
        if(numGiven < sellAmount) {
            int amountToDrop = sellAmount - numGiven;
            while(amountToDrop > 0) {
                ItemStack dropStack = sellItem.clone();
                dropStack.setAmount(Math.min(amountToDrop, dropStack.getMaxStackSize()));
                amountToDrop -= dropStack.getAmount();
                player.getWorld().dropItem(player.getLocation(), dropStack);

            }
            player.sendMessage(ChatColor.GREEN + "Your inventory was full so the items you bought was dropped on the floor.");
        }
    }

    public void displayItems() {
        switch (type) {
            case BUY:
                displayItem(chest.clone().add(0.5, 1, 0.5), buyItem);
                break;

            case SELL:
                displayItem(chest.clone().add(0.5, 1, 0.5), sellItem);
                break;

            case BARTER: {
                // Display it so that the buy item is on the left and the sell item is on the right
                switch (((WallSign) sign.getBlock()).getFacing()) {
                    case NORTH:
                        displayItem(chest.clone().add(0.8, 1, 0.5), buyItem);
                        displayItem(chest.clone().add(0.2, 1, 0.5), sellItem);
                        break;

                    case SOUTH:
                        displayItem(chest.clone().add(0.2, 1, 0.5), buyItem);
                        displayItem(chest.clone().add(0.8, 1, 0.5), sellItem);
                        break;

                    case EAST:
                        displayItem(chest.clone().add(0.5, 1, 0.2), buyItem);
                        displayItem(chest.clone().add(0.5, 1, 0.8), sellItem);
                        break;

                    case WEST:
                        displayItem(chest.clone().add(0.5, 1, 0.8), buyItem);
                        displayItem(chest.clone().add(0.5, 1, 0.2), sellItem);
                        break;
                }
            }
        }
    }

    private void displayItem(Location location, ItemStack stack) {
        // Check to see if the item already exists
        if (location.getWorld().getNearbyEntities(location, 0.2, 0.2, 0.2, entity -> {
            if (entity instanceof Item)
                return ((Item) entity).getItemStack().isSimilar(stack);

            return false;
        }).isEmpty())
        {
            // Summon a persistent, non-pickup-able item
            Item itemEntity = (Item) location.getWorld().spawnEntity(location, EntityType.DROPPED_ITEM);
            itemEntity.setItemStack(stack.clone());
            itemEntity.setTicksLived(-32768);
            itemEntity.setPickupDelay(32767);
        }
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound shopTag = new NBTTagCompound();

        shopTag.setInt("buyAmount", buyAmount);
        shopTag.setInt("sellAmount", sellAmount);
        shopTag.setInt("type", type.ordinal());
        shopTag.setString("owner", owner.toString());
        shopTag.set("sellItem", Utils.itemStackToNBT(sellItem));
        shopTag.set("buyItem", Utils.itemStackToNBT(buyItem));
        shopTag.set("location", Utils.locationToNBT(chest));

        return shopTag;
    }
}
