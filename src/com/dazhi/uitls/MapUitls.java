package com.dazhi.uitls;

public class MapUitls {
	/**
	 * google maps的脚本里代码
	 */
	private static double EARTH_RADIUS = 6378.137;

	private static double rad(double d) {
		return d * Math.PI / 180.0;
	}

	/**
	 * 根据两点间经纬度坐标（double值），计算两点间距离，单位为米
	 */
	public static double getDistance(double lat1, double lng1, double lat2,
			double lng2) {
		double radLat1 = rad(lat1);
		double radLat2 = rad(lat2);
		double a = radLat1 - radLat2;
		double b = rad(lng1) - rad(lng2);
		double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2)
				+ Math.cos(radLat1) * Math.cos(radLat2)
				* Math.pow(Math.sin(b / 2), 2)));
		s = s * EARTH_RADIUS;
		s = Math.round(s * 1000);
		return s;
	}

	/**
	 * 步行速度，单位km/h,单位换算成m/min;(1km/h = 50/3(m/min))
	 */
	public static final int RATE_WALK = 5;
	public static final int RATE_BUS = 40;
	public static final int RATE_DRIVER = 60;

	/**
	 * 根据速度距离（km）,计算步行，驾车，公交花费的时间
	 * 
	 * @param distance 单位m
	 * @param mode
	 * @return
	 */
	public static int getTime(Double distance, int mode) {
		double rate = 3f / 50;
		int time = 0;
		switch (mode) {
		case RATE_WALK:
		case RATE_BUS:
		case RATE_DRIVER:
			time = (int) (distance / mode * rate);
			break;
		default:
			throw new IllegalArgumentException("参数输入有误");
		}
		return time;
	}
}
