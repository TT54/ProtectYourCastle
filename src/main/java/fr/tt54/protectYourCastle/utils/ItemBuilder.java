package fr.tt54.protectYourCastle.utils;

import org.bukkit.Bukkit;
import org.bukkit.Color;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.attribute.AttributeModifier;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.inventory.meta.LeatherArmorMeta;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ItemBuilder {

    private ItemStack is;
    private String name;

    public ItemBuilder(ItemStack is) {
        this.is = is;
    }

    public ItemBuilder(Material material) {
        this.is = new ItemStack(material);
    }

    public ItemBuilder(Material material, int amount) {
        this.is = new ItemStack(material, amount);
    }

    public ItemBuilder(Material material, int amount, String name) {
        this.is = new ItemStack(material, amount);
        this.setName(name);
    }

    public ItemBuilder(Material material, String name) {
        this(material, 1, name);
    }

    public ItemBuilder(ItemStack is, String name) {
        this.is = is;
        this.setName(name);
    }

    public ItemBuilder(ItemBuilder builder) {
        this.is = builder.build();
    }


    public ItemBuilder setName(String name) {
        ItemMeta meta = this.is.getItemMeta();
        if (meta == null) {
            System.out.println("Un item n'a pas d'item meta ??? " + this.is.getType());
            return this;
        }
        meta.setDisplayName(name);
        this.is.setItemMeta(meta);
        this.name = name;
        return this;
    }

    public ItemBuilder addEnchant(Enchantment enchantment, int level) {
        ItemMeta meta = this.is.getItemMeta();
        meta.addEnchant(enchantment, level, true);
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder hideEnchantmentNames(boolean hidden) {
        ItemMeta meta = this.is.getItemMeta();
        if (hidden) {
            meta.addItemFlags(ItemFlag.HIDE_ENCHANTS);
        } else {
            meta.removeItemFlags(ItemFlag.HIDE_ENCHANTS);
        }
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder addLoreLine(String... lines) {
        ItemMeta meta = this.is.getItemMeta();

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        lore.addAll(Arrays.asList(lines));
        meta.setLore(lore);
        this.is.setItemMeta(meta);

        return this;
    }

    public ItemBuilder addLoreLine(List<String> lines) {
        ItemMeta meta = this.is.getItemMeta();

        List<String> lore = meta.getLore();
        if (lore == null) lore = new ArrayList<>();

        lore.addAll(lines);
        meta.setLore(lore);
        this.is.setItemMeta(meta);

        return this;
    }

    public ItemBuilder setLore(List<String> lore) {
        ItemMeta meta = this.is.getItemMeta();
        meta.setLore(lore);
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setLore(String... lore) {
        ItemMeta meta = this.is.getItemMeta();
        meta.setLore(new ArrayList<>(Arrays.asList(lore)));
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder addFlag(ItemFlag... flags) {
        ItemMeta meta = this.is.getItemMeta();
        meta.addItemFlags(flags);
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder removeFlag(ItemFlag... flags) {
        ItemMeta meta = this.is.getItemMeta();
        meta.removeItemFlags(flags);
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemStack build() {
        return this.is.clone();
    }

    @Override
    public ItemBuilder clone() {
        return new ItemBuilder(this.is.clone(), this.name);
    }

    public ItemBuilder setEnchanted() {
        this.addEnchant(Enchantment.ARROW_DAMAGE, 0);
        this.hideEnchantmentNames(true);
        return this;
    }

    public ItemBuilder removeEnchant(Enchantment enchantment) {
        ItemMeta meta = this.is.getItemMeta();
        meta.removeEnchant(enchantment);
        this.is.setItemMeta(meta);
        return this;
    }

    public List<String> getLore() {
        ItemMeta meta = this.is.getItemMeta();
        return meta.getLore() == null ? new ArrayList<>() : meta.getLore();
    }

    public ItemBuilder replaceInName(String toReplace, String replace) {
        ItemMeta meta = this.is.getItemMeta();
        meta.setDisplayName(meta.getDisplayName().replace(toReplace, replace));
        this.is.setItemMeta(meta);
        return this;
    }

    public ItemBuilder setLoreLine(int index, String newLine) {
        List<String> lore = this.getLore();

        while (lore.size() <= index) {
            lore.add("");
        }

        lore.set(index, newLine);
        this.setLore(lore);
        return this;
    }

    public ItemBuilder replaceLastLoreLine(String line) {
        if (this.getLore().isEmpty()) {
            return this.addLoreLine(line);
        }
        return this.setLoreLine(this.getLore().size() - 1, line);
    }

    public ItemBuilder removeLoreLine(int index) {
        List<String> lore = this.getLore();
        lore.remove(index);
        this.setLore(lore);
        return this;
    }

    public ItemBuilder replaceInLore(String toReplace, String replace) {
        List<String> lore = this.getLore();
        for (int i = 0; i < lore.size(); i++) {
            this.setLoreLine(i, lore.get(i).replace(toReplace, replace));
        }

        return this;
    }

    public ItemBuilder setAmount(int amount) {
        this.is.setAmount(amount);
        return this;
    }

    public ItemBuilder setType(Material material) {
        this.is.setType(material);
        return this;
    }

    public ItemBuilder executeIf(Predicate<ItemBuilder> condition, Consumer<ItemBuilder> action) {
        return executeIfElse(condition, action, itemBuilder -> {
        });
    }

    public ItemBuilder executeIfElse(Predicate<ItemBuilder> condition, Consumer<ItemBuilder> action, Consumer<ItemBuilder> elseAction) {
        if (condition.test(this)) {
            action.accept(this);
        } else {
            elseAction.accept(this);
        }
        return this;
    }

    public ItemBuilder addPrefix(String prefix) {
        return this.setName(prefix + name);
    }

    /**
     * Similar to setType(material)
     *
     * @param material
     * @return the ItemBuilder
     */
    public ItemBuilder setMaterial(Material material) {
        return this.setType(material);
    }

    public ItemBuilder setHeadOwner(OfflinePlayer owner) {
        if (this.is.getItemMeta() instanceof SkullMeta meta) {
            meta.setOwningPlayer(owner);
            this.is.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder setHeadOwner(String ownerName) {
        return this.setHeadOwner(Bukkit.getOfflinePlayer(ownerName));
    }

    public ItemBuilder setLeatherColor(Color color) {
        if (this.is.getItemMeta() instanceof LeatherArmorMeta meta) {
            meta.setColor(color);
            this.is.setItemMeta(meta);
        }
        return this;
    }

    public ItemBuilder addAttribute(Attribute attribute, AttributeModifier modifier) {
        ItemMeta meta = this.is.getItemMeta();
        meta.addAttributeModifier(attribute, modifier);
        this.is.setItemMeta(meta);
        return this;
    }
}
