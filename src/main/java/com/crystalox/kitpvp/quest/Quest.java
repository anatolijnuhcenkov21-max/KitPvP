package com.crystalox.kitpvp.quest;

import org.bukkit.configuration.ConfigurationSection;

public class Quest {

    private final String id;
    private final String name;
    private final int target;
    private final int reward;
    private final String period;
    private final String kit;

    public Quest(String id, String name, int target, int reward, String period, String kit) {
        this.id = id;
        this.name = name;
        this.target = target;
        this.reward = reward;
        this.period = period;
        this.kit = kit;
    }

    public static Quest fromConfig(String id, ConfigurationSection section) {
        return new Quest(id,
                section.getString("name", id),
                section.getInt("target", 1),
                section.getInt("reward", 0),
                section.getString("period", "DAILY"),
                section.getString("kit"));
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public int getTarget() {
        return target;
    }

    public int getReward() {
        return reward;
    }

    public String getPeriod() {
        return period;
    }

    public String getKit() {
        return kit;
    }
}
