package org.jetlinks.community.auth.service;

import com.google.common.cache.CacheBuilder;
import io.r2dbc.spi.ConnectionFactories;
import org.hswebframework.ezorm.core.NestConditional;
import org.hswebframework.ezorm.rdb.mapping.ReactiveDelete;
import org.hswebframework.ezorm.rdb.mapping.ReactiveQuery;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.DefaultDimensionType;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.DimensionProvider;
import org.hswebframework.web.authorization.ReactiveAuthenticationInitializeService;
import org.hswebframework.web.authorization.events.AuthorizationInitializeEvent;
import org.hswebframework.web.authorization.simple.SimpleAuthentication;
import org.hswebframework.web.authorization.simple.SimpleUser;
import org.hswebframework.web.cache.supports.GuavaReactiveCacheManager;
import org.hswebframework.web.crud.events.EntityCreatedEvent;
import org.hswebframework.web.crud.events.EntitySavedEvent;
import org.hswebframework.web.crud.utils.TransactionUtils;
import org.hswebframework.web.system.authorization.api.entity.ActionEntity;
import org.hswebframework.web.system.authorization.api.entity.AuthorizationSettingEntity;
import org.hswebframework.web.system.authorization.api.entity.PermissionEntity;
import org.hswebframework.web.system.authorization.api.event.ClearUserAuthorizationCacheEvent;
import org.hswebframework.web.system.authorization.defaults.configuration.PermissionProperties;
import org.hswebframework.web.system.authorization.defaults.service.DefaultAuthorizationSettingService;
import org.hswebframework.web.system.authorization.defaults.service.DefaultPermissionService;
import org.hswebframework.web.system.authorization.defaults.service.DefaultReactiveAuthenticationManager;
import org.jetlinks.community.auth.configuration.MenuProperties;
import org.jetlinks.community.auth.dimension.UserAuthenticationEventPublisher;
import org.jetlinks.community.auth.entity.MenuBindEntity;
import org.jetlinks.community.auth.entity.MenuEntity;
import org.jetlinks.community.auth.entity.MenuView;
import org.jetlinks.community.auth.entity.PermissionInfo;
import org.jetlinks.community.auth.initialize.MenuAuthenticationInitializeService;
import org.jetlinks.community.auth.initialize.PermissionCacheHelper;
import org.jetlinks.community.auth.service.request.MenuGrantRequest;
import org.jetlinks.community.auth.web.request.AuthorizationSettingDetail;
import org.jetlinks.core.event.EventBus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.r2dbc.connection.R2dbcTransactionManager;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.reactive.TransactionalOperator;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Sinks;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
class AuthorizationCacheLifecycleTest {
    private static final String USER = "reader";
    private static final String ROLE = "reader-role";

    private final List<ClearUserAuthorizationCacheEvent> invalidations = new ArrayList<>();
    private final List<Authentication> refreshed = new ArrayList<>();
    private final List<AuthorizationSettingEntity> savedSettings = new ArrayList<>();
    private final DefaultReactiveAuthenticationManager authentications = new DefaultReactiveAuthenticationManager();
    private final DefaultAuthorizationSettingService settings = mock(DefaultAuthorizationSettingService.class);
    private final DimensionProvider roles = mock(DimensionProvider.class);
    private final GuavaReactiveCacheManager caches = new GuavaReactiveCacheManager(CacheBuilder.newBuilder());

    private DatabaseClient database;
    private TransactionalOperator transactions;
    private DefaultMenuService menus;
    private MenuGrantService grants;
    private AuthorizationSettingDetailService details;
    private MenuEntity menu;

    private final ApplicationEventPublisher publisher = value -> {
        ClearUserAuthorizationCacheEvent event = (ClearUserAuthorizationCacheEvent) value;
        authentications.handleClearAuthCache(event);
        event.async(Mono.fromRunnable(() -> invalidations.add(event)));
        event.async(authentications.getByUserId(USER).doOnNext(refreshed::add));
    };

