package org.Cry_Legende.sHWithdraw;

import io.papermc.paper.event.player.PlayerPurchaseEvent;
import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.block.Crafter;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;
import org.bukkit.entity.ThrownExpBottle;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.CrafterCraftEvent;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.MerchantInventory;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.StringUtil;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

public class SHWithdraw extends JavaPlugin implements Listener, CommandExecutor, TabCompleter {

    private static final String ADMIN = "shwithdraw.admin";

    private final NoteType cash = new NoteType("cheque", "shwithdraw.cheque");
    private final NoteType xp = new NoteType("bouteille", "shwithdraw.xpbottle");
    // Le clic droit arrive parfois deux fois (main + seconde main) : un encaissement par clic
    private final Set<UUID> redeemDelay = new HashSet<>();
    private Economy economy;
    private boolean dropOnFloor;
    private boolean wholeAmounts;
    private boolean blockCrafts;
    private boolean blockVillagers;

    @Override
    public void onEnable() {
        saveDefaultConfig();
        loadSettings();
        RegisteredServiceProvider<Economy> provider = getServer().getServicesManager().getRegistration(Economy.class);
        if (provider == null) {
            getLogger().severe("Aucune économie Vault trouvée, le plugin se désactive.");
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        economy = provider.getProvider();
        getServer().getPluginManager().registerEvents(this, this);
        for (String name : List.of("cheque", "xpbottle", "shwithdraw")) {
            getCommand(name).setExecutor(this);
            getCommand(name).setTabCompleter(this);
        }
    }

    private void loadSettings() {
        reloadConfig();
        dropOnFloor = getConfig().getBoolean("deposer-au-sol", true);
        wholeAmounts = getConfig().getBoolean("montants-entiers", true);
        blockCrafts = getConfig().getBoolean("bloquer-crafts", true);
        blockVillagers = getConfig().getBoolean("bloquer-villageois", true);
        cash.load(getConfig().getConfigurationSection("cheque"));
        xp.load(getConfig().getConfigurationSection("bouteille"));
    }

    // ------------------------------------------------------------------ retrait (/cheque, /xpbottle)

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (command.getName().equals("shwithdraw")) {
            return onAdmin(sender, args);
        }
        if (!(sender instanceof Player player)) {
            msg(sender, "console");
            return true;
        }
        NoteType type = command.getName().equals("cheque") ? cash : xp;
        if (!player.hasPermission(type.permission + ".withdraw")) {
            msg(player, "pas-de-permission");
            return true;
        }
        double balance = balance(type, player);
        if (args.length == 0) {
            msg(player, type.id + ".aide", "%balance%", type.format(balance));
            return true;
        }

        int count = 1;
        if (args.length >= 2) {
            Integer parsed = parseInt(args[1]);
            if (parsed == null) {
                msg(player, "pas-un-nombre", "%amount%", args[1]);
                return true;
            }
            count = parsed;
            if (count < 1 || count > type.maxPerCommand) {
                msg(player, "max-par-commande", "%stack%", String.valueOf(type.maxPerCommand));
                return true;
            }
        }

        String raw = args[0].toLowerCase(Locale.ROOT);
        double amount;
        if (raw.equals("all")) {
            if (!player.hasPermission(type.permission + ".withdraw.all")) {
                msg(player, "pas-de-permission");
                return true;
            }
            amount = balance / count;
        } else if (type == xp && raw.endsWith("l") && parseInt(raw.substring(0, raw.length() - 1)) != null) {
            // "/xpbottle 10l" : l'EXP des 10 derniers niveaux du joueur
            int levels = parseInt(raw.substring(0, raw.length() - 1));
            if (levels < 1 || levels > player.getLevel()) {
                msg(player, "bouteille.pas-assez-niveaux");
                return true;
            }
            amount = xpAtLevel(player.getLevel()) - xpAtLevel(player.getLevel() - levels);
        } else {
            try {
                amount = Double.parseDouble(raw.replace(',', '.'));
            } catch (NumberFormatException e) {
                msg(player, "pas-un-nombre", "%amount%", args[0]);
                return true;
            }
        }
        if (type == xp || wholeAmounts) {
            amount = Math.floor(amount);
        }

        boolean bypass = player.hasPermission(ADMIN);
        if (amount <= 0 || (!bypass && amount < type.min)) {
            msg(player, type.id + ".min", "%min-amount%", type.format(type.min));
            return true;
        }
        if (!bypass && amount > type.max) {
            msg(player, type.id + ".max", "%max-amount%", type.format(type.max));
            return true;
        }
        double total = amount * count;
        if (balance < total) {
            msg(player, type.id + ".pas-assez", "%balance%", type.format(balance), "%taken-amount%", type.format(total));
            return true;
        }
        if (!dropOnFloor && player.getInventory().firstEmpty() == -1) {
            msg(player, "inventaire-plein");
            return true;
        }

        if (type == cash) {
            if (!economy.withdrawPlayer(player, total).transactionSuccess()) {
                msg(player, "cheque.echec");
                return true;
            }
        } else {
            setTotalXp(player, totalXp(player) - (int) total);
        }

        // La taxe est retirée de la valeur de l'objet, pas du solde
        double tax = bypass ? 0 : amount * type.taxPercent / 100;
        if (tax > 0) {
            msg(player, type.id + ".taxe", "%tax%", type.format(tax * count));
        }
        give(player, type.create(amount - tax, count, player.getName()));
        msg(player, type.id + ".creation", "%taken-amount%", type.format(total), "%balance%", type.format(balance(type, player)));
        sound(player, type.soundCreate);
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (command.getName().equals("shwithdraw")) {
            if (args.length == 1 && sender.hasPermission(ADMIN)) {
                return StringUtil.copyPartialMatches(args[0], List.of("givecashnote", "givecashnoteall",
                    "givexpbottle", "givexpbottleall", "reload", "help"), new ArrayList<>());
            }
            return args.length == 2 && !args[0].toLowerCase(Locale.ROOT).endsWith("all") ? null : List.of();
        }
        if (args.length == 1) {
            return StringUtil.copyPartialMatches(args[0], List.of("all"), new ArrayList<>());
        }
        return List.of();
    }

