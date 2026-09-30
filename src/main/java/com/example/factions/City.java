package com.example.factions;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.LongTag;
import net.minecraft.world.level.ChunkPos;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

public class City {
    public final String name;
    public UUID mayor;
    public final Set<Long> claimedChunks = new HashSet<>();

    public City(String name, UUID mayor) {
        this.name = name;
        this.mayor = mayor;
    }

    public void claim(ChunkPos pos) {
        claimedChunks.add(pos.toLong());
    }

    public boolean owns(ChunkPos pos) {
        return claimedChunks.contains(pos.toLong());
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putString("name", name);
        tag.putUUID("mayor", mayor);
        ListTag chunks = new ListTag();
        for (long c : claimedChunks) {
            chunks.add(LongTag.valueOf(c));
        }
        tag.put("chunks", chunks);
        return tag;
    }

    public static City load(CompoundTag tag) {
        City city = new City(tag.getString("name"), tag.getUUID("mayor"));
        ListTag chunks = tag.getList("chunks", 4);
        for (int i = 0; i < chunks.size(); i++) {
            city.claimedChunks.add(((LongTag) chunks.get(i)).getAsLong());
        }
        return city;
    }
}
