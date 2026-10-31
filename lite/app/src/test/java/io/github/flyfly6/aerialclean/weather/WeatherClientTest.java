package io.github.flyfly6.aerialclean.weather;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WeatherClientTest {

    @Test
    public void testWeatherDescriptions() {
        assertEquals("晴", WeatherClient.getWeatherDescription(0));
        assertEquals("晴间多云", WeatherClient.getWeatherDescription(1));
        assertEquals("晴间多云", WeatherClient.getWeatherDescription(2));
        assertEquals("多云", WeatherClient.getWeatherDescription(3));
        assertEquals("雾", WeatherClient.getWeatherDescription(45));
        assertEquals("雨", WeatherClient.getWeatherDescription(61));
        assertEquals("雪", WeatherClient.getWeatherDescription(71));
        assertEquals("雷阵雨", WeatherClient.getWeatherDescription(95));
        assertEquals("晴", WeatherClient.getWeatherDescription(999));
    }
}
