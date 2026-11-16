package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.raptorlegacy._data.transit.TestTripSchedule;
import org.opentripplanner.routing.api.request.preference.TirStopSafetyPreferences;
import org.opentripplanner.transfer.constrained.model.TransferConstraint;
import org.opentripplanner.transit.model._data.TransitRepositoryForTest;
import org.opentripplanner.transit.model.site.StopLocation;
import org.opentripplanner.utils.time.TimeUtils;

class TirShelterLightingCostCalculatorTest {

  private static final int UNLIT_COST = 300;
  private static final int UNSHELTERED_COST = 120;
  private static final int BOARD_COST_SEC = 5;
  private static final int TRANSFER_COST_SEC = 2;
  private static final double WAIT_RELUCTANCE_FACTOR = 0.5;

  private static final int STOP_LIT_AND_SHELTERED = 0;
  private static final int STOP_UNLIT = 1;
  private static final int STOP_UNSHELTERED = 2;
  private static final int STOP_UNLIT_AND_UNSHELTERED = 3;
  private static final int STOP_INDEX_HOLE = 4;

  private static final int NIGHT = TimeUtils.time("23:00");
  private static final int DAY = TimeUtils.time("12:00");
  private static final int SECONDS_IN_DAY = 24 * 60 * 60;

  private static final TransitRepositoryForTest TEST_MODEL = TransitRepositoryForTest.of();

  /** The list index is the stop index, the last element is a hole in the stop index. */
  private static final List<StopLocation> STOPS = Arrays.asList(
    TEST_MODEL.stop("A").withLitAtNight(1).withShelter(1).build(),
    TEST_MODEL.stop("B").withLitAtNight(0).withShelter(1).build(),
    TEST_MODEL.stop("C").withLitAtNight(1).withShelter(0).build(),
    TEST_MODEL.stop("D").build(),
    null
  );

  private static final TirStopSafetyPreferences PREFERENCES = TirStopSafetyPreferences.ofCost(
    UNLIT_COST,
    UNSHELTERED_COST
  );

  private static final RaptorTransferConstraint STAY_SEATED = TransferConstraint.of()
    .staySeated()
    .build();

  private final TestTripSchedule schedule = TestTripSchedule.schedule("12:00 12:01").build();

  private final DefaultCostCalculator<TestTripSchedule> defaultCostCalculator =
    new DefaultCostCalculator<>(
      BOARD_COST_SEC,
      TRANSFER_COST_SEC,
      WAIT_RELUCTANCE_FACTOR,
      null,
      null
    );

  private final TirShelterLightingCostCalculator<TestTripSchedule> subject = createSubject(
    PREFERENCES
  );

  static Stream<Arguments> testCases() {
    return Stream.of(
      Arguments.of(STOP_LIT_AND_SHELTERED, NIGHT, 0),
      Arguments.of(STOP_LIT_AND_SHELTERED, DAY, 0),
      Arguments.of(STOP_UNLIT, NIGHT, UNLIT_COST),
      Arguments.of(STOP_UNLIT, DAY, 0),
      Arguments.of(STOP_UNSHELTERED, NIGHT, UNSHELTERED_COST),
      Arguments.of(STOP_UNSHELTERED, DAY, UNSHELTERED_COST),
      Arguments.of(STOP_UNLIT_AND_UNSHELTERED, NIGHT, UNLIT_COST + UNSHELTERED_COST),
      Arguments.of(STOP_UNLIT_AND_UNSHELTERED, DAY, UNSHELTERED_COST),
      Arguments.of(STOP_INDEX_HOLE, NIGHT, 0),
      Arguments.of(STOP_INDEX_HOLE, DAY, 0)
    );
  }

  @ParameterizedTest(name = "boarding at stop index {0} at {1}s should add an extra cost of {2}")
  @MethodSource("testCases")
  void calculateExtraBoardingCost(int stopIndex, int boardTime, int expectedExtraCost) {
    for (boolean firstBoarding : List.of(true, false)) {
      assertEquals(expectedExtraCost, extraCost(subject, firstBoarding, stopIndex, boardTime));
    }
  }

  @ParameterizedTest(name = "with the default night 20:00-06:00, is {0} at night: {1}")
  @CsvSource({
    "19:59:59, false",
    "20:00, true",
    "23:59:59, true",
    "00:00, true",
    "05:59:59, true",
    "06:00, false",
    "12:00, false",
  })
  void nightSpanningMidnight(String time, boolean expectNight) {
    int boardTime = TimeUtils.time(time);
    int expected = expectNight ? UNLIT_COST : 0;

    assertEquals(expected, extraCost(subject, true, STOP_UNLIT, boardTime));
    // The day before and the day after the search date
    assertEquals(expected, extraCost(subject, true, STOP_UNLIT, boardTime - SECONDS_IN_DAY));
    assertEquals(expected, extraCost(subject, true, STOP_UNLIT, boardTime + SECONDS_IN_DAY));
  }

