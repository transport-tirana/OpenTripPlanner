package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import java.util.function.IntFunction;
import org.jspecify.annotations.Nullable;
import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorCostConverter;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.routing.api.request.preference.TirStopSafetyPreferences;
import org.opentripplanner.transit.model.site.StationElement;
import org.opentripplanner.transit.model.site.StopLocation;

/**
 * TIR: Adds an extra cost to boarding at a stop which is not lit at night and/or does not have a
 * shelter. The passenger has to wait at the stop before every boarding, so the cost is added to
 * both the first boarding and to boardings after a transfer. It is not added when the passenger
 * stays seated, since there is no waiting at the stop in that case.
 * <p>
 * The cost for an unlit stop is only added if the vehicle is boarded at night, while the cost for
 * a stop without a shelter is added at any time of the day.
 * <p>
 * This class is immutable and thread safe.
 */
public class TirShelterLightingCostCalculator<T extends DefaultTripSchedule> implements
  RaptorCostCalculator<T> {

  private static final int SECONDS_IN_DAY = 24 * 60 * 60;

  private final RaptorCostCalculator<T> delegate;

  /** The cost of boarding at night at each stop, indexed by stop index. Unit is Raptor cost. */
  private final int[] unlitBoardCosts;

  /** The cost of boarding at each stop, indexed by stop index. Unit is Raptor cost. */
  private final int[] unshelteredBoardCosts;

  /** The start of the night (inclusive) in seconds since midnight. */
  private final int nightStart;

  /** The end of the night (exclusive) in seconds since midnight. */
  private final int nightEnd;

  /**
   * @param stopCount   the size of the stop index.
   * @param stopByIndex look up a stop by its index, may return {@code null} since there can be
   *                    holes in the stop index.
   */
  public TirShelterLightingCostCalculator(
    RaptorCostCalculator<T> delegate,
    TirStopSafetyPreferences preferences,
    int stopCount,
    IntFunction<@Nullable StopLocation> stopByIndex
  ) {
    this.delegate = delegate;
    this.nightStart = preferences.nightStart().toSecondOfDay();
    this.nightEnd = preferences.nightEnd().toSecondOfDay();
    this.unlitBoardCosts = new int[stopCount];
    this.unshelteredBoardCosts = new int[stopCount];

    int unlitCost = RaptorCostConverter.toRaptorCost(preferences.unlitCost().toSeconds());
    int unshelteredCost = RaptorCostConverter.toRaptorCost(
      preferences.unshelteredCost().toSeconds()
    );

    for (int i = 0; i < stopCount; ++i) {
      // Only station elements have lighting and shelter information. Other stop locations, like
      // flex areas, are not penalized.
      if (stopByIndex.apply(i) instanceof StationElement<?, ?> stop) {
        if (stop.getLitAtNight() != 1) {
          unlitBoardCosts[i] = unlitCost;
        }
        if (stop.getShelter() != 1) {
          unshelteredBoardCosts[i] = unshelteredCost;
        }
      }
    }
  }

  @Override
  public int boardingCost(
    boolean firstBoarding,
    int prevArrivalTime,
    int boardStopIndex,
    int boardTime,
    T trip,
    RaptorTransferConstraint transferConstraints
  ) {
    int cost = delegate.boardingCost(
      firstBoarding,
      prevArrivalTime,
      boardStopIndex,
      boardTime,
      trip,
      transferConstraints
    );
    if (transferConstraints.isStaySeated()) {
      return cost;
    }
    cost += unshelteredBoardCosts[boardStopIndex];

    if (isNight(boardTime)) {
      cost += unlitBoardCosts[boardStopIndex];
    }
    return cost;
  }

  @Override
  public int transitCost(int transitDuration, T tripScheduledBoarded) {
    return delegate.transitCost(transitDuration, tripScheduledBoarded);
  }

  @Override
  public int transitArrivalCost(
    int boardCost,
    int alightSlack,
    int transitDuration,
    T trip,
    int toStopIndex
  ) {
    return delegate.transitArrivalCost(boardCost, alightSlack, transitDuration, trip, toStopIndex);
  }

  @Override
  public int waitCost(int waitTimeInSeconds) {
    return delegate.waitCost(waitTimeInSeconds);
  }

  /**
   * The stop safety costs are never negative, so the delegate's value is still a valid lower
   * bound.
   */
  @Override
  public int calculateRemainingMinCost(
    int minTravelDuration,
    int minNumTransfers,
    int fromStopIndex
  ) {
    return delegate.calculateRemainingMinCost(minTravelDuration, minNumTransfers, fromStopIndex);
  }

  @Override
  public int costEgress(int stopIndex, boolean egressHasRides) {
    return delegate.costEgress(stopIndex, egressHasRides);
  }

  /**
   * Return {@code true} if the given time is at night. The time is in seconds relative to the
   * start of the service day of the search, in the transit time zone. It is negative for the day
   * before and more than 24 hours for the following days.
   */
  private boolean isNight(int time) {
    int timeOfDay = Math.floorMod(time, SECONDS_IN_DAY);

    if (nightStart < nightEnd) {
      return nightStart <= timeOfDay && timeOfDay < nightEnd;
    }
    // The night spans midnight, or lasts the entire day if the start and end are equal
    return nightStart <= timeOfDay || timeOfDay < nightEnd;
  }
}