    // ------------------------------------------------------------------ administration (/shwithdraw)

    private boolean onAdmin(CommandSender sender, String[] args) {
        if (!sender.hasPermission(ADMIN)) {
            msg(sender, "pas-de-permission");
            return true;
        }
        String sub = args.length == 0 ? "help" : args[0].toLowerCase(Locale.ROOT);
        switch (sub) {
            case "reload" -> {
                loadSettings();
                msg(sender, "recharge");
            }
            case "givecashnote", "givenote" -> give(sender, cash, args, false);
            case "givecashnoteall" -> give(sender, cash, args, true);
            case "givexpbottle", "givexpb" -> give(sender, xp, args, false);
            case "givexpbottleall", "givexpball" -> give(sender, xp, args, true);
            default ->
                getConfig().getStringList("messages.aide-admin").forEach(line -> sender.sendMessage(NoteType.color(line)));
        }
        return true;
    }

    /**
     * givecashnote &lt;joueur&gt; &lt;montant&gt; [nombre] [signataire] ; les variantes "all" n'ont pas de joueur.
     */
    private void give(CommandSender sender, NoteType type, String[] args, boolean all) {
        int first = all ? 1 : 2;
        if (args.length <= first) {
            getConfig().getStringList("messages.aide-admin").forEach(line -> sender.sendMessage(NoteType.color(line)));
            return;
        }
        List<Player> targets = new ArrayList<>();
        if (all) {
            targets.addAll(Bukkit.getOnlinePlayers());
        } else {
            Player target = Bukkit.getPlayerExact(args[1]);
            if (target == null) {
                msg(sender, "joueur-introuvable", "%player%", args[1]);
                return;
            }
            targets.add(target);
        }
        double amount;
        try {
            amount = Double.parseDouble(args[first].replace(',', '.'));
        } catch (NumberFormatException e) {
            msg(sender, "pas-un-nombre", "%amount%", args[first]);
            return;
        }
        if (type == xp) {
            amount = Math.floor(amount);
        }
        Integer count = args.length > first + 1 ? parseInt(args[first + 1]) : Integer.valueOf(1);
        if (amount <= 0 || count == null || count < 1) {
            msg(sender, "pas-un-nombre", "%amount%", args.length > first + 1 ? args[first + 1] : args[first]);
            return;
        }
        String signer = args.length > first + 2 ? args[first + 2] : null;
        String noteAmount = count > 1 ? "&7(x" + count + ")" : "";
        for (Player target : targets) {
            give(target, type.create(amount, count, signer));
            msg(target, type.id + (all ? ".donne-a-tous" : ".recu"), "%received-amount%", type.format(amount), "%note-amount%", noteAmount);
        }
        if (sender instanceof Player && !all) {
            msg(sender, "admin-donne", "%player%", targets.get(0).getName(), "%received-amount%", type.format(amount), "%note-amount%", noteAmount);
        }
    }

    // ------------------------------------------------------------------ encaissement

