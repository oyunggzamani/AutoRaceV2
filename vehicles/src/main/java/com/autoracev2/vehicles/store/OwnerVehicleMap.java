package com.autoracev2.vehicles.store;

import com.autoracev2.vehicles.api.OwnerRef;
import com.autoracev2.vehicles.api.VehicleRecord;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * One active AutoRaceV2 vehicle per owner username.
 * Replacement overwrites the previous mapping in a single put.
 */
public final class OwnerVehicleMap {
    private final Map<String, VehicleRecord> byOwner = new LinkedHashMap<>();

    public Optional<VehicleRecord> get(OwnerRef owner) {
        if (owner == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byOwner.get(owner.username()));
    }

    public Optional<VehicleRecord> get(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byOwner.get(username.trim()));
    }

    public VehicleRecord put(VehicleRecord record) {
        if (record == null || !record.isActive()) {
            throw new IllegalArgumentException("only an active vehicle can occupy the owner map");
        }
        return byOwner.put(record.owner().username(), record);
    }

    public Optional<VehicleRecord> remove(OwnerRef owner) {
        if (owner == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(byOwner.remove(owner.username()));
    }

    public Optional<VehicleRecord> remove(String username) {
        if (username == null || username.isBlank()) {
            return Optional.empty();
        }
        return Optional.ofNullable(byOwner.remove(username.trim()));
    }

    public List<VehicleRecord> all() {
        return List.copyOf(byOwner.values());
    }

    public int size() {
        return byOwner.size();
    }

    public boolean isEmpty() {
        return byOwner.isEmpty();
    }

    public List<VehicleRecord> clear() {
        List<VehicleRecord> removed = new ArrayList<>(byOwner.values());
        byOwner.clear();
        return List.copyOf(removed);
    }
}
