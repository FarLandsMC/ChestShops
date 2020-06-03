package com.kicasmads.cs;

import net.minecraft.server.v1_15_R1.NBTTagCompound;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import java.util.UUID;

public class Shop {
    private final ShopType type;
    private final UUID owner;
    private final Location chest;
    private final ItemStack buyItem;
    private final ItemStack sellItem;
    private final int sellAmount;
    private final int buyAmount;

    public Shop(ShopType type, UUID owner, Location chest, ItemStack buyItem, ItemStack sellItem, int buyAmount, int sellAmount) {
        this.type = type;
        this.owner = owner;
        this.chest = chest;
        this.buyItem = buyItem;
        this.sellItem = sellItem;
        this.buyAmount = buyAmount;
        this.sellAmount = sellAmount;
    }

    public Shop(NBTTagCompound tag) {
        type = ShopType.values()[tag.getInt("type")];
        owner = UUID.fromString(tag.getString("owner"));
        chest = Utils.locationFromNBT(tag.getCompound("location"));
        buyItem = Utils.itemStackFromNBT(tag.getCompound("buyItem"));
        sellItem = Utils.itemStackFromNBT(tag.getCompound("sellItem"));
        buyAmount = tag.getInt("buyAmount");
        sellAmount = tag.getInt("sellAmount");
    }

    public final ShopType getType() {
        return type;
    }

    public void tryTransaction(Player player) {
        Chest shopChest = (Chest) chest.getBlock();
        Inventory inv = shopChest.getBlockInventory();

        // Make sure shop can pay out
        if(!inv.containsAtLeast(sellItem, sellAmount)) {
            player.chat("This shop is out of stock. Come back later.");
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
            player.chat("Your inventory was full so the items you bought was dropped on the floor.");
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

    public UUID getOwner() {
        return owner;
    }
}
