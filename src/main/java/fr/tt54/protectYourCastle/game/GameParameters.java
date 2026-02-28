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

    public static Parameter<Boolean> DISPLAY_SCORE = new Parameter<>("display_score", true);
    public static Parameter<Double> PERSONAL_SCORE_WIN = new Parameter<>("personal_score_win", 50d);
    public static Parameter<Double> PERSONAL_SCORE_KILLS_COEFF = new Parameter<>("personal_score_kills_coeff", 80d);
    public static Parameter<Double> PERSONAL_SCORE_KILLS_BASE = new Parameter<>("personal_score_kills_base", 0.2d);
    public static Parameter<Double> PERSONAL_SCORE_DEATHS_COEFF = new Parameter<>("personal_score_deaths_coeff", 20d);
    public static Parameter<Double> SCORE_POINTS_REDUCTION = new Parameter<>("score_points_reduction", 6d);
    public static Parameter<Double> BANNER_RATIO_POINTS_REDUCTION = new Parameter<>("banner_ratio_points_reduction", 4d);
    public static Parameter<Double> TEAM_BANNERS_BROKEN_COEFF = new Parameter<>("team_banners_broken_coeff", 20d);
    public static Parameter<Double> BEST_GAME_FACTOR = new Parameter<>("best_game_factor", 0.6d);
    public static Parameter<Double> WORST_GAME_FACTOR = new Parameter<>("worst_game_factor", 0.4d);
    public static Parameter<Integer> SCORES_USED = new Parameter<>("scores_used", 5);

    public static Parameter<Boolean> ENABLE_BOOST_FOR_SMALL_TEAM = new Parameter<>("enable_boost_for_small_team", true);
    public static Parameter<Double> HEALTH_BOOST_FOR_SMALL_TEAM = new Parameter<>("health_boost_for_small_team", 10d);
    public static Parameter<Boolean> ENABLE_RANDOM_WEAPONS = new Parameter<>("enable_random_weapons", true);
    public static Parameter<Integer> WEAPONS_TO_SELECT = new Parameter<>("weapons_to_select", 4);
    public static Parameter<Boolean> PROGRESSIVE_WEAPONS = new Parameter<>("progressive_weapons", true);
    public static Parameter<Integer> PROGRESSIVE_WEAPONS_BASE = new Parameter<>("progressive_weapons_base", 2);
    public static Parameter<Integer> PROGRESSIVE_WEAPONS_DELAY = new Parameter<>("progressive_weapons_delay", 20 * 60);
    public static Parameter<Double> BASE_KNOCKBACK_RESISTANCE = new Parameter<>("base_knockback_resistance", 0.4d);

    public static Parameter<Boolean> ENABLE_DAMAGE_INDICATOR = new Parameter<>("enable_damage_indicator", true);
    public static Parameter<Boolean> ENABLE_MOVEMENT_TRACE = new Parameter<>("enable_movement_trace", true);
    public static Parameter<Integer> MOVEMENT_TRACE_SAMPLE_TICKS = new Parameter<>("movement_trace_sample_ticks", 10);
    public static Parameter<Double> MOVEMENT_TRACE_MIN_DISTANCE = new Parameter<>("movement_trace_min_distance", 0.35d);
    public static Parameter<Integer> MOVEMENT_TRACE_HEARTBEAT_SECONDS = new Parameter<>("movement_trace_heartbeat_seconds", 5);

    private static final Map<String, String> LEGACY_PARAMETER_ALIASES = new HashMap<>();

    static {
        LEGACY_PARAMETER_ALIASES.put("keep_artifactes", "keep_artifacts");
    }

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

        try {
            gameParameters = Game.gson.fromJson(FileManager.read(parametersFile), GameParameters.class);
        } catch (Throwable throwable){
            ProtectYourCastleMain.getInstance().getLogger().warning("parameters.json invalide, fallback sur valeurs par defaut: " + throwable.getClass().getSimpleName());
            gameParameters = null;
        }
        if(gameParameters == null){
            gameParameters = new GameParameters();
        }
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
        Object value = parametersMap.get(param);
        if(value == null){
            return param.getDefaultValue();
        }
        return (T) value;
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
            try {
                T t = getDefaultValue();
                if(t instanceof Integer){
                    return element.getAsInt();
                } else if(t instanceof Double){
                    return element.getAsDouble();
                } else if(t instanceof Boolean){
                    return element.getAsBoolean();
                }
            } catch (Exception ignored){
                return this.getDefaultValue();
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

        private JsonElement getParameterElement(JsonObject object, String parameterName){
            if(object.has(parameterName)){
                return object.get(parameterName);
            }

            String alias = LEGACY_PARAMETER_ALIASES.get(parameterName);
            if(alias != null && object.has(alias)){
                return object.get(alias);
            }

            return null;
        }

        @Override
        public GameParameters deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext jsonDeserializationContext) throws JsonParseException {
            JsonObject object = jsonElement.getAsJsonObject();
            Map<Parameter<?>, Object> settings = new HashMap<>();
            for(String paramName : Parameter.parametersName.keySet()){
                Parameter<?> param = Parameter.getParameter(paramName);
                JsonElement valueElement = getParameterElement(object, paramName);
                if(valueElement != null){
                    settings.put(param, param.getFromJson(valueElement));
                } else {
                    settings.put(param, param.getDefaultValue());
                }
            }
            return new GameParameters(settings);
        }
    }
}
