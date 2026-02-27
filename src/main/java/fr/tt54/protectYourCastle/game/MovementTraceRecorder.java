package fr.tt54.protectYourCastle.game;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.zip.GZIPOutputStream;

public class MovementTraceRecorder {

    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.ROOT)
            .withZone(ZoneId.systemDefault());

    private final Game game;
    private final World world;
    private final long startedAtEpochMs;
    private final int sampleTicks;
    private final double minDistanceBlocks;
    private final long heartbeatMs;
    private final File outputFile;

    private final ConcurrentLinkedQueue<String> pendingLines = new ConcurrentLinkedQueue<>();
    private final Map<UUID, Integer> playerIds = new HashMap<>();
    private final Map<UUID, LastSample> lastSamples = new HashMap<>();

    private int nextPlayerId = 1;
    private BukkitTask sampleTask;
    private BukkitTask flushTask;
    private BufferedWriter writer;
    private volatile boolean running = false;

    public MovementTraceRecorder(Game game, World world) {
        this.game = game;
        this.world = world;
        this.startedAtEpochMs = System.currentTimeMillis();
        this.sampleTicks = Math.max(1, GameParameters.MOVEMENT_TRACE_SAMPLE_TICKS.get());
        this.minDistanceBlocks = Math.max(0d, GameParameters.MOVEMENT_TRACE_MIN_DISTANCE.get());
        this.heartbeatMs = Math.max(1, GameParameters.MOVEMENT_TRACE_HEARTBEAT_SECONDS.get()) * 1000L;

        String worldName = world != null ? world.getName() : "unknown_world";
        String fileName = worldName + "_" + FILE_DATE_FORMAT.format(Instant.ofEpochMilli(this.startedAtEpochMs)) + ".trace.csv.gz";
        this.outputFile = new File(new File(ProtectYourCastleMain.getInstance().getDataFolder(), "movement_traces"), fileName);
    }

    public File getOutputFile() {
        return outputFile;
    }

    public boolean start() {
        if(!GameParameters.ENABLE_MOVEMENT_TRACE.get()){
            return false;
        }

        if(this.running){
            return true;
        }

        File parent = this.outputFile.getParentFile();
        if(parent != null && !parent.exists() && !parent.mkdirs()){
            ProtectYourCastleMain.getInstance().getLogger().warning("Impossible de créer le dossier movement_traces");
            return false;
        }

        try {
            this.writer = new BufferedWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(this.outputFile)), StandardCharsets.UTF_8));
        } catch (IOException e) {
            ProtectYourCastleMain.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Impossible d'ouvrir le traceur de mouvements", e);
            return false;
        }

        this.running = true;
        this.enqueueHeader();
        this.flushPendingLines();

        this.sampleTask = Bukkit.getScheduler().runTaskTimer(ProtectYourCastleMain.getInstance(), this::samplePlayers, 1L, this.sampleTicks);
        this.flushTask = Bukkit.getScheduler().runTaskTimerAsynchronously(ProtectYourCastleMain.getInstance(), this::flushPendingLines, 20L, 20L);
        return true;
    }

    public void stop() {
        if(!this.running){
            return;
        }

        this.samplePlayers();
        this.running = false;

        if(this.sampleTask != null){
            this.sampleTask.cancel();
            this.sampleTask = null;
        }
        if(this.flushTask != null){
            this.flushTask.cancel();
            this.flushTask = null;
        }

        this.flushPendingLines();
        synchronized (this) {
            if(this.writer != null){
                try {
                    this.writer.flush();
                    this.writer.close();
                } catch (IOException e) {
                    ProtectYourCastleMain.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Erreur lors de la fermeture du traceur de mouvements", e);
                } finally {
                    this.writer = null;
                }
            }
        }
    }

    private void enqueueHeader() {
        this.pendingLines.add("#PYC_MOVEMENT_TRACE_V1");
        this.pendingLines.add("#world=" + (this.world != null ? this.world.getName() : "unknown_world"));
        this.pendingLines.add("#start_epoch_ms=" + this.startedAtEpochMs);
        this.pendingLines.add("#sample_ticks=" + this.sampleTicks);
        this.pendingLines.add("#min_distance_blocks=" + this.minDistanceBlocks);
        this.pendingLines.add("#heartbeat_seconds=" + (this.heartbeatMs / 1000L));
        this.pendingLines.add("#format=P;playerId;uuid;name;team | M;tMs;playerId;x10;y10;z10;yaw10;pitch10;flags");
    }

    private void samplePlayers() {
        if(!this.running || this.world == null || !this.game.isRunning()){
            return;
        }

        long now = System.currentTimeMillis();
        long relativeMs = now - this.startedAtEpochMs;

        for(Player player : Bukkit.getOnlinePlayers()){
            if(player == null || !player.isOnline() || player.getWorld() != this.world){
                continue;
            }
            if(player.getGameMode() != GameMode.SURVIVAL){
                continue;
            }

            Team team = Team.getPlayerTeam(player.getUniqueId());
            if(team == null){
                continue;
            }

            int playerId = this.getOrRegisterPlayerId(player, team);
            EncodedSample encodedSample = EncodedSample.from(player);
            LastSample previous = this.lastSamples.get(player.getUniqueId());
            if(this.shouldRecord(previous, encodedSample, now)){
                this.pendingLines.add("M;" + relativeMs + ";" + playerId + ";" + encodedSample.x10 + ";" + encodedSample.y10 + ";" + encodedSample.z10 + ";" + encodedSample.yaw10 + ";" + encodedSample.pitch10 + ";" + encodedSample.flags);
                this.lastSamples.put(player.getUniqueId(), new LastSample(now, encodedSample.x10, encodedSample.y10, encodedSample.z10, encodedSample.yaw10, encodedSample.pitch10, encodedSample.flags));
            }
        }
    }

    private int getOrRegisterPlayerId(Player player, Team team) {
        Integer existing = this.playerIds.get(player.getUniqueId());
        if(existing != null){
            return existing;
        }

        int id = this.nextPlayerId++;
        this.playerIds.put(player.getUniqueId(), id);
        this.pendingLines.add("P;" + id + ";" + player.getUniqueId() + ";" + sanitizeToken(player.getName()) + ";" + team.getColor().name().toLowerCase(Locale.ROOT));
        return id;
    }

    private boolean shouldRecord(LastSample previous, EncodedSample current, long now) {
        if(previous == null){
            return true;
        }
        if(now - previous.timestampMs >= this.heartbeatMs){
            return true;
        }
        if(previous.flags != current.flags){
            return true;
        }

        double dx = (current.x10 - previous.x10) / 10d;
        double dy = (current.y10 - previous.y10) / 10d;
        double dz = (current.z10 - previous.z10) / 10d;
        if(dx * dx + dy * dy + dz * dz >= this.minDistanceBlocks * this.minDistanceBlocks){
            return true;
        }

        return angleDiff10(current.yaw10, previous.yaw10) >= 100 || Math.abs(current.pitch10 - previous.pitch10) >= 100;
    }

    private void flushPendingLines() {
        if(this.writer == null){
            return;
        }

        StringBuilder buffer = new StringBuilder();
        String line;
        while((line = this.pendingLines.poll()) != null){
            buffer.append(line).append('\n');
        }

        if(buffer.isEmpty()){
            return;
        }

        synchronized (this) {
            if(this.writer == null){
                return;
            }
            try {
                this.writer.write(buffer.toString());
                this.writer.flush();
            } catch (IOException e) {
                ProtectYourCastleMain.getInstance().getLogger().log(java.util.logging.Level.WARNING, "Impossible d'écrire dans le traceur de mouvements", e);
            }
        }
    }

    private static String sanitizeToken(String text) {
        if(text == null || text.isBlank()){
            return "unknown";
        }
        return text.replace(";", "_").replace("\n", "_").replace("\r", "_");
    }

    private static int angleDiff10(int a, int b) {
        int diff = Math.abs(a - b) % 3600;
        return diff > 1800 ? 3600 - diff : diff;
    }

    private static float normalizeYaw(float yaw) {
        float normalized = yaw % 360f;
        if(normalized > 180f){
            normalized -= 360f;
        } else if(normalized <= -180f){
            normalized += 360f;
        }
        return normalized;
    }

    private static class EncodedSample {
        private final int x10;
        private final int y10;
        private final int z10;
        private final int yaw10;
        private final int pitch10;
        private final int flags;

        private EncodedSample(int x10, int y10, int z10, int yaw10, int pitch10, int flags) {
            this.x10 = x10;
            this.y10 = y10;
            this.z10 = z10;
            this.yaw10 = yaw10;
            this.pitch10 = pitch10;
            this.flags = flags;
        }

        private static EncodedSample from(Player player) {
            Location loc = player.getLocation();
            int flags = 0;
            if(player.isOnGround()){
                flags |= 1;
            }
            if(player.getVehicle() != null){
                flags |= 2;
            }
            if(player.isSprinting()){
                flags |= 4;
            }
            if(player.isSneaking()){
                flags |= 8;
            }
            if(player.getGameMode() == GameMode.SURVIVAL){
                flags |= 16;
            }
            if(player.getGameMode() == GameMode.SPECTATOR){
                flags |= 32;
            }
            if(player.getGameMode() == GameMode.CREATIVE){
                flags |= 64;
            }

            return new EncodedSample(
                    (int) Math.round(loc.getX() * 10d),
                    (int) Math.round(loc.getY() * 10d),
                    (int) Math.round(loc.getZ() * 10d),
                    (int) Math.round(normalizeYaw(loc.getYaw()) * 10d),
                    (int) Math.round(loc.getPitch() * 10d),
                    flags
            );
        }
    }

    private static class LastSample {
        private final long timestampMs;
        private final int x10;
        private final int y10;
        private final int z10;
        private final int yaw10;
        private final int pitch10;
        private final int flags;

        private LastSample(long timestampMs, int x10, int y10, int z10, int yaw10, int pitch10, int flags) {
            this.timestampMs = timestampMs;
            this.x10 = x10;
            this.y10 = y10;
            this.z10 = z10;
            this.yaw10 = yaw10;
            this.pitch10 = pitch10;
            this.flags = flags;
        }
    }
}
