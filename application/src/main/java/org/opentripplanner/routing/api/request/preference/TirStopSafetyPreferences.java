package org.opentripplanner.routing.api.request.preference;

import java.io.Serializable;
import java.time.LocalTime;
import java.util.Objects;
import java.util.function.Consumer;
import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.utils.tostring.ToStringBuilder;

/**
 * TIR: Preferences for how to treat boarding at stops that a passenger may perceive as unsafe or
 * uncomfortable to wait at: stops which are not lit at night and stops without a shelter.
 * <p>
 * Each cost is added once every time a transit vehicle is boarded at a stop lacking the given
 * feature. The unlit cost is only added when the vehicle is boarded at night, see
 * {@link #nightStart()} and {@link #nightEnd()}, while the unsheltered cost is added at any time
 * of the day. If both apply, both costs are added. The default is zero for both costs, which turns
 * the feature off.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class TirStopSafetyPreferences implements Serializable {

  public static final TirStopSafetyPreferences DEFAULT = new TirStopSafetyPreferences(
    Cost.ZERO,
    Cost.ZERO,
    LocalTime.of(20, 0),
    LocalTime.of(6, 0)
  );

  private final Cost unlitCost;
  private final Cost unshelteredCost;
  private final LocalTime nightStart;
  private final LocalTime nightEnd;

  private TirStopSafetyPreferences(
    Cost unlitCost,
    Cost unshelteredCost,
    LocalTime nightStart,
    LocalTime nightEnd
  ) {
    this.unlitCost = requireNotNegative(unlitCost, "unlitCost");
    this.unshelteredCost = requireNotNegative(unshelteredCost, "unshelteredCost");
    this.nightStart = Objects.requireNonNull(nightStart, "nightStart");
    this.nightEnd = Objects.requireNonNull(nightEnd, "nightEnd");
  }

  /**
   * Create preferences with the given costs in seconds, see {@link #unlitCost()} and
   * {@link #unshelteredCost()}.
   */
  public static TirStopSafetyPreferences ofCost(int unlitCost, int unshelteredCost) {
    return of().withUnlitCost(unlitCost).withUnshelteredCost(unshelteredCost).build();
  }

  public static Builder of() {
    return DEFAULT.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this);
  }

  /**
   * The extra cost to add when boarding at a stop which is not lit at night. The cost is only
   * added if the vehicle is boarded at night.
   */
  public Cost unlitCost() {
    return unlitCost;
  }

  /**
   * The extra cost to add when boarding at a stop which does not have a shelter.
   */
  public Cost unshelteredCost() {
    return unshelteredCost;
  }

  /**
   * The local time of day the night starts (inclusive). The night may span midnight, the default
   * night is from 20:00 to 06:00. If the start and end are the same, the night lasts the entire
   * day - the unlit cost is then added regardless of when the vehicle is boarded.
   */
  public LocalTime nightStart() {
    return nightStart;
  }

  /**
   * The local time of day the night ends (exclusive), see {@link #nightStart()}.
   */
  public LocalTime nightEnd() {
    return nightEnd;
  }

  /**
   * Return {@code true} if at least one of the costs is set. If not, the stop safety does not
   * influence the routing at all.
   */
  public boolean isEnabled() {
    return !(unlitCost.isZero() && unshelteredCost.isZero());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TirStopSafetyPreferences that = (TirStopSafetyPreferences) o;
    return (
      Objects.equals(unlitCost, that.unlitCost) &&
      Objects.equals(unshelteredCost, that.unshelteredCost) &&
      Objects.equals(nightStart, that.nightStart) &&
      Objects.equals(nightEnd, that.nightEnd)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(unlitCost, unshelteredCost, nightStart, nightEnd);
  }

  @Override
  public String toString() {
    return ToStringBuilder.of(TirStopSafetyPreferences.class)
      .addObj("unlitCost", unlitCost, DEFAULT.unlitCost)
      .addObj("unshelteredCost", unshelteredCost, DEFAULT.unshelteredCost)
      .addObj("nightStart", nightStart, DEFAULT.nightStart)
      .addObj("nightEnd", nightEnd, DEFAULT.nightEnd)
      .toString();
  }

  private static Cost requireNotNegative(Cost cost, String name) {
    Objects.requireNonNull(cost, name);
    if (cost.lessThan(Cost.ZERO)) {
      throw new IllegalArgumentException("The " + name + " can not be negative: " + cost);
    }
    return cost;
  }

  public static class Builder {

    private final TirStopSafetyPreferences original;
    private Cost unlitCost;
    private Cost unshelteredCost;
    private LocalTime nightStart;
    private LocalTime nightEnd;

    private Builder(TirStopSafetyPreferences original) {
      this.original = original;
      this.unlitCost = original.unlitCost;
      this.unshelteredCost = original.unshelteredCost;
      this.nightStart = original.nightStart;
      this.nightEnd = original.nightEnd;
    }

    public TirStopSafetyPreferences original() {
      return original;
    }

    public Builder withUnlitCost(Cost unlitCost) {
      this.unlitCost = unlitCost;
      return this;
    }

    public Builder withUnlitCost(int unlitCost) {
      return withUnlitCost(Cost.costOfSeconds(unlitCost));
    }

    public Builder withUnshelteredCost(Cost unshelteredCost) {
      this.unshelteredCost = unshelteredCost;
      return this;
    }

    public Builder withUnshelteredCost(int unshelteredCost) {
      return withUnshelteredCost(Cost.costOfSeconds(unshelteredCost));
    }

    public Builder withNightStart(LocalTime nightStart) {
      this.nightStart = nightStart;
      return this;
    }

    public Builder withNightEnd(LocalTime nightEnd) {
      this.nightEnd = nightEnd;
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public TirStopSafetyPreferences build() {
      var value = new TirStopSafetyPreferences(unlitCost, unshelteredCost, nightStart, nightEnd);
      return original.equals(value) ? original : value;
    }
  }
}
