package com.faforever.client.game;

import com.faforever.client.legacy.domain.GameInfo;
import com.faforever.client.map.MapService;
import com.faforever.client.test.AbstractSpringJavaFxTest;
import com.faforever.client.util.Callback;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.Node;
import javafx.scene.layout.Pane;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;

import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.collection.IsCollectionWithSize.hasSize;
import static org.hamcrest.collection.IsEmptyCollection.empty;
import static org.junit.Assert.*;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.internal.verification.VerificationModeFactory.times;

public class GameTiledControllerTest extends AbstractSpringJavaFxTest {

  @Autowired
  GameCardController gameCardController;
  private GameTiledController instance;

  @Before
  public void setUp() throws Exception {
    instance = loadController("games_tiled.fxml");
  }

  @Test
  public void testCreateTiledFlowPaneWithEmptyList() throws Exception {
    when(gameCardController.getRoot()).thenReturn(new Pane());
    ObservableList<GameInfoBean> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList);

    assertThat(instance.tiledFlowPane.getChildren(), empty());
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedList() throws Exception {
    when(gameCardController.getRoot()).thenReturn(new Pane());
    ObservableList<GameInfoBean> observableList = FXCollections.observableArrayList();
    observableList.add(new GameInfoBean());

    instance.createTiledFlowPane(observableList);

    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPostInstantiatedGameInfoBean() throws Exception {
    when(gameCardController.getRoot()).thenReturn(new Pane());
    ObservableList<GameInfoBean> observableList = FXCollections.observableArrayList();

    instance.createTiledFlowPane(observableList);
    observableList.add(new GameInfoBean());

    assertThat(instance.tiledFlowPane.getChildren(), hasSize(1));
  }

  @Test
  public void testCreateTiledFlowPaneWithPopulatedListAndPostInstantiatedGameInfoBean() throws Exception {
    when(gameCardController.getRoot())
        .thenReturn(new Pane())
        .thenReturn(new Pane());

    ObservableList<GameInfoBean> observableList = FXCollections.observableArrayList();

    observableList.add(new GameInfoBean());
    instance.createTiledFlowPane(observableList);
    observableList.add(new GameInfoBean());

    assertThat(instance.tiledFlowPane.getChildren(), hasSize(2));
  }

  @Test
  public void testGetRoot() throws Exception {
    assertThat(instance.getRoot(), instanceOf(Node.class));
  }
}
