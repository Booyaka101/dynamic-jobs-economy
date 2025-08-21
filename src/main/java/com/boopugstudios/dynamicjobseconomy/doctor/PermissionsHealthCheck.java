package com.boopugstudios.dynamicjobseconomy.doctor;

import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Checks that important permission nodes are registered on the server.
 *
 * This is a runtime sanity check that the permissions declared in plugin.yml
 * are actually registered with the PluginManager. It does NOT verify player-specific
 * permissions. It focuses on presence of nodes and basic structure.
 */
public class PermissionsHealthCheck {

    public static class Result {
        private final boolean ok; // true if no critical permissions are missing
        private final List<String> missingCritical;
        private final List<String> missingOptional;

        public Result(boolean ok, List<String> missingCritical, List<String> missingOptional) {
            this.ok = ok;
            this.missingCritical = missingCritical == null ? Collections.emptyList() : missingCritical;
            this.missingOptional = missingOptional == null ? Collections.emptyList() : missingOptional;
        }

        public boolean isOk() { return ok; }
        public List<String> getMissingCritical() { return missingCritical; }
        public List<String> getMissingOptional() { return missingOptional; }
    }

    private final Set<String> criticalNodes = new HashSet<>(Arrays.asList(
        "djeconomy.admin",
        "djeconomy.system.doctor",
        "djeconomy.system.reload"
    ));

    private final Set<String> optionalNodes = new HashSet<>(Arrays.asList(
        "djeconomy.admin.economy",
        "djeconomy.admin.level.set",
        "djeconomy.admin.level.get",
        "djeconomy.admin.level.reset",
        "djeconomy.admin.level.addxp",
        "djeconomy.admin.jobs.refresh",
        "djeconomy.admin.jobs.invalidate",
        "djeconomy.admin.history.view",
        "djeconomy.admin.businessinfo",
        // Legacy or convenience wildcards (may not be defined explicitly)
        "djeconomy.system.*",
        "dynamicjobs.admin.*"
    ));

    public Result run() {
        List<String> missingCrit = new ArrayList<>();
        List<String> missingOpt = new ArrayList<>();

        for (String node : criticalNodes) {
            if (!permissionRegistered(node)) {
                missingCrit.add(node);
            }
        }
        for (String node : optionalNodes) {
            if (!permissionRegistered(node)) {
                missingOpt.add(node);
            }
        }

        boolean ok = missingCrit.isEmpty();
        return new Result(ok, missingCrit, missingOpt);
    }

    /**
     * Seam for tests so we don't need to mock Bukkit statics.
     */
    protected boolean permissionRegistered(String node) {
        try {
            return Bukkit.getPluginManager() != null && Bukkit.getPluginManager().getPermission(node) != null;
        } catch (Throwable ignored) {
            return false;
        }
    }
}
