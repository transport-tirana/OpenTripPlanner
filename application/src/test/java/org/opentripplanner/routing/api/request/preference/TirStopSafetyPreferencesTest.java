package org.opentripplanner.routing.api.request.preference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.opentripplanner.routing.api.request.preference.ImmutablePreferencesAsserts.assertEqualsAndHashCode;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;
import org.opentripplanner.core.model.basic.Cost;

class TirStopSafetyPreferencesTest {

  private static final int UNLIT_COST = 300;
  private static final int UNSHELTERED_COST = 120;

  private final TirStopSafetyPreferences subject = TirStopSafetyPreferences.ofCost(
    UNLIT_COST,
    UNSHELTERED_COST
  );

  @Test
  void unlitCost() {
    assertEquals(Cost.costOfSeconds(UNLIT_COST), subject.unlitCost());
    assertEquals(Cost.ZERO, TirStopSafetyPreferences.DEFAULT.unlitCost());
  }

  @Test
  void unshelteredCost() {
    assertEquals(Cost.costOfSeconds(UNSHELTERED_COST), subject.unshelteredCost());
    assertEquals(Cost.ZERO, TirStopSafetyPreferences.DEFAULT.unshelteredCost());
  }

  @Test
  void night() {
    assertEquals(LocalTime.of(20, 0), subject.nightStart());
    assertEquals(LocalTime.of(6, 0), subject.nightEnd());

    var other = subject
      .copyOf()
      .withNightStart(LocalTime.of(22, 30))
      .withNightEnd(LocalTime.of(5, 0))
      .build();
    assertEquals(LocalTime.of(22, 30), other.nightStart());
    assertEquals(LocalTime.of(5, 0), other.nightEnd());
    assertEquals(
      "TirStopSafetyPreferences{unlitCost: $300, unshelteredCost: $120, nightStart: 22:30, nightEnd: 05:00}",
      other.toString()
    );
  }

  @Test
  void isEnabled() {
    assertFalse(TirStopSafetyPreferences.DEFAULT.isEnabled());
    assertTrue(subject.isEnabled());
    assertTrue(TirStopSafetyPreferences.ofCost(UNLIT_COST, 0).isEnabled());
    assertTrue(TirStopSafetyPreferences.ofCost(0, UNSHELTERED_COST).isEnabled());
  }

  @Test
  void negativeCostIsNotAllowed() {
    assertThrows(IllegalArgumentException.class, () -> TirStopSafetyPreferences.ofCost(-1, 0));
    assertThrows(IllegalArgumentException.class, () -> TirStopSafetyPreferences.ofCost(0, -1));
  }

  @Test
  void testCopyOfReturnsSameInstanceIfNotChanged() {
    assertSame(TirStopSafetyPreferences.DEFAULT, TirStopSafetyPreferences.of().build());
    assertSame(subject, subject.copyOf().build());
    assertSame(TirStopSafetyPreferences.DEFAULT, TirStopSafetyPreferences.ofCost(0, 0));
  }

  @Test
  void testEqualsAndHashCode() {
    var same = TirStopSafetyPreferences.ofCost(UNLIT_COST, UNSHELTERED_COST);
    var other = TirStopSafetyPreferences.ofCost(2, 2);
    assertEqualsAndHashCode(subject, other, same);

    var otherNight = subject.copyOf().withNightStart(LocalTime.of(21, 0)).build();
    assertEqualsAndHashCode(subject, otherNight, same);
  }

  @Test
  void testToString() {
    assertEquals("TirStopSafetyPreferences{}", TirStopSafetyPreferences.DEFAULT.toString());
    assertEquals(
      "TirStopSafetyPreferences{unlitCost: $300, unshelteredCost: $120}",
      subject.toString()
    );
  }
}
