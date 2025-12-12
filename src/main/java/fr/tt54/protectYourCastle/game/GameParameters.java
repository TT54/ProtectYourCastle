package fr.tt54.protectYourCastle.game;

import com.google.gson.*;
import fr.tt54.protectYourCastle.ProtectYourCastleMain;
import fr.tt54.protectYourCastle.utils.FileManager;

import java.io.File;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GameParameters {

    public static GameParameters gameParameters;

    public static Parameter<Integer> MAP_RADIUS = new Parameter<>("map_radius", 500);
    public static Parameter<Integer> GAME_DURATION = new Parameter<>("game_duration", 60);
    public static Parameter<Integer> RESPAWN_DELAY = new Parameter<>("respawn_delay", 20);

    public static Parameter<Integer> LOBBY_X = new Parameter<>("lobby_x", 0);
    public static Parameter<Integer> LOBBY_Y = new Parameter<>("lobby_y", 100);
    public static Parameter<Integer> LOBBY_Z = new Parameter<>("lobby_z", 0);

    public static Parameter<Boolean> KEEP_ARTIFACTS = new Parameter<>("keep_artifactes", true);
    public static Parameter<Boolean> KEEP_ARMOR = new Parameter<>("keep_armor", true);
    public static Parameter<Boolean> INCREASED_RESOURCES = new Parameter<>("increased_resources", true);

    private final Map<Parameter<?>, Object> parametersMap;

    public GameParameters() {
        this.parametersMap = new HashMap<>();
        for(Parameter<?> parameter : Parameter.existingParameters){
            this.parametersMap.put(parameter, parameter.defaultValue);
        }
    }

    public GameParameters(Map<Parameter<?>, Object> parametersMap) {
        this.parametersMap = parametersMap;
    }

    public static void load(){
        File parametersFile = FileManager.getFileWithoutCreating("parameters.json", ProtectYourCastleMain.getInstance());

        if (!parametersFile.exists()) {
            ProtectYourCastleMain.getInstance().saveResource("parameters.json", false);
            gameParameters = new GameParameters();
            return;
        }

        gameParameters = Game.gson.fromJson(FileManager.read(parametersFile), GameParameters.class);
    }

    public static void save(){
        File parametersFile = FileManager.getFile("parameters.json", ProtectYourCastleMain.getInstance());
        FileManager.write(Game.gson.toJson(gameParameters), parametersFile);
    }

    public <T> void setParameter(Parameter<T> param, T value){
        parametersMap.put(param, value);
    }

    public void setParameterWithoutChecks(Parameter<?> param, Object value){
        parametersMap.put(param, value);
    }

    public <T> T getParameter(Parameter<T> param){
        return (T) parametersMap.get(param);
    }

    public boolean setParameter(String paramName, String value) {
        Parameter<?> param = Parameter.getParameter(paramName);

        if(param == null) {
            return false;
        }

        Object cast = param.cast(value);

        if(cast != null){
            this.parametersMap.put(param, cast);
            return true;
        }

        return false;
    }

    public static class Parameter<T>{

        public static final List<Parameter<?>> existingParameters = new ArrayList<>();
        private static final Map<String, Parameter<?>> parametersName = new HashMap<>();

        private final String name;
        private final T defaultValue;

        private Parameter(String name, T defaultValue){
            this.name = name;
            this.defaultValue = defaultValue;
            existingParameters.add(this);
            parametersName.put(name, this);
        }

        public T get(){
            return gameParameters.getParameter(this);
        }

        public T getDefaultValue() {
            return defaultValue;
        }

        public String getName() {
            return name;
        }

        public static Parameter<?> getParameter(String name){
            return parametersName.get(name);
        }

        public Object cast(String value) {
            T t = getDefaultValue();
            try {
                if (t instanceof Integer) {
                    return Integer.valueOf(value);
                } else if (t instanceof Double) {
                    return Double.valueOf(value);
                } else if(t instanceof Boolean){
                    return Boolean.valueOf(value);
                }
            } catch (Exception e){
                return null;
            }
            return null;
        }

        public Object getFromJson(JsonElement element){
            T t = getDefaultValue();
            if(t instanceof Integer){
                return element.getAsInt();
            } else if(t instanceof Double){
                return element.getAsDouble();
            } else if(t instanceof Boolean){
                return element.getAsBoolean();
            }
            return this.getDefaultValue();
        }

        public JsonElement toJson(Object o){
            if(o instanceof Integer i){
                return new JsonPrimitive(i);
            } else if(o instanceof Double d){
                return new JsonPrimitive(d);
            } else if(o instanceof Boolean b){
                return new JsonPrimitive(b);
            }
            return new JsonPrimitive(o.toString());
        }
    }

    @Override
    protected GameParameters clone() {
        return new GameParameters(new HashMap<>(parametersMap));
    }

    public static class GameParametersJsonSerializer implements JsonSerializer<GameParameters>{

        @Override
        public JsonElement serialize(GameParameters parameters, Type type, JsonSerializationContext jsonSerializationContext) {
            JsonObject object = new JsonObject();
            for(String paramName : Parameter.parametersName.keySet()){
                Parameter<?> param = Parameter.getParameter(paramName);
                object.add(paramName, param.toJson(parameters.getParameter(param)));
            }
            return object;
        }
    }

    public static class GameParametersJsonDeserializer implements JsonDeserializer<GameParameters>{

        @Override
        public GameParameters deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject object = jsonElement.getAsJsonObject();
            Map<Parameter<?>, Object> settings = new HashMap<>();
            for(String paramName : Parameter.parametersName.keySet()){
                Parameter<?> param = Parameter.getParameter(paramName);
                if(object.has(paramName)){
                    settings.put(param, param.getFromJson(object.get(paramName)));
                } else {
                    settings.put(param, param.getDefaultValue());
                }
            }
            return new GameParameters(settings);
        }
    }
}
