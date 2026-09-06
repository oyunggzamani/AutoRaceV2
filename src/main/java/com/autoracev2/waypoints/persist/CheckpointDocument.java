package com.autoracev2.waypoints.persist;

import com.autoracev2.waypoints.api.Checkpoint;
import com.autoracev2.waypoints.api.WorldPosition;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

final class CheckpointDocument {
    private final List<IndexedPosition> entries;
    private final WorldPosition start;
    private final WorldPosition finish;

    CheckpointDocument(List<IndexedPosition> entries, WorldPosition start, WorldPosition finish) {
        this.entries = new ArrayList<>(entries);
        this.entries.sort(Comparator.comparingInt(IndexedPosition::id));
        this.start = start;
        this.finish = finish;
    }

    static CheckpointDocument fromStore(List<Checkpoint> checkpoints, WorldPosition start, WorldPosition finish) {
        List<IndexedPosition> entries = new ArrayList<>();
        for (Checkpoint checkpoint : checkpoints) {
            entries.add(new IndexedPosition(checkpoint.id(), checkpoint.position()));
        }
        return new CheckpointDocument(entries, start, finish);
    }

    List<WorldPosition> orderedPositions() {
        List<WorldPosition> positions = new ArrayList<>(entries.size());
        for (IndexedPosition entry : entries) {
            positions.add(entry.position());
        }
        return positions;
    }

    WorldPosition start() {
        return start;
    }

    WorldPosition finish() {
        return finish;
    }

    List<IndexedPosition> entries() {
        return List.copyOf(entries);
    }
}