    @BeforeEach
    void setUp() {
        var factory = ConnectionFactories.get("r2dbc:h2:mem:///auth-cache-" + UUID.randomUUID() + ";DB_CLOSE_DELAY=-1");
        database = DatabaseClient.create(factory);
        var transactionManager = new R2dbcTransactionManager(factory);
        transactions = TransactionalOperator.create(transactionManager);
        TransactionUtils.setup(transactionManager);
        database.sql("create table menu_binding (menu_id varchar(64) primary key)").then().block();

        ReactiveRepository<MenuBindEntity, String> bindings = mock(ReactiveRepository.class);
        ReactiveDelete delete = mock(ReactiveDelete.class, RETURNS_SELF);
        NestConditional<ReactiveDelete> nestedDelete = mock(NestConditional.class, RETURNS_SELF);
        when(delete.nest()).thenReturn(nestedDelete);
        doReturn(delete).when(nestedDelete).end();
        when(bindings.createDelete()).thenReturn(delete);
        when(delete.execute()).thenReturn(database.sql("delete from menu_binding")
            .fetch().rowsUpdated().map(Long::intValue));
        when(bindings.save(anyCollection())).thenAnswer(call -> Flux
            .fromIterable((List<MenuBindEntity>) call.getArgument(0))
            .concatMap(bind -> database.sql("insert into menu_binding values (:menu)")
                .bind("menu", bind.getMenuId()).fetch().rowsUpdated())
            .then().thenReturn(SaveResult.of(1, 0)));
        ReactiveQuery<MenuBindEntity> query = mock(ReactiveQuery.class, RETURNS_SELF);
        when(bindings.createQuery()).thenReturn(query);
        when(query.fetch()).thenReturn(database.sql("select menu_id from menu_binding")
            .map((row, metadata) -> {
                MenuBindEntity bind = new MenuBindEntity();
                bind.setMenuId(row.get("menu_id", String.class));
                bind.setMerge(true);
                bind.setPriority(10);
                return bind;
            }).all());

        menu = new MenuEntity();
        menu.setId("devices");
        menu.setName("Devices");
        menu.setOwner("test");
        menu.setStatus((byte) 1);
        PermissionInfo permission = new PermissionInfo();
        permission.setPermission("device");
        permission.setActions(Set.of("query"));
        menu.setPermissions(List.of(permission));
        menus = spy(new DefaultMenuService(bindings, publisher, caches, new MenuProperties()));
        doReturn(Mono.just(Map.of(menu.getId(), menu))).when(menus).getEnabledMenus();

        DefaultPermissionService permissions = mock(DefaultPermissionService.class);
        ReactiveQuery<PermissionEntity> permissionQuery = mock(ReactiveQuery.class, RETURNS_SELF);
        when(permissions.createQuery()).thenReturn(permissionQuery);
        PermissionEntity definition = new PermissionEntity();
        definition.setId("device");
        ActionEntity action = new ActionEntity();
        action.setAction("query");
        definition.setActions(List.of(action));
        when(permissionQuery.fetch()).thenReturn(Flux.just(definition));
        var initializer = new MenuAuthenticationInitializeService(menus, new PermissionCacheHelper(permissions));
        ReflectionTestUtils.setField(authentications, "cacheManager", caches);
        ReflectionTestUtils.setField(authentications, "initializeService", (ReactiveAuthenticationInitializeService) userId -> Mono.defer(() -> {
            SimpleAuthentication authentication = new SimpleAuthentication();
            authentication.setUser(SimpleUser.builder().id(userId).username(userId).build());
            authentication.setDimensions(List.of(Dimension.of(ROLE, ROLE, DefaultDimensionType.role)));
            AuthorizationInitializeEvent event = new AuthorizationInitializeEvent(authentication);
            return event.publish(value -> initializer.refactorPermission((AuthorizationInitializeEvent) value))
                .then(Mono.fromSupplier(event::getAuthentication));
        }));

        doReturn(Flux.just(DefaultDimensionType.role)).when(roles).getAllType();
        when(roles.getUserIdByDimensionId(ROLE)).thenReturn(Flux.just(USER));
        doReturn(Mono.just(Dimension.of(ROLE, ROLE, DefaultDimensionType.role)))
            .when(roles).getDimensionById(DefaultDimensionType.role, ROLE);
        details = new AuthorizationSettingDetailService(settings, List.of(roles), new PermissionProperties(), publisher);
        grants = new MenuGrantService(details, bindings, mock(ReactiveRepository.class), publisher, menus);

        ReactiveRepository<AuthorizationSettingEntity, String> settingRepository = mock(ReactiveRepository.class);
        when(settings.getRepository()).thenReturn(settingRepository);
        ReactiveDelete settingDelete = mock(ReactiveDelete.class, RETURNS_SELF);
        when(settingRepository.createDelete()).thenReturn(settingDelete);
        when(settingDelete.execute()).thenReturn(Mono.just(1));
        when(settings.save(any(Publisher.class))).thenAnswer(call -> Flux
            .from((Publisher<AuthorizationSettingEntity>) call.getArgument(0))
            .doOnNext(savedSettings::add)
            .then(Mono.defer(() -> ClearUserAuthorizationCacheEvent.of(USER).publish(publisher)))
            .thenReturn(SaveResult.of(1, 0)));
    }

