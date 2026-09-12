extends Node2D

const MAX_CONCURRENT_DRONES: int = 5

@export var drone_scene: PackedScene

@onready var camera: Camera2D = get_parent().get_node_or_null("Player/Camera2D")
@onready var alert_overlay: ColorRect = get_parent().get_node_or_null("AlertOverlay/ColorRect")

var weight_good: float = 0.28
var weight_bad: float = 0.68
var weight_guard: float = 0.04
var total_weight: float = weight_good + weight_bad + weight_guard

var active_guards: int = 0
var active_drones: int = 0
var alert_active: bool = false

var spawn_timer: float = 0.4

func _ready() -> void:
	spawn_timer = randf_range(0.3, 0.567)

func _process(delta: float) -> void:
	update_alert_overlay(delta)

	var main_parent: Node = get_parent()
	if main_parent != null and main_parent.get("countdown_active") == true:
		return

	spawn_timer -= delta
	if spawn_timer > 0.0:
		return
	spawn_timer = randf_range(0.3, 0.567)
	if active_drones >= MAX_CONCURRENT_DRONES:
		return
	spawn_drone()

func spawn_drone() -> void:
	var drone = drone_scene.instantiate() as Drone
	var roll = randf() * total_weight
	
	if roll < weight_good:
		drone.drone_type = Drone.DroneType.GOOD
	elif roll < weight_good + weight_bad:
		drone.drone_type = Drone.DroneType.BAD
	else:
		drone.drone_type = Drone.DroneType.GUARD
		handle_guard_spawn()

	var main_mgr: Node = get_parent()
	if main_mgr != null and main_mgr.has_method("register_drone_spawned"):
		main_mgr.register_drone_spawned(drone.drone_type)

	var camera_x: float = 0.0
	var camera_y: float = 0.0
	if camera != null:
		drone.reference_camera = camera
		camera_x = camera.global_position.x
		camera_y = camera.global_position.y

	# Bad drones randomly begin from one of three entry points:
	# top-right corner, top, or right, then fly normally.
	if drone.drone_type == Drone.DroneType.BAD:
		var entry := randi() % 3
		match entry:
			0:  # top-right corner
				drone.position = Vector2(
					camera_x + randf_range(750.0, 1700.0),
					camera_y - randf_range(350.0, 600.0)
				)
				drone.move_direction = Vector2(-0.7, 0.55).normalized()
			1:  # top (right half, never the left side)
				drone.position = Vector2(
					camera_x + randf_range(100.0, 700.0),
					camera_y - randf_range(450.0, 700.0)
				)
				drone.move_direction = Vector2(-0.35, 0.9).normalized()
			2:  # right
				drone.position = Vector2(
					camera_x + randf_range(650.0, 1600.0),
					camera_y + randf_range(-260.0, -40.0)
				)
				drone.move_direction = Vector2.LEFT
	else:
		# Good/guard drones: half from the right edge, half from above.
		if randf() < 0.5:
			drone.position = Vector2(
				camera_x + randf_range(650.0, 1600.0),
				camera_y + randf_range(-260.0, -40.0)
			)
			drone.move_direction = Vector2.LEFT
		else:
			drone.position = Vector2(
				camera_x + randf_range(-500.0, 500.0),
				camera_y - randf_range(450.0, 700.0)
			)
			drone.move_direction = Vector2(-0.55, 0.85).normalized()

	active_drones += 1
	drone.tree_exited.connect(_on_drone_removed.bind(drone.drone_type))
	add_child(drone)

func handle_guard_spawn() -> void:
	active_guards += 1
	alert_active = true

func is_alert_active() -> bool:
	return alert_active

func destroy_bad_on_screen() -> void:
	if camera == null:
		return
	var cam_pos := camera.global_position
	for child in get_children():
		if not (child is Drone):
			continue
		var drone: Drone = child
		if drone.drone_type != Drone.DroneType.BAD or drone.is_destroyed:
			continue
		var offset := drone.global_position - cam_pos
		if offset.x < -700.0 or offset.x > 700.0 or offset.y < -200.0 or offset.y > 900.0:
			continue
		drone.die()

func _on_drone_removed(type_exited: int) -> void:
	active_drones -= 1
	if type_exited == Drone.DroneType.GUARD:
		active_guards -= 1
		# Only fade out once the LAST guard has exited, so the alert
		# lingers for however long the newest guard stayed.
		if active_guards <= 0:
			alert_active = false

func update_alert_overlay(delta: float) -> void:
	var main_mgr: Node = get_parent()
	if main_mgr != null and main_mgr.get("game_over") == true:
		return
	if not is_instance_valid(alert_overlay):
		return
	# Single-writer fade: no tweens, so rapid guard spawns/exits can
	# never make two fades fight each other.
	var target_alpha := 0.45 if alert_active else 0.0
	var fade_rate := 0.9 if alert_active else 0.225
	alert_overlay.color.a = move_toward(alert_overlay.color.a, target_alpha, delta * fade_rate)
