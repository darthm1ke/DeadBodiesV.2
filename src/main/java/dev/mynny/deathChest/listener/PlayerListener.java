package dev.mynny.deathChest.listener;

import dev.mynny.deathChest.Main;
import dev.mynny.deathChest.manager.BodyManager;
import dev.mynny.deathChest.manager.BodyManager.BodyData;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.GameRule;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.ArmorStand;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;
import org.bukkit.util.EulerAngle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class PlayerListener implements Listener {

    private final BodyManager bodyManager;
    private final Map<UUID, UUID> lastOpenedBody = new HashMap<>();

    public PlayerListener(BodyManager bodyManager) {
        this.bodyManager = bodyManager;
    }

    @EventHandler
    public void onPlayerDeath(PlayerDeathEvent event) {
        Player player = event.getEntity();
        Location deathLocation = player.getLocation().getBlock().getLocation();
        Boolean keepInventory = player.getWorld().getGameRuleValue(GameRule.KEEP_INVENTORY);

        if (keepInventory != null && keepInventory) {
            return;
        }

        event.getDrops().clear();
        event.setDroppedExp(0);

        List<ItemStack> items = new ArrayList<>();
        for (ItemStack item : player.getInventory().getContents()) {
            addIfItem(items, item);
        }
        for (ItemStack item : player.getInventory().getArmorContents()) {
            addIfItem(items, item);
        }
        addIfItem(items, player.getInventory().getItemInOffHand());

        player.getInventory().clear();
        player.getInventory().setArmorContents(new ItemStack[4]);
        player.getInventory().setItemInOffHand(null);

        Inventory bodyInventory = Bukkit.createInventory(null, 54,
                ChatColor.of("#8d8d8d") + "Body of " + ChatColor.of("#c7cedd") + player.getName());
        for (ItemStack item : items) {
            bodyInventory.addItem(item);
        }

        Location bodyLocation = deathLocation.clone().add(0.5, 0.1, 0.5);
        ArmorStand stand = spawnBodyStand(player, bodyLocation);
        bodyManager.registerBody(stand, bodyInventory, bodyLocation);

        player.sendMessage(ChatColor.of("#ff4400") + "Your items have been stored in your body.");
        if (player.hasPermission(Main.deathchest_perm_location)) {
            player.sendMessage(ChatColor.of("#ff4400") + "Your body is located at " + ChatColor.of("#ff0400")
                    + "X: " + deathLocation.getBlockX() + " Y: " + deathLocation.getBlockY() + " Z: "
                    + deathLocation.getBlockZ());
        }
    }

    @EventHandler
    public void onPlayerInteract(PlayerInteractAtEntityEvent event) {
        if (!(event.getRightClicked() instanceof ArmorStand stand)) {
            return;
        }

        BodyData body = bodyManager.getBody(stand.getUniqueId());
        if (body == null) {
            return;
        }

        event.setCancelled(true);
        openBody(event.getPlayer(), body);
    }

    @EventHandler
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.getTo() == null) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
                && event.getFrom().getBlockY() == event.getTo().getBlockY()
                && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        BodyData body = bodyManager.findNearest(event.getTo(), 2.2);
        if (body == null || body.getInventory().isEmpty()) {
            return;
        }

        UUID playerId = event.getPlayer().getUniqueId();
        UUID bodyId = body.getStand().getUniqueId();
        if (bodyId.equals(lastOpenedBody.get(playerId))) {
            return;
        }

        openBody(event.getPlayer(), body);
    }

    @EventHandler
    public void onInventoryClose(InventoryCloseEvent event) {
        BodyData body = bodyManager.getBodyByInventory(event.getInventory());
        if (body == null) {
            return;
        }

        if (body.getInventory().isEmpty()) {
            bodyManager.removeBody(body.getStand().getUniqueId());
        }
        lastOpenedBody.remove(event.getPlayer().getUniqueId());
    }

    private void openBody(Player player, BodyData body) {
        player.openInventory(body.getInventory());
        lastOpenedBody.put(player.getUniqueId(), body.getStand().getUniqueId());
    }

    private ArmorStand spawnBodyStand(Player player, Location location) {
        ArmorStand stand = player.getWorld().spawn(location, ArmorStand.class);
        stand.setArms(true);
        stand.setBasePlate(false);
        stand.setGravity(false);
        stand.setInvulnerable(true);
        stand.setCollidable(false);
        stand.setCanPickupItems(false);
        stand.setSilent(true);
        stand.setVisible(false);
        stand.setHeadPose(new EulerAngle(Math.toRadians(90), 0.0, 0.0));
        stand.setBodyPose(new EulerAngle(Math.toRadians(90), 0.0, 0.0));
        stand.setLeftArmPose(new EulerAngle(0.0, 0.0, Math.toRadians(90)));
        stand.setRightArmPose(new EulerAngle(0.0, 0.0, Math.toRadians(-90)));
        stand.setLeftLegPose(new EulerAngle(0.0, 0.0, 0.0));
        stand.setRightLegPose(new EulerAngle(0.0, 0.0, 0.0));
        stand.setRotation(player.getLocation().getYaw(), 0.0f);
        stand.setCustomName(ChatColor.of("#ff4400") + "☠ " + ChatColor.of("#c7cedd")
                + "R.I.P " + ChatColor.of("#ff0400") + player.getName());
        stand.setCustomNameVisible(true);

        stand.getEquipment().setHelmet(createPlayerHead(player));
        stand.getEquipment().setChestplate(createGrayLeather(Material.LEATHER_CHESTPLATE));
        stand.getEquipment().setLeggings(createGrayLeather(Material.LEATHER_LEGGINGS));
        stand.getEquipment().setBoots(createGrayLeather(Material.LEATHER_BOOTS));

        return stand;
    }

    private ItemStack createPlayerHead(Player player) {
        ItemStack head = new ItemStack(Material.PLAYER_HEAD);
        SkullMeta meta = (SkullMeta) head.getItemMeta();
        if (meta != null) {
            meta.setOwningPlayer(player);
            head.setItemMeta(meta);
        }
        return head;
    }

    private ItemStack createGrayLeather(Material material) {
        ItemStack item = new ItemStack(material);
        LeatherArmorMeta meta = (LeatherArmorMeta) item.getItemMeta();
        if (meta != null) {
            meta.setColor(Color.fromRGB(96, 96, 96));
            item.setItemMeta(meta);
        }
        return item;
    }

    private void addIfItem(List<ItemStack> items, ItemStack item) {
        if (item != null && item.getType() != Material.AIR) {
            items.add(item);
        }
    }
}
