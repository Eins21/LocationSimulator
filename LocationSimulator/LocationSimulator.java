package com.LocationSimulator;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONObject;


public class LocationSimulator {

	public static void main(String[] args) throws Exception {
		List<LatLng> result = getLocations("13.0826846,80.2707516","10.7874654,78.89536029999999",20000);
		for(LatLng obj : result)
			System.out.println(obj.latitude+","+obj.longitude+","+"marker");
	}
	/**
	 * @param sourceLatLng- source latlng, destinationLatLng - destination latlng,distance - constant interval distance in meters
	 * @return coordinates in every n meters between source and destination 
	 * Get the source, destination coordinates and distance and return all the coordinates between source and destination at every n meters
	 **/
	public static List<LatLng> getLocations(String sourceLatLng, String destinationLatLng, int distance) throws Exception {

		String directionUrl = getDirectionsUrl(sourceLatLng, destinationLatLng);
		String directionsResponse = getDirectionsResponse(directionUrl);
		JSONObject directionsJO = new JSONObject(directionsResponse);
		List<LatLng> result = new DirectionsJSONParser().parse(directionsJO);
		result = getLocationsForEveryNMeters(result, distance);
		return result;
	}

	/**
	 * @param directionUrl
	 * @return google direction api response
	 **/
	private static String getDirectionsResponse(String directionUrl) throws IOException {
		String data = "";
		InputStream iStream = null;
		HttpURLConnection urlConnection = null;
		try {
			URL url = new URL(directionUrl);
			urlConnection = (HttpURLConnection) url.openConnection();
			urlConnection.connect();
			iStream = urlConnection.getInputStream();
			BufferedReader br = new BufferedReader(new InputStreamReader(iStream));
			StringBuffer sb = new StringBuffer();
			String line = "";
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			data = sb.toString();
			br.close();

		} catch (Exception e) {
		} finally {
			iStream.close();
			urlConnection.disconnect();
		}
		return data;
	}

	/**
	 * @param origin,dest
	 * @return google direction url end point 
	 * Get the source and destination locations and return the google direction url
	 **/
	private static String getDirectionsUrl(String origin, String dest) {

		String str_origin = "origin=" + origin;
		String str_dest = "destination=" + dest;
		String key = "key=" + "AIzaSyAEQvKUVouPDENLkQlCF6AAap1Ze-6zMos";
		String parameters = str_origin + "&" + str_dest + "&" + key;
		String output = "json";
		String url = "https://maps.googleapis.com/maps/api/directions/" + output + "?" + parameters;
		return url;
	}

	/**
	 * @param path
	 *            - all the coordinates between source and destination, distance - constant interval distance in meters
	 * @return coordinates in latitude and longitude form at every n meters return the every coordinates in every n meters
	 **/
	private static List<LatLng> getLocationsForEveryNMeters(List<LatLng> path, double distance) {

		List<LatLng> res = new ArrayList();
		LatLng p0 = path.get(0);
		res.add(p0);
		if (path.size() > 2) {
			double tmp = 0;
			LatLng prev = p0;
			for (LatLng p : path) {
				tmp += computeDistanceBetween(prev, p);
				if (tmp < distance) {
					prev = p;
					continue;
				} else {
					double diff = tmp - distance;
					double heading = computeHeading(prev, p);

					LatLng pp = computeOffsetOrigin(p, diff, heading);
					tmp = 0;
					prev = pp;
					res.add(pp);
					continue;
				}
			}
			LatLng plast = path.get(path.size() - 1);
			res.add(plast);
		}
		return res;
	}

	private static double computeHeading(LatLng from, LatLng to) {
		double fromLat = Math.toRadians(from.latitude);
		double fromLng = Math.toRadians(from.longitude);
		double toLat = Math.toRadians(to.latitude);
		double toLng = Math.toRadians(to.longitude);
		double dLng = toLng - fromLng;
		double heading = Math.atan2(Math.sin(dLng) * Math.cos(toLat), Math.cos(fromLat) * Math.sin(toLat) - Math.sin(fromLat) * Math.cos(toLat) * Math.cos(dLng));
		return heading;
	}

	private static LatLng computeOffsetOrigin(LatLng to, double distance, double heading) {
		heading = Math.toRadians(heading);
		distance /= 6371000;
		double n1 = Math.cos(distance);
		double n2 = Math.sin(distance) * Math.cos(heading);
		double n3 = Math.sin(distance) * Math.sin(heading);
		double n4 = Math.sin(Math.toRadians(to.latitude));
		double n12 = n1 * n1;
		double discriminant = n2 * n2 * n12 + n12 * n12 - n12 * n4 * n4;
		if (discriminant < 0) {
			return null;
		}
		double b = n2 * n4 + Math.sqrt(discriminant);
		b /= n1 * n1 + n2 * n2;
		double a = (n4 - n2 * b) / n1;
		double fromLatRadians = Math.atan2(a, b);
		if (fromLatRadians < -Math.PI / 2 || fromLatRadians > Math.PI / 2) {
			b = n2 * n4 - Math.sqrt(discriminant);
			b /= n1 * n1 + n2 * n2;
			fromLatRadians = Math.atan2(a, b);
		}
		if (fromLatRadians < -Math.PI / 2 || fromLatRadians > Math.PI / 2) {
			return null;
		}
		double fromLngRadians = Math.toRadians(to.longitude) - Math.atan2(n3, n1 * Math.cos(fromLatRadians) - n2 * Math.sin(fromLatRadians));
		return new LatLng(Math.toDegrees(fromLatRadians), Math.toDegrees(fromLngRadians));
	}

	private static double computeDistanceBetween(LatLng latLng1, LatLng latLng2) {
		double lat1 = latLng1.latitude, lat2 = latLng2.latitude;
		double lng1 = latLng1.longitude, lng2 = latLng2.longitude;
		double a = (lat1 - lat2) * distPerLat(lat1);
		double b = (lng1 - lng2) * distPerLng(lat1);
		return Math.sqrt(a * a + b * b);
	}

	private static double distPerLng(double lat) {
		return 0.0003121092 * Math.pow(lat, 4) + 0.0101182384 * Math.pow(lat, 3) - 17.2385140059 * lat * lat + 5.5485277537 * lat + 111301.967182595;
	}

	private static double distPerLat(double lat) {
		return -0.000000487305676 * Math.pow(lat, 4) - 0.0033668574 * Math.pow(lat, 3) + 0.4601181791 * lat * lat - 1.4558127346 * lat + 110579.25662316;
	}
}
