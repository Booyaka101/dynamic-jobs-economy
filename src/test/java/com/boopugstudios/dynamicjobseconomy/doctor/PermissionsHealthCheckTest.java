package com.boopugstudios.dynamicjobseconomy.doctor;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class PermissionsHealthCheckTest {

    static class TestablePermissionsHealthCheck extends PermissionsHealthCheck {
        private final Set<String> registered;
        TestablePermissionsHealthCheck(Set<String> registered) {
            this.registered = registered;
        }
        @Override
        protected boolean permissionRegistered(String node) {
            return registered.contains(node);
        }
    }

    @Test
    void all_permissions_present_passes() {
        Set<String> regs = new HashSet<>();
        // critical
        regs.add("djeconomy.admin");
        regs.add("djeconomy.system.doctor");
        regs.add("djeconomy.system.reload");
        // optional
        regs.add("djeconomy.admin.economy");
        regs.add("djeconomy.admin.level.set");
        regs.add("djeconomy.admin.level.get");
        regs.add("djeconomy.admin.level.reset");
        regs.add("djeconomy.admin.level.addxp");
        regs.add("djeconomy.admin.jobs.refresh");
        regs.add("djeconomy.admin.jobs.invalidate");
        regs.add("djeconomy.admin.history.view");
        regs.add("djeconomy.admin.businessinfo");
        regs.add("djeconomy.system.*");
        regs.add("dynamicjobs.admin.*");

        PermissionsHealthCheck.Result res = new TestablePermissionsHealthCheck(regs).run();
        assertTrue(res.isOk());
        assertTrue(res.getMissingCritical().isEmpty());
        assertTrue(res.getMissingOptional().isEmpty());
    }

    @Test
    void missing_critical_permissions_fails() {
        Set<String> regs = new HashSet<>();
        // Only some present, leave out a critical node
        regs.add("djeconomy.admin");
        // missing djeconomy.system.doctor
        regs.add("djeconomy.system.reload");

        PermissionsHealthCheck.Result res = new TestablePermissionsHealthCheck(regs).run();
        assertFalse(res.isOk());
        assertTrue(res.getMissingCritical().contains("djeconomy.system.doctor"));
    }

    @Test
    void missing_optional_permissions_warns_but_ok_true() {
        Set<String> regs = new HashSet<>();
        // All critical present
        regs.add("djeconomy.admin");
        regs.add("djeconomy.system.doctor");
        regs.add("djeconomy.system.reload");
        // Leave out some optional
        regs.add("djeconomy.admin.level.set");

        PermissionsHealthCheck.Result res = new TestablePermissionsHealthCheck(regs).run();
        assertTrue(res.isOk());
        assertTrue(res.getMissingOptional().contains("djeconomy.admin.economy"));
    }
}
