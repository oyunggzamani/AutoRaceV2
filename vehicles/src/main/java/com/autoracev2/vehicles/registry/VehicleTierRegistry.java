package com.autoracev2.vehicles.registry;

import com.autoracev2.vehicles.api.EngineOverride;
import com.autoracev2.vehicles.api.TierDefinition;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Locked AutoRaceV2 1.0.0 tiers. Thresholds are data only; this class does not
 * accumulate coins. Pack IDs come from R.A.G.E V2.3 {@code blockyragecivil}
 * / {@code blockyrageaftermarket} jsondefs.
 */
public final class VehicleTierRegistry {
    public static final String CIVIL_PACK = "blockyragecivil";
    public static final String AFTERMARKET_PACK = "blockyrageaftermarket";

    private static final VehicleTierRegistry INSTANCE = new VehicleTierRegistry();

    private final List<TierDefinition> tiers;

    public VehicleTierRegistry() {
        this.tiers = List.of(
                tier(1, "ZAZ 1102", 0, "brzaz1102", null),
                tier(2, "Fiat Tipo", 10, "brfiattipo", null),
                tier(3, "Mercedes W124", 30, "brmercedesw124", null),
                tier(4, "Nissan GT-R R35", 100, "brnissangtrr35", null),
                tier(5, "Nissan GT-R R35 Nismo", 500, "brnissangtrr35_2015_nismo", null),
                tier(6, "Bugatti La Voiture Noire", 1000, "brbugattilvn", null),
                tier(7, "Nissan GT-R R35 + Tuned Engine", 2150, "brnissangtrr35",
                        new EngineOverride(AFTERMARKET_PACK, "brnissangtrr35enginetuned", 41500))
        );
    }

    public static VehicleTierRegistry instance() {
        return INSTANCE;
    }

    public List<TierDefinition> all() {
        return tiers;
    }

    public int size() {
        return tiers.size();
    }

    public Optional<TierDefinition> byId(int tierId) {
        return tiers.stream().filter(tier -> tier.id() == tierId).findFirst();
    }

    /**
     * Highest locked tier whose threshold is {@code <= value}.
     * Negative values are not a coin amount and return empty.
     */
    public Optional<TierDefinition> byThreshold(long value) {
        if (value < 0) {
            return Optional.empty();
        }
        return tiers.stream()
                .filter(tier -> tier.threshold() <= value)
                .max(Comparator.comparingInt(TierDefinition::threshold));
    }

    public List<Integer> thresholds() {
        return tiers.stream().map(TierDefinition::threshold).collect(Collectors.toList());
    }

    private static TierDefinition tier(int id, String name, int threshold, String systemName, EngineOverride engine) {
        return new TierDefinition(id, name, threshold, CIVIL_PACK, systemName, engine);
    }
}
