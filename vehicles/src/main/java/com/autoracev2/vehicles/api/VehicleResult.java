package com.autoracev2.vehicles.api;

import java.util.Optional;

public final class VehicleResult {
    private final boolean success;
    private final String message;
    private final VehicleRecord vehicle;
    private final VehicleRecord previous;

    private VehicleResult(boolean success, String message, VehicleRecord vehicle, VehicleRecord previous) {
        this.success = success;
        this.message = message == null ? "" : message;
        this.vehicle = vehicle;
        this.previous = previous;
    }

    public static VehicleResult ok(String message, VehicleRecord vehicle) {
        return new VehicleResult(true, message, vehicle, null);
    }

    public static VehicleResult replaced(String message, VehicleRecord vehicle, VehicleRecord previous) {
        return new VehicleResult(true, message, vehicle, previous);
    }

    public static VehicleResult fail(String message) {
        return new VehicleResult(false, message, null, null);
    }

    public boolean success() {
        return success;
    }

    public String message() {
        return message;
    }

    public Optional<VehicleRecord> vehicle() {
        return Optional.ofNullable(vehicle);
    }

    public Optional<VehicleRecord> previous() {
        return Optional.ofNullable(previous);
    }
}
