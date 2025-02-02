package com.faforever.client.remote;

import com.faforever.client.FafClientApplication;
import com.faforever.client.fa.relay.GpgGameMessage;
import com.faforever.client.game.KnownFeaturedMod;
import com.faforever.client.game.NewGameInfo;
import com.faforever.client.i18n.I18n;
import com.faforever.client.net.ConnectionState;
import com.faforever.client.notification.Action;
import com.faforever.client.notification.NotificationService;
import com.faforever.client.notification.PersistentNotification;
import com.faforever.client.notification.Severity;
import com.faforever.client.remote.domain.Avatar;
import com.faforever.client.remote.domain.GameAccess;
import com.faforever.client.remote.domain.GameInfoMessage;
import com.faforever.client.remote.domain.GameLaunchMessage;
import com.faforever.client.remote.domain.GameStatus;
import com.faforever.client.remote.domain.IceServersServerMessage.IceServer;
import com.faforever.client.remote.domain.LoginMessage;
import com.faforever.client.remote.domain.MatchmakingState;
import com.faforever.client.remote.domain.PeriodType;
import com.faforever.client.remote.domain.Player;
import com.faforever.client.remote.domain.PlayersMessage;
import com.faforever.client.remote.domain.ServerMessage;
import com.faforever.client.task.CompletableTask;
import com.faforever.client.task.TaskService;
import com.faforever.client.teammatchmaking.MatchmakingQueue;
import com.faforever.client.user.event.LoginSuccessEvent;
import com.faforever.commons.api.dto.Faction;
import com.google.common.eventbus.EventBus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.ReadOnlyObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

import static com.faforever.client.remote.domain.GameAccess.PASSWORD;
import static com.faforever.client.remote.domain.GameAccess.PUBLIC;
import static com.faforever.client.task.CompletableTask.Priority.HIGH;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

@Lazy
@Component
@Profile(FafClientApplication.PROFILE_OFFLINE)
@RequiredArgsConstructor
@Slf4j
// NOSONAR
public class MockFafServerAccessor implements FafServerAccessor {

  private static final String USER_NAME = "MockUser";
  private final Timer timer = new Timer("LobbyServerAccessorTimer", true);
  private final HashMap<Class<? extends ServerMessage>, Collection<Consumer<ServerMessage>>> messageListeners = new HashMap<>();

  private final TaskService taskService;
  private final NotificationService notificationService;
  private final I18n i18n;
  private final EventBus eventBus;

  private final ObjectProperty<ConnectionState> connectionState = new SimpleObjectProperty<>(ConnectionState.DISCONNECTED);

  @Override
  @SuppressWarnings("unchecked")
  public <T extends ServerMessage> void addOnMessageListener(Class<T> type, Consumer<T> listener) {
    if (!messageListeners.containsKey(type)) {
      messageListeners.put(type, new LinkedList<>());
    }
    messageListeners.get(type).add((Consumer<ServerMessage>) listener);
  }

  @Override
  public <T extends ServerMessage> void removeOnMessageListener(Class<T> type, Consumer<T> listener) {
    messageListeners.get(type).remove(listener);
  }

  @Override
  public ReadOnlyObjectProperty<ConnectionState> connectionStateProperty() {
    return connectionState;
  }

