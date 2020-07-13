package com.kicasmads.cs.data;

import com.kicasmads.cs.ChestShops;
import com.kicasmads.cs.Utils;
import com.kicasmads.cs.event.ShopTransactionEvent;

import net.minecraft.server.v1_16_R1.NBTTagCompound;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.data.type.WallSign;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import java.util.UUID;

public class Shop {
    private final ShopType  type;
    private final UUID      owner;

    private final Location  sign;
    private final Location  chest;

    private final ItemStack buyItem;
    private final ItemStack sellItem;
    private final int       sellAmount;
    private final int       buyAmount;

    private       UUID      buyItemEntity;
    private       UUID      sellItemEntity;

    public Shop(ShopType type, UUID owner, Location sign, Location chest, ItemStack buyItem, ItemStack sellItem,
                int buyAmount, int sellAmount) {
        this.type       = type;
        this.owner      = owner;

        this.sign       = sign;
        this.chest      = chest;

        this.buyItem    = buyItem;
        this.sellItem   = sellItem;
        this.buyAmount  = buyAmount;
        this.sellAmount = sellAmount;
    }

    public Shop(NBTTagCompound tag) {
        type  = ShopType.values()[tag.getInt("type")];
        owner = UUID.fromString(tag.getString("owner"));

        sign  = Utils.locationFromNBT(tag.getCompound("signLocation"));
        chest = Utils.locationFromNBT(tag.getCompound("chestLocation"));

        buyItem    = Utils.itemStackFromNBT(tag.getCompound("buyItem"));
        sellItem   = Utils.itemStackFromNBT(tag.getCompound("sellItem"));
        buyAmount  = tag.getInt("buyAmount");
        sellAmount = tag.getInt("sellAmount");

        if (tag.hasKey("buyDisplayItem"))
            buyItemEntity = UUID.fromString(tag.getString("buyDisplayItem"));
        if (tag.hasKey("sellDisplayItem"))
            sellItemEntity = UUID.fromString(tag.getString("sellDisplayItem"));
    }

    public final ShopType getType() {
        return type;
    }

    public UUID getOwner() {
        return owner;
    }

    public boolean isOwner(Player player) {
        return owner.equals(player.getUniqueId());
    }

    public ItemStack getBuyItem() {
        return buyItem;
    }

    public ItemStack getSellItem() {
        return sellItem;
    }

    public int getBuyAmount() {
        return buyAmount;
    }

    public int getSellAmount() {
        return sellAmount;
    }

    public int getRequiredOpenSlots() {
        double changeOnTransaction = ((double) buyAmount) / buyItem.getMaxStackSize() -
                ((double) sellAmount) / sellItem.getMaxStackSize();

        // The number of items in the chest decreases when a transaction occurs
        if (changeOnTransaction <= 0)
            return 1;
        else {
            Inventory chestinventory = ((Chest) chest.getBlock().getState()).getInventory();
            return chestinventory.getSize() - (int) ((((double) chestinventory.getSize()) * sellAmount * buyItem.getMaxStackSize()) /
                    (buyAmount * sellItem.getMaxStackSize()));
        }
    }

    public boolean isEmpty() {
        Chest shopChest = (Chest) chest.getBlock().getState();
        Inventory chestinventory = shopChest.getInventory();
        return chestinventory.first(sellItem) < 0;
    }

    public void tryTransaction(Player player, boolean requireHoldingBuyItem) {
        Chest shopChest = (Chest) chest.getBlock().getState();
        Inventory chestinventory = shopChest.getInventory();

        // Make sure shop can pay out
        if (!chestinventory.containsAtLeast(sellItem, sellAmount)) {
            player.sendMessage(ChatColor.RED + "This shop is out of stock. Come back later.");
            return;
        }

        if (chestinventory.firstEmpty() == -1 && Utils.firstInsertableStack(chestinventory, buyItem) == -1) {
            player.sendMessage(ChatColor.RED + "Chest is full and cannot accept any more transactions");
            return;
        }

        PlayerInventory playerInventory = player.getInventory();

        // Make sure player can pay
        if (!playerInventory.containsAtLeast(buyItem, buyAmount)) {
            player.sendMessage(ChatColor.RED + "You need " + buyItem.getAmount() + " " + Utils.getItemName(buyItem) +
                    " in order to buy this.");
            return;
        }

        // The player must be holding the buy item
        if (requireHoldingBuyItem && !playerInventory.getItemInMainHand().isSimilar(buyItem)) {
            player.sendMessage(ChatColor.RED + "You must be holding item this shop requires from you.");
            return;
        }

        ShopTransactionEvent event = new ShopTransactionEvent(player, this);
        ChestShops.getInstance().getServer().getPluginManager().callEvent(event);
        if (event.isCancelled())
            return;

        removePlayerCost(playerInventory);
        removeSellItems();
        givePlayerItems(player);
        putBuyItems();
    }

