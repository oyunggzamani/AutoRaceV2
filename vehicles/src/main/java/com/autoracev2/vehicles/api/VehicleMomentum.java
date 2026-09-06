package com.autoracev2.vehicles.api;

/**
 * Reserved motion snapshot. Vehicles 1.0.0 preserves pose only.
 * Later modules can fill this without changing spawn/swap ownership.
 */
public final class VehicleMomentum {
    public static final VehicleMomentum NONE = new VehicleMomentum();

    private VehicleMomentum() {
    }

    public boolean isPreserved() {
        return false;
    }

    @Override
    public String toString() {
        return "VehicleMomentum{none}";
    }
}
