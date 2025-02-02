package com.faforever.client.replay;

import com.faforever.client.config.ClientProperties;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.game.GameService;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.i18n.I18n;
import com.faforever.client.map.MapBeanBuilder;
import com.faforever.client.map.MapService;
import com.faforever.client.map.generator.MapGeneratorService;
import com.faforever.client.mod.ModService;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.remote.FafService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.task.TaskService;
import com.faforever.client.test.FakeTestException;
import com.faforever.client.user.UserService;
import com.faforever.client.util.Tuple;
import com.faforever.client.vault.search.SearchController.SortConfig;
import com.faforever.client.vault.search.SearchController.SortOrder;
import com.faforever.commons.replay.ReplayDataParser;
import com.faforever.commons.replay.ReplayMetadata;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.ApplicationContext;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class ReplayServiceTest {
  /**
   * First 64 bytes of a SCFAReplay file with version 3599. ASCII representation:
   * <pre>
   * Supreme Commande
   * r v1.50.3599....
   * Replay v1.9../ma
   * ps/forbidden pas
   * s.v0001/forbidde
   * n pass.scmap....
   * </pre>
   */
  private static final byte[] REPLAY_FIRST_BYTES = new byte[]{
      0x53, 0x75, 0x70, 0x72, 0x65, 0x6D, 0x65, 0x20, 0x43, 0x6F, 0x6D, 0x6D, 0x61, 0x6E, 0x64, 0x65,
      0x72, 0x20, 0x76, 0x31, 0x2E, 0x35, 0x30, 0x2E, 0x33, 0x35, 0x39, 0x39, 0x00, 0x0D, 0x0A, 0x00,
      0x52, 0x65, 0x70, 0x6C, 0x61, 0x79, 0x20, 0x76, 0x31, 0x2E, 0x39, 0x0D, 0x0A, 0x2F, 0x6D, 0x61,
      0x70, 0x73, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65, 0x6E, 0x20, 0x70, 0x61, 0x73,
      0x73, 0x2E, 0x76, 0x30, 0x30, 0x30, 0x31, 0x2F, 0x66, 0x6F, 0x72, 0x62, 0x69, 0x64, 0x64, 0x65,
      0x6E, 0x20, 0x70, 0x61, 0x73, 0x73, 0x2E, 0x73, 0x63, 0x6D, 0x61, 0x70, 0x00, 0x0D, 0x0A, 0x1A
  };
  private static final String TEST_VERSION_STRING = "Supreme Commander v1.50.3599";
  private static final String TEST_MAP_PATH = "/maps/forbidden_pass.v0001/forbidden_pass_scenario.lua";
  private static final String TEST_MAP_NAME = "forbidden_pass.v0001";
  private static final String COOP_MAP_PATH = "/maps/scca_coop_r02.v0015/scca_coop_r02_scenario.lua";
  private static final String COOP_MAP_NAME = "scca_coop_r02.v0015";
  private static final String BAD_MAP_PATH = "/maps/forbidden_?pass.v0001/forbidden_pass_?scenario.lua";
  private static final String TEST_MAP_PATH_GENERATED = "/maps/neroxis_map_generator_1.0.0_ABcd/neroxis_map_generator_1.0.0_ABcd_scenario.lua";
  private static final String TEST_MAP_NAME_GENERATED = "neroxis_map_generator_1.0.0_ABcd";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();
  @Rule
  public TemporaryFolder replayDirectory = new TemporaryFolder();
  @Rule
  public TemporaryFolder cacheDirectory = new TemporaryFolder();
  private ReplayService instance;
  @Mock
  private I18n i18n;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private ReplayFileReader replayFileReader;
  @Mock
  private NotificationService notificationService;
  @Mock
  private ApplicationContext applicationContext;
  @Mock
  private TaskService taskService;
  @Mock
  private GameService gameService;
  @Mock
  private PlayerService playerService;
  @Mock
  private FafService fafService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private PlatformService platformService;
  @Mock
  private ModService modService;
  @Mock
  private MapService mapService;
  @Mock
  private EventBus publisher;
  @Mock
  private MapGeneratorService mapGeneratorService;
  @Mock
  private ExecutorService executorService;
  @Mock
  private UserService userService;
  @Mock
  private ReplayDataParser replayDataParser;

  @Before
  public void setUp() throws Exception {
    MockitoAnnotations.initMocks(this);

    instance = new ReplayService(new ClientProperties(), preferencesService, userService, replayFileReader, notificationService, gameService, playerService,
        taskService, i18n, reportingService, applicationContext, platformService, fafService, modService, mapService, publisher);

    ReplayMetadata replayMetadata = new ReplayMetadata();
    replayMetadata.setUid(123);
    replayMetadata.setSimMods(emptyMap());
    replayMetadata.setFeaturedModVersions(emptyMap());
    replayMetadata.setFeaturedMod("faf");
    replayMetadata.setMapname(TEST_MAP_NAME);

    when(replayFileReader.parseReplay(any())).thenReturn(replayDataParser);
    when(replayDataParser.getMetadata()).thenReturn(replayMetadata);
    when(replayDataParser.getData()).thenReturn(REPLAY_FIRST_BYTES);
    when(replayDataParser.getChatMessages()).thenReturn(List.of());
    when(replayDataParser.getGameOptions()).thenReturn(List.of());
    when(replayDataParser.getMods()).thenReturn(Map.of());
    when(replayDataParser.getMap()).thenReturn(TEST_MAP_PATH);
    when(replayDataParser.getReplayPatchFieldId()).thenReturn(TEST_VERSION_STRING);
    when(preferencesService.getReplaysDirectory()).thenReturn(replayDirectory.getRoot().toPath());
    when(preferencesService.getCorruptedReplaysDirectory()).thenReturn(replayDirectory.getRoot().toPath().resolve("corrupt"));
    when(preferencesService.getCacheDirectory()).thenReturn(cacheDirectory.getRoot().toPath());
    doAnswer(invocation -> invocation.getArgument(0)).when(taskService).submitTask(any());
  }

  @Test
  public void testParseSupComVersion() throws Exception {
    when(replayDataParser.getReplayPatchFieldId()).thenReturn(TEST_VERSION_STRING);
    Integer version = ReplayService.parseSupComVersion(replayDataParser);

    assertEquals((Integer) 3599, version);
  }

  @Test
  public void testParseMapFolderName() throws Exception {
    when(replayDataParser.getMap()).thenReturn(COOP_MAP_PATH);
    String mapName = ReplayService.parseMapFolderName(replayDataParser);
    assertEquals(COOP_MAP_NAME, mapName);
  }

  @Test
  public void testParseBadFolderNameThrowsException() throws Exception {
    expectedException.expect(IllegalArgumentException.class);
    when(replayDataParser.getMap()).thenReturn(BAD_MAP_PATH);
    ReplayService.parseMapFolderName(replayDataParser);
  }

  @Test
  public void testGuessModByFileNameModIsMissing() throws Exception {
    String mod = ReplayService.guessModByFileName("110621-2128 Saltrock Colony.SCFAReplay");

    assertEquals(KnownFeaturedMod.DEFAULT.getTechnicalName(), mod);
  }

  @Test
  public void testGuessModByFileNameModIsBlackops() throws Exception {
    String mod = ReplayService.guessModByFileName("110621-2128 Saltrock Colony.blackops.SCFAReplay");

    assertEquals("blackops", mod);
  }

  @Test
  public void testGetLocalReplaysMovesCorruptFiles() throws Exception {
    Path file1 = replayDirectory.newFile("replay.fafreplay").toPath();
    Path file2 = replayDirectory.newFile("replay2.fafreplay").toPath();

    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(file1);
    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(file2);

    Collection<Replay> localReplays = new ArrayList<>();
    try {
      localReplays.addAll(instance.loadLocalReplayPage(2, 1).get().getFirst());
    } catch (FakeTestException exception) {
      // expected
    }

    assertThat(localReplays, empty());
    verify(notificationService, times(2)).addNotification(any(PersistentNotification.class));

    assertThat(Files.exists(file1), is(false));
    assertThat(Files.exists(file2), is(false));
  }

  @Test
  public void testLoadLocalReplays() throws Exception {
    Path file1 = replayDirectory.newFile("replay.fafreplay").toPath();

    ReplayMetadata replayMetadata = new ReplayMetadata();
    replayMetadata.setUid(123);
    replayMetadata.setTitle("title");

    when(replayDataParser.getMetadata()).thenReturn(replayMetadata);
    when(replayFileReader.parseReplay(file1)).thenReturn(replayDataParser);
    when(modService.getFeaturedMod(any())).thenReturn(CompletableFuture.completedFuture(null));
    when(mapService.findByMapFolderName(any())).thenReturn(CompletableFuture.completedFuture(Optional.of(MapBeanBuilder.create().defaultValues().get())));

    Collection<Replay> localReplays = instance.loadLocalReplayPage(1, 1).get().getFirst();

    assertThat(localReplays, hasSize(1));
    assertThat(localReplays.iterator().next().getId(), is(123));
    assertThat(localReplays.iterator().next().getTitle(), is("title"));
  }

  @Test
  public void testRunFafReplayFile() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    Replay replay = new Replay();
    replay.setReplayFile(replayFile);

    instance.runReplay(replay);

    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunFafReplayFileGeneratedMap() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    Replay replay = new Replay();
    replay.setReplayFile(replayFile);

    when(replayDataParser.getMap()).thenReturn(TEST_MAP_PATH_GENERATED);
    when(mapGeneratorService.isGeneratedMap(TEST_MAP_NAME_GENERATED)).thenReturn(true);

    instance.runReplay(replay);

    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME_GENERATED));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaReplayFile() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();

    Replay replay = new Replay();
    replay.setReplayFile(replayFile);

    when(replayFileReader.parseReplay(replayFile)).thenReturn(replayDataParser);

    instance.runReplay(replay);

    verify(gameService).runWithReplay(any(), eq(null), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();

    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(replayFile);

    Replay replay = new Replay();
    replay.setReplayFile(replayFile);

    instance.runReplay(replay);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testRunFafReplayFileExceptionTriggersNotification() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(replayFile);

    Replay replay = new Replay();
    replay.setReplayFile(replayFile);

    instance.runReplay(replay);
    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString());
  }

  @Test
  public void testOwnReplays() throws Exception {
    ArgumentCaptor<String> queryCatcher = ArgumentCaptor.forClass(String.class);
    ArgumentCaptor<Integer> pageSizeCatcher = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<Integer> pageCatcher = ArgumentCaptor.forClass(Integer.class);
    ArgumentCaptor<SortConfig> sortCatcher = ArgumentCaptor.forClass(SortConfig.class);
    when(userService.getUserId()).thenReturn(47);
    when(fafService.findReplaysByQueryWithPageCount(queryCatcher.capture(), pageSizeCatcher.capture(), pageCatcher.capture(), sortCatcher.capture())).thenReturn(CompletableFuture.completedFuture(null));
    CompletableFuture<Tuple<List<Replay>, Integer>> ownReplays = instance.getOwnReplaysWithPageCount(100, 1);
    ownReplays.get();
    assertEquals("playerStats.player.id==\"47\"", queryCatcher.getValue());
    assertEquals(100, (int) pageSizeCatcher.getValue());
    assertEquals(1, (int) pageCatcher.getValue());
    assertEquals(SortOrder.DESC, sortCatcher.getValue().getSortOrder());
    assertEquals("startTime", sortCatcher.getValue().getSortProperty());
  }

  @Test
  public void testRunFafOnlineReplay() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.fafreplay").toPath();

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));
    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    Replay replay = new Replay();

    ReplayMetadata replayMetadata = new ReplayMetadata();
    replayMetadata.setUid(123);
    replayMetadata.setSimMods(emptyMap());
    replayMetadata.setFeaturedModVersions(emptyMap());
    replayMetadata.setFeaturedMod("faf");
    replayMetadata.setMapname(TEST_MAP_NAME);

    when(replayFileReader.parseReplay(replayFile)).thenReturn(replayDataParser);
    when(replayDataParser.getMetadata()).thenReturn(replayMetadata);

    instance.runReplay(replay);

    verify(taskService).submitTask(replayDownloadTask);
    verify(gameService).runWithReplay(any(), eq(123), eq("faf"), eq(3599), eq(emptyMap()), eq(emptySet()), eq(TEST_MAP_NAME));
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplay() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));

    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    Replay replay = new Replay();

    when(replayFileReader.parseReplay(replayFile)).thenReturn(replayDataParser);

    instance.runReplay(replay);

    verify(taskService).submitTask(replayDownloadTask);
    verify(gameService).runWithReplay(replayFile, null, "faf", 3599, emptyMap(), emptySet(), TEST_MAP_NAME);
    verifyZeroInteractions(notificationService);
  }

  @Test
  public void testRunScFaOnlineReplayExceptionTriggersNotification() throws Exception {
    Path replayFile = replayDirectory.newFile("replay.scfareplay").toPath();
    doThrow(new FakeTestException()).when(replayFileReader).parseReplay(replayFile);

    ReplayDownloadTask replayDownloadTask = mock(ReplayDownloadTask.class);
    when(replayDownloadTask.getFuture()).thenReturn(CompletableFuture.completedFuture(replayFile));
    when(applicationContext.getBean(ReplayDownloadTask.class)).thenReturn(replayDownloadTask);
    Replay replay = new Replay();

    instance.runReplay(replay);

    verify(notificationService).addImmediateErrorNotification(any(Throwable.class), anyString(), anyInt());
    verifyNoMoreInteractions(gameService);
  }

  @Test
  public void testRunLiveReplay() throws Exception {
    when(gameService.runWithLiveReplay(any(URI.class), anyInt(), anyString(), anyString()))
        .thenReturn(CompletableFuture.completedFuture(null));

    instance.runLiveReplay(new URI("faflive://example.com/123/456.scfareplay?mod=faf&map=map%20name"));

    verify(gameService).runWithLiveReplay(new URI("gpgnet://example.com/123/456.scfareplay"), 123, "faf", "map name");
  }

  @Test
  public void testEnrich() throws Exception {
    Path path = Paths.get("foo.bar");
    when(replayFileReader.parseReplay(path)).thenReturn(replayDataParser);

    instance.enrich(new Replay(), path);

    verify(replayDataParser).getChatMessages();
    verify(replayDataParser).getGameOptions();
  }
}
