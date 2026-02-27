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

    private static final Type tradersMapType = new TypeToken<Map<UUID, Trader>>() {}.getType();
    private static final Type weaponsType = new TypeToken<List<List<GameWeapon>>>() {}.getType();
    private static final Type traderTypesType = new TypeToken<Map<String, TraderTypeProfile>>() {}.getType();

    public static Map<UUID, Trader> traders = new HashMap<>();
    public static List<List<GameWeapon>> weapons = new ArrayList<>();
    public static Map<String, TraderTypeProfile> traderTypes = new HashMap<>();

    public static void load(){
        traders.clear();
        weapons.clear();
        traderTypes.clear();

        File traderTypesFile = FileManager.getFileWithoutCreating("trader_types.json", ProtectYourCastleMain.getInstance());
        if (!traderTypesFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("trader_types.json", false);
        }
        Map<String, TraderTypeProfile> loadedProfiles = Game.gson.fromJson(FileManager.read(traderTypesFile), traderTypesType);
        if(loadedProfiles != null){
            for(Map.Entry<String, TraderTypeProfile> entry : loadedProfiles.entrySet()){
                String normalizedTypeName = normalizeTypeName(entry.getKey());
                TraderTypeProfile profile = entry.getValue();
                if(normalizedTypeName == null || profile == null) continue;
                traderTypes.put(normalizedTypeName, new TraderTypeProfile(
                        cloneTradesSafely(profile.trades),
                        profile.weaponTrader
                ));
            }
        }

        File tradersFile = FileManager.getFileWithoutCreating("traders.json", ProtectYourCastleMain.getInstance());
        if (!tradersFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("traders.json", false);
        }
        Map<UUID, Trader> loadedTraders = Game.gson.fromJson(FileManager.read(tradersFile), tradersMapType);
        traders = new HashMap<>();
        if(loadedTraders != null) {
            for(Map.Entry<UUID, Trader> entry : loadedTraders.entrySet()){
                UUID traderUUID = entry.getKey();
                Trader loadedTrader = entry.getValue();
                if(traderUUID == null || loadedTrader == null) continue;

                Trader normalizedTrader = new Trader(
                        loadedTrader.name != null ? loadedTrader.name : "Trader",
                        cloneTradesSafely(loadedTrader.trades),
                        loadedTrader.weaponTrader
                );
                normalizedTrader.savedLocation = loadedTrader.savedLocation;
                normalizedTrader.traderType = normalizeTypeName(loadedTrader.traderType);
                if(normalizedTrader.traderType != null && !traderTypes.containsKey(normalizedTrader.traderType)) {
                    normalizedTrader.traderType = null;
                }
                traders.put(traderUUID, normalizedTrader);
            }
        }

        File weaponsFile = FileManager.getFileWithoutCreating("weapons.json", ProtectYourCastleMain.getInstance());
        if (!weaponsFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("weapons.json", false);
        }
        List<List<GameWeapon>> loadedWeapons = Game.gson.fromJson(FileManager.read(weaponsFile), weaponsType);
        weapons = new ArrayList<>();
        if(loadedWeapons != null){
            for(List<GameWeapon> bundle : loadedWeapons){
                if(bundle == null) continue;

                List<GameWeapon> normalizedBundle = new ArrayList<>();
                for(GameWeapon gameWeapon : bundle){
                    if(gameWeapon == null) continue;

                    NPCTrade gunTrade = cloneTradeSafely(gameWeapon.gunTrade);
                    NPCTrade ammoTrade = cloneTradeSafely(gameWeapon.ammoTrade);
                    if(gunTrade == null || ammoTrade == null) continue;

                    normalizedBundle.add(new GameWeapon(gunTrade, ammoTrade, gameWeapon.overPowered));
                }

                if(!normalizedBundle.isEmpty()){
                    weapons.add(normalizedBundle);
                }
            }
        }
    }

    public static void save(){
        File tradersFile = FileManager.getFile("traders.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(traders), tradersFile);

        File weaponsFile = FileManager.getFileWithoutCreating("weapons.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(weapons), weaponsFile);

        File traderTypesFile = FileManager.getFileWithoutCreating("trader_types.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(traderTypes), traderTypesFile);
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
        Trader trader = traders.get(entityUUID);
        if(trader != null) {
            player.openMerchant(trader.getMerchantMenu(), true);
        }
    }

    public static void openEditionMenu(UUID traderUUID, Player player) {
        Trader trader = traders.get(traderUUID);
        if(trader != null) {
            TradeListInventory inv = new TradeListInventory(player, 1, trader);
            inv.openInventory();
        }
    }

    public static Set<String> getTraderTypeNames() {
        return new TreeSet<>(traderTypes.keySet());
    }

    public static boolean hasTraderType(String typeName) {
        String normalized = normalizeTypeName(typeName);
        return normalized != null && traderTypes.containsKey(normalized);
    }

    public static int getTraderTypeTradeCount(String typeName) {
        TraderTypeProfile profile = getTraderTypeProfile(typeName);
        return profile == null ? -1 : profile.trades.size();
    }

    public static Boolean isTraderTypeWeaponTrader(String typeName) {
        TraderTypeProfile profile = getTraderTypeProfile(typeName);
        return profile == null ? null : profile.weaponTrader;
    }

    private static TraderTypeProfile getTraderTypeProfile(String typeName) {
        String normalized = normalizeTypeName(typeName);
        if(normalized == null) {
            return null;
        }
        return traderTypes.get(normalized);
    }

    public static boolean saveTypeFromTrader(String typeName, Trader sourceTrader) {
        if(sourceTrader == null) return false;

        String normalized = normalizeTypeName(typeName);
        if(!isTypeNameValid(normalized)) return false;

        TraderTypeProfile profile = traderTypes.computeIfAbsent(normalized, k -> new TraderTypeProfile(new ArrayList<>(), false));
        profile.trades = cloneTradesSafely(sourceTrader.getTrades());
        profile.weaponTrader = sourceTrader.isWeaponTrader();

        sourceTrader.traderType = normalized;
        return true;
    }

    public static boolean duplicateTraderType(String sourceTypeName, String targetTypeName) {
        String source = normalizeTypeName(sourceTypeName);
        String target = normalizeTypeName(targetTypeName);
        if(source == null || target == null || !isTypeNameValid(target)) return false;
        if(!traderTypes.containsKey(source) || traderTypes.containsKey(target)) return false;

        TraderTypeProfile sourceProfile = traderTypes.get(source);
        traderTypes.put(target, new TraderTypeProfile(cloneTradesSafely(sourceProfile.trades), sourceProfile.weaponTrader));
        return true;
    }

    public static boolean deleteTraderType(String typeName) {
        String normalized = normalizeTypeName(typeName);
        if(normalized == null) return false;

        TraderTypeProfile removedProfile = traderTypes.remove(normalized);
        if(removedProfile == null) return false;

        for(Trader trader : traders.values()) {
            if(trader != null && normalized.equals(trader.traderType)) {
                trader.unbindTypeWithFallback(removedProfile);
            }
        }

        return true;
    }

    private final List<NPCTrade> trades;
    private final String name;
    private boolean weaponTrader;
    private SavedLocation savedLocation;
    private String traderType;

    public Trader(String name, boolean weaponTrader) {
        this.weaponTrader = weaponTrader;
        this.trades = new ArrayList<>();
        this.name = name;
    }

    public Trader(String name, List<NPCTrade> trades, boolean weaponTrader) {
        this.trades = trades == null ? new ArrayList<>() : trades;
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

    public boolean bindToType(String typeName) {
        String normalized = normalizeTypeName(typeName);
        if(normalized == null || !traderTypes.containsKey(normalized)){
            return false;
        }
        this.traderType = normalized;
        return true;
    }

    public void unbindType() {
        TraderTypeProfile profile = this.getBoundTypeProfile();
        if(profile != null) {
            this.unbindTypeWithFallback(profile);
        } else {
            this.traderType = null;
        }
    }

    private void unbindTypeWithFallback(TraderTypeProfile profile) {
        this.trades.clear();
        this.trades.addAll(cloneTradesSafely(profile.trades));
        this.weaponTrader = profile.weaponTrader;
        this.traderType = null;
    }

    public void addTrade(NPCTrade trade){
        this.getTrades().add(trade);
    }

    public String getName() {
        return name;
    }

    public Merchant buildMerchantMenu(){
        Merchant merchantMenu = Bukkit.createMerchant(this.name);
        List<MerchantRecipe> recipes = new ArrayList<>();
        final List<NPCTrade> merchantTrades = new ArrayList<>();
        final Game currentGame = Game.getCurrentGame();
        final List<GameWeapon> weaponsToTrade = currentGame != null ? currentGame.getSelectedWeapons() : List.of();
        if(this.isWeaponTrader() && currentGame != null && !weaponsToTrade.isEmpty() && GameParameters.ENABLE_RANDOM_WEAPONS.get()){
            int progressiveDelay = Math.max(1, GameParameters.PROGRESSIVE_WEAPONS_DELAY.get());
            int weaponsToAdd = Math.min(weaponsToTrade.size(),
                    Math.max(0, GameParameters.PROGRESSIVE_WEAPONS_BASE.get()) + currentGame.getTime() / progressiveDelay);

            for(int i = 0; i < weaponsToAdd; i++){
                merchantTrades.add(weaponsToTrade.get(i).gunTrade);
                merchantTrades.add(weaponsToTrade.get(i).ammoTrade);
            }
        } else{
            merchantTrades.addAll(this.getTrades());
        }
        for(NPCTrade trade : merchantTrades){
            if(trade == null || trade.reward == null || trade.input == null) continue;
            MerchantRecipe recipe = new MerchantRecipe(trade.reward.clone(), Integer.MAX_VALUE);
            for(ItemStack is : trade.input){
                if(is != null){
                    recipe.addIngredient(is.clone());
                }
            }
            recipes.add(recipe);
        }
        merchantMenu.setRecipes(recipes);
        return merchantMenu;
    }

    private static List<NPCTrade> cloneTradesSafely(List<NPCTrade> trades) {
        List<NPCTrade> clonedTrades = new ArrayList<>();
        if(trades == null) return clonedTrades;

        for(NPCTrade trade : trades) {
            NPCTrade cloned = cloneTradeSafely(trade);
            if(cloned != null) {
                clonedTrades.add(cloned);
            }
        }
        return clonedTrades;
    }

    private static NPCTrade cloneTradeSafely(NPCTrade trade) {
        if(trade == null || trade.reward == null || trade.input == null){
            return null;
        }

        List<ItemStack> clonedInput = new ArrayList<>();
        for(ItemStack is : trade.input){
            if(is != null){
                clonedInput.add(is.clone());
            }
        }
        return new NPCTrade(clonedInput, trade.reward.clone());
    }

    private TraderTypeProfile getBoundTypeProfile() {
        String normalized = normalizeTypeName(this.traderType);
        if(normalized == null) {
            return null;
        }
        TraderTypeProfile profile = traderTypes.get(normalized);
        if(profile != null){
            this.traderType = normalized;
        }
        return profile;
    }

    private static String normalizeTypeName(String typeName) {
        if(typeName == null) {
            return null;
        }
        String normalized = typeName.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank() ? null : normalized;
    }

    private static boolean isTypeNameValid(String normalizedTypeName) {
        return normalizedTypeName != null && normalizedTypeName.matches("[a-z0-9_-]+");
    }

    private Merchant getMerchantMenu() {
        return this.buildMerchantMenu();
    }

    public List<NPCTrade> getTrades() {
        TraderTypeProfile profile = this.getBoundTypeProfile();
        return profile != null ? profile.trades : this.trades;
    }

    public void removeTrade(NPCTrade trade) {
        this.getTrades().remove(trade);
    }

    public boolean isWeaponTrader() {
        TraderTypeProfile profile = this.getBoundTypeProfile();
        return profile != null ? profile.weaponTrader : this.weaponTrader;
    }

    public void setWeaponTrader(boolean weaponTrader) {
        TraderTypeProfile profile = this.getBoundTypeProfile();
        if(profile != null) {
            profile.weaponTrader = weaponTrader;
        } else {
            this.weaponTrader = weaponTrader;
        }
    }

    public String getTraderType() {
        return traderType;
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
            if(this.input != null){
                for(ItemStack is : this.input){
                    if(is != null){
                        clonedInput.add(is.clone());
                    }
                }
            }
            return new NPCTrade(clonedInput, this.reward == null ? null : this.reward.clone());
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

    public static class TraderTypeProfile {

        private List<NPCTrade> trades;
        private boolean weaponTrader;

        public TraderTypeProfile(List<NPCTrade> trades, boolean weaponTrader) {
            this.trades = trades == null ? new ArrayList<>() : trades;
            this.weaponTrader = weaponTrader;
        }
    }

}
