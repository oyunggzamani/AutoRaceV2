package com.autoracev2.vehicles;

import com.autoracev2.vehicles.api.TierDefinition;
import com.autoracev2.vehicles.registry.VehicleTierRegistry;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VehicleTierRegistryTest {
    private final VehicleTierRegistry registry = new VehicleTierRegistry();

    @Test
    void containsExactlySevenTiers() {
        assertEquals(7, registry.size());
        assertEquals(7, registry.all().size());
    }

    @Test
    void thresholdsAreLocked() {
        assertEquals(List.of(0, 10, 30, 100, 500, 1000, 2150), registry.thresholds());
    }

    @Test
    void lockedDisplayNamesAndPackIds() {
        assertEquals("ZAZ 1102", registry.byId(1).orElseThrow().displayName());
        assertEquals("blockyragecivil:brzaz1102", registry.byId(1).orElseThrow().packVehicleId());
        assertEquals("Fiat Tipo", registry.byId(2).orElseThrow().displayName());
        assertEquals("blockyragecivil:brfiattipo", registry.byId(2).orElseThrow().packVehicleId());
        assertEquals("Mercedes W124", registry.byId(3).orElseThrow().displayName());
        assertEquals("blockyragecivil:brmercedesw124", registry.byId(3).orElseThrow().packVehicleId());
        assertEquals("Nissan GT-R R35", registry.byId(4).orElseThrow().displayName());
        assertEquals("blockyragecivil:brnissangtrr35", registry.byId(4).orElseThrow().packVehicleId());
        assertEquals("Nissan GT-R R35 Nismo", registry.byId(5).orElseThrow().displayName());
        assertEquals("blockyragecivil:brnissangtrr35_2015_nismo", registry.byId(5).orElseThrow().packVehicleId());
        assertEquals("Bugatti La Voiture Noire", registry.byId(6).orElseThrow().displayName());
        assertEquals("blockyragecivil:brbugattilvn", registry.byId(6).orElseThrow().packVehicleId());
        TierDefinition tuned = registry.byId(7).orElseThrow();
        assertEquals("Nissan GT-R R35 + Tuned Engine", tuned.displayName());
        assertEquals("blockyragecivil:brnissangtrr35", tuned.packVehicleId());
        assertEquals("blockyrageaftermarket:brnissangtrr35enginetuned", tuned.engineOverride().orElseThrow().packPartId());
        assertEquals(41500, tuned.engineOverride().orElseThrow().maxRpm());
    }

    @Test
    void invalidTierIsEmpty() {
        assertTrue(registry.byId(0).isEmpty());
        assertTrue(registry.byId(8).isEmpty());
        assertTrue(registry.byId(-1).isEmpty());
    }
}
