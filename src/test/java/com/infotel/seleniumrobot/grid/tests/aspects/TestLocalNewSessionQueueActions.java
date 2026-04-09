package com.infotel.seleniumrobot.grid.tests.aspects;

import com.infotel.seleniumrobot.grid.config.GridNodeConfiguration;
import com.infotel.seleniumrobot.grid.config.LaunchConfig;
import com.infotel.seleniumrobot.grid.distributor.SeleniumRobotSlotSelector;
import com.infotel.seleniumrobot.grid.servlets.client.NodeClient;
import com.infotel.seleniumrobot.grid.tests.BaseMockitoTest;
import com.seleniumtests.browserfactory.SeleniumRobotCapabilityType;
import com.seleniumtests.util.helper.WaitHelper;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.ImmutableCapabilities;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.SessionNotCreatedException;
import org.openqa.selenium.events.EventBus;
import org.openqa.selenium.events.local.GuavaEventBus;
import org.openqa.selenium.grid.config.MapConfig;
import org.openqa.selenium.grid.data.*;
import org.openqa.selenium.grid.distributor.local.LocalDistributor;
import org.openqa.selenium.grid.node.HealthCheck;
import org.openqa.selenium.grid.node.Node;
import org.openqa.selenium.grid.node.local.LocalNode;
import org.openqa.selenium.grid.security.Secret;
import org.openqa.selenium.grid.server.BaseServerOptions;
import org.openqa.selenium.grid.sessionmap.local.LocalSessionMap;
import org.openqa.selenium.grid.sessionqueue.NewSessionQueue;
import org.openqa.selenium.grid.sessionqueue.local.LocalNewSessionQueue;
import org.openqa.selenium.internal.Either;
import org.openqa.selenium.remote.HttpSessionId;
import org.openqa.selenium.remote.SessionId;
import org.openqa.selenium.remote.http.HttpHandler;
import org.openqa.selenium.remote.http.HttpRequest;
import org.openqa.selenium.remote.http.HttpResponse;
import org.openqa.selenium.remote.tracing.Propagator;
import org.openqa.selenium.remote.tracing.Span;
import org.openqa.selenium.remote.tracing.TraceContext;
import org.openqa.selenium.remote.tracing.Tracer;
import org.openqa.selenium.remote.tracing.empty.NullAttributeMap;
import org.openqa.selenium.support.ui.FluentWait;
import org.openqa.selenium.support.ui.Wait;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.when;
import static org.openqa.selenium.remote.Dialect.W3C;
import static org.openqa.selenium.remote.http.HttpMethod.GET;

public class TestLocalNewSessionQueueActions extends BaseMockitoTest {


    @Mock
    private Tracer tracer;

    @Mock
    private Propagator propagator;

    @Mock
    private TraceContext traceContext;

    @Mock
    private Span span;

    private final Secret registrationSecret = new Secret("bavarian smoked");
    private static final int NEW_SESSION_THREAD_POOL_SIZE = Runtime.getRuntime().availableProcessors();
    private EventBus bus;
    private URI uri;
    private Wait<Object> wait;

    @BeforeMethod(alwaysRun = true)
    public void setup() throws URISyntaxException {
        when(tracer.getPropagator()).thenReturn(propagator);
        when(propagator.extractContext(any(), any(), any())).thenReturn(traceContext);
        when(traceContext.createSpan(anyString())).thenReturn(span);
        when(tracer.getCurrentContext()).thenReturn(traceContext);
        when(tracer.createAttributeMap()).thenReturn(new NullAttributeMap());

        bus = new GuavaEventBus();

        uri = new URI("http://localhost:1234");

        wait =
                new FluentWait<>(new Object()).ignoring(Throwable.class).withTimeout(Duration.ofSeconds(5));
    }