    @EventHandler(priority = EventPriority.HIGH)
    public void onInteract(PlayerInteractEvent event) {
        if (event.getAction() != Action.RIGHT_CLICK_AIR && event.getAction() != Action.RIGHT_CLICK_BLOCK) {
            return;
        }
        ItemStack item = event.getItem();
        NoteType type = typeOf(item);
        if (type == null) {
            return;
        }
        event.setCancelled(true);
        Player player = event.getPlayer();
        if (!player.hasPermission(type.permission + ".redeem")) {
            msg(player, "pas-de-permission");
            return;
        }
        UUID uuid = player.getUniqueId();
        if (!redeemDelay.add(uuid)) {
            return;
        }
        Bukkit.getScheduler().runTaskLater(this, () -> redeemDelay.remove(uuid), 4L);

        double value = type.read(item);
        EquipmentSlot hand = event.getHand() == null ? EquipmentSlot.HAND : event.getHand();
        ItemStack single = item.asOne();
        // On retire l'objet avant de créditer : impossible d'encaisser deux fois le même
        if (item.getAmount() > 1) {
            item.setAmount(item.getAmount() - 1);
            player.getInventory().setItem(hand, item);
        } else {
            player.getInventory().setItem(hand, null);
        }

        if (type == xp && !xp.autoCollect) {
            // Bouteille lancée : l'EXP est fixée à l'impact (onBottle)
            ThrownExpBottle bottle = player.launchProjectile(ThrownExpBottle.class);
            bottle.setItem(single);
            return;
        }
        if (type == cash) {
            economy.depositPlayer(player, value);
        } else {
            setTotalXp(player, totalXp(player) + (int) value);
        }
        msg(player, type.id + ".encaissement", "%received-amount%", type.format(value), "%balance%", type.format(balance(type, player)));
        sound(player, type.soundRedeem);
    }

    /**
     * Bouteilles lancées par un joueur ou par un distributeur : l'EXP vaut la valeur de la bouteille.
     */
    @EventHandler
    public void onBottle(ExpBottleEvent event) {
        double value = xp.read(event.getEntity().getItem());
        if (value >= 0) {
            event.setExperience((int) value);
        }
    }

    // ------------------------------------------------------------------ protections

    @EventHandler
    public void onPrepareCraft(PrepareItemCraftEvent event) {
        if (blockCrafts && containsNote(event.getInventory().getMatrix())) {
            event.getInventory().setResult(null);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onCrafter(CrafterCraftEvent event) {
        if (blockCrafts && event.getBlock().getState(false) instanceof Crafter crafter
            && containsNote(crafter.getInventory().getContents())) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPurchase(PlayerPurchaseEvent event) {
        if (blockVillagers && event.getPlayer().getOpenInventory().getTopInventory() instanceof MerchantInventory merchant
            && (typeOf(merchant.getItem(0)) != null || typeOf(merchant.getItem(1)) != null)) {
            event.setCancelled(true);
            msg(event.getPlayer(), "craft-bloque");
        }
    }

    private boolean containsNote(ItemStack[] items) {
        for (ItemStack item : items) {
            if (typeOf(item) != null) {
                return true;
            }
        }
        return false;
    }

    private NoteType typeOf(ItemStack item) {
        if (cash.read(item) >= 0) {
            return cash;
        }
        return xp.read(item) >= 0 ? xp : null;
    }

    // ------------------------------------------------------------------ utilitaires

    private double balance(NoteType type, Player player) {
        return type == cash ? economy.getBalance(player) : totalXp(player);
    }

    private void give(Player player, ItemStack item) {
        for (ItemStack left : player.getInventory().addItem(item).values()) {
            player.getWorld().dropItem(player.getLocation(), left);
        }
    }

    private void sound(Player player, String sound) {
        if (sound != null && !sound.isEmpty()) {
            player.playSound(player.getLocation(), sound, 1f, 1f);
        }
    }

    private void msg(CommandSender sender, String key, String... replacements) {
        String message = getConfig().getString("messages." + key, "");
        if (message.isEmpty()) {
            return;
        }
        for (int i = 0; i + 1 < replacements.length; i += 2) {
            message = message.replace(replacements[i], replacements[i + 1]);
        }
        for (String line : message.split("\n")) {
            sender.sendMessage(NoteType.color(line));
        }
    }

    private static Integer parseInt(String raw) {
        try {
            return Integer.parseInt(raw);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    // EXP totale d'un niveau (formules vanilla)
    static int xpAtLevel(int level) {
        if (level <= 16) {
            return level * level + 6 * level;
        }
        if (level <= 31) {
            return (int) (2.5 * level * level - 40.5 * level + 360);
        }
        return (int) (4.5 * level * level - 162.5 * level + 2220);
    }

    static int xpToNextLevel(int level) {
        if (level <= 15) {
            return 2 * level + 7;
        }
        if (level <= 30) {
            return 5 * level - 38;
        }
        return 9 * level - 158;
    }

    static int totalXp(Player player) {
        return xpAtLevel(player.getLevel()) + Math.round(player.getExp() * xpToNextLevel(player.getLevel()));
    }

    static void setTotalXp(Player player, int total) {
        player.setExp(0);
        player.setLevel(0);
        player.setTotalExperience(0);
        player.giveExp(Math.max(0, total));
    }
}
