package io.github.flyfly6.aerialclean.weather;

import java.util.Calendar;

/**
 * 农历（阴历）日期计算工具类。
 *
 * <p>兼容 Android 4.4 (API 19)，纯 Java 算法，涵盖 1900-2050 年。
 */
public final class LunarCalendar {

    private static final long[] LUNAR_INFO = new long[]{
            0x04bd8, 0x04ae0, 0x0a570, 0x054d5, 0x0d260, 0x0d950, 0x16554, 0x056a0, 0x09ad0, 0x055d2,
            0x04ae0, 0x0a5b6, 0x0a4d0, 0x0d250, 0x1d255, 0x0b540, 0x0d6a0, 0x0ada2, 0x095b0, 0x14977,
            0x04970, 0x0a4b0, 0x0b4b5, 0x06a50, 0x06d40, 0x1ab54, 0x02b60, 0x09570, 0x052f2, 0x04970,
            0x06566, 0x0d4a0, 0x0ea50, 0x06e95, 0x05ad0, 0x02b60, 0x186e3, 0x092e0, 0x1c8d7, 0x0c950,
            0x0d4a0, 0x1d8a6, 0x0b550, 0x056a0, 0x1a5b4, 0x025d0, 0x092d0, 0x0d2b2, 0x0a950, 0x0b557,
            0x06ca0, 0x0b550, 0x15355, 0x04da0, 0x0a5d0, 0x14573, 0x052d0, 0x0a9a8, 0x0e950, 0x06aa0,
            0x0aea6, 0x0ab50, 0x04b60, 0x0aae4, 0x0a570, 0x05260, 0x0f263, 0x0d950, 0x05b57, 0x056a0,
            0x096d0, 0x04dd5, 0x04ad0, 0x0a4d0, 0x0d4d4, 0x0d250, 0x0d558, 0x0b540, 0x0b5a0, 0x195a6,
            0x095b0, 0x049b0, 0x0a974, 0x0a4b0, 0x0b27a, 0x06a50, 0x06d40, 0x0af46, 0x0ab60, 0x09570,
            0x04af5, 0x04970, 0x064b0, 0x074a3, 0x0ea50, 0x06b58, 0x055c0, 0x0ab60, 0x096d5, 0x092e0,
            0x0c960, 0x0d954, 0x0d4a0, 0x0da50, 0x07552, 0x056a0, 0x0abb7, 0x025d0, 0x092d0, 0x0cab5,
            0x0a950, 0x0b4a0, 0x0baa4, 0x0ad50, 0x055d9, 0x04ba0, 0x0a5b0, 0x15176, 0x052b0, 0x0a930,
            0x07954, 0x06aa0, 0x0ad50, 0x05b52, 0x04b60, 0x0a6e6, 0x0a4e0, 0x0d260, 0x0ea65, 0x0d530,
            0x05aa0, 0x076a3, 0x096d0, 0x04afb, 0x04ad0, 0x0a4d0, 0x1d0b6, 0x0d250, 0x0d520, 0x0dd45,
            0x0b5a0, 0x056d0, 0x055b2, 0x049b0, 0x0a577, 0x0a4b0, 0x0aa50, 0x1b255, 0x06d20, 0x0ada0
    };

    private static final String[] CHINESE_MONTHS = new String[]{
            "正", "二", "三", "四", "五", "六", "七", "八", "九", "十", "冬", "腊"
    };

    private static final String[] CHINESE_DAYS_TEN = new String[]{"初", "十", "廿", "三"};

    private LunarCalendar() {
    }

    private static int lYearDays(int y) {
        int sum = 348;
        for (int i = 0x8000; i > 0x8; i >>= 1) {
            if ((LUNAR_INFO[y - 1900] & i) != 0) {
                sum++;
            }
        }
        return sum + leapDays(y);
    }

    private static int leapMonth(int y) {
        return (int) (LUNAR_INFO[y - 1900] & 0xf);
    }

    private static int leapDays(int y) {
        if (leapMonth(y) != 0) {
            return (LUNAR_INFO[y - 1900] & 0x10000) != 0 ? 30 : 29;
        }
        return 0;
    }

    private static int monthDays(int y, int m) {
        return (LUNAR_INFO[y - 1900] & (0x10000 >> m)) != 0 ? 30 : 29;
    }

    /**
     * 根据公历 Calendar 计算农历月日，格式如："九月廿二"、"正月初一"。
     */
    public static String getLunarDateString(Calendar cal) {
        Calendar baseCal = Calendar.getInstance();
        baseCal.set(1900, Calendar.JANUARY, 31, 0, 0, 0);
        baseCal.set(Calendar.MILLISECOND, 0);

        Calendar targetCal = (Calendar) cal.clone();
        targetCal.set(Calendar.HOUR_OF_DAY, 0);
        targetCal.set(Calendar.MINUTE, 0);
        targetCal.set(Calendar.SECOND, 0);
        targetCal.set(Calendar.MILLISECOND, 0);

        long diffMs = targetCal.getTimeInMillis() - baseCal.getTimeInMillis();
        long offset = diffMs / (24 * 60 * 60 * 1000L);

        int y = 1900;
        while (y < 2050 && offset > 0) {
            int temp = lYearDays(y);
            if (offset < temp) {
                break;
            }
            offset -= temp;
            y++;
        }

        int leap = leapMonth(y);
        boolean isLeap = false;
        int m = 1;
        int temp = 0;
        while (m <= 12 && offset > 0) {
            if (leap > 0 && m == (leap + 1) && !isLeap) {
                m--;
                isLeap = true;
                temp = leapDays(y);
            } else {
                temp = monthDays(y, m);
            }
            if (isLeap && m == (leap + 1)) {
                isLeap = false;
            }
            offset -= temp;
            if (!isLeap) {
                m++;
            }
        }

        if (offset == 0 && leap > 0 && m == leap + 1) {
            if (isLeap) {
                isLeap = false;
            } else {
                isLeap = true;
                m--;
            }
        }
        if (offset < 0) {
            offset += temp;
            m--;
        }

        int day = (int) offset + 1;

        StringBuilder sb = new StringBuilder();
        if (isLeap) {
            sb.append("闰");
        }
        if (m >= 1 && m <= 12) {
            sb.append(CHINESE_MONTHS[m - 1]).append("月");
        }

        if (day == 10) {
            sb.append("初十");
        } else if (day == 20) {
            sb.append("二十");
        } else if (day == 30) {
            sb.append("三十");
        } else {
            int ten = day / 10;
            int unit = day % 10;
            sb.append(CHINESE_DAYS_TEN[ten]);
            String[] digits = new String[]{"", "一", "二", "三", "四", "五", "六", "七", "八", "九"};
            if (unit > 0 && unit <= 9) {
                sb.append(digits[unit]);
            }
        }

        return sb.toString();
    }
}
