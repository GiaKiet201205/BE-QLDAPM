package com.example.demo.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ClassAccessPolicyTest {
    @Test
    void courseManagerCanManageAcrossClassLifecycle() {
        for (String status : new String[] {"DRAFT", "READY", "RUNNING", "COMPLETED"}) {
            assertTrue(ClassManagementService.canCsManage("MANAGER", status));
        }
    }

    @Test
    void creatorCanPrepareClassButCannotEditAlreadyRunningClass() {
        assertTrue(ClassManagementService.canCsManage("CREATOR", "DRAFT"));
        assertTrue(ClassManagementService.canCsManage("CREATOR", "READY"));
        assertFalse(ClassManagementService.canCsManage("CREATOR", "RUNNING"));
        assertFalse(ClassManagementService.canCsManage("CREATOR", "COMPLETED"));
    }

    @Test
    void aSingleSupportShiftNeverConfersWholeClassManagement() {
        for (String status : new String[] {"DRAFT", "READY", "RUNNING", "COMPLETED"}) {
            assertFalse(ClassManagementService.canCsManage("SUPPORT", status));
            assertFalse(ClassManagementService.canCsManage(null, status));
        }
    }
}
