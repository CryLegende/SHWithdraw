package org.Cry_Legende.sHWithdraw;

import de.tr7zw.nbtapi.NBT;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Un type d'objet encaissable (chèque ou bouteille d'EXP), au format de BeastWithdraw.
 */
final class NoteType {

    final String id;
    final String permission;

    Material material;
    boolean glow;
    String nbtKey;
    String playerName;
    String serverName;
    List<String> playerLore;
    List<String> serverLore;
    double min;
    double max;
    int maxPerCommand;
    double taxPercent;
    String soundCreate;
    String soundRedeem;
    boolean autoCollect;
    private DecimalFormat format;

    NoteType(String id, String permission) {
        this.id = id;
        this.permission = permission;
    }

    void load(ConfigurationSection s) {
        Material configured = Material.matchMaterial(s.getString("materiau", "PAPER"));
        material = configured == null ? Material.PAPER : configured;
        glow = s.getBoolean("brillant", true);
        nbtKey = s.getString("cle-nbt");
        playerName = s.getString("nom-joueur", "");
        serverName = s.getString("nom-serveur", playerName);
        playerLore = s.getStringList("lore-joueur");
        serverLore = s.getStringList("lore-serveur");
        min = s.getDouble("min", 1);
        max = s.getDouble("max", Double.MAX_VALUE);
        maxPerCommand = Math.max(1, s.getInt("max-par-commande", 64));
        taxPercent = Math.max(0, Math.min(100, s.getDouble("taxe", 0)));
        soundCreate = s.getString("son-creation", "");
        soundRedeem = s.getString("son-encaissement", "");
        autoCollect = s.getBoolean("ramassage-auto", true);
        format = new DecimalFormat(s.getString("format", "###,##0.00"), DecimalFormatSymbols.getInstance(Locale.ENGLISH));
        format.setRoundingMode(RoundingMode.DOWN);
    }

    String format(double value) {
        return format.format(value);
    }

    /**
     * signer null : objet "serveur" (donné par une commande), sans signataire dans le lore.
     */
    ItemStack create(double value, int count, String signer) {
        ItemStack item = new ItemStack(material, Math.max(1, count));
        ItemMeta meta = item.getItemMeta();
        boolean signed = signer != null;
        meta.setDisplayName(color(signed ? playerName : serverName));
        List<String> lore = new ArrayList<>();
        for (String line : signed ? playerLore : serverLore) {
            lore.add(color(line.replace("%amount%", format(value)).replace("%player%", signed ? signer : "")));
        }
        meta.setLore(lore);
        if (glow) {
            meta.setEnchantmentGlintOverride(true);
        }
        item.setItemMeta(meta);
        // Mêmes clés que BeastWithdraw : les deux plugins lisent les objets de l'autre
        NBT.modify(item, nbt -> {
            nbt.setDouble(nbtKey, value);
            nbt.setBoolean("bCraft", true);
        });
        return item;
    }

    /**
     * Valeur enregistrée dans l'objet, ou -1 si ce n'est pas un objet de ce type.
     */
    double read(ItemStack item) {
        if (item == null || item.getType() != material || !item.hasItemMeta()) {
            return -1;
        }
        return NBT.get(item, nbt -> nbt.hasTag(nbtKey) ? nbt.getDouble(nbtKey) : -1d);
    }

    static String color(String text) {
        return ChatColor.translateAlternateColorCodes('&', text);
    }
}
