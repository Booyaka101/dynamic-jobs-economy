package com.boopugstudios.dynamicjobseconomy.util;

import com.boopugstudios.dynamicjobseconomy.jobs.Job;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

public class JobNameUtilTest {

    private Map<String, Job> sampleJobs() {
        Map<String, Job> jobs = new HashMap<>();
        jobs.put("Miner", new Job("Miner", "Miner", "Mine stuff", 10, 5, 50));
        jobs.put("Chef", new Job("Chef", "Chef", "Cook stuff", 8, 4, 50));
        jobs.put("Farmer", new Job("Farmer", "Farmer", "Farm stuff", 7, 3, 50));
        return jobs;
    }

    @Test
    void findJobIgnoreCase_exactMatch() {
        Map<String, Job> jobs = sampleJobs();
        Job j = JobNameUtil.findJobIgnoreCase(jobs, "Miner");
        assertNotNull(j);
        assertEquals("Miner", j.getName());
    }

    @Test
    void findJobIgnoreCase_caseInsensitive() {
        Map<String, Job> jobs = sampleJobs();
        Job j1 = JobNameUtil.findJobIgnoreCase(jobs, "miner");
        Job j2 = JobNameUtil.findJobIgnoreCase(jobs, "MINER");
        assertNotNull(j1);
        assertNotNull(j2);
        assertEquals("Miner", j1.getName());
        assertEquals("Miner", j2.getName());
    }

    @Test
    void findJobIgnoreCase_unknownReturnsNull() {
        Map<String, Job> jobs = sampleJobs();
        assertNull(JobNameUtil.findJobIgnoreCase(jobs, "Builder"));
        assertNull(JobNameUtil.findJobIgnoreCase(jobs, null));
    }

    @Test
    void suggestJobs_byPrefix_caseInsensitive_sorted() {
        Set<String> names = sampleJobs().keySet();
        List<String> suggestions = JobNameUtil.suggestJobs(names, "c");
        assertEquals(Collections.singletonList("Chef"), suggestions);

        suggestions = JobNameUtil.suggestJobs(names, "F");
        assertEquals(Collections.singletonList("Farmer"), suggestions);
    }

    @Test
    void suggestJobs_emptyPrefix_returnsAllSorted() {
        Set<String> names = new HashSet<>(Arrays.asList("Miner", "Chef", "Farmer"));
        List<String> suggestions = JobNameUtil.suggestJobs(names, "");
        assertEquals(Arrays.asList("Chef", "Farmer", "Miner"), suggestions);
    }
}
