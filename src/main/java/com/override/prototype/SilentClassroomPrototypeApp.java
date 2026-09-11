package com.override.prototype;

import com.almasb.fxgl.app.GameApplication;
import com.almasb.fxgl.app.GameSettings;
import com.almasb.fxgl.entity.Entity;
import com.almasb.fxgl.input.UserAction;
import com.override.game.minigames.ChiptuneSfx;
import com.override.game.minigames.MiniGameResult;
import javafx.geometry.Point2D;
import javafx.geometry.Rectangle2D;
import javafx.scene.Group;
import javafx.scene.image.ImageView;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Polygon;
import javafx.scene.shape.Rectangle;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static com.almasb.fxgl.dsl.FXGL.*;

public final class SilentClassroomPrototypeApp
        extends GameApplication {

    private static final int VIEW_WIDTH = 1280;
    private static final int VIEW_HEIGHT = 720;

    private static final double PLAYER_SIZE = 24;
    private static final double PLAYER_SPRITE_SIZE = 40;
    private static final double PLAYER_SPRITE_OFFSET_X =
            (PLAYER_SIZE - PLAYER_SPRITE_SIZE) / 2.0;
    private static final double PLAYER_SPRITE_BASE_OFFSET_Y =
            PLAYER_SIZE - PLAYER_SPRITE_SIZE;
    private static final double PLAYER_WALK_SPEED = 150;
    private static final double PLAYER_SPRINT_SPEED = 210;
    private static final double MAX_STAMINA_SECONDS = 3.0;
    private static final double STAMINA_RECOVERY_PER_SECOND = 0.75;

    private static final double INTERACTION_DISTANCE = 105;
    private static final double SENTINEL_SIZE = 28;
    private static final double SENTINEL_CORRIDOR_MIN_X = 976;
    private static final double SENTINEL_CORRIDOR_MAX_X = 1320;
    private static final double CAPTURE_DISTANCE = 30;

    private static final String CORRIDOR_MAP =
            "academic-building-2-floor-2-v2.tmx";


    private static final String DEFAULT_SPAWN =
            "PlayerSpawn";

    private static final boolean SHOW_COLLISION_DEBUG =
            false;

    private final List<Rectangle2D> collisionBoxes =
            new ArrayList<>();

    private final List<DoorRuntime> doors =
            new ArrayList<>();

    private final List<RoomRuntime> rooms =
            new ArrayList<>();

    private final List<PointRuntime> arcades =
            new ArrayList<>();

    private final Map<String, String> arcadeSlotAssignments =
            randomizeArcadeSlots();

    private final List<PointRuntime> dialogues =
            new ArrayList<>();

    private final List<PointRuntime> exits =
            new ArrayList<>();

    private final List<PointRuntime> breakers =
            new ArrayList<>();

    private final List<LightRuntime> lights =
            new ArrayList<>();

    private final List<Point2D> patrolPoints =
            new ArrayList<>();

    private final Set<KeyCode> heldMovementKeys =
            EnumSet.noneOf(KeyCode.class);

    private final SilentClassroomSession session;
    private final SilentClassroomHost host;
    private final SilentClassroomPreferences preferences;
    private final SentinelController sentinelController =
            new SentinelController();

    private Entity player;
    private Entity sentinel;
    private Entity visionConeEntity;
    private Polygon visionCone;
    private Group playerView;
    private ImageView playerSprite;
    private String playerDirection = "south";
    private Group sentinelView;
    private Point2D sentinelSpawn = new Point2D(1136, 720);
    private boolean mapHasSentinel;

    private String currentMapFile;
    private String currentMapDisplayName;

    private int mapPixelWidth;
    private int mapPixelHeight;

    private Text roomHudText;
    private Text objectiveHudText;
    private Text threatHudText;
    private Text captionText;
    private Text interactionText;
    private Rectangle interactionBackground;

    private DoorRuntime nearbyDoor;
    private PointRuntime nearbyPoint;
    private RoomRuntime currentRoom;

    private Point2D lastSeenPlayer = Point2D.ZERO;
    private Point2D sentinelFacing = new Point2D(0, 1);
    private int patrolIndex;
    private double staminaSeconds = MAX_STAMINA_SECONDS;
    private double breakerStunSeconds;
    private double captionSeconds;
    private double animationTime;
    private double servoCueCooldown;
    private double chaseCueCooldown;
    private double appliedFontScale = -1;
    private boolean sprintHeld;
    private boolean tutorialComplete;
    private boolean modalOpen;
    private boolean breakerUsed;
    private boolean patrolClueRead;
    private boolean previousHidden;
    private boolean lockdownAnnounced;

    public SilentClassroomPrototypeApp() {
        this(new SilentClassroomSession(), null);
    }

    public SilentClassroomPrototypeApp(
            SilentClassroomSession session,
            SilentClassroomHost host
    ) {
        this.session = Objects.requireNonNull(session, "session");
        this.host = host;
        this.preferences = SilentClassroomPreferences.shared();
        this.tutorialComplete = host == null;
    }

    /**
     * Shuffles the three required game IDs onto three of the six
     * candidate hiding spots across the classroom and ICT lab room
     * maps, chosen fresh per instance so every attempt hides the
     * terminals in a different combination of rooms and spots.
     */
    private static Map<String, String> randomizeArcadeSlots() {
        List<String> classroomSlots = new ArrayList<>(
                List.of("classroom-1", "classroom-2", "classroom-3")
        );
        List<String> ictLabSlots = new ArrayList<>(
                List.of("ictlab-1", "ictlab-2", "ictlab-3")
        );
        Collections.shuffle(classroomSlots);
        Collections.shuffle(ictLabSlots);

        List<String> games = new ArrayList<>(
                SilentClassroomSession.REQUIRED_GAME_IDS
        );
        Collections.shuffle(games);

        /*
         * Split the three games 2-1 (or 1-2) between the two rooms so
         * a search never comes up completely empty in either one.
         */
        int classroomCount = 1 + (int) (Math.random() * 2);

        Map<String, String> assignment = new HashMap<>();
        for (int i = 0; i < classroomCount; i++) {
            assignment.put(classroomSlots.get(i), games.get(i));
        }
        for (int i = classroomCount; i < games.size(); i++) {
            assignment.put(ictLabSlots.get(i - classroomCount), games.get(i));
        }
        return assignment;
    }

    @Override
    protected void initSettings(
            GameSettings settings
    ) {
        settings.setWidth(VIEW_WIDTH);
        settings.setHeight(VIEW_HEIGHT);

        settings.setTitle(
                "Silent Classroom - Corridor Navigation"
        );

        settings.setVersion("1.2");

        settings.setIntroEnabled(false);
        settings.setMainMenuEnabled(false);
        settings.setGameMenuEnabled(false);

        settings.setManualResizeEnabled(true);
        settings.setPreserveResizeRatio(true);

        settings.setFullScreenAllowed(true);
        settings.setFullScreenFromStart(false);
    }

    @Override
    protected void initGame() {
        getGameScene().setBackgroundColor(
                Color.rgb(17, 21, 27)
        );

        getGameWorld().addEntityFactory(
                new SilentClassroomFactory()
        );

        /*
         * The game now starts in the corridor.
         * There are no number-key room shortcuts.
         */
        loadMap(
                CORRIDOR_MAP,
                DEFAULT_SPAWN
        );
    }

    @Override
    protected void initUI() {
        createRoomHud();
        createInteractionPrompt();

        updateRoomHud();
        hideInteractionPrompt();
        showCaption(
                tutorialComplete
                        ? "Complete all three terminals and reach Room 301."
                        : "ROBOT OFFLINE — find Professor Rahman in Room 302."
        );
    }

    private void createRoomHud() {
        Rectangle background = new Rectangle(
                1252,
                82,
                Color.rgb(0, 0, 0, 0.76)
        );

        background.setArcWidth(14);
        background.setArcHeight(14);

        roomHudText = new Text();
        roomHudText.setFill(Color.WHITE);
        roomHudText.setFont(
                Font.font(
                        "Consolas",
                        FontWeight.BOLD,
                        18
                )
        );

        objectiveHudText = new Text();
        objectiveHudText.setFill(
                Color.rgb(206, 220, 230)
        );
        objectiveHudText.setFont(
                Font.font("Consolas", 15)
        );

        threatHudText = new Text();
        threatHudText.setFill(
                Color.rgb(255, 204, 80)
        );
        threatHudText.setFont(
                Font.font(
                        "Consolas",
                        FontWeight.BOLD,
                        16
                )
        );

        captionText = new Text();
        captionText.setFill(Color.WHITE);
        captionText.setStroke(Color.BLACK);
        captionText.setStrokeWidth(0.6);
        captionText.setFont(
                Font.font(
                        "Consolas",
                        FontWeight.BOLD,
                        17
                )
        );

        addUINode(background, 14, 12);
        addUINode(roomHudText, 28, 41);
        addUINode(objectiveHudText, 28, 72);
        addUINode(threatHudText, 870, 41);
        addUINode(captionText, 300, 612);
    }

    private void createInteractionPrompt() {
        interactionBackground = new Rectangle(
                820,
                64,
                Color.rgb(0, 0, 0, 0.88)
        );

        interactionBackground.setArcWidth(18);
        interactionBackground.setArcHeight(18);
        interactionBackground.setStroke(
                Color.rgb(219, 188, 88)
        );
        interactionBackground.setStrokeWidth(2);

        interactionText = new Text();
        interactionText.setFill(Color.WHITE);
        interactionText.setFont(
                Font.font(
                        "Consolas",
                        FontWeight.BOLD,
                        18
                )
        );

        addUINode(
                interactionBackground,
                230,
                638
        );

        addUINode(
                interactionText,
                255,
                677
        );
    }

    @Override
    protected void initInput() {
        registerMovementKey(KeyCode.W);
        registerMovementKey(KeyCode.A);
        registerMovementKey(KeyCode.S);
        registerMovementKey(KeyCode.D);
        registerMovementKey(KeyCode.I);
        registerMovementKey(KeyCode.J);
        registerMovementKey(KeyCode.K);
        registerMovementKey(KeyCode.L);
        registerMovementKey(KeyCode.UP);
        registerMovementKey(KeyCode.LEFT);
        registerMovementKey(KeyCode.DOWN);
        registerMovementKey(KeyCode.RIGHT);

        registerSprintKey(KeyCode.SHIFT);
        registerSprintKey(KeyCode.CONTROL);
        registerInteractKey(KeyCode.E);
        registerInteractKey(KeyCode.F);
        registerInteractKey(KeyCode.SPACE);
        registerInteractKey(KeyCode.ENTER);
        onKeyDown(KeyCode.ESCAPE, this::requestPause);
    }

    @Override
    protected void onUpdate(double tpf) {
        double dt = Math.min(0.05, Math.max(0.0, tpf));
        session.synchronizeTimer();
        animationTime += dt;

        if (session.isLockdown()
                && !lockdownAnnounced) {
            lockdownAnnounced = true;
            showCaption(
                    "LOCKDOWN — time bonus lost; Sentinel speed increased 15%."
            );
            ChiptuneSfx.boss();
        }

        updatePlayerMovement(dt);

        if (checkDoorTransition()) {
            return;
        }

        RoomRuntime previousRoom = currentRoom;
        currentRoom = findCurrentRoom();
        boolean playerEnteredRoom =
                previousRoom == null && currentRoom != null;

        updateNearbyInteraction();
        updateInteractionPrompt();

        if (tutorialComplete && mapHasSentinel) {
            updateSentinel(dt, playerEnteredRoom);
        }

        updateWorldEffects(dt);
        updateRoomHud();
    }

    private void loadMap(
            String mapFile,
            String requestedSpawn
    ) {
        currentMapFile = mapFile;

        /*
         * FXGL replaces the previous level here.
         */
        setLevelFromMap(mapFile);

        loadMapData(
                mapFile,
                requestedSpawn
        );

        createRuntimeDoors();
        createPlayer();
        createSentinel();
        createWorldEffects();
        configureCamera();

        nearbyDoor = null;
        nearbyPoint = null;

        if (SHOW_COLLISION_DEBUG) {
            drawCollisionDebugBoxes();
        }

        updateRoomHud();
        hideInteractionPrompt();

        System.out.println("--------------------------------");
        System.out.println(
                "Loaded: " + currentMapDisplayName
        );
        System.out.println(
                "Map: " + currentMapFile
        );
        System.out.println(
                "Doors: " + doors.size()
        );
        System.out.println(
                "Collision boxes: "
                        + collisionBoxes.size()
        );
    }

    private void loadMapData(
            String mapFile,
            String requestedSpawn
    ) {
        collisionBoxes.clear();
        doors.clear();
        rooms.clear();
        arcades.clear();
        dialogues.clear();
        exits.clear();
        breakers.clear();
        lights.clear();
        patrolPoints.clear();
        patrolIndex = 0;
        mapHasSentinel = false;

        Document document =
                loadTmxDocument(mapFile);

        Element map =
                document.getDocumentElement();

        int columns =
                readIntAttribute(
                        map,
                        "width",
                        1
                );

        int rows =
                readIntAttribute(
                        map,
                        "height",
                        1
                );

        int tileWidth =
                readIntAttribute(
                        map,
                        "tilewidth",
                        32
                );

        int tileHeight =
                readIntAttribute(
                        map,
                        "tileheight",
                        32
                );

        mapPixelWidth =
                columns * tileWidth;

        mapPixelHeight =
                rows * tileHeight;

        currentMapDisplayName =
                readMapDisplayName(
                        document,
                        mapFile
                );

        loadCollisionObjects(document);
        loadDoorObjects(document);
        loadRoomObjects(document);
        loadPointObjects(document);
        loadPatrolPath(document);
        loadLightObjects(document);

        Point2D spawn =
                findSpawn(
                        document,
                        requestedSpawn
                );

        /*
         * Store the selected spawn temporarily.
         */
        selectedSpawn = spawn;
    }

    private Point2D selectedSpawn;

    private void createPlayer() {
        playerSprite = new ImageView(
                SilentClassroomPlayerAssets.idle(playerDirection)
        );
        playerSprite.setFitWidth(PLAYER_SPRITE_SIZE);
        playerSprite.setFitHeight(PLAYER_SPRITE_SIZE);
        playerSprite.setPreserveRatio(true);
        playerSprite.setSmooth(true);
        playerSprite.setTranslateX(PLAYER_SPRITE_OFFSET_X);
        playerSprite.setTranslateY(PLAYER_SPRITE_BASE_OFFSET_Y);

        playerView = new Group(playerSprite);

        player = entityBuilder()
                .at(
                        selectedSpawn.getX()
                                - PLAYER_SIZE / 2.0,
                        selectedSpawn.getY()
                                - PLAYER_SIZE / 2.0
                )
                .type(SilentClassroomType.PLAYER)
                .viewWithBBox(playerView)
                .zIndex(1200)
                .buildAndAttach();

        if (isTouchingMapCollision()) {
            throw new IllegalStateException(
                    "Spawn "
                            + selectedSpawn
                            + " overlaps collision in "
                            + currentMapFile
                            + "."
            );
        }
    }

    private void createRuntimeDoors() {
        for (DoorRuntime door : doors) {
            door.createEntity();
            door.refreshVisual();
        }
    }

    private void createSentinel() {
        visionCone = new Polygon();
        visionCone.setFill(
                Color.rgb(255, 174, 38, 0.20)
        );
        visionCone.setStroke(
                Color.rgb(255, 211, 92, 0.72)
        );
        visionCone.setStrokeWidth(1.2);
        visionCone.setVisible(tutorialComplete && mapHasSentinel);

        visionConeEntity = entityBuilder()
                .at(sentinelSpawn)
                .view(visionCone)
                .zIndex(900)
                .buildAndAttach();

        Rectangle shell = new Rectangle(
                4,
                5,
                20,
                19
        );
        shell.setArcWidth(7);
        shell.setArcHeight(7);
        shell.setFill(Color.rgb(62, 68, 78));
        shell.setStroke(Color.rgb(220, 229, 234));
        shell.setStrokeWidth(1.5);

        Circle eye = new Circle(
                SENTINEL_SIZE / 2.0,
                9,
                4.5,
                Color.rgb(255, 70, 65)
        );
        Rectangle base = new Rectangle(
                7,
                23,
                14,
                4
        );
        base.setFill(Color.rgb(30, 33, 39));
        sentinelView = new Group(shell, eye, base);

        sentinel = entityBuilder()
                .at(
                        sentinelSpawn.getX()
                                - SENTINEL_SIZE / 2.0,
                        sentinelSpawn.getY()
                                - SENTINEL_SIZE / 2.0
                )
                .type(SilentClassroomType.SENTINEL)
                .viewWithBBox(sentinelView)
                .zIndex(1190)
                .buildAndAttach();

        updateVisionCone();
    }

    private void createWorldEffects() {
        for (LightRuntime light : lights) {
            Circle glow = new Circle(
                    light.radius,
                    Color.rgb(122, 205, 230, 0.035)
            );
            light.view = glow;
            light.entity = entityBuilder()
                    .at(light.position)
                    .view(glow)
                    .zIndex(850)
                    .buildAndAttach();
        }

        for (PointRuntime arcade : arcades) {
            Circle glow = new Circle(
                    18,
                    Color.rgb(35, 236, 198, 0.20)
            );
            glow.setStroke(Color.rgb(35, 236, 198, 0.70));
            arcade.view = glow;
            arcade.entity = entityBuilder()
                    .at(arcade.position)
                    .view(glow)
                    .zIndex(1080)
                    .buildAndAttach();
        }
    }

    private void configureCamera() {
        var viewport =
                getGameScene().getViewport();

        viewport.setBounds(
                0,
                0,
                mapPixelWidth,
                mapPixelHeight
        );

        viewport.bindToEntity(
                player,
                getAppWidth() / 2.0,
                getAppHeight() / 2.0
        );

        viewport.setZoom(1.0);
    }

    private void registerMovementKey(KeyCode key) {
        /*
         * FXGL only allows a single action to be bound to a given
         * key trigger, so press and release must be handled by one
         * UserAction rather than separate onKeyDown/onKeyUp calls
         * (which both try to claim the same trigger and throw
         * "Trigger is already bound").
         */
        getInput().addAction(new UserAction("Move " + key) {
            @Override
            protected void onActionBegin() {
                heldMovementKeys.add(key);
            }

            @Override
            protected void onActionEnd() {
                heldMovementKeys.remove(key);
            }
        }, key);
    }

    private void registerSprintKey(KeyCode key) {
        /*
         * SHIFT and CONTROL are on FXGL's ILLEGAL_KEYS list and can
         * never be bound as an action trigger (addAction throws
         * "Cannot bind to illegal key"). Track them via raw key
         * events instead, which FXGL exposes precisely for cases
         * like this.
         */
        getInput().addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == key && preferences.sprintKey() == key) {
                sprintHeld = true;
            }
        });
        getInput().addEventHandler(KeyEvent.KEY_RELEASED, event -> {
            if (event.getCode() == key) {
                sprintHeld = false;
            }
        });
    }

    private void registerInteractKey(KeyCode key) {
        onKeyDown(key, () -> {
            if (preferences.interactKey() == key) {
                interact();
            }
        });
    }

    private void releaseMovementInput() {
        heldMovementKeys.clear();
        sprintHeld = false;
    }

    private void updatePlayerMovement(double dt) {
        if (player == null || modalOpen) {
            return;
        }

        double x = 0;
        double y = 0;

        if (heldMovementKeys.stream().anyMatch(preferences::isLeft)) {
            x -= 1;
        }
        if (heldMovementKeys.stream().anyMatch(preferences::isRight)) {
            x += 1;
        }
        if (heldMovementKeys.stream().anyMatch(preferences::isUp)) {
            y -= 1;
        }
        if (heldMovementKeys.stream().anyMatch(preferences::isDown)) {
            y += 1;
        }

        Point2D direction = SilentClassroomGeometry.normalizedDirection(x, y);
        boolean moving = direction.magnitude() > 0;
        boolean sprinting = moving
                && sprintHeld
                && staminaSeconds > 0;

        if (sprinting) {
            staminaSeconds = Math.max(
                    0,
                    staminaSeconds - dt
            );
        } else {
            staminaSeconds = Math.min(
                    MAX_STAMINA_SECONDS,
                    staminaSeconds
                            + STAMINA_RECOVERY_PER_SECOND * dt
            );
        }

        if (!moving) {
            playerSprite.setImage(
                    SilentClassroomPlayerAssets.idle(playerDirection)
            );
            playerSprite.setTranslateY(PLAYER_SPRITE_BASE_OFFSET_Y);
            return;
        }

        double speed = sprinting
                ? PLAYER_SPRINT_SPEED
                : PLAYER_WALK_SPEED;

        movePlayer(
                direction.getX() * speed * dt,
                direction.getY() * speed * dt
        );

        playerDirection = SilentClassroomGeometry.compassDirection8(
                direction.getX(),
                direction.getY()
        );
        playerSprite.setImage(
                SilentClassroomPlayerAssets.walking(playerDirection)
        );
        playerSprite.setTranslateY(
                PLAYER_SPRITE_BASE_OFFSET_Y
                        + (preferences.reducedFlashing()
                                ? 0
                                : Math.sin(animationTime * (sprinting ? 16 : 10)) * 1.2)
        );
    }

    private void movePlayer(
            double movementX,
            double movementY
    ) {
        moveHorizontally(movementX);
        moveVertically(movementY);
    }

    private void moveHorizontally(
            double amount
    ) {
        if (player == null
                || amount == 0) {
            return;
        }

        double previousX =
                player.getX();

        double nextX =
                clamp(
                        previousX + amount,
                        0,
                        mapPixelWidth - PLAYER_SIZE
                );

        player.setX(nextX);

        if (isTouchingBlockingObject()) {
            player.setX(previousX);
        }
    }

    private void moveVertically(
            double amount
    ) {
        if (player == null
                || amount == 0) {
            return;
        }

        double previousY =
                player.getY();

        double nextY =
                clamp(
                        previousY + amount,
                        0,
                        mapPixelHeight - PLAYER_SIZE
                );

        player.setY(nextY);

        if (isTouchingBlockingObject()) {
            player.setY(previousY);
        }
    }

    /**
     * Walking into an open door that carries a targetMap (e.g. the
     * corridor's doors into the dedicated IUT classroom/ICT lab
     * rooms) reloads that map at its targetSpawn. Returns true if a
     * transition just happened, so the caller can skip the rest of
     * this frame's update against the map that no longer exists.
     */
    private boolean checkDoorTransition() {
        if (player == null) {
            return false;
        }

        Rectangle2D playerBox = getPlayerBox();

        for (DoorRuntime door : doors) {
            if (door.state != DoorState.OPEN
                    || door.targetMap.isEmpty()
                    || !playerBox.intersects(door.getDoorRectangle())) {
                continue;
            }

            loadMap(door.targetMap, door.targetSpawn);
            return true;
        }

        return false;
    }

    private boolean isTouchingBlockingObject() {
        Rectangle2D playerBox =
                getPlayerBox();

        boolean touchingMap =
                collisionBoxes
                        .stream()
                        .anyMatch(
                                playerBox::intersects
                        );

        if (touchingMap) {
            return true;
        }

        for (DoorRuntime door : doors) {
            if (door.blocksMovement()
                    && playerBox.intersects(
                    door.getDoorRectangle()
            )) {
                return true;
            }
        }

        return false;
    }

    private boolean isTouchingMapCollision() {
        Rectangle2D playerBox =
                getPlayerBox();

        return collisionBoxes
                .stream()
                .anyMatch(
                        playerBox::intersects
                );
    }

    private Rectangle2D getPlayerBox() {
        return new Rectangle2D(
                player.getX(),
                player.getY(),
                PLAYER_SIZE,
                PLAYER_SIZE
        );
    }

    private Point2D getPlayerCentre() {
        return new Point2D(
                player.getX()
                        + PLAYER_SIZE / 2.0,
                player.getY()
                        + PLAYER_SIZE / 2.0
        );
    }

    private RoomRuntime findCurrentRoom() {
        if (player == null) {
            return null;
        }

        Point2D centre = getPlayerCentre();
        for (RoomRuntime room : rooms) {
            if (room.zone.contains(centre)) {
                return room;
            }
        }
        return null;
    }

    private void updateNearbyInteraction() {
        nearbyDoor = null;
        nearbyPoint = null;

        if (player == null) {
            return;
        }

        Point2D centre =
                getPlayerCentre();

        double nearestDistance = Double.MAX_VALUE;

        for (DoorRuntime door : doors) {
            double distance = centre.distance(door.getCentre());

            if (distance
                    <= INTERACTION_DISTANCE
                    && distance
                    < nearestDistance) {

                nearbyDoor = door;
                nearestDistance = distance;
            }
        }

        for (PointRuntime point : allPointInteractions()) {
            double distance = centre.distance(point.position);
            if (distance <= INTERACTION_DISTANCE
                    && distance < nearestDistance) {
                nearbyDoor = null;
                nearbyPoint = point;
                nearestDistance = distance;
            }
        }
    }

    private List<PointRuntime> allPointInteractions() {
        List<PointRuntime> points = new ArrayList<>();
        points.addAll(arcades);
        points.addAll(dialogues);
        points.addAll(exits);
        points.addAll(breakers);
        return points;
    }

    private void updateInteractionPrompt() {
        if (nearbyPoint != null) {
            showInteractionPrompt();
            interactionText.setText(promptFor(nearbyPoint));
            return;
        }

        if (nearbyDoor == null) {
            hideInteractionPrompt();
            return;
        }

        showInteractionPrompt();

        switch (nearbyDoor.state) {
            case CLOSED ->
                    interactionText.setText(
                            nearbyDoor.label
                                    + "    " + interactTag() + " Open"
                    );

            case OPEN ->
                    interactionText.setText(
                            nearbyDoor.label
                                    + "    " + interactTag() + " Close"
                    );

            case LOCKED ->
                    interactionText.setText(
                            nearbyDoor.label
                                    + "    Door locked"
                    );
        }
    }

    private String promptFor(PointRuntime point) {
        String prompt = switch (point.kind) {
            case "Arcade" -> session.clearedGameIds().contains(point.id)
                    ? point.label + "    ✓ COMPLETED"
                    : tutorialComplete
                    ? withInteractBinding(point.prompt)
                    : "Talk to Professor Rahman before using terminals";
            case "Exit" -> session.isExitUnlocked()
                    ? interactTag() + " Upload scores and escape through Room 301"
                    : "EXIT LOCKED — complete all three terminals";
            case "Breaker" -> breakerUsed
                    ? "Patrol breaker depleted"
                    : "[E] Overload patrol grid — one use";
            case "Dialogue" -> "[E] " + point.prompt.replace("Press E to ", "");
            default -> withInteractBinding(point.prompt);
        };
        return withInteractBinding(prompt);
    }

    private String interactTag() {
        return "[" + SilentClassroomPreferences.display(
                preferences.interactKey()
        ) + "]";
    }

    private String withInteractBinding(String prompt) {
        return prompt
                .replace(
                        "Press E",
                        "Press " + SilentClassroomPreferences.display(
                                preferences.interactKey()
                        )
                )
                .replace("[E]", interactTag());
    }

    private void interact() {
        if (modalOpen) {
            return;
        }

        if (nearbyPoint != null) {
            interactWithPoint(nearbyPoint);
            return;
        }

        if (nearbyDoor == null) {
            return;
        }

        if (nearbyDoor.state == DoorState.LOCKED) {
            showCaption("The door is locked.");
            return;
        }

        if (nearbyDoor.state == DoorState.OPEN) {
            if (getPlayerBox().intersects(
                    nearbyDoor.getDoorRectangle()
            )) {
                showCaption("Move clear of the doorway first.");
                return;
            }
            nearbyDoor.state = DoorState.CLOSED;
        } else {
            nearbyDoor.state = DoorState.OPEN;
        }

        nearbyDoor.refreshVisual();
        ChiptuneSfx.door();
        updateInteractionPrompt();
    }

    private void interactWithPoint(PointRuntime point) {
        switch (point.kind) {
            case "Arcade" -> launchMiniGame(point);
            case "Dialogue" -> showDialogue(point);
            case "Breaker" -> activateBreaker();
            case "Exit" -> finishAtExit();
            default -> showCaption(point.prompt);
        }
    }

    private void launchMiniGame(PointRuntime arcade) {
        if (!tutorialComplete) {
            showCaption("Professor Rahman must unlock the lesson network first.");
            return;
        }
        if (session.clearedGameIds().contains(arcade.id)) {
            showCaption("Terminal already cleared. Find another lesson node.");
            return;
        }

        if (host == null) {
            showCaption("Run through the main game to launch this terminal.");
            return;
        }

        modalOpen = true;
        releaseMovementInput();
        session.synchronizeTimer();
        getGameController().pauseEngine();
        host.launchMiniGame(
                arcade.id,
                result -> resolveMiniGame(arcade, result)
        );
    }

    private void resolveMiniGame(
            PointRuntime arcade,
            MiniGameResult result
    ) {
        session.synchronizeTimer();
        boolean firstClear = session.recordMiniGameResult(
                arcade.id,
                result.won(),
                result.chapterPoints(),
                result.dependencyUsed()
        );

        if (host != null) {
            host.onMiniGameResolved(
                    arcade.id,
                    result,
                    firstClear
            );
        }

        if (firstClear) {
            if (arcade.view != null) {
                arcade.view.setFill(
                        Color.rgb(72, 224, 112, 0.22)
                );
                arcade.view.setStroke(
                        Color.rgb(95, 255, 140)
                );
            }
            showCaption(
                    "LESSON CLEARED  +"
                            + result.chapterPoints()
                            + " points"
            );
            ChiptuneSfx.wave();
        } else if (result.won()) {
            showCaption("This lesson was already banked — no repeat rewards.");
        } else {
            showCaption("Lesson incomplete. You can retry this terminal.");
        }

        modalOpen = false;
        getGameController().resumeEngine();
    }

    private void showDialogue(PointRuntime point) {
        if ("chapter1-professor".equals(point.id)) {
            if (tutorialComplete) {
                showCaption("Professor: Use the rooms to break sight. Room 301 is the exit.");
                return;
            }

            if (host == null) {
                tutorialComplete = true;
                session.resumeTimer();
                return;
            }

            modalOpen = true;
            releaseMovementInput();
            session.pauseTimer();
            getGameController().pauseEngine();
            host.showTutorial(() -> {
                tutorialComplete = true;
                session.resumeTimer();
                modalOpen = false;
                showCaption("MISSION LIVE — three terminals, then Room 301.");
                getGameController().resumeEngine();
            });
            return;
        }

        if ("room306-patrol-clue".equals(point.id)) {
            if (patrolClueRead) {
                showCaption("Patrol route: the Sentinel owns the central hallway.");
                return;
            }
            patrolClueRead = true;
            if (host == null) {
                showCaption("PATROL MAP FOUND — live direction tracking enabled.");
                return;
            }
            boolean timerWasRunning = session.isTimerRunning();
            modalOpen = true;
            releaseMovementInput();
            session.pauseTimer();
            getGameController().pauseEngine();
            host.showPatrolClue(() -> {
                if (timerWasRunning) {
                    session.resumeTimer();
                }
                modalOpen = false;
                showCaption("PATROL MAP FOUND — live direction tracking enabled.");
                getGameController().resumeEngine();
            });
        }
    }

    private void activateBreaker() {
        if (!tutorialComplete) {
            showCaption("The patrol grid is offline until the mission begins.");
            return;
        }
        if (breakerUsed) {
            showCaption("The patrol breaker has already burned out.");
            return;
        }
        breakerUsed = true;
        breakerStunSeconds = 5.0;
        ChiptuneSfx.emp();
        showCaption("PATROL GRID OVERLOADED — Sentinel disabled for 5 seconds.");
    }

    private void finishAtExit() {
        if (!session.isExitUnlocked()) {
            showCaption("Room 301 remains sealed: clear all three terminals.");
            return;
        }
        SilentClassroomResult result = session.finish();
        modalOpen = true;
        releaseMovementInput();
        getGameController().pauseEngine();
        if (host != null) {
            host.finishChapter(result);
        } else {
            System.out.println("Silent Classroom score: " + result.finalScore());
        }
    }

    private void requestPause() {
        if (modalOpen) {
            return;
        }
        if (host == null) {
            return;
        }

        modalOpen = true;
        releaseMovementInput();
        session.pauseTimer();
        getGameController().pauseEngine();
        host.showPauseMenu(
                () -> {
                    if (tutorialComplete) {
                        session.resumeTimer();
                    }
                    modalOpen = false;
                    getGameController().resumeEngine();
                },
                () -> modalOpen = true
        );
    }

    private void updateSentinel(
            double dt,
            boolean playerEnteredRoom
    ) {
        if (sentinel == null || player == null) {
            return;
        }

        if (breakerStunSeconds > 0) {
            breakerStunSeconds = Math.max(
                    0,
                    breakerStunSeconds - dt
            );
            visionCone.setVisible(false);
            sentinelView.setOpacity(
                    preferences.reducedFlashing()
                            ? 0.72
                            : 0.55 + 0.25 * Math.sin(animationTime * 18)
            );
            return;
        }

        sentinelView.setOpacity(1.0);
        visionCone.setVisible(true);

        boolean visible = currentRoom == null
                && canSentinelSeePlayer();

        if (visible) {
            lastSeenPlayer = getPlayerCentre();
        }

        boolean patrolReached =
                distanceToNearestPatrolPoint()
                        <= 14;

        SentinelController.Output output =
                sentinelController.update(
                        new SentinelController.Input(
                                dt,
                                visible,
                                playerEnteredRoom,
                                patrolReached,
                                session.isLockdown()
                        )
                );

        if (output.stateChanged()) {
            announceSentinelState(output.state());
        }

        moveSentinelForState(
                output,
                dt
        );
        updateSentinelAudio(output, dt);
        updateVisionCone();

        boolean hidden = currentRoom != null;
        if (hidden && !previousHidden) {
            showCaption(
                    output.state()
                            == SentinelController.State.CHASE
                            ? "HIDDEN — the Sentinel is searching this doorway."
                            : "HIDDEN — hallway line of sight broken."
            );
        }
        previousHidden = hidden;

        if (output.captureArmed()
                && !hidden
                && getSentinelCentre().distance(
                getPlayerCentre()
        ) <= CAPTURE_DISTANCE) {
            capturePlayer();
        }
    }

    private void moveSentinelForState(
            SentinelController.Output output,
            double dt
    ) {
        Point2D target = null;

        switch (output.state()) {
            case PATROL -> {
                if (!patrolPoints.isEmpty()) {
                    Point2D waypoint =
                            patrolPoints.get(patrolIndex);
                    if (getSentinelCentre().distance(
                            waypoint
                    ) < 12) {
                        patrolIndex =
                                (patrolIndex + 1)
                                        % patrolPoints.size();
                        waypoint = patrolPoints.get(patrolIndex);
                    }
                    target = waypoint;
                }
            }
            case SUSPICIOUS -> faceToward(getPlayerCentre());
            case CHASE -> target = new Point2D(
                    clamp(
                            getPlayerCentre().getX(),
                            SENTINEL_CORRIDOR_MIN_X,
                            SENTINEL_CORRIDOR_MAX_X
                    ),
                    getPlayerCentre().getY()
            );
            case SEARCH -> {
                target = new Point2D(
                        clamp(
                                lastSeenPlayer.getX(),
                                SENTINEL_CORRIDOR_MIN_X,
                                SENTINEL_CORRIDOR_MAX_X
                        ),
                        lastSeenPlayer.getY()
                );
                if (getSentinelCentre().distance(target) < 10) {
                    double angle = animationTime * 2.4;
                    sentinelFacing = new Point2D(
                            Math.cos(angle),
                            Math.sin(angle)
                    );
                    target = null;
                }
            }
            case RETURN -> target =
                    nearestPatrolPoint();
        }

        if (target != null) {
            moveSentinelToward(
                    target,
                    output.movementSpeed(),
                    dt
            );
        }
    }

    private void updateSentinelAudio(
            SentinelController.Output output,
            double dt
    ) {
        servoCueCooldown = Math.max(0, servoCueCooldown - dt);
        chaseCueCooldown = Math.max(0, chaseCueCooldown - dt);

        double playerDistance = getSentinelCentre().distance(getPlayerCentre());
        if (output.movementSpeed() > 0
                && playerDistance <= 620
                && servoCueCooldown <= 0) {
            ChiptuneSfx.servoStep();
            servoCueCooldown = output.state() == SentinelController.State.CHASE
                    ? 0.38
                    : 0.68;
        }

        if (output.state() == SentinelController.State.CHASE) {
            if (chaseCueCooldown <= 0) {
                ChiptuneSfx.chaseBeat();
                chaseCueCooldown = 0.56;
            }
        } else {
            chaseCueCooldown = 0;
        }
    }

    private void moveSentinelToward(
            Point2D target,
            double speed,
            double dt
    ) {
        Point2D centre = getSentinelCentre();
        Point2D delta = target.subtract(centre);
        if (delta.magnitude() < 0.001
                || speed <= 0) {
            return;
        }

        Point2D direction = delta.normalize();
        double distance = Math.min(
                delta.magnitude(),
                speed * dt
        );

        double nextCentreX = clamp(
                centre.getX()
                        + direction.getX() * distance,
                SENTINEL_CORRIDOR_MIN_X,
                SENTINEL_CORRIDOR_MAX_X
        );
        double nextCentreY = clamp(
                centre.getY()
                        + direction.getY() * distance,
                112,
                mapPixelHeight - 112
        );

        sentinel.setPosition(
                nextCentreX - SENTINEL_SIZE / 2.0,
                nextCentreY - SENTINEL_SIZE / 2.0
        );
        sentinelFacing = direction;
        sentinelView.setTranslateY(
                preferences.reducedFlashing()
                        ? 0
                        : Math.sin(animationTime * 12) * 1.1
        );
    }

    private void faceToward(Point2D target) {
        Point2D delta = target.subtract(
                getSentinelCentre()
        );
        if (delta.magnitude() > 0.001) {
            sentinelFacing = delta.normalize();
        }
    }

    private boolean canSentinelSeePlayer() {
        Point2D sentinelCentre = getSentinelCentre();
        Point2D playerCentre = getPlayerCentre();
        return SilentClassroomGeometry.hasLineOfSight(
                sentinelCentre,
                playerCentre,
                sentinelFacing,
                SentinelController.VISION_RANGE_PIXELS,
                40,
                14,
                this::blocksSentinelSight
        );
    }

    private boolean blocksSentinelSight(Point2D sample) {
        for (Rectangle2D collision : collisionBoxes) {
            if (collision.contains(sample)) {
                return true;
            }
        }
        for (DoorRuntime door : doors) {
            if (door.blocksMovement()
                    && door.getDoorRectangle().contains(sample)) {
                return true;
            }
        }
        return false;
    }

    private void updateVisionCone() {
        if (visionCone == null
                || sentinel == null) {
            return;
        }

        Point2D centre = getSentinelCentre();
        visionConeEntity.setPosition(centre);

        double baseAngle = Math.atan2(
                sentinelFacing.getY(),
                sentinelFacing.getX()
        );
        double halfAngle = Math.toRadians(40);
        double range =
                SentinelController.VISION_RANGE_PIXELS;

        visionCone.getPoints().setAll(
                0.0,
                0.0,
                Math.cos(baseAngle - halfAngle) * range,
                Math.sin(baseAngle - halfAngle) * range,
                Math.cos(baseAngle + halfAngle) * range,
                Math.sin(baseAngle + halfAngle) * range
        );

        Color coneColor = threatColor(sentinelController.state());
        visionCone.setFill(new Color(
                coneColor.getRed(),
                coneColor.getGreen(),
                coneColor.getBlue(),
                0.20
        ));
        visionCone.setStroke(new Color(
                coneColor.getRed(),
                coneColor.getGreen(),
                coneColor.getBlue(),
                0.82
        ));
        visionCone.getStrokeDashArray().clear();
        if (preferences.colorBlindSafe()) {
            visionCone.getStrokeDashArray().addAll(10.0, 7.0);
        }

        sentinelView.setRotate(
                Math.toDegrees(baseAngle) + 90
        );
    }

    private void announceSentinelState(
            SentinelController.State state
    ) {
        switch (state) {
            case SUSPICIOUS ->
                    showCaption(
                            "WARNING — Sentinel scanning movement."
                    );
            case CHASE -> {
                showCaption(
                        "DETECTED — sprint for a classroom!"
                );
                ChiptuneSfx.boss();
            }
            case SEARCH ->
                    showCaption(
                            "Sentinel searching last-seen doorway..."
                    );
            case RETURN ->
                    showCaption(
                            "Signal fading — Sentinel returning to patrol."
                    );
            case PATROL -> {
                // The HUD already exposes the calm state.
            }
        }
    }

    private void capturePlayer() {
        session.registerCapture();
        releaseMovementInput();
        ChiptuneSfx.breach();
        showCaption(
                "CAPTURED  -20 seconds  -150 points"
        );

        player.setPosition(
                880 - PLAYER_SIZE / 2.0,
                2720 - PLAYER_SIZE / 2.0
        );
        sentinel.setPosition(
                sentinelSpawn.getX()
                        - SENTINEL_SIZE / 2.0,
                sentinelSpawn.getY()
                        - SENTINEL_SIZE / 2.0
        );
        sentinelController.reset(
                session.isLockdown()
        );
        patrolIndex = 0;
        breakerStunSeconds = Math.max(
                breakerStunSeconds,
                1.5
        );
        currentRoom = findCurrentRoom();
        previousHidden = currentRoom != null;
    }

    private Point2D getSentinelCentre() {
        return new Point2D(
                sentinel.getX()
                        + SENTINEL_SIZE / 2.0,
                sentinel.getY()
                        + SENTINEL_SIZE / 2.0
        );
    }

    private Point2D nearestPatrolPoint() {
        if (patrolPoints.isEmpty()) {
            return sentinelSpawn;
        }

        Point2D centre = getSentinelCentre();
        Point2D nearest = patrolPoints.get(0);
        double nearestDistance =
                centre.distance(nearest);

        for (Point2D point : patrolPoints) {
            double distance = centre.distance(point);
            if (distance < nearestDistance) {
                nearest = point;
                nearestDistance = distance;
            }
        }
        return nearest;
    }

    private double distanceToNearestPatrolPoint() {
        return getSentinelCentre().distance(
                nearestPatrolPoint()
        );
    }

    private void updateWorldEffects(double dt) {
        if (captionSeconds > 0) {
            captionSeconds = Math.max(
                    0,
                    captionSeconds - dt
            );
            captionText.setVisible(
                    captionSeconds > 0
            );
        }

        for (LightRuntime light : lights) {
            if (light.view == null) {
                continue;
            }
            double opacity = light.flicker && !preferences.reducedFlashing()
                    ? 0.55
                    + 0.35
                    * Math.sin(
                    animationTime * 9
                            + light.position.getY()
                            * 0.01
            )
                    : 0.75;
            light.view.setOpacity(
                    clamp(opacity, 0.15, 1.0)
            );
        }

        for (PointRuntime arcade : arcades) {
            if (arcade.view != null) {
                double scale = preferences.reducedFlashing()
                        ? 1.0
                        : 1.0
                        + 0.10
                        * Math.sin(
                                animationTime * 4
                                        + arcade.position.getY() * 0.01
                        );
                arcade.view.setScaleX(scale);
                arcade.view.setScaleY(scale);
            }
        }
    }

    private void showCaption(String message) {
        if (captionText == null) {
            return;
        }
        captionText.setText(message);
        captionText.setVisible(true);
        captionSeconds = 3.4;
    }

    private void loadCollisionObjects(
            Document document
    ) {
        Element collisionLayer =
                findObjectGroup(
                        document,
                        "Collision"
                );

        if (collisionLayer == null) {
            throw new IllegalStateException(
                    currentMapFile
                            + " does not contain a Collision layer."
            );
        }

        NodeList objects =
                collisionLayer
                        .getElementsByTagName(
                                "object"
                        );

        for (int i = 0;
             i < objects.getLength();
             i++) {

            Element object =
                    (Element) objects.item(i);

            double width =
                    readDoubleAttribute(
                            object,
                            "width",
                            0
                    );

            double height =
                    readDoubleAttribute(
                            object,
                            "height",
                            0
                    );

            if (width <= 0
                    || height <= 0) {
                continue;
            }

            collisionBoxes.add(
                    new Rectangle2D(
                            readDoubleAttribute(
                                    object,
                                    "x",
                                    0
                            ),
                            readDoubleAttribute(
                                    object,
                                    "y",
                                    0
                            ),
                            width,
                            height
                    )
            );
        }
    }

    private void loadDoorObjects(
            Document document
    ) {
        NodeList objects =
                document
                        .getElementsByTagName(
                                "object"
                        );

        for (int i = 0;
             i < objects.getLength();
             i++) {

            Element object =
                    (Element) objects.item(i);

            String type =
                    object.getAttribute("type");

            String objectClass =
                    object.getAttribute("class");

            if (!"Door".equals(type)
                    && !"Door".equals(
                    objectClass
            )) {
                continue;
            }

            double x =
                    readDoubleAttribute(
                            object,
                            "x",
                            0
                    );

            double y =
                    readDoubleAttribute(
                            object,
                            "y",
                            0
                    );

            double width =
                    readDoubleAttribute(
                            object,
                            "width",
                            32
                    );

            double height =
                    readDoubleAttribute(
                            object,
                            "height",
                            64
                    );

            String label =
                    readObjectProperty(
                            object,
                            "label",
                            object.getAttribute(
                                    "name"
                            )
                    );

            String targetMap = readObjectProperty(
                    object,
                    "targetMap",
                    ""
            );

            String targetSpawn = readObjectProperty(
                    object,
                    "targetSpawn",
                    ""
            );

            String openDirection =
                    readObjectProperty(
                            object,
                            "openDirection",
                            "right"
                    );

            boolean initiallyLocked =
                    Boolean.parseBoolean(
                            readObjectProperty(
                                    object,
                                    "initiallyLocked",
                                    "false"
                            )
                    );

            doors.add(
                    new DoorRuntime(
                            x,
                            y,
                            width,
                            height,
                            label,
                            targetMap,
                            targetSpawn,
                            openDirection,
                            initiallyLocked
                                    ? DoorState.LOCKED
                                    : DoorState.CLOSED
                    )
            );
        }
    }

    private void loadRoomObjects(Document document) {
        NodeList objects = document.getElementsByTagName("object");
        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            if (!isObjectType(object, "RoomZone")) {
                continue;
            }

            String name = object.getAttribute("name");
            if (!name.matches("Room 30[1-7].*")) {
                continue;
            }

            rooms.add(new RoomRuntime(
                    name,
                    new Rectangle2D(
                            readDoubleAttribute(object, "x", 0),
                            readDoubleAttribute(object, "y", 0),
                            readDoubleAttribute(object, "width", 0),
                            readDoubleAttribute(object, "height", 0)
                    )
            ));
        }
    }

    private void loadPointObjects(Document document) {
        NodeList objects = document.getElementsByTagName("object");
        boolean sentinelFound = false;

        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            Point2D position = new Point2D(
                    readDoubleAttribute(object, "x", 0),
                    readDoubleAttribute(object, "y", 0)
            );

            if (isObjectType(object, "EnemySpawn")
                    && !sentinelFound
                    && object.getAttribute("name").contains("Sentinel01")) {
                sentinelSpawn = position;
                sentinelFound = true;
                mapHasSentinel = true;
                continue;
            }

            if (isObjectType(object, "Arcade")) {
                String gameId = readObjectProperty(
                        object,
                        "gameId",
                        object.getAttribute("name")
                );
                arcades.add(new PointRuntime(
                        "Arcade",
                        gameId,
                        displayNameForGame(gameId),
                        readObjectProperty(
                                object,
                                "prompt",
                                "Press E to play"
                        ),
                        position
                ));
            } else if (isObjectType(object, "ArcadeSpawn")) {
                String slotId = readObjectProperty(
                        object,
                        "slotId",
                        object.getAttribute("name")
                );
                String gameId = arcadeSlotAssignments.get(slotId);
                if (gameId != null) {
                    arcades.add(new PointRuntime(
                            "Arcade",
                            gameId,
                            displayNameForGame(gameId),
                            "Press E to play " + displayNameForGame(gameId),
                            position
                    ));
                }
            } else if (isObjectType(object, "Dialogue")) {
                dialogues.add(new PointRuntime(
                        "Dialogue",
                        readObjectProperty(
                                object,
                                "dialogueId",
                                object.getAttribute("name")
                        ),
                        object.getAttribute("name"),
                        readObjectProperty(
                                object,
                                "prompt",
                                "inspect"
                        ),
                        position
                ));
            } else if (isObjectType(object, "Exit")) {
                exits.add(new PointRuntime(
                        "Exit",
                        object.getAttribute("name"),
                        "Room 301 Exit",
                        "Press E to leave",
                        position
                ));
            } else if (isObjectType(object, "Breaker")) {
                breakers.add(new PointRuntime(
                        "Breaker",
                        object.getAttribute("name"),
                        "Patrol Breaker",
                        readObjectProperty(
                                object,
                                "prompt",
                                "Press E to disable the Sentinel"
                        ),
                        position
                ));
            }
        }
    }

    private void loadPatrolPath(Document document) {
        NodeList objects = document.getElementsByTagName("object");
        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            if (!isObjectType(object, "PatrolPath")
                    || !"Sentinel01Path".equals(
                    object.getAttribute("name")
            )) {
                continue;
            }

            Element polyline = directChild(object, "polyline");
            if (polyline == null) {
                continue;
            }

            double originX = readDoubleAttribute(object, "x", 0);
            double originY = readDoubleAttribute(object, "y", 0);
            String rawPoints = polyline.getAttribute("points").trim();
            if (rawPoints.isEmpty()) {
                continue;
            }

            for (String rawPoint : rawPoints.split("\\s+")) {
                String[] coordinates = rawPoint.split(",");
                if (coordinates.length != 2) {
                    continue;
                }
                patrolPoints.add(new Point2D(
                        originX + Double.parseDouble(coordinates[0]),
                        originY + Double.parseDouble(coordinates[1])
                ));
            }
            break;
        }

        if (patrolPoints.isEmpty()) {
            patrolPoints.add(new Point2D(1136, 448));
            patrolPoints.add(new Point2D(1136, 1664));
        }
    }

    private void loadLightObjects(Document document) {
        NodeList objects = document.getElementsByTagName("object");
        for (int i = 0; i < objects.getLength(); i++) {
            Element object = (Element) objects.item(i);
            if (!isObjectType(object, "Light")) {
                continue;
            }

            double radius = parseDouble(
                    readObjectProperty(object, "radius", "180"),
                    180
            );
            boolean flicker = Boolean.parseBoolean(
                    readObjectProperty(object, "flicker", "false")
            );
            lights.add(new LightRuntime(
                    new Point2D(
                            readDoubleAttribute(object, "x", 0),
                            readDoubleAttribute(object, "y", 0)
                    ),
                    radius,
                    flicker
            ));
        }
    }

    private boolean isObjectType(Element object, String expected) {
        return expected.equals(object.getAttribute("type"))
                || expected.equals(object.getAttribute("class"));
    }

    private String displayNameForGame(String gameId) {
        return switch (gameId) {
            case SilentClassroomSession.KERNEL_PANIC_GAME_ID -> "Kernel Panic";
            case SilentClassroomSession.SNAKE_GAME_ID -> "Syntax Snake";
            case SilentClassroomSession.COMPOSE_GAME_ID -> "Human Compose";
            default -> gameId;
        };
    }

    private double parseDouble(String value, double fallback) {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private Point2D findSpawn(
            Document document,
            String requestedSpawn
    ) {
        NodeList objects =
                document
                        .getElementsByTagName(
                                "object"
                        );

        Point2D fallback = null;

        for (int i = 0;
             i < objects.getLength();
             i++) {

            Element object =
                    (Element) objects.item(i);

            String type =
                    object.getAttribute("type");

            String objectClass =
                    object.getAttribute("class");

            if (!"Spawn".equals(type)
                    && !"Spawn".equals(
                    objectClass
            )) {
                continue;
            }

            Point2D point =
                    new Point2D(
                            readDoubleAttribute(
                                    object,
                                    "x",
                                    0
                            ),
                            readDoubleAttribute(
                                    object,
                                    "y",
                                    0
                            )
                    );

            if (fallback == null) {
                fallback = point;
            }

            String objectName =
                    object.getAttribute("name");

            String spawnId =
                    readObjectProperty(
                            object,
                            "spawnId",
                            ""
                    );

            if (requestedSpawn.equals(
                    objectName
            ) || requestedSpawn.equals(
                    spawnId
            )) {
                return point;
            }
        }

        if (fallback != null) {
            return fallback;
        }

        throw new IllegalStateException(
                currentMapFile
                        + " has no Spawn object."
        );
    }

    private Element findObjectGroup(
            Document document,
            String groupName
    ) {
        NodeList groups =
                document
                        .getElementsByTagName(
                                "objectgroup"
                        );

        for (int i = 0;
             i < groups.getLength();
             i++) {

            Element group =
                    (Element) groups.item(i);

            if (groupName.equals(
                    group.getAttribute("name")
            )) {
                return group;
            }
        }

        return null;
    }

    private String readMapDisplayName(
            Document document,
            String fallback
    ) {
        Element mapProperties =
                document
                        .getDocumentElement()
                        .getElementsByTagName(
                                "properties"
                        )
                        .getLength() > 0
                        ? (Element) document
                        .getDocumentElement()
                        .getElementsByTagName(
                                "properties"
                        )
                        .item(0)
                        : null;

        if (mapProperties == null) {
            return fallback;
        }

        for (Element property
                : childProperties(
                mapProperties
        )) {
            if ("displayName".equals(
                    property.getAttribute(
                            "name"
                    )
            )) {
                String value =
                        property.getAttribute(
                                "value"
                        );

                if (!value.isBlank()) {
                    return value;
                }
            }
        }

        return fallback;
    }

    private String readRequiredObjectProperty(
            Element object,
            String propertyName
    ) {
        String value =
                readObjectProperty(
                        object,
                        propertyName,
                        ""
                );

        if (value.isBlank()) {
            throw new IllegalStateException(
                    "Door "
                            + object.getAttribute(
                            "name"
                    )
                            + " is missing property "
                            + propertyName
                            + "."
            );
        }

        return value;
    }

    private String readObjectProperty(
            Element object,
            String propertyName,
            String defaultValue
    ) {
        Element properties =
                directChild(
                        object,
                        "properties"
                );

        if (properties == null) {
            return defaultValue;
        }

        for (Element property
                : childProperties(
                properties
        )) {
            if (propertyName.equals(
                    property.getAttribute(
                            "name"
                    )
            )) {
                String value =
                        property.getAttribute(
                                "value"
                        );

                if (!value.isBlank()) {
                    return value;
                }

                return property.getTextContent();
            }
        }

        return defaultValue;
    }

    private List<Element> childProperties(
            Element properties
    ) {
        List<Element> result =
                new ArrayList<>();

        NodeList children =
                properties.getChildNodes();

        for (int i = 0;
             i < children.getLength();
             i++) {

            if (children.item(i)
                    instanceof Element element
                    && "property".equals(
                    element.getTagName()
            )) {
                result.add(element);
            }
        }

        return result;
    }

    private Element directChild(
            Element parent,
            String tagName
    ) {
        NodeList children =
                parent.getChildNodes();

        for (int i = 0;
             i < children.getLength();
             i++) {

            if (children.item(i)
                    instanceof Element element
                    && tagName.equals(
                    element.getTagName()
            )) {
                return element;
            }
        }

        return null;
    }

    private Document loadTmxDocument(
            String mapFile
    ) {
        String resourcePath =
                "/assets/levels/" + mapFile;

        try (InputStream inputStream =
                     getClass()
                             .getResourceAsStream(
                                     resourcePath
                             )) {

            if (inputStream == null) {
                throw new IllegalStateException(
                        "Could not find "
                                + resourcePath
                                + "."
                );
            }

            DocumentBuilderFactory factory =
                    DocumentBuilderFactory
                            .newInstance();

            factory.setFeature(
                    "http://apache.org/xml/features/disallow-doctype-decl",
                    true
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-general-entities",
                    false
            );

            factory.setFeature(
                    "http://xml.org/sax/features/external-parameter-entities",
                    false
            );

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_DTD,
                    ""
            );

            factory.setAttribute(
                    XMLConstants.ACCESS_EXTERNAL_SCHEMA,
                    ""
            );

            Document document =
                    factory
                            .newDocumentBuilder()
                            .parse(inputStream);

            document
                    .getDocumentElement()
                    .normalize();

            return document;

        } catch (Exception exception) {
            throw new RuntimeException(
                    "Failed to load "
                            + mapFile,
                    exception
            );
        }
    }

    private int readIntAttribute(
            Element element,
            String name,
            int defaultValue
    ) {
        String value =
                element.getAttribute(name);

        if (value == null
                || value.isBlank()) {
            return defaultValue;
        }

        return Integer.parseInt(value);
    }

    private double readDoubleAttribute(
            Element element,
            String name,
            double defaultValue
    ) {
        String value =
                element.getAttribute(name);

        if (value == null
                || value.isBlank()) {
            return defaultValue;
        }

        return Double.parseDouble(value);
    }

    private void updateRoomHud() {
        if (roomHudText == null) {
            return;
        }
        applyAccessibilitySettings();

        int remaining = session.remainingWholeSeconds();
        String timer = String.format(
                "%d:%02d",
                remaining / 60,
                remaining % 60
        );
        String roomName = currentRoom == null
                ? "Central Hallway"
                : currentRoom.name;

        roomHudText.setText(
                "SILENT CLASSROOM   "
                        + (session.isLockdown()
                        ? "LOCKDOWN"
                        : timer)
                        + "   SCORE "
                        + session.currentScore()
        );

        int staminaPercent = (int) Math.round(
                staminaSeconds
                        / MAX_STAMINA_SECONDS
                        * 100
        );
        objectiveHudText.setText(
                roomName
                        + "   LESSONS "
                        + session.clearedGameIds().size()
                        + "/3   STAMINA "
                        + staminaPercent
                        + "%   "
                        + (currentRoom == null
                        ? "EXPOSED"
                        : "HIDDEN")
                        + controlHint()
        );

        if (!tutorialComplete) {
            threatHudText.setText(
                    "SENTINEL: OFFLINE"
            );
            threatHudText.setFill(
                    Color.rgb(130, 220, 190)
            );
            return;
        }

        SentinelController.State state =
                sentinelController.state();
        int detection = (int) Math.round(
                sentinelController.detectionProgress()
                        * 100
        );
        String direction = "";
        if (patrolClueRead
                || state == SentinelController.State.CHASE) {
            double dy = getSentinelCentre().getY()
                    - getPlayerCentre().getY();
            direction = Math.abs(dy) < 70
                    ? "  NEARBY"
                    : dy < 0
                    ? "  ↑ NORTH"
                    : "  ↓ SOUTH";
        }

        String disabled = breakerStunSeconds > 0
                ? "DISABLED "
                + String.format("%.1fs", breakerStunSeconds)
                : state.name()
                + (detection > 0
                ? " " + detection + "%"
                : "");
        String stateMarker = switch (state) {
            case CHASE -> "[!!!]";
            case SUSPICIOUS -> "[?]";
            case SEARCH -> "[~~~]";
            case RETURN -> "[<<]";
            case PATROL -> "[--]";
        };
        threatHudText.setText(
                stateMarker + " SENTINEL: " + disabled + direction
        );
        threatHudText.setFill(threatColor(state));
    }

    private void applyAccessibilitySettings() {
        double scale = preferences.fontScale();
        if (Math.abs(scale - appliedFontScale) < 0.001) {
            return;
        }
        appliedFontScale = scale;
        roomHudText.setFont(Font.font(
                "Consolas", FontWeight.BOLD, 18 * scale
        ));
        objectiveHudText.setFont(Font.font("Consolas", 15 * scale));
        threatHudText.setFont(Font.font(
                "Consolas", FontWeight.BOLD, 16 * scale
        ));
        captionText.setFont(Font.font(
                "Consolas", FontWeight.BOLD, 17 * scale
        ));
        interactionText.setFont(Font.font(
                "Consolas", FontWeight.BOLD, 18 * scale
        ));
    }

    private String controlHint() {
        String interact = SilentClassroomPreferences.display(
                preferences.interactKey()
        );
        if (preferences.fontScale() > 1.0) {
            return "   " + interact + " action • Esc pause";
        }
        return "   " + preferences.movementSummary()
                + " • "
                + SilentClassroomPreferences.display(preferences.sprintKey())
                + " sprint • " + interact + " interact • Esc pause";
    }

    private Color threatColor(SentinelController.State state) {
        if (preferences.colorBlindSafe()) {
            return switch (state) {
                case CHASE -> Color.rgb(213, 94, 0);
                case SUSPICIOUS, SEARCH -> Color.rgb(240, 228, 66);
                case PATROL, RETURN -> Color.rgb(86, 180, 233);
            };
        }
        return state == SentinelController.State.CHASE
                ? Color.rgb(255, 76, 70)
                : state == SentinelController.State.SUSPICIOUS
                ? Color.rgb(255, 204, 80)
                : Color.rgb(130, 220, 190);
    }

    private void showInteractionPrompt() {
        interactionBackground.setVisible(true);
        interactionText.setVisible(true);
    }

    private void hideInteractionPrompt() {
        if (interactionBackground != null) {
            interactionBackground.setVisible(false);
        }

        if (interactionText != null) {
            interactionText.setVisible(false);
        }
    }

    private void drawCollisionDebugBoxes() {
        for (Rectangle2D box
                : collisionBoxes) {

            Rectangle debug =
                    new Rectangle(
                            box.getWidth(),
                            box.getHeight(),
                            Color.rgb(
                                    255,
                                    0,
                                    0,
                                    0.25
                            )
                    );

            debug.setStroke(Color.RED);

            entityBuilder()
                    .at(
                            box.getMinX(),
                            box.getMinY()
                    )
                    .view(debug)
                    .zIndex(2500)
                    .buildAndAttach();
        }
    }

    private double clamp(
            double value,
            double minimum,
            double maximum
    ) {
        return Math.max(
                minimum,
                Math.min(maximum, value)
        );
    }

    private static final class RoomRuntime {
        private final String name;
        private final Rectangle2D zone;

        private RoomRuntime(
                String name,
                Rectangle2D zone
        ) {
            this.name = name;
            this.zone = zone;
        }
    }

    private static final class PointRuntime {
        private final String kind;
        private final String id;
        private final String label;
        private final String prompt;
        private final Point2D position;
        private Entity entity;
        private Circle view;

        private PointRuntime(
                String kind,
                String id,
                String label,
                String prompt,
                Point2D position
        ) {
            this.kind = kind;
            this.id = id;
            this.label = label;
            this.prompt = prompt;
            this.position = position;
        }
    }

    private static final class LightRuntime {
        private final Point2D position;
        private final double radius;
        private final boolean flicker;
        private Entity entity;
        private Circle view;

        private LightRuntime(
                Point2D position,
                double radius,
                boolean flicker
        ) {
            this.position = position;
            this.radius = radius;
            this.flicker = flicker;
        }
    }

    private enum DoorState {
        CLOSED,
        OPEN,
        LOCKED
    }

    private final class DoorRuntime {

        private final double x;
        private final double y;
        private final double width;
        private final double height;

        private final String label;
        private final String targetMap;
        private final String targetSpawn;
        private final String openDirection;

        private DoorState state;

        private Entity entity;
        private Group view;

        private DoorRuntime(
                double x,
                double y,
                double width,
                double height,
                String label,
                String targetMap,
                String targetSpawn,
                String openDirection,
                DoorState state
        ) {
            this.x = x;
            this.y = y;
            this.width = width;
            this.height = height;
            this.label = label;
            this.targetMap = targetMap;
            this.targetSpawn = targetSpawn;
            this.openDirection = openDirection;
            this.state = state;
        }

        private void createEntity() {
            view = new Group();

            entity = entityBuilder()
                    .at(x, y)
                    .type(
                            SilentClassroomType.DOOR
                    )
                    .view(view)
                    .zIndex(1050)
                    .buildAndAttach();
        }

        private void refreshVisual() {
            view.getChildren().clear();

            if (state == DoorState.OPEN) {
                drawOpenDoor();
            } else {
                drawClosedDoor();
            }
        }

        private void drawClosedDoor() {
            Rectangle frame =
                    new Rectangle(
                            width,
                            height,
                            Color.rgb(
                                    66,
                                    38,
                                    24
                            )
                    );

            Rectangle panel =
                    new Rectangle(
                            Math.max(
                                    10,
                                    width - 8
                            ),
                            Math.max(
                                    20,
                                    height - 8
                            ),
                            Color.rgb(
                                    126,
                                    76,
                                    42
                            )
                    );

            panel.setTranslateX(4);
            panel.setTranslateY(4);
            panel.setStroke(
                    Color.rgb(
                            54,
                            31,
                            20
                    )
            );
            panel.setStrokeWidth(2);

            Circle knob =
                    new Circle(
                            3,
                            Color.rgb(
                                    228,
                                    192,
                                    92
                            )
                    );

            knob.setCenterX(
                    width * 0.72
            );

            knob.setCenterY(
                    height * 0.52
            );

            view.getChildren().addAll(
                    frame,
                    panel,
                    knob
            );

            if (state == DoorState.LOCKED) {
                Rectangle lock =
                        new Rectangle(
                                10,
                                12,
                                Color.rgb(
                                        65,
                                        67,
                                        72
                                )
                        );

                lock.setTranslateX(
                        width / 2.0 - 5
                );

                lock.setTranslateY(
                        height / 2.0 - 6
                );

                Circle red =
                        new Circle(
                                2.5,
                                Color.RED
                        );

                red.setCenterX(
                        width / 2.0
                );

                red.setCenterY(
                        height / 2.0 - 2
                );

                view.getChildren().addAll(
                        lock,
                        red
                );
            }
        }

        private void drawOpenDoor() {
            Rectangle threshold =
                    new Rectangle(
                            width,
                            height,
                            Color.rgb(
                                    27,
                                    29,
                                    32,
                                    0.35
                            )
                    );

            Rectangle panel =
                    new Rectangle(
                            Math.max(
                                    height,
                                    54
                            ),
                            12,
                            Color.rgb(
                                    126,
                                    76,
                                    42
                            )
                    );

            panel.setStroke(
                    Color.rgb(
                            54,
                            31,
                            20
                    )
            );

            panel.setStrokeWidth(2);

            if ("left".equalsIgnoreCase(
                    openDirection
            )) {
                panel.setTranslateX(
                        -Math.max(
                                height - width,
                                24
                        )
                );
            }

            Rectangle hinge =
                    new Rectangle(
                            8,
                            16,
                            Color.rgb(
                                    66,
                                    38,
                                    24
                            )
                    );

            hinge.setTranslateY(
                    height - 16
            );

            view.getChildren().addAll(
                    threshold,
                    panel,
                    hinge
            );
        }

        private boolean blocksMovement() {
            return state != DoorState.OPEN;
        }

        private Rectangle2D getDoorRectangle() {
            return new Rectangle2D(
                    x,
                    y,
                    width,
                    height
            );
        }

        private Point2D getCentre() {
            return new Point2D(
                    x + width / 2.0,
                    y + height / 2.0
            );
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