    @AfterEach
    void tearDown() {
        TransactionUtils.setup(null);
        if (database != null) {
            database.sql("shutdown").then().block();
        }
    }

    @Test
    void grantReplacesCachedEmptyIdentityAfterCommit() {
        assertTrue(authentication().getPermissions().isEmpty());
        commit(grants.grant(request()), () -> {
            assertTrue(authentication().getPermissions().isEmpty());
            assertFalse(menus.grantedCaching.isEmpty());
        });
        assertTrue(authentication().hasPermission("device", "query"));
        assertEquals(1, refreshed.size());
        assertTrue(refreshed.get(0).hasPermission("device", "query"));
    }

    @Test
    void clearGrantRevokesCachedPermissionsAfterCommit() {
        transactions.transactional(grants.grant(request())).block();
        assertTrue(authentication().hasPermission("device", "query"));
        invalidations.clear();
        refreshed.clear();
        commit(grants.clearGrant("role", ROLE, Set.of("test")),
            () -> assertTrue(authentication().hasPermission("device", "query")));
        assertTrue(authentication().getPermissions().isEmpty());
        assertTrue(refreshed.get(0).getPermissions().isEmpty());
    }

    @Test
    void rolledBackGrantPreservesCacheAndDatabase() {
        Authentication before = authentication();
        var failure = new IllegalStateException("rollback");
        StepVerifier.create(transactions.transactional(grants.grant(request()).then(Mono.error(failure))))
            .expectErrorMatches(error -> error == failure).verify();
        assertTrue(invalidations.isEmpty());
        assertSame(before, authentication());
        assertEquals(0L, database.sql("select count(*) as total from menu_binding")
            .map(row -> row.get("total", Long.class)).one().block());
    }

    @Test
    void menuDefinitionChangesClearBothMenuSnapshotsAfterCommit() {
        authentication();
        caches.getCache(DefaultMenuService.MENU_CACHE_KEY)
            .put(DefaultMenuService.MENU_CACHE_KEY, Mono.just("old")).block();
        EntitySavedEvent<MenuEntity> event = new EntitySavedEvent<>(List.of(menu), MenuEntity.class);
        menus.handleMenuEntity(event);
        commit(event.getAsync(), () -> assertFalse(menus.grantedCaching.isEmpty()));
        assertTrue(caches.getCache(DefaultMenuService.MENU_CACHE_KEY)
            .getMono(DefaultMenuService.MENU_CACHE_KEY).blockOptional().isEmpty());
        assertTrue(invalidations.get(0).isAll());
    }

    @Test
    void rolledBackMenuCreationDoesNotClearDefinitionCache() {
        caches.getCache(DefaultMenuService.MENU_CACHE_KEY)
            .put(DefaultMenuService.MENU_CACHE_KEY, Mono.just("old")).block();
        var event = new EntityCreatedEvent<>(List.of(menu), MenuEntity.class);
        menus.handleMenuEntity(event);
        StepVerifier.create(transactions.transactional(event.getAsync().then(Mono.error(new IllegalStateException("rollback")))))
            .expectError(IllegalStateException.class).verify();
        assertEquals("old", caches.getCache(DefaultMenuService.MENU_CACHE_KEY)
            .getMono(DefaultMenuService.MENU_CACHE_KEY).block());
    }

    @Test
    void noAffectedUsersDoesNotClearAllAuthentications() {
        when(roles.getUserIdByDimensionId(ROLE)).thenReturn(Flux.empty());
        details.clearPermissionUserAuth("role", ROLE).block();
        details.clearPermissionUserAuth("unknown", ROLE).block();
        assertTrue(invalidations.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"save", "add", "delete"})
    void authorizationDetailMutationsInvalidateOnceAfterCommit(String operation) {
        AtomicInteger subscriptions = new AtomicInteger();
        Flux<AuthorizationSettingDetail> input = Flux.defer(() -> {
            assertEquals(1, subscriptions.incrementAndGet(), "请求流只能消费一次");
            AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
            detail.setTargetType("role");
            detail.setTargetId(ROLE);
            detail.setPermissionList(List.of(AuthorizationSettingDetail.PermissionInfo.of("device", Set.of("query"))));
            return Flux.just(detail);
        });
        Mono<Void> mutation = switch (operation) {
            case "save" -> details.saveDetail(null, input);
            case "add" -> details.addDetail(null, input);
            default -> details.deleteDetail(input);
        };
        commit(mutation, () -> {});
        assertEquals(Set.of(USER), invalidations.get(0).getUserId());
        if (!operation.equals("delete")) {
            assertEquals(1, savedSettings.size());
            assertEquals(Set.of("query"), savedSettings.get(0).getActions());
        }
    }

