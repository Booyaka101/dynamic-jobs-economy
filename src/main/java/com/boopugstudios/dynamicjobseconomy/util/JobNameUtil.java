package com.boopugstudios.dynamicjobseconomy.util;

import com.boopugstudios.dynamicjobseconomy.jobs.Job;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Utility methods for case-insensitive job name handling.
 */
public final class JobNameUtil {
    private JobNameUtil() {}

    /**
     * Find a Job by name in a case-insensitive way. Prefers exact match.
     */
    public static Job findJobIgnoreCase(Map<String, Job> jobs, String name) {
        if (name == null || jobs == null) return null;
        Job exact = jobs.get(name);
        if (exact != null) return exact;
        for (Map.Entry<String, Job> e : jobs.entrySet()) {
            if (e.getKey().equalsIgnoreCase(name)) {
                return e.getValue();
            }
        }
        return null;
    }

    /**
     * Suggest job names by prefix, case-insensitive, sorted ascending.
     */
    public static List<String> suggestJobs(Collection<String> jobNames, String prefix) {
        String p = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        return jobNames.stream()
                .filter(j -> j.toLowerCase(Locale.ROOT).startsWith(p))
                .sorted()
                .collect(Collectors.toList());
    }
}
