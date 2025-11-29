package fr.tt54.protectYourCastle.cmd;

import org.bukkit.command.CommandExecutor;
import org.bukkit.command.TabCompleter;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public abstract class CoreCommand implements CommandExecutor, TabCompleter {

    public static List<String> tabComplete(String arg, String... responses) {
        return tabComplete(arg, Stream.of(responses));
    }

    public static List<String> tabComplete(String arg, List<String> responses) {
        return tabComplete(arg, responses.stream());
    }

    public static List<String> tabComplete(String arg, Stream<String> responses) {
        return responses.filter(s -> s.toLowerCase().startsWith(arg.toLowerCase())).collect(Collectors.toList());
    }

}
