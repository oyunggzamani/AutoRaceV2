package com.autoracev2.vehicles;

import com.autoracev2.vehicles.api.OwnerRef;
import com.autoracev2.vehicles.api.VehiclePose;
import com.autoracev2.vehicles.api.VehicleRecord;
import com.autoracev2.vehicles.store.OwnerVehicleMap;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OwnerVehicleMapTest {
    private static final String OVERWORLD = "minecraft:overworld";

    @Test
    void addMapsOwnerToVehicle() {
        OwnerVehicleMap map = new OwnerVehicleMap();
        VehicleRecord first = record("Ali", 1, "iv-1");
        map.put(first);
        assertEquals(first.vehicleId(), map.get("Ali").orElseThrow().vehicleId());
        assertEquals(1, map.size());
    }

    @Test
    void replaceLeavesOneActiveVehicle() {
        OwnerVehicleMap map = new OwnerVehicleMap();
        VehicleRecord first = record("Ali", 1, "iv-old");
        VehicleRecord second = record("Ali", 4, "iv-new");
        map.put(first);
        VehicleRecord previous = map.put(second);
        assertEquals(first.vehicleId(), previous.vehicleId());
        assertEquals(second.vehicleId(), map.get("Ali").orElseThrow().vehicleId());
        assertEquals("iv-new", map.get("Ali").orElseThrow().ivUniqueId());
        assertEquals(1, map.size());
    }

    @Test
    void ownersDoNotCross() {
        OwnerVehicleMap map = new OwnerVehicleMap();
        map.put(record("Ali", 1, "iv-ali"));
        map.put(record("Furkan", 2, "iv-furkan"));
        assertEquals("iv-ali", map.get("Ali").orElseThrow().ivUniqueId());
        assertEquals("iv-furkan", map.get("Furkan").orElseThrow().ivUniqueId());
        assertEquals(2, map.size());
    }

    @Test
    void removeClearsOwnerMapping() {
        OwnerVehicleMap map = new OwnerVehicleMap();
        map.put(record("Ali", 1, "iv-1"));
        assertTrue(map.remove("Ali").isPresent());
        assertTrue(map.get("Ali").isEmpty());
        assertTrue(map.remove("Ali").isEmpty());
    }

    @Test
    void unknownOwnerIsEmpty() {
        OwnerVehicleMap map = new OwnerVehicleMap();
        assertTrue(map.get("missing").isEmpty());
        assertTrue(map.get((OwnerRef) null).isEmpty());
        assertTrue(map.remove("missing").isEmpty());
    }

    private static VehicleRecord record(String owner, int tier, String iv) {
        return VehicleRecord.active(OwnerRef.named(owner), tier, iv, new VehiclePose(OVERWORLD, 1, 64, 1, 90, 0));
    }
}
