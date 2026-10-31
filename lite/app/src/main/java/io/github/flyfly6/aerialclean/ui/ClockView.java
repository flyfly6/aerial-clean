package io.github.flyfly6.aerialclean.ui;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Handler;
import android.os.Looper;
import android.util.AttributeSet;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import java.util.Calendar;

import io.github.flyfly6.aerialclean.R;
import io.github.flyfly6.aerialclean.weather.LunarCalendar;

/**
 * 顶栏大时钟与状态区域（参考 aerial-tv 经典三行排版）：
 * 1. 第一行：日期 + 星期几 + 农历 (如 "10月31日 星期六 九月廿二")
 * 2. 第二行：时间 (84sp Outfit 现代字体)
 * 3. 第三行：天气图标 + 温度 + 位置 + 天气 + 温度 (如 "☀ 7°C 北京 晴 7°C")
 */
public class ClockView extends LinearLayout {

    private static final long TICK_MS = 30_000L;
    private static final String[] WEEKS = new String[]{"", "周日", "周一", "周二", "周三", "周四", "周五", "周六"};

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            updateDateAndWeather();
            handler.postDelayed(this, TICK_MS);
        }
    };

    private TextView dateLunarView;
    private TextClock timeView;
    private TextView weatherView;
    private String weatherText = "";

    public ClockView(Context context, AttributeSet attrs) {
        super(context, attrs);
        setOrientation(VERTICAL);
    }

    @Override
    protected void onFinishInflate() {
        super.onFinishInflate();
        dateLunarView = (TextView) findViewById(R.id.clock_date_lunar);
        timeView = (TextClock) findViewById(R.id.clock_time);
        weatherView = (TextView) findViewById(R.id.clock_weather);

        try {
            Typeface outfit = Typeface.createFromAsset(getContext().getAssets(), "fonts/outfit.ttf");
            if (outfit != null && timeView != null) {
                timeView.setTypeface(outfit);
            }
        } catch (Exception ignored) {
        }
        updateDateAndWeather();
    }

    public void setWeatherText(String weather) {
        this.weatherText = weather != null ? weather.trim() : "";
        updateDateAndWeather();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        updateDateAndWeather();
        handler.removeCallbacks(tick);
        handler.postDelayed(tick, TICK_MS);
    }

    @Override
    protected void onDetachedFromWindow() {
        handler.removeCallbacks(tick);
        super.onDetachedFromWindow();
    }

    private void updateDateAndWeather() {
        Calendar cal = Calendar.getInstance();
        int month = cal.get(Calendar.MONTH) + 1;
        int day = cal.get(Calendar.DAY_OF_MONTH);
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        String weekName = (dayOfWeek >= 1 && dayOfWeek <= 7) ? WEEKS[dayOfWeek] : "";
        String lunarStr = LunarCalendar.getLunarDateString(cal);

        // 第一行：公历日期 + 星期 + 农历
        if (dateLunarView != null) {
            dateLunarView.setText(month + "月" + day + "日 " + weekName + " " + lunarStr);
        }

        // 第三行：天气行
        if (weatherView != null) {
            if (weatherText.length() > 0) {
                weatherView.setText(weatherText);
                weatherView.setVisibility(VISIBLE);
            } else {
                weatherView.setVisibility(GONE);
            }
        }
    }
}