  @Override
  public CompletableFuture<LoginMessage> connectAndLogIn(String username, String password) {
    return taskService.submitTask(new CompletableTask<LoginMessage>(HIGH) {
      @Override
      protected LoginMessage call() throws Exception {
        updateTitle(i18n.get("login.progress.message"));

        Player player = new Player();
        player.setId(4812);
        player.setLogin(USER_NAME);
        player.setClan("ABC");
        player.setCountry("A1");
        player.setNumberOfGames(330);

        PlayersMessage playersMessage = new PlayersMessage();
        playersMessage.setPlayers(singletonList(player));

        eventBus.post(new LoginSuccessEvent(username, password, player.getId()));

        messageListeners.getOrDefault(playersMessage.getClass(), Collections.emptyList()).forEach(consumer -> consumer.accept(playersMessage));

        timer.schedule(new TimerTask() {
          @Override
          public void run() {
            UpdatedAchievementsMessage updatedAchievementsMessage = new UpdatedAchievementsMessage();
            UpdatedAchievement updatedAchievement = new UpdatedAchievement();
            updatedAchievement.setAchievementId("50260d04-90ff-45c8-816b-4ad8d7b97ecd");
            updatedAchievement.setNewlyUnlocked(true);
            updatedAchievementsMessage.setUpdatedAchievements(Arrays.asList(updatedAchievement));

            messageListeners.getOrDefault(updatedAchievementsMessage.getClass(), Collections.emptyList()).forEach(consumer -> consumer.accept(updatedAchievementsMessage));
          }
        }, 7000);

        List<GameInfoMessage> gameInfoMessages = Arrays.asList(
            createGameInfo(1, "Mock game 500 - 800", PUBLIC, "faf", "scmp_010", 1, 6, "Mock user"),
            createGameInfo(2, "Mock game 500+", PUBLIC, "faf", "scmp_011", 2, 6, "Mock user"),
            createGameInfo(3, "Mock game +500", PUBLIC, "faf", "scmp_012", 3, 6, "Mock user"),
            createGameInfo(4, "Mock game <1000", PUBLIC, "faf", "scmp_013", 4, 6, "Mock user"),
            createGameInfo(5, "Mock game >1000", PUBLIC, "faf", "scmp_014", 5, 6, "Mock user"),
            createGameInfo(6, "Mock game ~600", PASSWORD, "faf", "scmp_015", 6, 6, "Mock user"),
            createGameInfo(7, "Mock game 7", PASSWORD, "faf", "scmp_016", 7, 6, "Mock user")
        );

        gameInfoMessages.forEach(gameInfoMessage ->
            messageListeners.getOrDefault(gameInfoMessage.getClass(), Collections.emptyList())
                .forEach(consumer -> consumer.accept(gameInfoMessage)));

        notificationService.addNotification(
            new PersistentNotification(
                "How about a long-running (7s) mock task?",
                Severity.INFO,
                Arrays.asList(
                    new Action("Execute", event ->
                        taskService.submitTask(new CompletableTask<Void>(HIGH) {
                          @Override
                          protected Void call() throws Exception {
                            updateTitle("Mock task");
                            Thread.sleep(2000);
                            for (int i = 0; i < 5; i++) {
                              updateProgress(i, 5);
                              Thread.sleep(1000);
                            }
                            return null;
                          }
                        })),
                    new Action("Nope")
                )
            )
        );

        LoginMessage sessionInfo = new LoginMessage();
        sessionInfo.setId(123);
        sessionInfo.setLogin(USER_NAME);
        return sessionInfo;
      }
    }).getFuture();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestHostGame(NewGameInfo newGameInfo) {
    return taskService.submitTask(new CompletableTask<GameLaunchMessage>(HIGH) {
      @Override
      protected GameLaunchMessage call() throws Exception {
        updateTitle("Hosting game");

        GameLaunchMessage gameLaunchMessage = new GameLaunchMessage();
        gameLaunchMessage.setArgs(Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234"));
        gameLaunchMessage.setMod("faf");
        gameLaunchMessage.setUid(1234);
        return gameLaunchMessage;
      }
    }).getFuture();
  }

  @Override
  public CompletableFuture<GameLaunchMessage> requestJoinGame(int gameId, String password) {
    return taskService.submitTask(new CompletableTask<GameLaunchMessage>(HIGH) {
      @Override
      protected GameLaunchMessage call() throws Exception {
        updateTitle("Joining game");

        GameLaunchMessage gameLaunchMessage = new GameLaunchMessage();
        gameLaunchMessage.setArgs(Arrays.asList("/ratingcolor d8d8d8d8", "/numgames 1234"));
        gameLaunchMessage.setMod("faf");
        gameLaunchMessage.setUid(1234);
        return gameLaunchMessage;
      }
    }).getFuture();
  }

  @Override
  public void disconnect() {

  }

  @Override
  public void reconnect() {

  }

  @Override
  public void addFriend(int playerId) {

  }

  @Override
  public void addFoe(int playerId) {

  }

  @Override
  public void requestMatchmakerInfo() {

  }

  @Override
  public CompletableFuture<GameLaunchMessage> startSearchMatchmaker() {
    log.debug("Starting matchmaker game");
    GameLaunchMessage gameLaunchMessage = new GameLaunchMessage();
    gameLaunchMessage.setUid(123);
    gameLaunchMessage.setMod(KnownFeaturedMod.DEFAULT.getTechnicalName());
    return CompletableFuture.completedFuture(gameLaunchMessage);
  }

  @Override
  public void stopSearchMatchmaker() {

  }

  @Override
  public void sendGpgMessage(GpgGameMessage message) {

  }

  @Override
  public void removeFriend(int playerId) {

  }

  @Override
  public void removeFoe(int playerId) {

  }

  @Override
  public void selectAvatar(URL url) {

  }

  @Override
  public void banPlayer(int playerId, int duration, PeriodType periodType, String reason) {

  }

  @Override
  public void closePlayersGame(int playerId) {

  }

  @Override
  public void closePlayersLobby(int playerId) {

  }

  @Override
  public void broadcastMessage(String message) {

  }

  @Override
  public List<Avatar> getAvailableAvatars() {
    return emptyList();
  }

  @Override
  public CompletableFuture<List<IceServer>> getIceServers() {
    return CompletableFuture.completedFuture(Collections.emptyList());
  }

  @Override
  public void restoreGameSession(int id) {

  }

  @Override
  public void ping() {

  }

  @Override
  public void gameMatchmaking(MatchmakingQueue queue, MatchmakingState state) {

  }

  @Override
  public void inviteToParty(com.faforever.client.player.Player recipient) {

  }

  @Override
  public void acceptPartyInvite(com.faforever.client.player.Player sender) {

  }

  @Override
  public void kickPlayerFromParty(com.faforever.client.player.Player kickedPlayer) {

  }

  @Override
  public void readyParty() {

  }

  @Override
  public void unreadyParty() {

  }

  @Override
  public void leaveParty() {

  }

  @Override
  public void setPartyFactions(List<Faction> factions) {

  }


  private GameInfoMessage createGameInfo(int uid, String title, GameAccess access, String featuredMod, String mapName, int numPlayers, int maxPlayers, String host) {
    GameInfoMessage gameInfoMessage = new GameInfoMessage();
    gameInfoMessage.setUid(uid);
    gameInfoMessage.setTitle(title);
    gameInfoMessage.setFeaturedMod(featuredMod);
    gameInfoMessage.setMapname(mapName);
    gameInfoMessage.setNumPlayers(numPlayers);
    gameInfoMessage.setMaxPlayers(maxPlayers);
    gameInfoMessage.setHost(host);
    gameInfoMessage.setState(GameStatus.OPEN);
    gameInfoMessage.setSimMods(Collections.emptyMap());
    gameInfoMessage.setTeams(Collections.emptyMap());
    gameInfoMessage.setPasswordProtected(access == GameAccess.PASSWORD);

    return gameInfoMessage;
  }
}
