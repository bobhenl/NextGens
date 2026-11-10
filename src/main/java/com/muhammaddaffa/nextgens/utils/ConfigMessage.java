package com.muhammaddaffa.nextgens.utils;

import com.muhammaddaffa.mdlib.utils.Common;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import me.clip.placeholderapi.PlaceholderAPI;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class ConfigMessage {

    private final List<String> messages = new ArrayList<>();

    public ConfigMessage(FileConfiguration config, String path) {
        if (config.isList(path)) {
            this.messages.addAll(config.getStringList(path));
        } else {
            this.messages.add(config.getString(path));
        }
    }

    private List<String> getTranslatedMessage(@Nullable Placeholder placeholder) {
        List<String> cloned = new ArrayList<>(this.messages);
        if (cloned.isEmpty() || placeholder == null) return cloned;
        return placeholder.translate(cloned);
    }

    public void send(CommandSender sender, @Nullable Placeholder placeholder) {
        Common.sendMessage(sender, this.getTranslatedMessage(placeholder));
    }

    public void broadcast(@Nullable Placeholder placeholder) {
        for (String message : this.getTranslatedMessage(placeholder)) {
            Settings.broadcast(message, new Placeholder());
        }
    }

}