    /**
     * Check only one session can be created due to selector limitations
     */
    @Test(groups = {"grid"})
    void shouldNotBeAbleToAddMultipleSessionsConcurrently() throws Exception {

        GridNodeConfiguration gridConfiguration = new GridNodeConfiguration();
        gridConfiguration.setServerOptions(new BaseServerOptions(new MapConfig()));
        LaunchConfig.setCurrentNodeConfig(gridConfiguration);

        NewSessionQueue queue =
                new LocalNewSessionQueue(
                        tracer,
                        new DefaultSlotMatcher(),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(1),
                        registrationSecret,
                        5);

        // Add one node to ensure that everything is created in that.
        MutableCapabilities stereotype = new MutableCapabilities();
        stereotype.setCapability("browserName", "cheese");
        stereotype.setCapability(LaunchConfig.MAX_SESSIONS, 1);

        class VerifyingHandler extends Session implements HttpHandler {
            private VerifyingHandler(SessionId id, Capabilities capabilities) {
                super(id, uri, new ImmutableCapabilities(), capabilities, Instant.now());
            }

            @Override
            public HttpResponse execute(HttpRequest req) {
                Optional<SessionId> id = HttpSessionId.getSessionId(req.getUri()).map(SessionId::new);
                Assert.assertEquals(id.get(), getId());
                return new HttpResponse();
            }
        }

        // Only use one node.
        Node node =
                LocalNode.builder(tracer, bus, uri, uri, registrationSecret)
                        .add(stereotype, new TestSessionFactory(VerifyingHandler::new))
                        .add(stereotype, new TestSessionFactory(VerifyingHandler::new))
                        .add(stereotype, new TestSessionFactory(VerifyingHandler::new))
                        .maximumConcurrentSessions(3)
                        .advanced()
                        .healthCheck(() -> new HealthCheck.Result(Availability.UP, "UP!"))
                        .build();

        LocalDistributor distributor =
                new LocalDistributor(
                        tracer,
                        bus,
                        new PassthroughHttpClient.Factory(node),
                        new LocalSessionMap(tracer, bus),
                        queue,
                        new SeleniumRobotSlotSelector(),
                        registrationSecret,
                        Duration.ofMinutes(5),
                        false,
                        Duration.ofSeconds(5),
                        NEW_SESSION_THREAD_POOL_SIZE,
                        new DefaultSlotMatcher(),
                        Duration.ofSeconds(30));

        distributor.add(node);
        wait.until(obj -> distributor.getStatus().hasCapacity());

        SessionRequest sessionRequest =
                new SessionRequest(
                        new RequestId(UUID.randomUUID()),
                        Instant.now(),
                        Set.of(W3C),
                        Set.of(new ImmutableCapabilities("browserName", "cheese")),
                        Map.of(),
                        Map.of());

        List<Callable<SessionId>> callables = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            callables.add(
                    () -> {
                        try (MockedConstruction<NodeClient> mockedNodeClient = mockConstruction(NodeClient.class, (nodeClient, context) -> {
                            when(nodeClient.isBusyOnOtherSlot(any())).thenReturn(true);
                        })) {
                            Either<SessionNotCreatedException, CreateSessionResponse> result =
                                    distributor.newSession(sessionRequest);
                            if (result.isRight()) {
                                CreateSessionResponse res = result.right();
                                Assert.assertEquals(res.getSession().getCapabilities().getBrowserName(), "cheese");
                                return res.getSession().getId();
                            }
                            return null;
                        }
                    });
        }

        List<Future<SessionId>> futures = Executors.newFixedThreadPool(3).invokeAll(callables);

        int createdSessions = 0;
        for (Future<SessionId> future : futures) {
            SessionId id = future.get(2, TimeUnit.SECONDS);
            if (id != null) {
                createdSessions++;

                // Now send a random command.
                HttpResponse res = node.execute(new HttpRequest(GET, String.format("/session/%s/url", id)));
                Assert.assertTrue(res.isSuccessful());
            }
        }