    @Test
    void emptyPermissionReplacementStillInvalidatesAffectedUser() {
        AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
        detail.setTargetType("role");
        detail.setTargetId(ROLE);
        detail.setPermissionList(List.of());
        commit(details.saveDetail(null, Flux.just(detail)), () -> {});
        assertTrue(savedSettings.isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = {"save", "add", "delete"})
    void rolledBackAuthorizationDetailDoesNotInvalidate(String operation) {
        Authentication before = authentication();
        AuthorizationSettingDetail detail = new AuthorizationSettingDetail();
        detail.setTargetType("role");
        detail.setTargetId(ROLE);
        detail.setPermissionList(List.of(AuthorizationSettingDetail.PermissionInfo.of("device", Set.of("query"))));
        Mono<Void> mutation = switch (operation) {
            case "save" -> details.saveDetail(null, Flux.just(detail));
            case "add" -> details.addDetail(null, Flux.just(detail));
            default -> details.deleteDetail(Flux.just(detail));
        };
        StepVerifier.create(transactions.transactional(mutation.then(Mono.error(new IllegalStateException("rollback")))))
            .expectError(IllegalStateException.class).verify();
        assertTrue(invalidations.isEmpty());
        assertSame(before, authentication());
    }

    @ParameterizedTest
    @ValueSource(ints = {1, 2})
    void authenticationNotificationsWaitForCommitIncludingBatchEnqueue(int userCount) {
        EventBus bus = mock(EventBus.class);
        Sinks.Many<String> topics = Sinks.many().unicast().onBackpressureBuffer();
        when(bus.publish(anyString(), any(Publisher.class))).thenAnswer(call -> {
            topics.tryEmitNext(call.getArgument(0));
            return Mono.empty();
        });
        var notifications = new UserAuthenticationEventPublisher(bus);
        try {
            var event = ClearUserAuthorizationCacheEvent.of(userCount == 1 ? List.of(USER) : List.of(USER, "other"));
            notifications.handleEvent(event);
            verifyNoInteractions(bus);
            Mono<Void> mutation = event.getAsync().then(Mono.fromRunnable(() -> verifyNoInteractions(bus)));
            StepVerifier.create(transactions.transactional(mutation).thenMany(topics.asFlux().take(userCount)))
                .expectNextCount(userCount).expectComplete().verify(Duration.ofSeconds(4));
        } finally {
            notifications.shutdown();
        }
    }

    @Test
    void rolledBackAuthenticationNotificationDoesNotPublish() {
        EventBus bus = mock(EventBus.class);
        var notifications = new UserAuthenticationEventPublisher(bus);
        try {
            var event = ClearUserAuthorizationCacheEvent.of(USER, "other");
            notifications.handleEvent(event);
            StepVerifier.create(transactions.transactional(event.getAsync().then(Mono.error(new IllegalStateException("rollback")))))
                .expectError(IllegalStateException.class).verify();
            verifyNoInteractions(bus);
        } finally {
            notifications.shutdown();
        }
    }

    private void commit(Mono<Void> mutation, Runnable beforeCommit) {
        transactions.transactional(mutation.then(Mono.fromRunnable(() -> {
            assertTrue(invalidations.isEmpty(), "提交前不能清理用户认证缓存");
            beforeCommit.run();
        }))).block();
        assertEquals(1, invalidations.size());
    }

    private Authentication authentication() {
        return authentications.getByUserId(USER).block();
    }

    private MenuGrantRequest request() {
        MenuView view = new MenuView();
        view.setId(menu.getId());
        view.setOwner(menu.getOwner());
        view.setGranted(true);
        MenuGrantRequest request = new MenuGrantRequest();
        request.setTargetType("role");
        request.setTargetId(ROLE);
        request.setMenus(List.of(view));
        return request;
    }
}
