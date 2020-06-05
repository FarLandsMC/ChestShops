package com.kicasmads.cs.gui;

import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryAction;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GuiHandler implements Listener {
    private final List<Gui> activeGuis;

    public GuiHandler() {
        this.activeGuis = new CopyOnWriteArrayList<>();
    }

    public void registerActiveGui(Gui gui) {
        activeGuis.add(gui);
    }

    public void removeActiveGui(Gui gui) {
        activeGuis.remove(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        activeGuis.stream().filter(gui -> gui.matches(event.getClickedInventory())).forEach(gui -> gui.onItemClick(event));

        if (activeGuis.stream().anyMatch(gui -> gui.matches(event.getView().getTopInventory()))) {
            InventoryAction action = event.getAction();
            if (event.getClickedInventory() != null && event.getClickedInventory().equals(event.getView().getTopInventory())) {
                if (action == InventoryAction.PLACE_ALL || action == InventoryAction.PLACE_ONE ||
                        action == InventoryAction.PLACE_SOME || action == InventoryAction.HOTBAR_SWAP) {
                    event.setCancelled(true);
                }
            } else {
                if (action == InventoryAction.MOVE_TO_OTHER_INVENTORY)
                    event.setCancelled(true);
            }
        }
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        activeGuis.stream().filter(gui -> gui.matches(event.getInventory())).forEach(Gui::onInventoryClosed);
    }

    @EventHandler
    public void onInventoryDrag(InventoryDragEvent event) {
        event.setCancelled(activeGuis.stream().anyMatch(gui -> gui.matches(event.getView().getTopInventory())));
    }
}
