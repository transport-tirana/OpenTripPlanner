package org.opentripplanner.routing.api.request.preference;

import org.opentripplanner.core.model.basic.Cost;
import org.opentripplanner.utils.tostring.ToStringBuilder;

import java.util.Objects;
import java.util.function.Consumer;

/**
 * Preferences for how to treat trips or stops with accessibility restrictions, like wheelchair
 * accessibility.
 * <p>
 * THIS CLASS IS IMMUTABLE AND THREAD-SAFE.
 */
public final class TirStopSafetyPreferences {

  /**
   * Set the unknown cost to a very high number, so in case it is used accidentally it will not
   * cause any harm.
   */
  private static final Cost NOT_SET = Cost.costOfSeconds(9_999_999);

  private static final TirStopSafetyPreferences DEFAULT_UNSET = new TirStopSafetyPreferences(
    false,
    NOT_SET,
    NOT_SET
  );
  private static final TirStopSafetyPreferences ONLY_CONSIDER_ACCESSIBLE =
    new TirStopSafetyPreferences(true, NOT_SET, NOT_SET);

  private final boolean onlyConsiderAccessible;
  private final Cost unknownCost;
  private final Cost inaccessibleCost;

  private TirStopSafetyPreferences(
    boolean onlyConsiderAccessible,
    Cost unknownCost,
    Cost inaccessibleCost
  ) {
    this.onlyConsiderAccessible = onlyConsiderAccessible;
    this.unknownCost = unknownCost;
    this.inaccessibleCost = inaccessibleCost;
  }

  /**
   * Create a feature which only considers wheelchair-accessible trips/stops.
   */
  public static TirStopSafetyPreferences ofOnlyAccessible() {
    return ONLY_CONSIDER_ACCESSIBLE;
  }

  /**
   * Create a feature which considers trips/stops that don't have an accessibility of POSSIBLE.
   */
  public static TirStopSafetyPreferences ofCost(int unknownCost, int inaccessibleCost) {
    return new TirStopSafetyPreferences(
      false,
      Cost.costOfSeconds(unknownCost),
      Cost.costOfSeconds(inaccessibleCost)
    );
  }

  public static Builder of() {
    return DEFAULT_UNSET.copyOf();
  }

  public Builder copyOf() {
    return new Builder(this, this);
  }

  public Builder copyOfWithDefaultCosts(TirStopSafetyPreferences defaultCosts) {
    return new Builder(this, defaultCosts);
  }

  /**
   * Whether to include trips or stops which don't have a wheelchair accessibility of POSSIBLE
   */
  public boolean onlyConsiderAccessible() {
    return onlyConsiderAccessible;
  }

  /**
   * The extra cost to add when using a trip/stop which has an accessibility of UNKNOWN.
   */
  public int unknownCost() {
    return unknownCost.toSeconds();
  }

  /**
   * The extra cost to add when using a trip/stop which has an accessibility of NOT_POSSIBLE.
   */
  public int inaccessibleCost() {
    return inaccessibleCost.toSeconds();
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
      onlyConsiderAccessible == that.onlyConsiderAccessible &&
      Objects.equals(unknownCost, that.unknownCost) &&
      Objects.equals(inaccessibleCost, that.inaccessibleCost)
    );
  }

  @Override
  public int hashCode() {
    return Objects.hash(onlyConsiderAccessible, unknownCost, inaccessibleCost);
  }

  @Override
  public String toString() {
    return toString(DEFAULT_UNSET);
  }

  public String toString(TirStopSafetyPreferences defaultCosts) {
    if (onlyConsiderAccessible) {
      return "OnlyConsiderAccessible";
    }

    return ToStringBuilder.of(TirStopSafetyPreferences.class)
      .addObj("unknownCost", unknownCost, defaultCosts.unknownCost)
      .addObj("inaccessibleCost", inaccessibleCost, defaultCosts.inaccessibleCost)
      .toString();
  }

  public static class Builder {

    private final TirStopSafetyPreferences original;
    private boolean onlyConsiderAccessible;
    private Cost unknownCost;
    private Cost inaccessibleCost;

    private Builder(TirStopSafetyPreferences original, TirStopSafetyPreferences defaultCosts) {
      this.original = original;

      if (original.onlyConsiderAccessible) {
        this.onlyConsiderAccessible = true;
        this.unknownCost = defaultCosts.unknownCost;
        this.inaccessibleCost = defaultCosts.inaccessibleCost;
      } else {
        this.onlyConsiderAccessible = false;
        this.unknownCost = original.unknownCost;
        this.inaccessibleCost = original.inaccessibleCost;
      }
    }

    public boolean onlyConsiderAccessible() {
      return onlyConsiderAccessible;
    }

    public Builder withAccessibleOnly() {
      this.onlyConsiderAccessible = true;
      return this;
    }

    public int unknownCost() {
      return unknownCost.toSeconds();
    }

    public Builder withUnknownCost(int unknownCost) {
      this.onlyConsiderAccessible = false;
      this.unknownCost = Cost.costOfSeconds(unknownCost);
      return this;
    }

    public int inaccessibleCost() {
      return inaccessibleCost.toSeconds();
    }

    public Builder withInaccessibleCost(int inaccessibleCost) {
      this.onlyConsiderAccessible = false;
      this.inaccessibleCost = Cost.costOfSeconds(inaccessibleCost);
      return this;
    }

    public Builder apply(Consumer<Builder> body) {
      body.accept(this);
      return this;
    }

    public TirStopSafetyPreferences build() {
      var value = onlyConsiderAccessible
        ? ofOnlyAccessible()
        : ofCost(unknownCost.toSeconds(), inaccessibleCost.toSeconds());
      return original.equals(value) ? original : value;
    }
  }
}
