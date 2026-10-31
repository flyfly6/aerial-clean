package io.github.flyfly6.aerialclean.weather;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.util.Calendar;

public class LunarCalendarTest {

    @Test
    public void testLunarSpringFestival() {
        Calendar cal2024 = Calendar.getInstance();
        cal2024.set(2024, Calendar.FEBRUARY, 10);
        assertEquals("正月初一", LunarCalendar.getLunarDateString(cal2024));

        Calendar cal2023 = Calendar.getInstance();
        cal2023.set(2023, Calendar.JANUARY, 22);
        assertEquals("正月初一", LunarCalendar.getLunarDateString(cal2023));
    }

    @Test
    public void testLunarSpecificDate() {
        Calendar cal = Calendar.getInstance();
        cal.set(2026, Calendar.OCTOBER, 31);
        assertEquals("九月廿二", LunarCalendar.getLunarDateString(cal));
    }
}
