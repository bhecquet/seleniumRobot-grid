/**
 * Copyright 2017 www.infotel.com
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.infotel.seleniumrobot.grid.tests;

import com.infotel.seleniumrobot.grid.GridStarter;
import com.infotel.seleniumrobot.grid.servlets.client.GridStatusClient;
import com.infotel.seleniumrobot.grid.servlets.client.NodeClient;
import com.infotel.seleniumrobot.grid.servlets.client.entities.SeleniumNodeStatus;
import com.seleniumtests.browserfactory.BrowserInfo;
import com.seleniumtests.browserfactory.mobile.AdbWrapper;
import com.seleniumtests.driver.BrowserType;
import com.seleniumtests.util.helper.WaitHelper;
import com.seleniumtests.util.logging.SeleniumRobotLogger;
import com.seleniumtests.util.osutility.OSUtility;
import org.apache.logging.log4j.Logger;
import org.mockito.MockedConstruction;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.testng.Assert;
import org.testng.annotations.Test;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.util.*;

import static org.mockito.Mockito.*;

public class TestGridStarter2 extends BaseMockitoTest {

    private static final Logger logger = SeleniumRobotLogger.getLogger(TestGridStarter2.class);


    /**
     * Test that start a node and a hub, and checks that status API do not change
     * /!\ This does not check the "session" content on node
     */
    @Test(groups = {"grid"})
    public void testGridStart() throws Exception {


        Map<BrowserType, List<BrowserInfo>> browsers = new LinkedHashMap<>();
        BrowserInfo firefoxInfo = Mockito.spy(new BrowserInfo(BrowserType.FIREFOX, "90.0", "/usr/bin/firefox", false, true));
        BrowserInfo ieInfo = Mockito.spy(new BrowserInfo(BrowserType.INTERNET_EXPLORER, "11.0", "/home/iexplore", false, false));

        Mockito.doReturn("geckodriver").when(firefoxInfo).getDriverFileName();
        Mockito.doReturn("iedriver").when(ieInfo).getDriverFileName();

        browsers.put(BrowserType.FIREFOX, List.of(firefoxInfo));
        browsers.put(BrowserType.INTERNET_EXPLORER, List.of(ieInfo));

        // no mobile devices
        try (MockedConstruction<AdbWrapper> mockedAdbWrapper = mockConstruction(AdbWrapper.class, (adbWrapper, context) -> {
            when(adbWrapper.getDeviceList()).thenReturn(new ArrayList<>());
        })
        ) {
            StartGridThread gridHubThread = new StartGridThread(new String[]{"hub"}, browsers);
            gridHubThread.start();


            GridStatusClient gridStatusClient = new GridStatusClient(new URI(String.format("http://localhost:%d", gridHubThread.getPort())).toURL());
            // wait for hub to be up
            boolean started = false;
            for (int i = 0; i < 10; i++) {
                try {
                    gridStatusClient.getStatus();
                    started = true;
                    break;
                } catch (Exception e) {
                    logger.info("Hub not started, wait");
                    WaitHelper.waitForSeconds(5);
                }
            }
            Assert.assertTrue(started, "Hub never started");

            Assert.assertFalse(gridStatusClient.isReady()); // no connected nodes

            StartGridThread gridNodeThread = new StartGridThread(new String[]{"node",
                    "--max-sessions",
                    "3",
                    "--override-max-sessions",
                    "true",
                    "--tracing",
                    "false",
                    "--hub",
                    String.format("http://localhost:%d",
                            gridHubThread.getPort())}, browsers);
            gridNodeThread.start();

            // wait for grid to be ready
            started = false;
            for (int i = 0; i < 20; i++) {
                if (gridStatusClient.isReady()) {
                    started = true;
                    break;
                } else {

                    logger.info("Node not started, wait");
                    WaitHelper.waitForSeconds(5);
                }
            }
            Assert.assertTrue(started, "Node never started");

            NodeClient nodeClient = new NodeClient(new URI("http://localhost:5555").toURL());
            Assert.assertTrue(nodeClient.isReady());
            SeleniumNodeStatus nodeStatus = nodeClient.getStatus();
            Assert.assertTrue(nodeStatus.isReady());
            Assert.assertEquals(nodeStatus.getTestSlots(), 4);
            Assert.assertEquals(nodeStatus.getSessionList().size(), 0);
        }
    }

    static class StartGridThread extends Thread {

        private GridStarter starter;
        private final int port;
        private final String[] args;
        private final Map<BrowserType, List<BrowserInfo>> browsers;

        public StartGridThread(String[] args, Map<BrowserType, List<BrowserInfo>> browsers) {
            this.args = args;
            this.port = findFreePort();
            this.browsers = browsers;
        }


        private int findFreePort() {
            try (ServerSocket socket = new ServerSocket(0)) {
                socket.setReuseAddress(true);
                return socket.getLocalPort();

            } catch (IOException e) {
                // ignore
            }

            throw new IllegalStateException("Could not find a free TCP/IP port ");
        }

        @Override
        public void run() {
            logger.info("start");
            logger.info(OSUtility.getCurrentPlatorm());
            try (MockedStatic<OSUtility> mockedOSUtility2 = mockStatic(OSUtility.class, CALLS_REAL_METHODS)) {
                List<String> newArgs = new ArrayList<>(Arrays.asList(args));
                if (args[0].equals("hub")) {
                    newArgs.add("--port");
                    newArgs.add(Integer.toString(port));
                    newArgs.add("--host");
                    newArgs.add("127.0.0.1");
                    newArgs.add("--tracing");
                    newArgs.add("false");
                } else {
                    mockedOSUtility2.when(OSUtility::getInstalledBrowsersWithVersion).thenReturn(browsers);
                }
                logger.info("current OS {}", OSUtility.getCurrentPlatorm());
                logger.info("options: {}", newArgs);
                starter = new GridStarter(newArgs.toArray(new String[]{}));
                starter.configure();
                starter.start(starter.getLaunchConfig().getArgs());
            } catch (Exception e) {
                logger.info("current OS2 {}", OSUtility.getCurrentPlatorm());
            }

            logger.info("stop");
        }


        public GridStarter getStarter() {
            return starter;
        }


        public int getPort() {
            return port;
        }
    }
}