    private void removeSellItems() {
        Chest shopChest = (Chest) chest.getBlock().getState();
        int numRemoved = 0;

        while (numRemoved < sellAmount) {
            // Get first instance of sell item
            ItemStack stack = shopChest.getInventory().getItem(shopChest.getInventory().first(sellItem.getType()));
            numRemoved += stack.getAmount();
            // Set the stack to either how many we have left to remove or 0
            stack.setAmount(Math.max(stack.getAmount() - sellAmount, 0));
            // Remove the stack if it has an amount of 0
            if (stack.getAmount() == 0)
                shopChest.getInventory().remove(stack);
        }
    }

    private void removePlayerCost(Inventory playerInv) {
        int numRemoved = 0;

        while (numRemoved < buyAmount) {
            ItemStack stack = playerInv.getItem(playerInv.first(buyItem.getType()));
            numRemoved += stack.getAmount();

            stack.setAmount(Math.max(stack.getAmount() - buyAmount, 0));
            if (stack.getAmount() == 0)
                playerInv.remove(stack);
        }
    }

    private void givePlayerItems(Player player) {
        Inventory playerInv = player.getInventory();
        int numGiven = 0;

        // Loop until we have no more left to give or we can't insert any more items
        while (numGiven < sellAmount && (playerInv.firstEmpty() != -1 || Utils.firstInsertableStack(playerInv, sellItem) != -1)) {
            // See if we can insert into an already existing stack
            if (Utils.firstInsertableStack(playerInv, sellItem) == -1 || sellItem.getMaxStackSize() == 1) {
                ItemStack[] contents = playerInv.getStorageContents();
                ItemStack giveItem = sellItem.clone();
                // Set the stack size to either the max stack or the amount we have left to give
                giveItem.setAmount(Math.min(sellAmount - numGiven, sellItem.getMaxStackSize()));
                numGiven += giveItem.getAmount();
                // Insert the new stack into the player's inventory
                contents[playerInv.firstEmpty()] = giveItem;
                playerInv.setContents(contents);
            } else if (sellItem.getMaxStackSize() != 1) {
                ItemStack stack = playerInv.getItem(Utils.firstInsertableStack(playerInv, sellItem));
                int ogAmount = stack.getAmount();

                // Set the stack to either the amount we have left + the current amount or the max size
                stack.setAmount(Math.min(stack.getAmount() + (sellAmount - numGiven), sellItem.getMaxStackSize()));
                // Only count items we added, not ones that were already there
                numGiven += stack.getAmount() - ogAmount;
            }
        }

        // Drop any extra items
        if (numGiven < sellAmount) {
            int amountToDrop = sellAmount - numGiven;
            while (amountToDrop > 0) {
                ItemStack dropStack = sellItem.clone();
                dropStack.setAmount(Math.min(amountToDrop, dropStack.getMaxStackSize()));
                amountToDrop -= dropStack.getAmount();
                player.getWorld().dropItem(player.getLocation(), dropStack);

            }
            player.sendMessage(ChatColor.GREEN + "Your inventory was full so the items you bought was dropped on the floor.");
        }
    }

