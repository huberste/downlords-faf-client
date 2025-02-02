package com.faforever.client.chat;

import com.faforever.client.i18n.I18n;
import com.faforever.client.test.AbstractPlainJavaFxTest;
import com.faforever.client.util.TimeService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

public class PlayerRatingChartTooltipControllerTest extends AbstractPlainJavaFxTest {

  @Mock
  private TimeService timeService;

  @Mock
  private I18n i18n;

  private PlayerRatingChartTooltipController instance;

  @Before
  public void setUp() throws Exception {
    instance = new PlayerRatingChartTooltipController(timeService, i18n);
    loadFxml("theme/chat/player_rating_chart_tooltip.fxml", clazz -> instance);
  }

  @Test
  public void testDisplayedValues() {
    when(timeService.asDate(any())).thenReturn("date");
    when(i18n.number(500)).thenReturn("500");
    runOnFxThreadAndWait(() -> instance.setDateAndRating(10000000, 500));
    assertEquals("date", instance.dateLabel.getText());
    assertEquals("500", instance.ratingLabel.getText());

    runOnFxThreadAndWait(() -> instance.clear());
    assertTrue(instance.dateLabel.getText().isEmpty());
    assertTrue(instance.ratingLabel.getText().isEmpty());
  }
}
