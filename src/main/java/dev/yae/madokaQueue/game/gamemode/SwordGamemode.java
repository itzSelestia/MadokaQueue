package dev.yae.madokaQueue.game.gamemode;

import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

public class SwordGamemode extends Gamemode {
    @Override
    public String getName() {
        return "Sword";
    }

    @Override
    public void equipKit(Player player) {
        PlayerInventory inventory = player.getInventory();
        inventory.clear();

        inventory.setHelmet(protected_(Material.DIAMOND_HELMET));
        inventory.setChestplate(protected_(Material.DIAMOND_CHESTPLATE));
        inventory.setLeggings(protected_(Material.DIAMOND_LEGGINGS));
        inventory.setBoots(protected_(Material.DIAMOND_BOOTS));

        inventory.setItem(0, new ItemStack(Material.DIAMOND_SWORD));
        inventory.setItem(1, new ItemStack(Material.WOODEN_SWORD));
    }

    private ItemStack protected_(Material material) {
        ItemStack item = new ItemStack(material);
        item.addEnchantment(Enchantment.PROTECTION, 3);
        return item;
    }

    @Override
    public void prepareGamemode() {
        super.prepareGamemode();
    }
}
