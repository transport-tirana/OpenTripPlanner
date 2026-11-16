package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.function.IntFunction;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.transit.model.site.StopLocation;

public class CostCalculatorFactory {

  public static <T extends DefaultTripSchedule> RaptorCostCalculator<T> createCostCalculator(
    GeneralizedCostParameters generalizedCostParameters,
    int @Nullable [] stopBoardAlightTransferCosts
  ) {
    return createCostCalculator(
      generalizedCostParameters,
      stopBoardAlightTransferCosts,
      0,
      i -> null
    );
  }

  /**
   * @param stopCount   TIR: the size of the stop index, used together with {@code stopByIndex} to
   *                    find the stops which are unlit or do not have a shelter.
   * @param stopByIndex TIR: look up a stop by its index, may return {@code null} since there can
   *                    be holes in the stop index.
   */
  public static <T extends DefaultTripSchedule> RaptorCostCalculator<T> createCostCalculator(
    GeneralizedCostParameters generalizedCostParameters,
    int @Nullable [] stopBoardAlightTransferCosts,
    int stopCount,
    IntFunction<@Nullable StopLocation> stopByIndex
  ) {
    RaptorCostCalculator<T> calculator = new DefaultCostCalculator<>(
      generalizedCostParameters,
      stopBoardAlightTransferCosts
    );

    if (generalizedCostParameters.wheelchairEnabled()) {
      calculator = new WheelchairCostCalculator<>(
        calculator,
        generalizedCostParameters.wheelchairAccessibility()
      );
    }

    // append RouteCostCalculator to calculator stack if (un)preferred routes exist
    if (!generalizedCostParameters.unpreferredPatterns().isEmpty()) {
      calculator = new PatternCostCalculator<>(
        calculator,
        generalizedCostParameters.unpreferredPatterns(),
        generalizedCostParameters.unnpreferredCost()
      );
    }

    var stopSafety = generalizedCostParameters.stopSafetyPreferences();
    if (stopSafety.isEnabled()) {
      calculator = new TirShelterLightingCostCalculator<>(
        calculator,
        stopSafety,
        stopCount,
        stopByIndex
      );
    }

    return calculator;
  }
}