  @ParameterizedTest(name = "with the night 01:00-04:00, is {0} at night: {1}")
  @CsvSource({ "00:59:59, false", "01:00, true", "03:59:59, true", "04:00, false", "23:00, false" })
  void nightWithinSameDay(String time, boolean expectNight) {
    var calculator = createSubject(
      PREFERENCES.copyOf()
        .withNightStart(LocalTime.of(1, 0))
        .withNightEnd(LocalTime.of(4, 0))
        .build()
    );
    int expected = expectNight ? UNLIT_COST : 0;

    assertEquals(expected, extraCost(calculator, true, STOP_UNLIT, TimeUtils.time(time)));
  }

  @ParameterizedTest(name = "with the same night start and end, {0} is at night")
  @CsvSource({ "00:00", "07:59:59", "08:00", "12:00", "23:59:59" })
  void nightLastsTheEntireDayIfStartAndEndAreEqual(String time) {
    var calculator = createSubject(
      PREFERENCES.copyOf()
        .withNightStart(LocalTime.of(8, 0))
        .withNightEnd(LocalTime.of(8, 0))
        .build()
    );

    assertEquals(UNLIT_COST, extraCost(calculator, true, STOP_UNLIT, TimeUtils.time(time)));
  }

  @Test
  void noExtraCostWhenStayingSeated() {
    int stop = STOP_UNLIT_AND_UNSHELTERED;

    assertEquals(
      defaultCostCalculator.boardingCost(false, 0, stop, NIGHT, schedule, STAY_SEATED),
      subject.boardingCost(false, 0, stop, NIGHT, schedule, STAY_SEATED)
    );
  }

  @Test
  void onlyBoardingIsAffected() {
    int stop = STOP_UNLIT_AND_UNSHELTERED;
    assertEquals(defaultCostCalculator.waitCost(60), subject.waitCost(60));
    assertEquals(
      defaultCostCalculator.transitCost(60, schedule),
      subject.transitCost(60, schedule)
    );
    assertEquals(
      defaultCostCalculator.transitArrivalCost(100, 10, 60, schedule, stop),
      subject.transitArrivalCost(100, 10, 60, schedule, stop)
    );
    assertEquals(
      defaultCostCalculator.calculateRemainingMinCost(600, 1, stop),
      subject.calculateRemainingMinCost(600, 1, stop)
    );
    assertEquals(defaultCostCalculator.costEgress(stop, false), subject.costEgress(stop, false));
  }

  @Test
  void factoryOnlyAddsCalculatorWhenStopSafetyIsEnabled() {
    var disabled = GeneralizedCostParameters.DEFAULTS;
    var enabled = GeneralizedCostParameters.of().stopSafetyPreferences(PREFERENCES).build();

    assertInstanceOf(
      DefaultCostCalculator.class,
      CostCalculatorFactory.createCostCalculator(disabled, null, STOPS.size(), STOPS::get)
    );
    assertInstanceOf(
      TirShelterLightingCostCalculator.class,
      CostCalculatorFactory.createCostCalculator(enabled, null, STOPS.size(), STOPS::get)
    );
  }

  private TirShelterLightingCostCalculator<TestTripSchedule> createSubject(
    TirStopSafetyPreferences preferences
  ) {
    return new TirShelterLightingCostCalculator<>(
      defaultCostCalculator,
      preferences,
      STOPS.size(),
      STOPS::get
    );
  }

  /** The cost added by the given calculator on top of the default cost, in seconds. */
  private int extraCost(
    RaptorCostCalculator<TestTripSchedule> calculator,
    boolean firstBoarding,
    int stopIndex,
    int boardTime
  ) {
    int cost = boardingCost(calculator, firstBoarding, stopIndex, boardTime);
    int defaultCost = boardingCost(defaultCostCalculator, firstBoarding, stopIndex, boardTime);
    return RaptorCostConverter.toOtpDomainCost(cost - defaultCost);
  }

  private int boardingCost(
    RaptorCostCalculator<TestTripSchedule> calculator,
    boolean firstBoarding,
    int stopIndex,
    int boardTime
  ) {
    return calculator.boardingCost(
      firstBoarding,
      boardTime - 60,
      stopIndex,
      boardTime,
      schedule,
      RaptorTransferConstraint.REGULAR_TRANSFER
    );
  }
}
