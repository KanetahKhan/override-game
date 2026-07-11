package com.override.prototype;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;
import com.almasb.fxgl.physics.BoundingShape;
import com.almasb.fxgl.physics.HitBox;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

public final class SilentClassroomFactory
        implements EntityFactory {

    @Spawns("Wall")
    public Entity createWall(SpawnData data) {
        double width = readNumber(
                data,
                "width",
                32
        );

        double height = readNumber(
                data,
                "height",
                32
        );

        return entityBuilder(data)
                .type(SilentClassroomType.WALL)
                .bbox(
                        new HitBox(
                                BoundingShape.box(
                                        width,
                                        height
                                )
                        )
                )
                .collidable()
                .build();
    }

    @Spawns("Spawn")
    public Entity createSpawn(SpawnData data) {
        return createMarker(data);
    }

    @Spawns("Arcade")
    public Entity createArcade(SpawnData data) {
        return createMarker(data);
    }

    @Spawns("Dialogue")
    public Entity createDialogue(SpawnData data) {
        return createMarker(data);
    }

    @Spawns("Exit")
    public Entity createExit(SpawnData data) {
        return createMarker(data);
    }

    @Spawns("Transition")
    public Entity createTransition(SpawnData data) {
        return createMarker(data);
    }

    @Spawns("EnemySpawn")
    public Entity createEnemySpawn(SpawnData data) {
        return createMarker(data);
    }

    @Spawns("PatrolPath")
    public Entity createPatrolPath(SpawnData data) {
        return createMarker(data);
    }

    @Spawns("RoomZone")
    public Entity createRoomZone(SpawnData data) {
        return createMarker(data);
    }

    @Spawns("Light")
    public Entity createLight(SpawnData data) {
        return createMarker(data);
    }

    private Entity createMarker(SpawnData data) {
        return entityBuilder(data).build();
    }

    private double readNumber(
            SpawnData data,
            String key,
            double defaultValue
    ) {
        try {
            Object value = data.get(key);

            if (value instanceof Number number) {
                return number.doubleValue();
            }
        } catch (Exception ignored) {
            // Use the fallback value below.
        }

        return defaultValue;
    }
}