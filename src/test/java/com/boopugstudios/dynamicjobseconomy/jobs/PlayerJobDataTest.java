package com.boopugstudios.dynamicjobseconomy.jobs;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class PlayerJobDataTest {

    @Test
    void newInstance_defaultsAndLoadedFlag() {
        UUID id = UUID.randomUUID();
        PlayerJobData data = new PlayerJobData(id);
        assertEquals(id, data.getPlayerUUID());
        assertFalse(data.isLoaded());
        assertTrue(data.getJobs().isEmpty());

        data.setLoaded(true);
        assertTrue(data.isLoaded());
    }

    @Test
    void addJobAndMembership() {
        PlayerJobData data = new PlayerJobData(UUID.randomUUID());
        assertFalse(data.hasJob("Miner"));
        data.addJob("Miner");
        assertTrue(data.hasJob("Miner"));
        assertEquals(1, data.getJobs().size());
    }

    @Test
    void getJobLevel_createsDefaultAndMutates() {
        PlayerJobData data = new PlayerJobData(UUID.randomUUID());
        JobLevel jl = data.getJobLevel("Farmer");
        assertNotNull(jl);
        assertEquals(1, jl.getLevel());
        assertEquals(0, jl.getExperience());

        jl.addExperience(42);
        assertEquals(42, jl.getExperience());

        jl.setLevel(7);
        assertEquals(7, jl.getLevel());
    }
}
