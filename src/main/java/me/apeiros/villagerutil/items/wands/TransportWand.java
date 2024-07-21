package me.apeiros.villagerutil.items.wands;

import io.github.thebusybiscuit.slimefun4.api.items.ItemGroup;
import io.github.thebusybiscuit.slimefun4.api.items.SlimefunItem;
import io.github.thebusybiscuit.slimefun4.api.recipes.RecipeType;
import io.github.thebusybiscuit.slimefun4.core.handlers.EntityInteractHandler;
import io.github.thebusybiscuit.slimefun4.implementation.Slimefun;
import io.github.thebusybiscuit.slimefun4.implementation.SlimefunItems;
import io.github.thebusybiscuit.slimefun4.libraries.dough.common.ChatColors;
import io.github.thebusybiscuit.slimefun4.libraries.dough.protection.Interaction;
import jdk.jshell.execution.Util;
import me.apeiros.villagerutil.Setup;
import me.apeiros.villagerutil.util.UUIDTagType;
import me.apeiros.villagerutil.util.Utils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.persistence.PersistentDataContainer;

import java.util.List;

public class TransportWand extends SlimefunItem {

    // Charm lore when there is a villager linked
    private final List<String> linkedVillagerLore = List.of(
        ChatColors.color("&7一个魔法信标，可以将连接的村民"),
        ChatColors.color("&7传动到当前位置"),
        ChatColors.color("&e右键点击&7进行传送"),
        "",
        ChatColors.color("&a已连接村民"),
        "",
        ChatColors.color("&b工具")
    );

    // NamespacedKey for PDC
    private final NamespacedKey key = Utils.key("stored_villager_uuid");

    // Creates Villager Transport Wand
    public TransportWand(ItemGroup ig) {
        super(ig, Setup.TRANSPORT_WAND, "VU_TRANSPORT_WAND", RecipeType.ANCIENT_ALTAR, new ItemStack[] {
            SlimefunItems.VILLAGER_RUNE, new ItemStack(Material.MINECART), Setup.TOKEN,
            new ItemStack(Material.MINECART), new ItemStack(Material.END_ROD), new ItemStack(Material.EMERALD_BLOCK),
            Setup.TOKEN, new ItemStack(Material.EMERALD_BLOCK), SlimefunItems.STAFF_ELEMENTAL
        });
    }

    // Creates and returns handler
    private EntityInteractHandler getEntityInteractHandler() {
        return (e, i, offhand) -> {
            // Cancel event
            e.setCancelled(true);

            // Check if the clicked entity is a villager
            Entity en = e.getRightClicked();
            if (en instanceof Villager) {
                // Store villager, player, and inventory
                Villager v = (Villager) en;
                Player p = e.getPlayer();
                Inventory inv = p.getInventory();

                // Check for permission
                if (!Slimefun.getProtectionManager().hasPermission(p, p.getLocation(), Interaction.INTERACT_ENTITY)) {
                    p.sendMessage(ChatColors.color("&c你没有权限!"));
                    v.shakeHead();
                    return;
                }

                // Check for villager tokens
                if (!Utils.hasToken(p, inv)) {
                    p.sendMessage(ChatColors.color("&c村民令牌不足!"));
                    v.shakeHead();
                    return;
                }

                // 检查玩家是否拥有村民所在领地的 destroy 权限
                if (!Utils.hasPlayerFlagPermission(p, v.getLocation(), "destroy")){
                    p.sendMessage(ChatColors.color("&c你没有该村民所在领地的 destroy 权限!"));
                    v.shakeHead();
                    return;
                }

                // Create transport charm
                ItemStack charm = Setup.TRANSPORT_CHARM.clone();
                ItemMeta meta = charm.getItemMeta();

                // Null check
                if (meta != null) {
                    // Store PDC
                    PersistentDataContainer pdc = meta.getPersistentDataContainer();

                    // Add UUID to PDC
                    pdc.set(key, new UUIDTagType(), v.getUniqueId());

                    // Update lore of charm and set meta to charm
                    meta.setLore(linkedVillagerLore);
                    charm.setItemMeta(meta);

                    // Attempt to add charm to player inventory
                    if (inv.addItem(charm).isEmpty()) {
                        // Consume villager token
                        Utils.removeToken(p, inv);
                    } else {
                        p.sendMessage(ChatColors.color("&c你的物品栏已满!"));
                    }

                    // Play sounds
                    World w = v.getWorld();
                    Location l = v.getLocation();
                    w.playSound(l, Sound.ITEM_LODESTONE_COMPASS_LOCK, 1F, 1.5F);
                    w.playSound(l, Sound.BLOCK_BEACON_POWER_SELECT, 1F, 1F);
                }
            }
        };
    }

    // Registers handler
    public void preRegister() {
        this.addItemHandler(getEntityInteractHandler());
    }

}
