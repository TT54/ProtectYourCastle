package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.inventories.trades.TradeListInventory;
import fr.tt54.protectYourCastle.utils.FileManager;
import fr.tt54.protectYourCastle.utils.SavedLocation;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.entity.Villager;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.Merchant;
import org.bukkit.inventory.MerchantRecipe;

import java.io.File;
import java.lang.reflect.Type;
import java.util.*;

public class Trader {

    private static final Type traderType = new TypeToken<Map<UUID, Trader>>() {}.getType();
    private static final Type weaponsType = new TypeToken<List<GameWeapon>>() {}.getType();

    public static Map<UUID, Trader> traders = new HashMap<>();
    public static List<GameWeapon> weapons = new ArrayList<>();

    public static void load(){
        traders.clear();
        weapons.clear();

        File tradersFile = FileManager.getFileWithoutCreating("traders.json", ProtectYourCastleMain.getInstance());
        if (!tradersFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("traders.json", false);
        }
        traders = Game.gson.fromJson(FileManager.read(tradersFile), traderType);

        File weaponsFile = FileManager.getFileWithoutCreating("weapons.json", ProtectYourCastleMain.getInstance());
        if (!weaponsFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("weapons.json", false);
        }
        weapons = Game.gson.fromJson(FileManager.read(weaponsFile), weaponsType);
    }

    public static void save(){
        File tradersFile = FileManager.getFile("traders.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(traders), tradersFile);

        File weaponsFile = FileManager.getFileWithoutCreating("weapons.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(weapons), weaponsFile);
    }

    public static boolean isTrader(UUID entityUUID) {
        return traders.containsKey(entityUUID);
    }

    public static void removeTrader(UUID traderUUID) {
        traders.remove(traderUUID);
    }

    public static Trader getTrader(UUID traderUUID) {
        return traders.get(traderUUID);
    }

    public static void openTradeMenu(UUID entityUUID, Player player){
        player.openMerchant(traders.get(entityUUID).getMerchantMenu(), true);
    }

    public static void openEditionMenu(UUID traderUUID, Player player) {
        TradeListInventory inv = new TradeListInventory(player, 1, traders.get(traderUUID));
        inv.openInventory();
    }

    private final List<NPCTrade> trades;
    private final String name;
    private boolean weaponTrader;
    private SavedLocation savedLocation;

    public Trader(String name, boolean weaponTrader) {
        this.weaponTrader = weaponTrader;
        this.trades = new ArrayList<>();
        this.name = name;
    }

    public Trader(String name, List<NPCTrade> trades, boolean weaponTrader) {
        this.trades = trades;
        this.name = name;
        this.weaponTrader = weaponTrader;
    }

    public void respawn(){
        if(this.savedLocation != null){
            Location location = this.savedLocation.toLocation();
            if(location.getWorld() != null){
                if(!location.getWorld().isChunkLoaded(location.getChunk())) location.getWorld().loadChunk(location.getChunk());
                for(Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1, entity -> entity instanceof Villager && entity.getCustomName() != null && entity.getCustomName().equalsIgnoreCase(this.name))){
                    entity.remove();
                }
                for(Map.Entry<UUID, Trader> entry : new ArrayList<>(traders.entrySet())){
                    if(entry.getValue() == this) traders.remove(entry.getKey());
                }
                this.spawn(location);
            }
        }
    }

    public SavedLocation getSavedLocation() {
        return savedLocation;
    }

    public void spawn(Location location){
        location = location.clone();
        this.savedLocation = SavedLocation.fromLocation(location);
        location.setPitch(0);
        Villager villager = (Villager) location.getWorld().spawnEntity(location, EntityType.VILLAGER);
        villager.setAI(false);
        villager.setPersistent(true);
        villager.setInvulnerable(true);
        villager.setSilent(true);
        villager.setCustomName(this.name);
        villager.setCustomNameVisible(true);
        villager.setCollidable(false);
        villager.setMaxHealth(1024);
        villager.setHealth(1024);

        traders.put(villager.getUniqueId(), this);
    }



    public void addTrade(NPCTrade trade){
        this.trades.add(trade);
    }

    public String getName() {
        return name;
    }

    public Merchant buildMerchantMenu(){
        Merchant merchantMenu = Bukkit.createMerchant(this.name);
        List<MerchantRecipe> recipes = new ArrayList<>();
        final List<NPCTrade> merchantTrades = new ArrayList<>();
        if(this.weaponTrader && Game.currentGame != null && !Game.currentGame.selectedWeapons.isEmpty()){
            Game.currentGame.selectedWeapons.forEach(gameWeapon -> {
                merchantTrades.add(gameWeapon.gunTrade);
                merchantTrades.add(gameWeapon.ammoTrade);
            });
        } else{
            merchantTrades.addAll(trades);
        }
        for(NPCTrade trade : merchantTrades){
            MerchantRecipe recipe = new MerchantRecipe(trade.reward.clone(), Integer.MAX_VALUE);
            for(ItemStack is : trade.input){
                recipe.addIngredient(is.clone());
            }
            recipes.add(recipe);
        }
        merchantMenu.setRecipes(recipes);
        return merchantMenu;
    }

    private Merchant getMerchantMenu() {
        return this.buildMerchantMenu();
    }

    public List<NPCTrade> getTrades() {
        return this.trades;
    }

    public void removeTrade(NPCTrade trade) {
        this.trades.remove(trade);
    }

    public boolean isWeaponTrader() {
        return weaponTrader;
    }

    public void setWeaponTrader(boolean weaponTrader) {
        this.weaponTrader = weaponTrader;
    }

    public static class NPCTrade{

        private List<ItemStack> input;
        private ItemStack reward;

        public NPCTrade(List<ItemStack> input, ItemStack reward) {
            this.input = input;
            this.reward = reward;
        }

        public List<ItemStack> getInput() {
            return input;
        }

        public void setInput(List<ItemStack> input) {
            this.input = input;
        }

        public ItemStack getReward() {
            return reward;
        }

        public void setReward(ItemStack reward) {
            this.reward = reward;
        }

        @Override
        public NPCTrade clone() {
            List<ItemStack> clonedInput = new ArrayList<>();
            for(ItemStack is : this.input){
                clonedInput.add(is.clone());
            }
            return new NPCTrade(clonedInput, this.reward.clone());
        }
    }

    public static class GameWeapon{

        private final NPCTrade gunTrade;
        private final NPCTrade ammoTrade;

        public GameWeapon(NPCTrade gunTrade, NPCTrade ammoTrade) {
            this.gunTrade = gunTrade;
            this.ammoTrade = ammoTrade;
        }

        public NPCTrade getGunTrade() {
            return gunTrade;
        }

        public NPCTrade getAmmoTrade() {
            return ammoTrade;
        }
    }

}
