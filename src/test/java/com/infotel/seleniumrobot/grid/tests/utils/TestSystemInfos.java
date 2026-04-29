package com.infotel.seleniumrobot.grid.tests.utils;

import com.infotel.seleniumrobot.grid.utils.SystemInfos;
import com.seleniumtests.util.helper.WaitHelper;
import org.testng.Assert;
import org.testng.annotations.Test;

public class TestSystemInfos {

    /**
     * Get CPU
     * Call it several times as the first time, returned value is 0
     */
    @Test(groups = {"grid"})
    public void testGetCpu() throws Exception {
        SystemInfos.getCpuLoad();
        WaitHelper.waitForSeconds(1);
        SystemInfos.getCpuLoad();
        WaitHelper.waitForSeconds(1);
        Assert.assertTrue(SystemInfos.getCpuLoad() > 0.0);
    }

    @Test(groups = {"grid"})
    public void getMemory() throws Exception {
        Assert.assertTrue(SystemInfos.getMemory().getTotalMemory() > 0);
        Assert.assertTrue(SystemInfos.getMemory().getFreeMemory() > 0);
    }

}
