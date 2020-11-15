package com.faforever.client.chat;

import com.faforever.client.audio.AudioService;
import com.faforever.client.fx.PlatformService;
import com.faforever.client.fx.WebViewConfigurer;
import com.faforever.client.i18n.I18n;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.player.Player;
import com.faforever.client.player.PlayerBuilder;
import com.faforever.client.player.PlayerService;
import com.faforever.client.preferences.Preferences;
import com.faforever.client.preferences.PreferencesService;
import com.faforever.client.reporting.ReportingService;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.theme.UiService;
import com.faforever.client.uploader.ImageUploadService;
import com.faforever.client.user.UserService;
import com.faforever.client.util.TimeService;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.MapChangeListener;
import javafx.collections.MapChangeListener.Change;
import javafx.collections.ObservableMap;
import javafx.scene.control.TabPane;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.testfx.util.WaitForAsyncUtils;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static com.faforever.client.player.SocialStatus.FOE;
import static com.faforever.client.player.SocialStatus.FRIEND;
import static com.faforever.client.player.SocialStatus.OTHER;
import static com.faforever.client.player.SocialStatus.SELF;
import static com.faforever.client.theme.UiService.CHAT_CONTAINER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class ChannelTabControllerTest extends AbstractPlainJavaFxTest {

  private static final String USER_NAME = "junit";
  private static final String CHANNEL_NAME = "#testChannel";

  private ChannelTabController instance;

  @Mock
  private ChatService chatService;
  @Mock
  private UserService userService;
  @Mock
  private PreferencesService preferencesService;
  @Mock
  private PlayerService playerService;
  @Mock
  private TimeService timeService;
  @Mock
  private ImageUploadService imageUploadService;
  @Mock
  private I18n i18n;
  @Mock
  private NotificationService notificationService;
  @Mock
  private UiService uiService;
  @Mock
  private UserFilterController userFilterController;
  @Mock
  private ChatUserItemController chatUserItemController;
  @Mock
  private ChatUserItemCategoryController chatUserItemCategoryController;
  @Mock
  private WebViewConfigurer webViewConfigurer;
  @Mock
  private AudioService audioService;
  @Mock
  private ReportingService reportingService;
  @Mock
  private EventBus eventBus;
  @Mock
  private CountryFlagService countryFlagService;
  @Mock
  private PlatformService platformService;
  private Preferences preferences;
  private Channel defaultChannel;

  @Before
  public void setUp() throws Exception {
    instance = new ChannelTabController(userService, chatService,
        preferencesService, playerService,
        audioService, timeService, i18n, imageUploadService,
        notificationService, reportingService,
        uiService, eventBus, webViewConfigurer, countryFlagService,
        platformService);

    defaultChannel = new Channel(CHANNEL_NAME);
    preferences = new Preferences();
    when(preferencesService.getPreferences()).thenReturn(this.preferences);
    when(userService.getUsername()).thenReturn(USER_NAME);
    when(uiService.loadFxml("theme/chat/user_filter.fxml")).thenReturn(userFilterController);
    when(uiService.loadFxml("theme/chat/chat_user_item.fxml")).thenReturn(chatUserItemController);
    when(uiService.loadFxml("theme/chat/chat_user_category.fxml")).thenReturn(chatUserItemCategoryController);
    when(userFilterController.getRoot()).thenReturn(new Pane());
    when(userFilterController.filterAppliedProperty()).thenReturn(new SimpleBooleanProperty(false));
    when(chatUserItemController.getRoot()).thenReturn(new Pane());
    when(uiService.getThemeFileUrl(CHAT_CONTAINER)).thenReturn(getClass().getResource("/theme/chat/chat_container.html"));

    loadFxml("theme/chat/channel_tab.fxml", clazz -> instance);

    TabPane tabPane = new TabPane();
    tabPane.getTabs().add(instance.getRoot());
    WaitForAsyncUtils.waitForAsyncFx(5000, () -> getRoot().getChildren().add(tabPane));
  }


  @Test
  public void testGetMessagesWebView() {
    assertNotNull(instance.getMessagesWebView());
  }

  @Test
  public void testGetMessageTextField() {
    assertNotNull(instance.messageTextField());
  }

  @Test
  public void testSetChannelName() {
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));
    verify(chatService).addUsersListener(eq(CHANNEL_NAME), any());
  }

  @Test
  public void testSetChannelTopic() {
    Channel channel = new Channel("name");
    channel.setTopic("topic https://example.com/1");

    runOnFxThreadAndWait(() -> instance.setChannel(channel));

    verify(chatService).addUsersListener(eq("name"), any());
    assertEquals(2, instance.topicText.getChildren().size());
  }

  @Test
  public void testGetMessageCssClassModerator() {
    String playerName = "junit";
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(playerName).defaultValues().moderator(true).get();

    when(playerService.getCurrentPlayer()).thenReturn(Optional.empty());
    when(chatService.getChatUser(playerName, defaultChannel.getName())).thenReturn(chatUser);

    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    assertEquals(ChannelTabController.CSS_CLASS_MODERATOR, instance.getMessageCssClass(playerName));
  }

  @Test
  public void onSearchFieldCloseTest() {
    instance.onSearchFieldClose();
    assertFalse(instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
  }

  @Test
  public void onKeyReleasedTestEscape() {
    KeyEvent keyEvent = new KeyEvent(null, null, null, null, null, KeyCode.ESCAPE, false, false, false, false);

    assertFalse(instance.searchField.isVisible());
    runOnFxThreadAndWait(() -> instance.onKeyReleased(keyEvent));
    assertFalse(instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
  }

  @Test
  public void onKeyReleasedTestCtrlF() {
    KeyEvent keyEvent = new KeyEvent(null, null, null, null, null, KeyCode.F, false, true, false, false);

    assertFalse(instance.searchField.isVisible());
    runOnFxThreadAndWait(() -> instance.onKeyReleased(keyEvent));
    assertTrue(instance.searchField.isVisible());
    assertEquals("", instance.searchField.getText());
    runOnFxThreadAndWait(() -> instance.onKeyReleased(keyEvent));
    assertFalse(instance.searchField.isVisible());
  }

  @Test
  @SuppressWarnings("unchecked")
  public void testOnUserJoinsChannel() {
    String username1 = "player";
    Player player = PlayerBuilder.create(username1).socialStatus(OTHER).get();
    ChatChannelUser userInList = ChatChannelUserBuilder.create(username1).get();
    when(playerService.getPlayerForUsername(username1)).thenReturn(Optional.of(player));

    String username2 = "player joins";
    Player newPlayer = PlayerBuilder.create(username2).socialStatus(OTHER).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(username2).get();
    ObservableMap<String, ChatChannelUser> userMap = FXCollections.observableHashMap();
    userMap.put(username2, chatUser);

    Change<String, ChatChannelUser> change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(chatUser);
    when(change.getMap()).thenReturn(userMap);
    when(playerService.getPlayerForUsername(username2)).thenReturn(Optional.of(newPlayer));
    when(i18n.get("chat.userCount", 1)).thenReturn("2 Players");

    defaultChannel.addUser(userInList);
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    ArgumentCaptor<MapChangeListener<String, ChatChannelUser>> captor = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(chatService).addUsersListener(anyString(), captor.capture());

    assertEquals(1, instance.getChatUserItemsByCategory(ChatUserCategory.OTHER).size());

    runOnFxThreadAndWait(() -> captor.getValue().onChanged(change));

    assertEquals("2 Players", instance.userSearchTextField.getPromptText());
    assertEquals(2, instance.getChatUserItemsByCategory(ChatUserCategory.OTHER).size());
  }

  @Test
  public void testOnUserLeaveFromChannel() {
    String username1 = "player";
    Player player = PlayerBuilder.create(username1).socialStatus(OTHER).get();
    ChatChannelUser userInList = ChatChannelUserBuilder.create(username1).get();
    when(playerService.getPlayerForUsername(username1)).thenReturn(Optional.of(player));

    String username2 = "leaving player";
    Player leavingPlayer = PlayerBuilder.create(username2).socialStatus(OTHER).get();
    ChatChannelUser chatUser = ChatChannelUserBuilder.create(username2).get();
    ObservableMap<String, ChatChannelUser> userMap = FXCollections.observableHashMap();
    userMap.put(username2, chatUser);

    Change<String, ChatChannelUser> change = mock(Change.class);
    when(change.wasRemoved()).thenReturn(true);
    when(change.getValueRemoved()).thenReturn(chatUser);
    when(change.getMap()).thenReturn(userMap);
    when(playerService.getPlayerForUsername(username2)).thenReturn(Optional.of(leavingPlayer));
    when(i18n.get("chat.userCount", 1)).thenReturn("1 Players");

    defaultChannel.addUsers(Arrays.asList(userInList, chatUser));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    ArgumentCaptor<MapChangeListener<String, ChatChannelUser>> captor = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(chatService).addUsersListener(anyString(), captor.capture());

    assertEquals(2, instance.getChatUserItemsByCategory(ChatUserCategory.OTHER).size());

    runOnFxThreadAndWait(() -> captor.getValue().onChanged(change));

    assertEquals("1 Players", instance.userSearchTextField.getPromptText());
    assertEquals(1, instance.getChatUserItemsByCategory(ChatUserCategory.OTHER).size());
  }

  @Test
  public void testAtMentionTriggersNotification() {
    this.getRoot().setVisible(false);
    preferencesService.getPreferences().getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello @" + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testAtMentionTriggersNotificationWhenFlagIsEnabled() {
    this.getRoot().setVisible(false);
    preferencesService.getPreferences().getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello @" + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testNormalMentionTriggersNotification() {
    this.getRoot().setVisible(false);
    preferencesService.getPreferences().getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello " + USER_NAME + "!!"));
    verify(audioService).playChatMentionSound();
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationWhenFlagIsEnabled() {
    this.getRoot().setVisible(false);
    preferencesService.getPreferences().getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(true);
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello " + USER_NAME + "!!"));
    verify(audioService, never()).playChatMentionSound();
  }

  @Test
  public void testNormalMentionDoesNotTriggerNotificationFromFoe() {
    this.getRoot().setVisible(false);
    preferencesService.getPreferences().getNotification().notifyOnAtMentionOnlyEnabledProperty().setValue(false);
    when(playerService.getPlayerForUsername("junit")).thenReturn(Optional.ofNullable(PlayerBuilder.create("junit").defaultValues().socialStatus(FOE).get()));
    instance.onMention(new ChatMessage("junit", Instant.now(), "junit", "hello " + USER_NAME + "!!"));
    verify(audioService, never()).playChatMentionSound();
  }

  @Test
  public void getInlineStyleRandom() {
    String somePlayer = "somePlayer";
    Color color = ColorGeneratorUtil.generateRandomColor();
    ChatChannelUser chatUser = new ChatChannelUser(somePlayer, color, false);

    when(chatService.getChatUser(somePlayer, CHANNEL_NAME)).thenReturn(chatUser);
    runOnFxThreadAndWait(() -> {
      instance.setChannel(defaultChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });

    String expected = instance.createInlineStyleFromColor(color);
    String result = instance.getInlineStyle(somePlayer);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleCustom() {
    Color color = ColorGeneratorUtil.generateRandomColor();
    String colorStyle = instance.createInlineStyleFromColor(color);
    String username = "somePlayer";
    ChatChannelUser chatUser = new ChatChannelUser(username, color, false);

    when(chatService.getChatUser(username, CHANNEL_NAME)).thenReturn(chatUser);
    runOnFxThreadAndWait(() -> {
      instance.setChannel(defaultChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.CUSTOM);
      preferences.getChat().setHideFoeMessages(false);
    });

    String expected = String.format("%s%s", colorStyle, "");
    String result = instance.getInlineStyle(username);
    assertEquals(expected, result);
  }

  @Test
  public void getInlineStyleRandomFoeHide() {
    String playerName = "playerName";
    ChatChannelUser chatUser = new ChatChannelUser(playerName, null, false);

    when(playerService.getPlayerForUsername(playerName)).thenReturn(Optional.of(PlayerBuilder.create(playerName).socialStatus(FOE).get()));
    when(chatService.getChatUser(playerName, CHANNEL_NAME)).thenReturn(chatUser);
    runOnFxThreadAndWait(() -> {
      instance.setChannel(defaultChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(true);
    });

    String result = instance.getInlineStyle(playerName);
    assertEquals("display: none;", result);
  }

  @Test
  public void getInlineStyleRandomFoeShow() {
    String playerName = "somePlayer";
    ChatChannelUser chatUser = new ChatChannelUser(playerName, null, false);
    when(playerService.getPlayerForUsername(playerName)).thenReturn(Optional.of(PlayerBuilder.create(playerName).socialStatus(FOE).get()));

    when(chatService.getChatUser(playerName, CHANNEL_NAME)).thenReturn(chatUser);
    runOnFxThreadAndWait(() -> {
      instance.setChannel(defaultChannel);
      preferences.getChat().setChatColorMode(ChatColorMode.RANDOM);
      preferences.getChat().setHideFoeMessages(false);
    });

    String result = instance.getInlineStyle(playerName);
    assertEquals("", result);
  }

  @Test
  public void testUserIsRemovedFromCategoriesToUserListItems() {
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    ArgumentCaptor<MapChangeListener<String, ChatChannelUser>> captor = ArgumentCaptor.forClass(MapChangeListener.class);
    verify(chatService).addUsersListener(anyString(), captor.capture());

    ChatChannelUser chatUser = new ChatChannelUser("junit", null, false);
    ObservableMap<String, ChatChannelUser> userMap = FXCollections.observableHashMap();
    userMap.put("junit", chatUser);

    Change<String, ChatChannelUser> change = mock(Change.class);
    when(change.wasAdded()).thenReturn(true);
    when(change.getValueAdded()).thenReturn(chatUser);
    when(change.getMap()).thenReturn(userMap);

    when(i18n.get("chat.userCount", 1)).thenReturn("1 Players");

    runOnFxThreadAndWait(() -> captor.getValue().onChanged(change));

    assertEquals("1 Players", instance.userSearchTextField.getPromptText());

    userMap.remove("junit");

    Change<String, ChatChannelUser> changeUserLeft = mock(Change.class);
    when(changeUserLeft.wasAdded()).thenReturn(false);
    when(changeUserLeft.wasRemoved()).thenReturn(true);
    when(changeUserLeft.getValueRemoved()).thenReturn(chatUser);
    when(changeUserLeft.getMap()).thenReturn(userMap);

    runOnFxThreadAndWait(() -> captor.getValue().onChanged(changeUserLeft));

    boolean userStillListedInCategoryMap = instance.categoriesToUserListItems.entrySet().stream()
        .anyMatch(chatUserCategoryListEntry -> chatUserCategoryListEntry.getValue().stream()
            .anyMatch(categoryOrChatUserListItem -> categoryOrChatUserListItem.getUser().equals(chatUser))
        );

    assertFalse(userStillListedInCategoryMap);
  }

  @Test
  public void testChannelTopicUpdate() {
    defaultChannel.setTopic("topc1: https://faforever.com");
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    assertEquals(2, instance.topicText.getChildren().size());
    runOnFxThreadAndWait(() -> defaultChannel.setTopic("topic2: https://faforever.com topic3: https://faforever.com/example"));
    WaitForAsyncUtils.waitForFxEvents();
    assertEquals(4, instance.topicText.getChildren().size());
  }

  @Test
  public void testChannelListHide() {
    runOnFxThreadAndWait(() -> instance.toggleSidePaneButton.fire());

    assertFalse(instance.channelTabScrollPaneVBox.isManaged());
    assertFalse(instance.toggleSidePaneButton.isSelected());
  }

  @Test
  public void testHideSidePane() {
    runOnFxThreadAndWait(() -> instance.toggleSidePaneButton.fire());

    assertFalse(preferencesService.getPreferences().getChat().isPlayerListShown());
    verify(preferencesService, atLeast(1)).storeInBackground();

    assertFalse(instance.channelTabScrollPaneVBox.isManaged());
    assertFalse(instance.channelTabScrollPaneVBox.isVisible());
  }

  @Test
  public void testModeratorIsInModeratorsList() {
    String username = "moderator";
    Player player = PlayerBuilder.create(username).socialStatus(OTHER).get();
    ChatChannelUser moderator = ChatChannelUserBuilder.create(username).moderator(true).get();
    defaultChannel.addUser(moderator);

    when(playerService.getPlayerForUsername(username)).thenReturn(Optional.of(player));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.MODERATOR);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(moderator)));
    assertEquals(1, users.size());
  }

  @Test
  public void testFriendlyPlayerIsInFriendList() {
    String username = "friend";
    Player player = PlayerBuilder.create(username).socialStatus(FRIEND).get();
    ChatChannelUser friendUser = ChatChannelUserBuilder.create(username).get();
    defaultChannel.addUser(friendUser);

    when(playerService.getPlayerForUsername(username)).thenReturn(Optional.of(player));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.FRIEND);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(friendUser)));
    assertEquals(1, users.size());
  }

  @Test
  public void testEnemyPlayerIsInFoeList() {
    String username = "foe";
    Player player = PlayerBuilder.create(username).socialStatus(FOE).get();
    ChatChannelUser enemyUser = ChatChannelUserBuilder.create(username).get();
    defaultChannel.addUser(enemyUser);

    when(playerService.getPlayerForUsername(username)).thenReturn(Optional.of(player));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.FOE);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(enemyUser)));
    assertEquals(1, users.size());
  }

  @Test
  public void testOtherPlayersIsInOtherList() {
    String username1 = "player1";
    Player player1 = PlayerBuilder.create(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).get();
    String username2 = "player2";
    Player player2 = PlayerBuilder.create(username2).socialStatus(SELF).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).get();
    defaultChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerForUsername(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerForUsername(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(user2)));
    assertEquals(2, users.size());
  }

  @Test
  public void testPlayerIsInOnlyChatList() {
    ChatChannelUser user = ChatChannelUserBuilder.create("player").player(null).get();
    defaultChannel.addUser(user);

    when(playerService.getPlayerForUsername(user.getUsername())).thenReturn(Optional.empty());
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    List<CategoryOrChatUserListItem> users = instance.getChatUserItemsByCategory(ChatUserCategory.CHAT_ONLY);
    assertTrue(users.stream().anyMatch(userItem -> userItem.getUser().equals(user)));
    assertEquals(1, users.size());
  }

  @Test
  public void testPlayerBecomesFriendly() {
    String username1 = "player1";
    Player player1 = PlayerBuilder.create(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).get();
    String username2 = "player2";
    Player player2 = PlayerBuilder.create(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).get();
    defaultChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerForUsername(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerForUsername(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    runOnFxThreadAndWait(() -> player1.setSocialStatus(FRIEND));

    List<CategoryOrChatUserListItem> friends = instance.getChatUserItemsByCategory(ChatUserCategory.FRIEND);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(friends.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user2)));
    assertEquals(1, friends.size());
    assertEquals(1, otherUsers.size());
  }

  @Test
  public void testPlayerBecomesEnemy() {
    String username1 = "player1";
    Player player1 = PlayerBuilder.create(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).get();
    String username2 = "player2";
    Player player2 = PlayerBuilder.create(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).get();
    defaultChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerForUsername(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerForUsername(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    runOnFxThreadAndWait(() -> player1.setSocialStatus(FOE));

    List<CategoryOrChatUserListItem> enemies = instance.getChatUserItemsByCategory(ChatUserCategory.FOE);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(enemies.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user2)));
    assertEquals(1, enemies.size());
    assertEquals(1, otherUsers.size());
  }

  @Test
  public void testFriendlyPlayerBecomesOther() {
    String username1 = "player1";
    Player player1 = PlayerBuilder.create(username1).socialStatus(FRIEND).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).get();
    String username2 = "player2";
    Player player2 = PlayerBuilder.create(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).get();
    defaultChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerForUsername(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerForUsername(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    runOnFxThreadAndWait(() -> player1.setSocialStatus(OTHER));

    List<CategoryOrChatUserListItem> friends = instance.getChatUserItemsByCategory(ChatUserCategory.FRIEND);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertEquals(0, friends.size());
    assertEquals(2, otherUsers.size());
  }

  @Test
  public void testEnemyPlayerBecomesOther() {
    String username1 = "player1";
    Player player1 = PlayerBuilder.create(username1).socialStatus(FOE).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).get();
    String username2 = "player2";
    Player player2 = PlayerBuilder.create(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).get();
    defaultChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerForUsername(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerForUsername(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    runOnFxThreadAndWait(() -> player1.setSocialStatus(OTHER));

    List<CategoryOrChatUserListItem> enemies = instance.getChatUserItemsByCategory(ChatUserCategory.FOE);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertEquals(0, enemies.size());
    assertEquals(2, otherUsers.size());
  }

  @Test
  public void testWhenUserAddPlayerToFriendlyListByAccidentallyAndCancelsActionImmediately() {
    String username1 = "player1";
    Player player1 = PlayerBuilder.create(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).get();
    String username2 = "player2";
    Player player2 = PlayerBuilder.create(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).get();
    defaultChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerForUsername(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerForUsername(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    runOnFxThreadAndWait(() -> player1.setSocialStatus(FRIEND));
    runOnFxThreadAndWait(() -> player1.setSocialStatus(OTHER));

    List<CategoryOrChatUserListItem> friends = instance.getChatUserItemsByCategory(ChatUserCategory.FRIEND);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertEquals(0, friends.size());
    assertEquals(2, otherUsers.size());
  }

  @Test
  public void testWhenUserAddPlayerToFoeListByAccidentallyAndCancelsActionImmediately() {
    String username1 = "player1";
    Player player1 = PlayerBuilder.create(username1).socialStatus(OTHER).get();
    ChatChannelUser user1 = ChatChannelUserBuilder.create(username1).get();
    String username2 = "player2";
    Player player2 = PlayerBuilder.create(username2).socialStatus(OTHER).get();
    ChatChannelUser user2 = ChatChannelUserBuilder.create(username2).get();
    defaultChannel.addUsers(Arrays.asList(user1, user2));

    when(playerService.getPlayerForUsername(username1)).thenReturn(Optional.of(player1));
    when(playerService.getPlayerForUsername(username2)).thenReturn(Optional.of(player2));
    runOnFxThreadAndWait(() -> instance.setChannel(defaultChannel));

    runOnFxThreadAndWait(() -> player1.setSocialStatus(FOE));
    runOnFxThreadAndWait(() -> player1.setSocialStatus(OTHER));

    List<CategoryOrChatUserListItem> enemies = instance.getChatUserItemsByCategory(ChatUserCategory.FOE);
    List<CategoryOrChatUserListItem> otherUsers = instance.getChatUserItemsByCategory(ChatUserCategory.OTHER);
    assertTrue(otherUsers.stream().anyMatch(userItem -> userItem.getUser().equals(user1)));
    assertEquals(0, enemies.size());
    assertEquals(2, otherUsers.size());
  }
}
