package com.wanderwildwood.tancho;

import java.util.ArrayList;
import java.util.List;

public class BirdObservation {
    private int id;

    /**
     * Every row this one stands for.
     *
     * With "Every repeat" off, a run of the same bird collapses to its best reading and the
     * rest are left out of the list but not out of the database. A row removed from the log
     * has to take its whole run with it, or the next one along would surface in its place
     * and the removal would look like it had not worked.
     */
    private final List<Integer> coveredIds = new ArrayList<>();

    public List<Integer> getCoveredIds() {
        return coveredIds;
    }

    public void cover(int otherId) {
        if (!coveredIds.contains(otherId)) coveredIds.add(otherId);
    }

    public void coverAll(List<Integer> others) {
        for (int other : others) cover(other);
    }
    private long millis;
    private float latitude;
    private float longitude;
    private String name;
    private int speciesId;
    private float probability;

    public BirdObservation(int id, long millis, float latitude, float longitude, String name, int speciesId, float probability) {
        this.id = id;
        this.millis = millis;
        this.latitude = latitude;
        this.longitude = longitude;
        this.name = name;
        this.speciesId = speciesId;
        this.probability = probability;
    }

    public BirdObservation() {

    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        cover(id);
        this.id = id;
    }

    public long getMillis() {
        return millis;
    }

    public void setMillis(long millis) {
        this.millis = millis;
    }

    public float getLatitude() {
        return latitude;
    }

    public void setLatitude(float latitude) {
        this.latitude = latitude;
    }

    public float getLongitude() {
        return longitude;
    }

    public void setLongitude(float longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public int getSpeciesId() {
        return speciesId;
    }

    public void setSpeciesId(int speciesId) {
        this.speciesId = speciesId;
    }

    public float getProbability() {
        return probability;
    }

    public void setProbability(float probability) {
        this.probability = probability;
    }
}