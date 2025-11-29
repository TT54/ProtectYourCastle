package fr.tt54.protectYourCastle.utils;

import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.*;

public class FileManager {

    private static void createFile(String name, JavaPlugin plugin) {
        if (!plugin.getDataFolder().exists()) {
            plugin.getDataFolder().mkdir();
        }

        File file = new File(plugin.getDataFolder(), name);

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void createFile(String name, String dataFolder, JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder() + File.separator + dataFolder, name);

        if (!file.getParentFile().exists()) {
            file.getParentFile().mkdirs();
        }

        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public static File getYmlFile(String name, String dataFolder, JavaPlugin plugin) {
        return getFile(name + ".yml", dataFolder, plugin);
    }

    public static File getFile(String name, String dataFolder, JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder() + File.separator + dataFolder, name);

        if (!file.exists()) {
            createFile(name, dataFolder, plugin);
        }

        return file;
    }

    public static File getFile(String name, JavaPlugin plugin) {
        File file = new File(plugin.getDataFolder(), name);

        if (!file.exists()) {
            createFile(name, plugin);
        }

        return file;
    }

    public static File getFileWithoutCreating(String name, JavaPlugin plugin) {
        return new File(plugin.getDataFolder(), name);
    }

    public static File getYmlFile(String name, JavaPlugin plugin) {
        return getFile(name + ".yml", plugin);
    }

    public static FileConfiguration getYmlFile(String fileName) {
        return YamlConfiguration.loadConfiguration(getYmlFile(fileName, ProtectYourCastleMain.getInstance()));
    }

    public static FileConfiguration getYmlFile(String fileName, String dataFolder) {
        return YamlConfiguration.loadConfiguration(getYmlFile(fileName, dataFolder, ProtectYourCastleMain.getInstance()));
    }

    public static void saveFile(FileConfiguration file, String fileName) {
        saveFile(ProtectYourCastleMain.getInstance(), file, fileName);
    }

    public static void saveFile(JavaPlugin plugin, FileConfiguration file, String fileName) {
        try {
            file.save(getYmlFile(fileName, plugin));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void saveFile(FileConfiguration file, String fileName, String fileDataFolder) {
        try {
            file.save(getYmlFile(fileName, fileDataFolder, ProtectYourCastleMain.getInstance()));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static void updateYmlFile(JavaPlugin plugin, String internalName, String externalName) {
        File internal = getInternalFile(plugin, internalName);
        FileConfiguration internalFile = YamlConfiguration.loadConfiguration(internal);
        FileConfiguration externalFile = YamlConfiguration.loadConfiguration(getYmlFile(externalName, plugin));
        boolean edited = false;

        for (String key : internalFile.getKeys(false)) {
            if (externalFile.get(key) == null) {
                externalFile.set(key, internalFile.get(key));
                edited = true;
            }
        }
        externalFile.set("version", internalFile.get("version"));

        internal.delete();

        if (edited)
            saveFile(plugin, externalFile, externalName);
    }

    public static File getInternalFile(JavaPlugin javaPlugin, String fileName) {
        if (fileName != null && !fileName.equals("")) {
            String resourcePath = fileName + ".yml";
            resourcePath = resourcePath.replace('\\', '/');
            InputStream in = javaPlugin.getResource(resourcePath);

            if (in != null) {
                File outFile = new File(javaPlugin.getDataFolder(), "copy" + resourcePath);
                int lastIndex = resourcePath.lastIndexOf(47);
                File outDir = new File(javaPlugin.getDataFolder(), resourcePath.substring(0, lastIndex >= 0 ? lastIndex : 0));
                if (!outDir.exists()) {
                    outDir.mkdirs();
                }

                try {
                    if (!outFile.exists())
                        outFile.createNewFile();

                    OutputStream out = new FileOutputStream(outFile);
                    byte[] buf = new byte[1024];

                    int len;
                    while ((len = in.read(buf)) > 0) {
                        out.write(buf, 0, len);
                    }

                    out.close();
                    in.close();

                    return outFile;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return new File(javaPlugin.getDataFolder(), fileName + ".yml");
    }


    public static void write(String text, File file) {
        if (!file.exists())
            createFile(file.getAbsolutePath(), ProtectYourCastleMain.getInstance());
        try {
            FileWriter writer = new FileWriter(file);
            writer.write(text);
            writer.flush();
            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String read(File file) {
        if (file.exists()) {
            try {
                BufferedReader reader = new BufferedReader(new FileReader(file));
                StringBuilder text = new StringBuilder();

                String line;
                while ((line = reader.readLine()) != null) {
                    text.append(line);
                }
                return text.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return "";
    }

    /*public static void saveJson(JSONObject jsonObject, File targetFile) {
        targetFile.getParentFile().mkdirs();

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        JsonElement jsonElement = JsonParser.parseString(jsonObject.toJSONString());
        String prettyJson = gson.toJson(jsonElement).replace("\\u003d", "=");

        try (FileWriter fileWriter = new FileWriter(targetFile)) {
            fileWriter.write(prettyJson);
            fileWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }*/

}
