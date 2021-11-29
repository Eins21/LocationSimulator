package com.LocationSimulator;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DirectionsJSONParser {

	/**
	 * @param directionsJO
	 * @return all the coordinates in latitude and longitude form
	 * Parse the google direction api response and returns all the coordinates
	**/
	public List<LatLng> parse(JSONObject directionsJO) {

		List<LatLng> result = new ArrayList<LatLng>();
		try {
			JSONArray routesArr = directionsJO.optJSONArray("routes");
			for (int i = 0; i < routesArr.length(); i++) {
				JSONArray legsArr = ((JSONObject) routesArr.get(i)).optJSONArray("legs");
				for (int j = 0; j < legsArr.length(); j++) {
					JSONArray stepsArr = ((JSONObject) legsArr.get(j)).getJSONArray("steps");
					for (int k = 0; k < stepsArr.length(); k++) {
						String polyline = "";
						polyline = (String) ((JSONObject) ((JSONObject) stepsArr.get(k)).get("polyline")).get("points");
						List<LatLng> list = decodePolyLine(polyline);
						for (int l = 0; l < list.size(); l++) {
							result.add(list.get(l));
						}
					}
				}
			}

		} catch (Exception e) {
			e.printStackTrace();
		}
		return result;
	}
    
	/**
	 * @param encodedPolyLine
	 * @return coordinates in latitude and longitude form
	 * Decode the encoded polyline and return the coordinates
	**/
	private List<LatLng> decodePolyLine(String encodedPolyLine) {

		List<LatLng> poly = new ArrayList<LatLng>();
		int index = 0, len = encodedPolyLine.length();
		int lat = 0, lng = 0;

		while (index < len) {
			int b, shift = 0, result = 0;
			do {
				b = encodedPolyLine.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lat += dlat;

			shift = 0;
			result = 0;
			do {
				b = encodedPolyLine.charAt(index++) - 63;
				result |= (b & 0x1f) << shift;
				shift += 5;
			} while (b >= 0x20);
			int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
			lng += dlng;

			LatLng p = new LatLng((((double) lat / 1E5)), (((double) lng / 1E5)));
			poly.add(p);
		}

		return poly;
	}
}
