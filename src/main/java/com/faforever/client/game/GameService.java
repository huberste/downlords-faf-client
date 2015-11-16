package com.faforever.client.game;

import com.faforever.client.rankedmatch.OnRankedMatchNotificationListener;
import javafx.beans.property.BooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.collections.MapChangeListener;
import javafx.collections.ObservableList;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Downloads necessary maps, mods and updates before starting
 */
public interface GameService {

  void addOnGameInfoBeanListener(ListChangeListener<GameInfoBean> listener);

  CompletableFuture<Void> hostGame(NewGameInfo name);

  CompletableFuture<Void> joinGame(GameInfoBean gameInfoBean, String password);

  List<GameTypeBean> getGameTypes();

  void addOnGameTypeInfoListener(MapChangeListener<String, GameTypeBean> changeListener);

  void addOnGameStartedListener(OnGameStartedListener listener);

  /**
   * @param path a replay file that is readable by the game without any further conversion
   */
  void runWithReplay(Path path, @Nullable Integer replayId, String gameType, Integer version, Map<String, Integer> modVersions, Set<String> simMods) throws IOException;

  void runWithReplay(URL url, Integer replayId) throws IOException;

  ObservableList<GameInfoBean> getGameInfoBeans();

  @Nullable
  GameTypeBean getGameTypeByString(String gameTypeBeanName);

  GameInfoBean getByUid(int uid);

  void addOnRankedMatchNotificationListener(OnRankedMatchNotificationListener listener);

  CompletableFuture<Void> startSearchRanked1v1(Faction faction);

  void stopSearchRanked1v1();

  BooleanProperty searching1v1Property();
}
