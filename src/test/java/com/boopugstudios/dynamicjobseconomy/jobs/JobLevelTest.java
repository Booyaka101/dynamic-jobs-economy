package com.boopugstudios.dynamicjobseconomy.jobs;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JobLevelTest {

    @Test
    void defaultConstructor_setsLevel1AndZeroExperience() {
        JobLevel jl = new JobLevel();
        assertEquals(1, jl.getLevel());
        assertEquals(0, jl.getExperience());
    }

    @Test
    void addExperience_increasesExperience() {
        JobLevel jl = new JobLevel();
        jl.addExperience(50);
        assertEquals(50, jl.getExperience());
        jl.addExperience(25);
        assertEquals(75, jl.getExperience());
    }

    @Test
    void setters_updateValues() {
        JobLevel jl = new JobLevel();
        jl.setLevel(10);
        jl.setExperience(123);
        assertEquals(10, jl.getLevel());
        assertEquals(123, jl.getExperience());
    }
}
