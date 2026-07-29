package com.limou.movieticket.ticketing.api;

import java.math.BigDecimal;
import java.util.List;

public record CinemaSummary(String id, String name, String brand, String city, String district,
                            String address, BigDecimal latitude, BigDecimal longitude,
                            List<String> serviceTags, List<String> hallTypes,
                            Integer minimumPrice, Double distanceKm) { }