    private void putBuyItems() {
        Inventory chestInv = ((Chest) chest.getBlock().getState()).getInventory();
        int numGiven = 0;

        // Loop until we have no more left to give or we can't insert any more items
        while (numGiven < buyAmount && (chestInv.firstEmpty() != -1 || Utils.firstInsertableStack(chestInv, buyItem) != -1)) {
            // See if we can insert into an already existing stack
            if (Utils.firstInsertableStack(chestInv, buyItem) == -1) {
                ItemStack[] contents = chestInv.getStorageContents();
                ItemStack giveItem = buyItem.clone();
                // Set the stack size to either the max stack or the amount we have left to give
                giveItem.setAmount(Math.min(buyAmount - numGiven, buyItem.getMaxStackSize()));
                numGiven += giveItem.getAmount();
                // Insert the new stack into the chest's inventory
                contents[chestInv.firstEmpty()] = giveItem;
                chestInv.setContents(contents);
            } else {
                ItemStack stack = chestInv.getItem(Utils.firstInsertableStack(chestInv, buyItem));
                int ogAmount = stack.getAmount();
                // Set the stack to either the amount we have left + the current amount or the max size
                stack.setAmount(Math.min(stack.getAmount() + (buyAmount - numGiven), buyItem.getMaxStackSize()));
                // Only count items we added, not ones that were already there
                numGiven += stack.getAmount() - ogAmount;
            }
        }
    }

    public void displayItems() {
        switch (type) {
            case BUY:
                displayItem(chest.clone().add(0.5, 0.875, 0.5), buyItem, true);
                break;

            case SELL:
                displayItem(chest.clone().add(0.5, 0.875, 0.5), sellItem, false);
                break;

            case BARTER: {
                // Display it so that the buy item is on the left and the sell item is on the right
                switch (((WallSign) sign.getBlock().getBlockData()).getFacing()) {
                    case NORTH:
                        displayItem(chest.clone().add(0.75, 0.875, 0.5), buyItem,  true);
                        displayItem(chest.clone().add(0.25, 0.875, 0.5), sellItem, false);
                        break;

                    case SOUTH:
                        displayItem(chest.clone().add(0.25, 0.875, 0.5), buyItem,  true);
                        displayItem(chest.clone().add(0.75, 0.875, 0.5), sellItem, false);
                        break;

                    case EAST:
                        displayItem(chest.clone().add(0.5, 0.875, 0.25), buyItem,  true);
                        displayItem(chest.clone().add(0.5, 0.875, 0.75), sellItem, false);
                        break;

                    case WEST:
                        displayItem(chest.clone().add(0.5, 0.875, 0.75), buyItem,  true);
                        displayItem(chest.clone().add(0.5, 0.875, 0.25), sellItem, false);
                        break;
                }
            }
        }
    }

    private void displayItem(Location location, ItemStack stack, boolean isBuy) {
        // Check to see if the item already exists
        if (!((((type == ShopType.BUY    && buyItemEntity  != null) || (type == ShopType.SELL && sellItemEntity != null)) &&
                Bukkit.getEntity(type == ShopType.BUY ? buyItemEntity : sellItemEntity) != null) ||
                (type == ShopType.BARTER && sellItemEntity != null && buyItemEntity != null &&
                Bukkit.getEntity(buyItemEntity) != null && Bukkit.getEntity(sellItemEntity) != null))) {

            // Summon a persistent, non-pickup-able item
            switch (type) {
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
    }

    public void removeDisplayItems() {

        if (buyItemEntity != null) {
            Item buyItem = (Item) Bukkit.getEntity(buyItemEntity);
            if (buyItem != null)
                buyItem.remove();
        }

        if (sellItemEntity != null) {
            Item sellItem = (Item) Bukkit.getEntity(sellItemEntity);
            if (sellItem != null)
                sellItem.remove();
        }
    }

    public NBTTagCompound toNbt() {
        NBTTagCompound shopTag = new NBTTagCompound();

        shopTag.setInt("buyAmount", buyAmount);
        shopTag.setInt("sellAmount", sellAmount);
        shopTag.setInt("type", type.ordinal());
        shopTag.setString("owner", owner.toString());
        shopTag.set("sellItem", Utils.itemStackToNBT(sellItem));
        shopTag.set("buyItem",  Utils.itemStackToNBT(buyItem));

        if (chest != null)
            shopTag.set("chestLocation", Utils.locationToNBT(chest));
        if (sign != null)
            shopTag.set("signLocation",  Utils.locationToNBT(sign));

        if (buyItemEntity != null)
            shopTag.setString("buyDisplayItem",  buyItemEntity.toString());
        if (sellItemEntity != null)
            shopTag.setString("sellDisplayItem", sellItemEntity.toString());

        return shopTag;
    }

    public Location getSignLocation() {
        return sign;
    }

    public Location getChestLocation() {
        return chest;
    }
}