        // only 1 session is created as node limits it
        Assert.assertEquals(createdSessions, 1);
    }

    /**
     * Check more sessions can be created as expected capabilities allow it
     */
    @Test(groups = {"grid"})
    void shouldBeAbleToAddMultipleSessionsConcurrently() throws Exception {

        GridNodeConfiguration gridConfiguration = new GridNodeConfiguration();
        gridConfiguration.setServerOptions(new BaseServerOptions(new MapConfig()));
        LaunchConfig.setCurrentNodeConfig(gridConfiguration);

        NewSessionQueue queue =
                new LocalNewSessionQueue(
                        tracer,
                        new DefaultSlotMatcher(),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(1),
                        registrationSecret,
                        5);

        // Add one node to ensure that everything is created in that.
        Capabilities stereotype = new ImmutableCapabilities(
                "browserName", "cheese",
                LaunchConfig.MAX_SESSIONS, 1,
                // simulates the case where each test already running accepts other on node
                // this is due to the fact that on reservation, session capabilities are set to the value of stereotypes
                SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE, true);

        class VerifyingHandler extends Session implements HttpHandler {
            private VerifyingHandler(SessionId id, Capabilities capabilities) {
                super(id, uri, new ImmutableCapabilities(), capabilities, Instant.now());
            }

            @Override
            public HttpResponse execute(HttpRequest req) {
                Optional<SessionId> id = HttpSessionId.getSessionId(req.getUri()).map(SessionId::new);
                Assert.assertEquals(id.get(), getId());
                return new HttpResponse();
            }
        }

        // Only use one node.
        Node node =
                LocalNode.builder(tracer, bus, uri, uri, registrationSecret)
                        .add(stereotype, new TestSessionFactory(VerifyingHandler::new))
                        .add(stereotype, new TestSessionFactory(VerifyingHandler::new))
                        .add(stereotype, new TestSessionFactory(VerifyingHandler::new))
                        .maximumConcurrentSessions(3)
                        .advanced()
                        .healthCheck(() -> new HealthCheck.Result(Availability.UP, "UP!"))
                        .build();

        LocalDistributor distributor =
                new LocalDistributor(
                        tracer,
                        bus,
                        new PassthroughHttpClient.Factory(node),
                        new LocalSessionMap(tracer, bus),
                        queue,
                        new SeleniumRobotSlotSelector(),
                        registrationSecret,
                        Duration.ofMinutes(5),
                        false,
                        Duration.ofSeconds(5),
                        NEW_SESSION_THREAD_POOL_SIZE,
                        new DefaultSlotMatcher(),
                        Duration.ofSeconds(30));

        distributor.add(node);
        wait.until(obj -> distributor.getStatus().hasCapacity());

        SessionRequest sessionRequest =
                new SessionRequest(
                        new RequestId(UUID.randomUUID()),
                        Instant.now(),
                        Set.of(W3C),
                        Set.of(new ImmutableCapabilities("browserName", "cheese", SeleniumRobotCapabilityType.ALLOW_ADDITIONAL_SESSIONS_ON_NODE, true)),
                        Map.of(),
                        Map.of());

        List<Callable<SessionId>> callables = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            callables.add(
                    () -> {
                        try (MockedConstruction<NodeClient> mockedNodeClient = mockConstruction(NodeClient.class, (nodeClient, context) -> {
                            when(nodeClient.isBusyOnOtherSlot(any())).thenReturn(true);
                        })) {
                            Either<SessionNotCreatedException, CreateSessionResponse> result =
                                    distributor.newSession(sessionRequest);
                            if (result.isRight()) {
                                CreateSessionResponse res = result.right();
                                Assert.assertEquals(res.getSession().getCapabilities().getBrowserName(), "cheese");
                                return res.getSession().getId();
                            }
                            return null;
                        }
                    });
        }

        List<Future<SessionId>> futures = Executors.newFixedThreadPool(3).invokeAll(callables);

        int createdSessions = 0;
        for (Future<SessionId> future : futures) {
            SessionId id = future.get(2, TimeUnit.SECONDS);
            if (id != null) {
                createdSessions++;

                // Now send a random command.
                HttpResponse res = node.execute(new HttpRequest(GET, String.format("/session/%s/url", id)));
                Assert.assertTrue(res.isSuccessful());
            }
        }

        // only 1 session is created as node limits it
        Assert.assertEquals(createdSessions, 3);
    }

    /**
     * When session request to be attached to a specific node, through the ad-hoc capability, allow more sessions on node
     */
    @Test(groups = {"grid"})
    void shouldBeAbleToAddMultipleSessionsConcurrentlyWithAttachToNodeCapability() throws Exception {

        GridNodeConfiguration gridConfiguration = new GridNodeConfiguration();
        gridConfiguration.setServerOptions(new BaseServerOptions(new MapConfig()));
        LaunchConfig.setCurrentNodeConfig(gridConfiguration);

        NewSessionQueue queue =
                new LocalNewSessionQueue(
                        tracer,
                        new DefaultSlotMatcher(),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(2),
                        Duration.ofSeconds(1),
                        registrationSecret,
                        5);

        // Add one node to ensure that everything is created in that.
        MutableCapabilities stereotype = new MutableCapabilities();
        stereotype.setCapability("browserName", "cheese");
        stereotype.setCapability(LaunchConfig.MAX_SESSIONS, 1);

        class VerifyingHandler extends Session implements HttpHandler {
            private VerifyingHandler(SessionId id, Capabilities capabilities) {
                super(id, uri, new ImmutableCapabilities(), capabilities, Instant.now());
            }

            @Override
            public HttpResponse execute(HttpRequest req) {
                Optional<SessionId> id = HttpSessionId.getSessionId(req.getUri()).map(SessionId::new);
                Assert.assertEquals(id.get(), getId());
                return new HttpResponse();
            }
        }

        // Only use one node.
        Node node =
                LocalNode.builder(tracer, bus, uri, uri, registrationSecret)
                        .add(stereotype, new TestSessionFactory(VerifyingHandler::new))
                        .add(stereotype, new TestSessionFactory(VerifyingHandler::new))
                        .add(stereotype, new TestSessionFactory(VerifyingHandler::new))
                        .maximumConcurrentSessions(3)
                        .advanced()
                        .healthCheck(() -> new HealthCheck.Result(Availability.UP, "UP!"))
                        .build();

        LocalDistributor distributor =
                new LocalDistributor(
                        tracer,
                        bus,
                        new PassthroughHttpClient.Factory(node),
                        new LocalSessionMap(tracer, bus),
                        queue,
                        new SeleniumRobotSlotSelector(),
                        registrationSecret,
                        Duration.ofMinutes(5),
                        false,
                        Duration.ofSeconds(5),
                        NEW_SESSION_THREAD_POOL_SIZE,
                        new DefaultSlotMatcher(),
                        Duration.ofSeconds(30));

        distributor.add(node);
        wait.until(obj -> distributor.getStatus().hasCapacity());

        SessionRequest initialSessionRequest =
                new SessionRequest(
                        new RequestId(UUID.randomUUID()),
                        Instant.now(),
                        Set.of(W3C),
                        Set.of(new ImmutableCapabilities("browserName", "cheese")),
                        Map.of(),
                        Map.of());
        SessionRequest sessionRequestAttach =
                new SessionRequest(
                        new RequestId(UUID.randomUUID()),
                        Instant.now(),
                        Set.of(W3C),
                        Set.of(new ImmutableCapabilities("browserName", "cheese", SeleniumRobotCapabilityType.ATTACH_SESSION_ON_NODE, "http://localhost:1234")),
                        Map.of(),
                        Map.of());

        List<Callable<SessionId>> callables = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            int waitBeforeCreateSession = i;
            callables.add(
                    () -> {
                        WaitHelper.waitForSeconds(waitBeforeCreateSession);
                        SessionRequest sessionRequest = waitBeforeCreateSession == 0 ? initialSessionRequest : sessionRequestAttach;
                        try (MockedConstruction<NodeClient> mockedNodeClient = mockConstruction(NodeClient.class, (nodeClient, context) -> {
                            when(nodeClient.isBusyOnOtherSlot(any())).thenReturn(true);
                        })) {
                            Either<SessionNotCreatedException, CreateSessionResponse> result =
                                    distributor.newSession(sessionRequest);
                            if (result.isRight()) {
                                CreateSessionResponse res = result.right();
                                Assert.assertEquals(res.getSession().getCapabilities().getBrowserName(), "cheese");
                                return res.getSession().getId();
                            }
                            return null;
                        }
                    });
        }

        List<Future<SessionId>> futures = Executors.newFixedThreadPool(3).invokeAll(callables);

        int createdSessions = 0;
        for (Future<SessionId> future : futures) {
            SessionId id = future.get(2, TimeUnit.SECONDS);
            if (id != null) {
                createdSessions++;

                // Now send a random command.
                HttpResponse res = node.execute(new HttpRequest(GET, String.format("/session/%s/url", id)));
                Assert.assertTrue(res.isSuccessful());
            }
        }

        // 3 sessions should be allowed as we request to attach to the node
        Assert.assertEquals(createdSessions, 3);
    }
}
