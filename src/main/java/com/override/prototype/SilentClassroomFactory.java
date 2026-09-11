package com.override.prototype;

import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.entity.EntityFactory;
import com.almasb.fxgl.entity.SpawnData;
import com.almasb.fxgl.entity.Spawns;

import static com.almasb.fxgl.dsl.FXGL.entityBuilder;

public final class SilentClassroomFactory
        implements EntityFactory {

    @Spawns("Wall")
    public Entity createWall(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.WALL
        );
    }

    @Spawns("Spawn")
    public Entity createSpawn(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.SPAWN
        );
    }

    @Spawns("Door")
    public Entity createDoor(
            SpawnData data
    ) {
        /*
         * Door graphics and collision are handled by
         * SilentClassroomPrototypeApp.
         */
        return createMarker(
                data,
                SilentClassroomType.DOOR_MARKER
        );
    }

    @Spawns("Exit")
    public Entity createExit(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.EXIT
        );
    }

    @Spawns("Dialogue")
    public Entity createDialogue(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.DIALOGUE
        );
    }

    @Spawns("RoomZone")
    public Entity createRoomZone(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.ROOM_ZONE
        );
    }

    @Spawns("Light")
    public Entity createLight(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.LIGHT
        );
    }

    @Spawns("Arcade")
    public Entity createArcade(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.ARCADE
        );
    }

    @Spawns("ArcadeSpawn")
    public Entity createArcadeSpawn(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.ARCADE_SPAWN
        );
    }

    @Spawns("Transition")
    public Entity createTransition(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.TRANSITION
        );
    }

    @Spawns("EnemySpawn")
    public Entity createEnemySpawn(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.ENEMY_SPAWN
        );
    }

    @Spawns("PatrolPath")
    public Entity createPatrolPath(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.PATROL_PATH
        );
    }

    @Spawns("Breaker")
    public Entity createBreaker(
            SpawnData data
    ) {
        return createMarker(
                data,
                SilentClassroomType.BREAKER
        );
    }

    private Entity createMarker(
            SpawnData data,
            SilentClassroomType type
    ) {
        return entityBuilder(data)
                .type(type)
                .build();
    }
}
