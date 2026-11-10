package com.muhammaddaffa.nextgens.events;

import com.muhammaddaffa.mdlib.utils.Logger;
import com.muhammaddaffa.mdlib.utils.Placeholder;
import com.muhammaddaffa.mdlib.utils.TimeUtils;
import com.muhammaddaffa.nextgens.NextGens;
import com.muhammaddaffa.nextgens.utils.Settings;
import org.bukkit.configuration.file.FileConfiguration;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class Event implements Cloneable{

    private final String id;
    private final Type type;
    private final String displayName;
    private final List<String> startMessage;
    private final List<String> endMessage;
    private double duration;

    private Double sellMultiplier;
    private Integer tierUpgrade;
    private Double speedMultiplier;
    private Integer dropMultiplier;
    private List<String> blacklistedGenerators = new ArrayList<>();

    // settings
    private final double chance;
    private final boolean onlyByCommand;

    public Event(String id, Type type, FileConfiguration config, String path) {
        this.id = id;
        this.type = type;
        this.displayName = config.getString(path + ".display-name");
        this.startMessage = config.getStringList(path + ".start-message");
        this.endMessage = config.getStringList(path + ".end-message");
        this.duration = config.getDouble(path + ".duration");
        this.setBlacklistedGenerators(config.getStringList(path + ".blacklisted_generators"));
        this.chance = config.getDouble(path + ".chance", 65);
        this.onlyByCommand = config.getBoolean(path + ".only-by-command");
    }

    @Nullable
    public static Event createEvent(FileConfiguration config, String path, String id) {
        String type = config.getString(path + ".type");
        if (type == null) return null;
        if (Type.SELL_MULTIPLIER.name().equalsIgnoreCase(type)) {
            return new Event(id, Type.SELL_MULTIPLIER, config, path)
                    .setSellMultiplier(config.getDouble(path + ".multiplier"));
        }
        if (Type.GENERATOR_UPGRADE.name().equalsIgnoreCase(type)) {
            return new Event(id, Type.GENERATOR_UPGRADE, config, path)
                    .setTierUpgrade(config.getInt(path + ".tier-amount"));
        }
        if (Type.GENERATOR_SPEED.name().equalsIgnoreCase(type)) {
            return new Event(id, Type.GENERATOR_SPEED, config, path)
                    .setSpeedMultiplier(config.getDouble(path + ".percentage"));
        }
        if (Type.DROP_MULTIPLIER.name().equalsIgnoreCase(type)) {
            return new Event(id, Type.DROP_MULTIPLIER, config, path)
                    .setDropMultiplier(config.getInt(path + ".drop-amount"));
        }
        if (Type.MIXED_UP.name().equalsIgnoreCase(type)) {
            return new Event(id, Type.MIXED_UP, config, path);
        }
        if (Type.CUSTOM.name().equalsIgnoreCase(type)) {
            return new Event(id, Type.CUSTOM, config, path);
        }
        return null;
    }

    public String getId() {
        return id;
    }

    public Type getType() {
        return type;
    }

    public String getDisplayName() {
        return displayName;
    }

    public List<String> getStartMessage() {
        return startMessage;
    }

    public List<String> getEndMessage() {
        return endMessage;
    }

    public double getDuration() {
        return duration;
    }

    public Event setDuration(double duration) {
        this.duration = duration;
        return this;
    }

    public Double getSellMultiplier() {
        return sellMultiplier;
    }

    public Event setSellMultiplier(Double sellMultiplier) {
        this.sellMultiplier = sellMultiplier;
        return this;
    }

    public Integer getTierUpgrade() {
        return tierUpgrade;
    }

    public Event setTierUpgrade(Integer tierUpgrade) {
        this.tierUpgrade = tierUpgrade;
        return this;
    }

    public Double getSpeedMultiplier() {
        return speedMultiplier;
    }

    public Event setSpeedMultiplier(Double speedMultiplier) {
        this.speedMultiplier = speedMultiplier;
        return this;
    }

    public Integer getDropMultiplier() {
        return dropMultiplier;
    }

    public Event setDropMultiplier(Integer dropMultiplier) {
        this.dropMultiplier = dropMultiplier;
        return this;
    }

    public List<String> getBlacklistedGenerators() {
        return blacklistedGenerators;
    }

    public Event setBlacklistedGenerators(List<String> blacklistedGenerators) {
        this.blacklistedGenerators = blacklistedGenerators;
        return this;
    }

    public void sendStartMessage() {
        this.startMessage.forEach(message -> Settings.broadcast(message, new Placeholder()
                .add("{name}", this.displayName)
                .add("{duration}", TimeUtils.format((long) this.duration))));
    }

    public void sendEndMessage() {
        this.endMessage.forEach(message -> Settings.broadcast(message, new Placeholder()
                .add("{name}", this.displayName)
                .add("{next_duration}", TimeUtils.format((long) NextGens.EVENTS_CONFIG.getDouble("events.wait-time")))));
    }

    public double getChance() {
        return chance;
    }

    public boolean isOnlyByCommand() {
        return onlyByCommand;
    }

    @Override
    public Event clone() {
        try {
            // TODO: copy mutable state here, so the clone can't change the internals of the original
            return (Event) super.clone();
        } catch (CloneNotSupportedException e) {
            throw new AssertionError();
        }
    }

    public enum Type {
        SELL_MULTIPLIER,
        GENERATOR_UPGRADE,
        GENERATOR_SPEED,
        DROP_MULTIPLIER,
        MIXED_UP,
        CUSTOM
    }

}
