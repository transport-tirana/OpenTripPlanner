package org.opentripplanner.routing.algorithm.raptoradapter.transit.cost;

import org.opentripplanner.raptor.spi.RaptorCostCalculator;
import org.opentripplanner.raptor.spi.RaptorTransferConstraint;
import org.opentripplanner.routing.api.request.preference.TirStopSafetyPreferences;

public class TirShelterAndLightingCostCalculator<T extends DefaultTripSchedule>
  implements RaptorCostCalculator<T> {

  private final RaptorCostCalculator<T> delegate;
  private final TirStopSafetyPreferences requirements;

  public TirShelterAndLightingCostCalculator(
    RaptorCostCalculator<T> delegate,
    TirStopSafetyPreferences requirements
  ) {
    this.delegate = delegate;
    this.requirements = requirements;
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
    return delegate.boardingCost(firstBoarding, prevArrivalTime, boardStopIndex, boardTime, trip,
      transferConstraints);
  }

  @Override
  public int onTripRelativeRidingCost(int boardTime, T tripScheduledBoarded) {
    return delegate.onTripRelativeRidingCost(boardTime, tripScheduledBoarded);
  }

  @Override
  public int transitArrivalCost(
    int boardCost,
    int alightSlack,
    int transitTime,
    T trip,
    int toStopIndex
  ) {
    return delegate.transitArrivalCost(boardCost, alightSlack, transitTime, trip, toStopIndex);
  }

  @Override
  public int waitCost(int waitTimeInSeconds) {
    // TODO TIR: cost for waiting at a stop that is not lit and/or has no shelter
    return delegate.waitCost(waitTimeInSeconds);
  }

  @Override
  public int calculateRemainingMinCost(int minTravelTime, int minNumTransfers, int fromStopIndex) {
    return delegate.calculateRemainingMinCost(minTravelTime, minNumTransfers, fromStopIndex);
  }

  @Override
  public int costEgress(int stopIndex, boolean egressHasRides) {
    return delegate.costEgress(stopIndex, egressHasRides);
  }
}
