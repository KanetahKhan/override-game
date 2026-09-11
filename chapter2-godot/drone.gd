class_name Drone 
extends CharacterBody2D

enum DroneType { GOOD, BAD, GUARD }

const DRONE_FLOOR_Y: float = 430.0

@export var drone_type: DroneType = DroneType.GOOD
var speed: float = 150.0
var hp: int = 30
var move_direction: Vector2 = Vector2.LEFT
var reference_camera: Camera2D = null

var time_passed: float = 0.0
var bob_amplitude: float = 40.0
var bob_speed: float = 3.0
var wobble_phase: float = 0.0
var is_destroyed: bool = false
var aim_sprite: Sprite2D
var _click_held: bool = false
var ambient_player: AudioStreamPlayer2D

const AIM_TEXTURE: Texture2D = preload("res://assets/Drones/aim.png")
const AIM_SCALE: float = 0.10

@onready var animated_sprite = $AnimatedSprite2D
@onready var collision_shape = $CollisionShape2D

func _ready() -> void:
	time_passed = randf() * 10.0
	wobble_phase = randf() * TAU 
	
	match drone_type:
		DroneType.GOOD:
			speed = 120.0
			hp = 20
			animated_sprite.play("fly_good")
			animated_sprite.scale = Vector2.ONE * 0.154
		DroneType.BAD:
			speed = 480.0
			hp = 30
			animated_sprite.play("fly_bad")
			animated_sprite.scale = Vector2.ONE * 0.11
		DroneType.GUARD:
			speed = 220.0
			hp = 80
			bob_amplitude = 60.0
			bob_speed = 5.0
			animated_sprite.play("fly_guard")
			animated_sprite.scale = Vector2.ONE * 0.088

	speed *= randf_range(0.8, 1.3)
	animated_sprite.flip_h = move_direction.x < 0.0

	if drone_type != DroneType.GUARD:
		aim_sprite = Sprite2D.new()
		aim_sprite.texture = AIM_TEXTURE
		aim_sprite.position = animated_sprite.position
		aim_sprite.scale = Vector2.ONE * AIM_SCALE
		aim_sprite.visible = false
		aim_sprite.z_index = 10
		add_child(aim_sprite)

	ambient_player = AudioStreamPlayer2D.new()
	if drone_type == DroneType.GUARD:
		ambient_player.stream = Sfx.stream("guard_beep")
		ambient_player.volume_db = -10.0
	else:
		ambient_player.stream = Sfx.stream("drone_hum")
		ambient_player.pitch_scale = 1.0 if drone_type == DroneType.GOOD else 1.18
		ambient_player.volume_db = -16.0
	ambient_player.max_distance = 1600.0
	add_child(ambient_player)
	ambient_player.play()

func _physics_process(delta: float) -> void:
	if is_destroyed:
		return
		
	time_passed += delta
	# Smooth flight: steady horizontal glide with a very gentle, slow
	# vertical drift. No jittery per-frame bouncing.
	var glide = 1.0 + 0.06 * sin(time_passed * 0.5 + wobble_phase)
	velocity.x = move_direction.x * speed * glide
	velocity.y = move_direction.y * speed + sin(time_passed * 1.2 + wobble_phase) * 18.0
	# Keep the drone above the character's head line — cap the downward
	# drift so the sprite never sinks below head level.
	var projected_y := global_position.y + velocity.y * delta
	if projected_y > DRONE_FLOOR_Y:
		global_position.y = DRONE_FLOOR_Y
		velocity.y = min(velocity.y, 0.0)
	move_and_slide()
	cull_if_far_from_camera()

func _process(_delta: float) -> void:
	if drone_type == DroneType.GUARD or is_destroyed:
		return
	var main_node: Node = get_parent().get_parent()
	if main_node != null and main_node.get("game_over") == true:
		if aim_sprite != null:
			aim_sprite.visible = false
		return
	# No aiming during the red alert zone.
	if is_red_alert_active():
		if aim_sprite != null:
			aim_sprite.visible = false
		return
	var mouse_pos: Vector2 = get_global_mouse_position()
	var tex: Texture2D = animated_sprite.sprite_frames.get_frame_texture(animated_sprite.animation, animated_sprite.frame)
	var rect := Rect2(animated_sprite.global_position - tex.get_size() * animated_sprite.scale * 0.5, tex.get_size() * animated_sprite.scale)
	var hovering: bool = rect.has_point(mouse_pos)
	aim_sprite.visible = hovering
	if hovering and Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT) and not _click_held:
		_click_held = true
		trigger_player_shot()
	if not Input.is_mouse_button_pressed(MOUSE_BUTTON_LEFT):
		_click_held = false

func is_red_alert_active() -> bool:
	var mgr: Node = get_parent()
	return mgr != null and mgr.has_method("is_alert_active") and mgr.is_alert_active()

func trigger_player_shot() -> void:
	var mgr: Node = get_parent()
	if mgr == null:
		return
	var main: Node = mgr.get_parent()
	if main != null and main.has_method("trigger_shoot"):
		main.trigger_shoot(
			global_position + Vector2(0.0, -120.0),
			Callable(self, "die"),
		)
	else:
		die()

func cull_if_far_from_camera() -> void:
	if reference_camera == null:
		return
	var offset := global_position - reference_camera.global_position
	if offset.x < -700.0 or offset.x > 1800.0 or offset.y < -800.0 or offset.y > 950.0:
		queue_free()

func take_damage(amount: int) -> void:
	if is_destroyed:
		return
		
	hp -= amount
	if hp <= 0:
		die()

func die() -> void:
	if drone_type != DroneType.GUARD:
		var main_node: Node = get_parent().get_parent()
		if main_node != null and main_node.has_method("register_drone_killed"):
			main_node.register_drone_killed(drone_type)
	if ambient_player != null and is_instance_valid(ambient_player):
		ambient_player.stop()
	is_destroyed = true
	collision_shape.set_deferred("disabled", true)
	if aim_sprite != null:
		aim_sprite.visible = false

	# Guard drones have no explosion frames: fade out instead.
	if drone_type == DroneType.GUARD:
		var fade := create_tween()
		fade.tween_property(animated_sprite, "modulate:a", 0.0, 0.4)
		fade.tween_callback(queue_free)
		return

	# Randomize orientation so the 6 frames look slightly different per kill
	animated_sprite.flip_h = randi() % 2 == 0
	animated_sprite.flip_v = randi() % 2 == 0
	Sfx.play(
		"boom",
		-2.0,
		randf_range(0.9, 1.15)
	)
	# Bigger boom
	animated_sprite.scale *= 6.0
	animated_sprite.position = Vector2.ZERO
	
	# Play the correct colored explosion
	if drone_type == DroneType.GOOD:
		animated_sprite.play("destroy_good")
	else:
		animated_sprite.play("destroy_bad")

	await animated_sprite.animation_finished
	queue_free()

func _on_visible_on_screen_notifier_2d_screen_exited() -> void:
	queue_free()
