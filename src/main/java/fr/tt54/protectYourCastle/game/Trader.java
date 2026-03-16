package fr.tt54.protectYourCastle.game;

import com.google.common.reflect.TypeToken;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.inventories.traders.npc.EditTraderNPCInventory;
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

    private static final Type tradersNPCsType = new TypeToken<Map<UUID, Trader>>() {}.getType();
    private static final Type availableTradesType = new TypeToken<Map<String, TradesBase>>() {}.getType();
    private static final Type weaponsType = new TypeToken<List<List<GameWeapon>>>() {}.getType();

    public static Map<UUID, Trader> tradersNPCs = new HashMap<>();
    public static Map<String, TradesBase> availableTrades = new HashMap<>();
    public static List<List<GameWeapon>> weapons = new ArrayList<>();

    public static void load(){
        tradersNPCs.clear();
        weapons.clear();

        File tradersFile = FileManager.getFileWithoutCreating("tradersNPCs.json", ProtectYourCastleMain.getInstance());
        if (!tradersFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("tradersNPCs.json", false);
        }
        tradersNPCs = Game.gson.fromJson(FileManager.read(tradersFile), tradersNPCsType);

        File availableTradesFile = FileManager.getFileWithoutCreating("trades.json", ProtectYourCastleMain.getInstance());
        if (!availableTradesFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("trades.json", false);
        }
        availableTrades = Game.gson.fromJson(FileManager.read(availableTradesFile), availableTradesType);

        File weaponsFile = FileManager.getFileWithoutCreating("weapons.json", ProtectYourCastleMain.getInstance());
        if (!weaponsFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("weapons.json", false);
        }
        weapons = Game.gson.fromJson(FileManager.read(weaponsFile), weaponsType);
    }

    public static void save(){
        File tradersFile = FileManager.getFile("tradersNPCs.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(tradersNPCs), tradersFile);

        File availableTradesFile = FileManager.getFile("trades.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(availableTrades), availableTradesFile);

        File weaponsFile = FileManager.getFileWithoutCreating("weapons.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(weapons), weaponsFile);
    }

    public static boolean isTrader(UUID entityUUID) {
        return tradersNPCs.containsKey(entityUUID);
    }

    public static void removeTrader(UUID traderUUID) {
        tradersNPCs.remove(traderUUID);
    }

    public static Trader getTrader(UUID traderUUID) {
        return tradersNPCs.get(traderUUID);
    }

    public static void openTradeMenu(UUID entityUUID, Player player){
        player.openMerchant(tradersNPCs.get(entityUUID).getMerchantMenu(), true);
    }

    public static void openEditionMenu(UUID traderUUID, Player player) {
        EditTraderNPCInventory inv = new EditTraderNPCInventory(player, tradersNPCs.get(traderUUID));
        inv.openInventory();
    }

    public static TradesBase getTradesBase(String name){
        return availableTrades.get(name);
    }

    public static List<TradesBase> getAllTradesBases(){
        return new ArrayList<>(availableTrades.values());
    }

    public static TradesBase addTradesBase(TradesBase tradesBase){
        availableTrades.put(tradesBase.getName(), tradesBase);
        return tradesBase;
    }

    public static void removeTradesBase(String name){
        availableTrades.remove(name);
    }

    private final String name;
    private boolean weaponTrader;
    private SavedLocation savedLocation;
    private transient TradesBase tradesBase;

    public Trader(String name, boolean weaponTrader, TradesBase tradesBase) {
        this.weaponTrader = weaponTrader;
        this.name = name;
        this.tradesBase = tradesBase;
    }

    public Trader(String name, boolean weaponTrader) {
        this.name = name;
        this.weaponTrader = weaponTrader;
        this.tradesBase = addTradesBase(new TradesBase(name, new ArrayList<>(), name));
    }

    public void respawn(){
        if(this.savedLocation != null){
            Location location = this.savedLocation.toLocation();
            if(location.getWorld() != null){
                if(!location.getWorld().isChunkLoaded(location.getChunk())) location.getWorld().loadChunk(location.getChunk());
                for(Entity entity : location.getWorld().getNearbyEntities(location, 1, 1, 1, entity -> entity instanceof Villager && entity.getCustomName() != null && entity.getCustomName().equalsIgnoreCase(this.name))){
                    entity.remove();
                }
                for(Map.Entry<UUID, Trader> entry : new ArrayList<>(tradersNPCs.entrySet())){
                    if(entry.getValue() == this) tradersNPCs.remove(entry.getKey());
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
        villager.setCustomName(this.getDisplayName());
        villager.setCustomNameVisible(true);
        villager.setCollidable(false);
        villager.setMaxHealth(1024);
        villager.setHealth(1024);

        tradersNPCs.put(villager.getUniqueId(), this);
    }

    public String getName() {
        return name;
    }

    public Merchant buildMerchantMenu(){
        Merchant merchantMenu = Bukkit.createMerchant(this.name);
        List<MerchantRecipe> recipes = new ArrayList<>();
        final List<NPCTrade> merchantTrades = new ArrayList<>();
        if(this.weaponTrader && Game.getCurrentGame() != null && !Game.getCurrentGame().getSelectedWeapons().isEmpty() && GameParameters.ENABLE_RANDOM_WEAPONS.get()){
            final List<GameWeapon> weaponsToTrade = Game.getCurrentGame().getSelectedWeapons();
            int weaponsToAdd = GameParameters.ENABLE_PROGRESSIVE_WEAPONS.get() ?
                    Math.min(weaponsToTrade.size(), GameParameters.PROGRESSIVE_WEAPONS_BASE.get() + Game.getCurrentGame().getTime() / GameParameters.PROGRESSIVE_WEAPONS_DELAY.get())
                    :
                    weaponsToTrade.size();

            for(int i = 0; i < weaponsToAdd; i++){
                merchantTrades.add(weaponsToTrade.get(i).gunTrade);
                merchantTrades.add(weaponsToTrade.get(i).ammoTrade);
            }
        } else{
            merchantTrades.addAll(this.getTrades());
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

    public TradesBase getTradesBase(){
        if(this.tradesBase == null){
            this.tradesBase = getTradesBase(this.name);
        }
        return this.tradesBase;
    }

    public List<NPCTrade> getTrades() {
        return this.getTradesBase().getTrades();
    }

    public boolean isWeaponTrader() {
        return weaponTrader;
    }

    public void setWeaponTrader(boolean weaponTrader) {
        this.weaponTrader = weaponTrader;
    }

    public String getDisplayName() {
        return this.getTradesBase().getDisplayName();
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
        private boolean overPowered;

        public GameWeapon(NPCTrade gunTrade, NPCTrade ammoTrade, boolean overPowered) {
            this.gunTrade = gunTrade;
            this.ammoTrade = ammoTrade;
            this.overPowered = overPowered;
        }

        public boolean isOverPowered() {
            return overPowered;
        }

        public void setOverPowered(boolean overPowered) {
            this.overPowered = overPowered;
        }

        public NPCTrade getGunTrade() {
            return gunTrade;
        }

        public NPCTrade getAmmoTrade() {
            return ammoTrade;
        }
    }

    public static class TradesBase {

        private final String name;
        private String displayName;
        private final List<NPCTrade> trades;

        public TradesBase(String name, List<NPCTrade> trades, String displayName) {
            this.name = name;
            this.trades = trades;
            this.displayName = displayName;
        }

        public String getName() {
            return name;
        }

        public String getDisplayName() {
            return displayName;
        }

        public void setDisplayName(String displayName) {
            this.displayName = displayName;
        }

        public List<NPCTrade> getTrades() {
            return trades;
        }

        public void addTrade(NPCTrade trade) {
            this.trades.add(trade);
        }

        public void removeTrade(NPCTrade trade) {
            this.trades.remove(trade);
        }
    }

}
