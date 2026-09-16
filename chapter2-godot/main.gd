extends Node2D


const PLAYER_SPEED: float = 450.0
const PLAYER_CROUCH_SPEED: float = 120.0
const RED_ZONE_CRAWL_SPEED: float = 70.0
const CROUCH_BOB_SPEED: float = 15.0
const CROUCH_BOB_AMOUNT: float = 3.0
const CROUCH_BOB_RESET_LERP: float = 15.0


const AIM_30_LEFT := preload("res://assets/ren/aim_30_left.png")
const AIM_30_RIGHT := preload("res://assets/ren/aim_30_right.png")
const AIM_60_LEFT := preload("res://assets/ren/aim_60_left.png")
const AIM_60_RIGHT := preload("res://assets/ren/aim_60_right.png")
const BEAM_SHOT_7 := preload("res://assets/GUNGUN/beam_shot_07_short_trail.png")
const WALK_SOUND := preload("res://assets/ren/musicholder-walking-on-leaves-260279.mp3")


# Muzzle of the posed gun, in texture pixels relative to the sprite center.
const MUZZLE_30_RIGHT := Vector2(61.2, -20.4)
const MUZZLE_60_RIGHT := Vector2(48.8, -38.0)
const MUZZLE_30_LEFT := Vector2(-60.9, -20.6)
const MUZZLE_60_LEFT := Vector2(-49.7, -37.8)
const AIM_GROUND_LIFT: float = 13.0


# The shot sprite is a short trail: the bright round head sits at
# BEAM_MUZZLE_OFFSET (texture px relative to the sprite center) and the tail
# trails away at BEAM_TRAIL_ANGLE (radians) inside the texture.
const BEAM_MUZZLE_OFFSET := Vector2(-141.0, -147.2)
const BEAM_TRAIL_ANGLE: float = 0.796
const SHOT_SCALE: float = 0.3
const SHOT_SPEED: float = 1600.0


# =========================
# RUN / TIMER (reach the safe green zone in GAME_DURATION)
# =========================
const GAME_DURATION: float = 150.0
const PLAYER_START_X: float = 150.0
const TOTAL_RUN_DISTANCE: float = 20000.0
const PIXELS_PER_METER: float = 20.0
const HUD_FONT := preload("res://assets/Pixel Game.otf")
const HUD_FONT_SIZE: int = 20
const HUD_BIG_FONT_SIZE: int = 36
const DESTINATION_X: float = (
	PLAYER_START_X
	+
	TOTAL_RUN_DISTANCE
)
const RED_STAND_WARN: float = 3.0
const RED_BLEED_RATE: float = 4.0
const GOOD_DRONE_SCORE_PENALTY: float = 0.5


const SEGMENT_OVERLAP: float = 80.0


# Horizon atmospheric fade
@export_range(0.0, 1.0, 0.01)
var horizon_opacity: float = 0.75


@export_range(0.0, 400.0, 1.0)
var horizon_overlap: float = 100.0



# =========================
# CROP SETTINGS
# =========================

# =========================
# CROPS ONLY

# =========================
# PLAYER VARIABLES
# =========================

var camera_x: float = 0.0
var facing_right: bool = true
var player_gravity: float = 980.0


var is_attacking: bool = false
var is_defending: bool = false
var is_crouching: bool = false
var is_aiming: bool = false
var aim_timer: float = 0.0
var crouch_bob_time: float = 0.0


var beam_sprite: Sprite2D
var shot_tween: Tween
var walk_player: AudioStreamPlayer


var game_time_left: float = GAME_DURATION
var game_over: bool = false
var game_won_result: bool = false
var red_standing_time: float = 0.0
var bad_spawned: int = 0
var bad_killed: int = 0
var good_killed: int = 0
var final_score_percent: float = 0.0
var countdown_active: bool = true
var countdown_time: float = 3.0
var countdown_displayed: int = 0
var bg_start_requested: bool = false
var bg_start_revealed: bool = false
var bg_start_reveal_delay: float = 0.0
var time_label: Label
var warning_label: Label
var distance_bar_control: Control
var bar_image: TextureRect
var bar_fill: ColorRect
var bar_pointer: TextureRect
var bad_drone_icon: TextureRect
var bad_drone_label: Label
var current_run_pct: float = 0.0
var time_label_red: bool = false
var kk_space_was_pressed: bool = false
var end_card_layer: CanvasLayer
var end_card_label: Label
var countdown_label: Label


var hp: int = 100
var attack_timer: float = 0.0


var segment_w: float = 1005.0



# =========================
# NODES
# =========================

@onready var player: CharacterBody2D = $Player


@onready var player_sprite: Sprite2D = $Player/Sprite2D


@onready var camera_node: Camera2D = \
	$Player/Camera2D


@onready var anim: AnimationPlayer = \
	$Player/AnimationPlayer



@onready var ground: Node2D = $Ground


@onready var drone_manager: Node2D = $DroneManager




# Parallax

@onready var sky_layer: ParallaxLayer = \
	$ParallaxBackground/SkyLayer


@onready var sky_sprite: Sprite2D = \
	$ParallaxBackground/SkyLayer/sky2



@onready var horizon_layer: ParallaxLayer = \
	$ParallaxBackground/HorizonLayer


@onready var horizon_sprite: Sprite2D = \
	$ParallaxBackground/HorizonLayer/horizon





func _ready() -> void:

	bg_start_requested = (
		OS.get_environment("OVERRIDE_CH2_BG_START") == "1"
	)

	if bg_start_requested:

		DisplayServer.window_set_mode(
			DisplayServer.WINDOW_MODE_MINIMIZED
		)

		var delay_ms: float = float(
			OS.get_environment("OVERRIDE_CH2_REVEAL_DELAY_MS")
		)
		if delay_ms > 0.0:
			bg_start_reveal_delay = delay_ms / 1000.0


	texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST

	build_ground()

	build_crops()

	setup_sky()

	setup_horizon()

	build_destination()

	build_hud()

	walk_player = AudioStreamPlayer.new()
	walk_player.stream = WALK_SOUND
	walk_player.stream.loop = true
	add_child(walk_player)





# =====================================================
# SKY
# =====================================================

func setup_sky() -> void:

	if sky_sprite == null:
		return


	if sky_sprite.texture == null:
		return



	sky_sprite.texture_filter = \
		CanvasItem.TEXTURE_FILTER_NEAREST



	sky_sprite.position.x = round(
		sky_sprite.position.x
	)


	sky_sprite.position.y = round(
		sky_sprite.position.y
	)



	var sky_width: float = (
		sky_sprite.texture.get_width()
		* abs(sky_sprite.scale.x)
	)



	sky_width = round(
		sky_width
	)



	if sky_width < 1.0:
		sky_width = 1.0



	sky_layer.motion_mirroring = Vector2(
		sky_width,
		0.0
	)






# =====================================================
# HORIZON
# =====================================================


func setup_horizon() -> void:


	if horizon_sprite == null:
		return


	if horizon_sprite.texture == null:
		return



	# Solid atmospheric fading
	horizon_sprite.modulate = Color(
		horizon_opacity,
		horizon_opacity,
		horizon_opacity,
		1.0
	)



	horizon_sprite.texture_filter = \
		CanvasItem.TEXTURE_FILTER_NEAREST



	horizon_sprite.position.x = round(
		horizon_sprite.position.x
	)


	horizon_sprite.position.y = round(
		horizon_sprite.position.y
	)



	var horizon_width: float = (
		horizon_sprite.texture.get_width()
		*
		abs(horizon_sprite.scale.x)
	)



	var repeat_width: float = (
		horizon_width
		-
		horizon_overlap
	)



	repeat_width = round(
		repeat_width
	)



	if repeat_width < 1.0:

		repeat_width = 1.0



	horizon_layer.motion_mirroring = Vector2(
		repeat_width,
		0.0
	)

# =====================================================
# CROP SETTINGS
# =====================================================

const CROP_SEGMENTS: int = 8
const CROP_ROW_COUNT: int = 8
const MAXIMUM_CROP_RECYCLES: int = 128


@export_category("Crop Position")

# Global X coordinate where the crop field begins.
@export var crop_start_global_x: float = 0.0


# Bottom-contact Y coordinate for each crop row.
#
# Element 0 = highest/back row
# Element 7 = lowest/front row
@export var crop_row_ground_global_y: Array[float] = [
	390.0,
	420.0,
	450.0,
	480.0,
	510.0,
	540.0,
	570.0,
	600.0
]


# Horizontal starting offset for each row.
#
# Positive value = move row right
# Negative value = move row left
@export var crop_row_x_offset: Array[float] = [
	0.0,
	20.0,
	-15.0,
	30.0,
	-25.0,
	15.0,
	-10.0,
	25.0
]


@export_category("Crop Type")

# Crop0 = 0
# Crop1 = 1
# ...
# Crop6 = 6
@export_range(0, 6, 1)
var starting_crop_type: int = 0


# Number of recycled horizontal segments before the
# next crop type starts entering from the right.
#
# 8  = one complete field loop
# 16 = two complete loops
# 24 = three complete loops
@export_range(1, 200, 1)
var segments_before_crop_change: int = 4


@export_category("Crop Spacing")

# 0 = exact connection
# Negative = overlap
# Positive = gap
@export_range(-200.0, 200.0, 1.0)
var crop_gap: float = -8.0


# Enable slightly different spacing between segments.
@export var randomize_crop_gap: bool = false


@export_range(0.0, 100.0, 1.0)
var crop_gap_variation: float = 0.0


@export_category("Crop Sway")

# Random strength difference between generated crops.
@export_range(0.0, 1.0, 0.01)
var sway_strength_variation: float = 0.25


# Random speed difference between generated crops.
@export_range(0.0, 1.0, 0.01)
var sway_speed_variation: float = 0.20


# =====================================================
# CROP NODES
# =====================================================

@onready var crop_layer: Node2D = $CropLayer


@onready var crop_templates: Array[Sprite2D] = [
	$CropTemplates/Crop0,
	$CropTemplates/Crop1,
	$CropTemplates/Crop2,
	$CropTemplates/Crop3,
	$CropTemplates/Crop4,
	$CropTemplates/Crop5,
	$CropTemplates/Crop6
]


# =====================================================
# CROP RUNTIME VARIABLES
# =====================================================

var crop_segments: Array[Node2D] = []

var crop_row_ground_local_y: Array[float] = []

# Crop type currently entering from the right.
var active_crop_type: int = 0

# Counts recycled segments until the next type begins.
var recycled_segment_count: int = 0

var crop_rng: RandomNumberGenerator = (
	RandomNumberGenerator.new()
)


# =====================================================
# BUILD EIGHT CROP ROWS
# =====================================================

func build_crops() -> void:

	if crop_layer == null:
		return

	if crop_templates.is_empty():
		return


	crop_rng.randomize()


	# Hide all seven source templates.
	for template in crop_templates:

		if template != null:
			template.visible = false


	# Remove previously generated crop segments.
	for child in crop_layer.get_children():
		child.free()


	crop_segments.clear()
	crop_row_ground_local_y.clear()


	ensure_crop_row_settings()


	active_crop_type = clampi(
		starting_crop_type,
		0,
		crop_templates.size() - 1
	)


	recycled_segment_count = 0


	# Convert the global starting X point into
	# CropLayer-local coordinates.
	var start_local_x: float = crop_layer.to_local(
		Vector2(
			crop_start_global_x,
			crop_layer.global_position.y
		)
	).x


	# Convert all row-ground positions into
	# CropLayer-local coordinates.
	for row_index in range(CROP_ROW_COUNT):

		var local_ground_y: float = crop_layer.to_local(
			Vector2(
				crop_layer.global_position.x,
				crop_row_ground_global_y[row_index]
			)
		).y


		crop_row_ground_local_y.append(
			local_ground_y
		)


	var next_segment_origin_x: float = start_local_x


	for segment_index in range(CROP_SEGMENTS):

		# One segment contains all eight rows.
		var crop_segment := Node2D.new()

		crop_segment.name = (
			"CropSegment_%02d" % segment_index
		)


		crop_segment.position = Vector2(
			round(next_segment_origin_x),
			0.0
		)


		# Store the crop type used by this individual segment.
		crop_segment.set_meta(
			"crop_type",
			active_crop_type
		)


		crop_layer.add_child(crop_segment)
		crop_segments.append(crop_segment)


		for row_index in range(CROP_ROW_COUNT):

			var crop_row := Sprite2D.new()

			crop_row.name = (
				"CropRow_%02d" % row_index
			)


			crop_segment.add_child(crop_row)


			configure_crop_sprite(
				crop_row,
				active_crop_type,
				crop_row_ground_local_y[row_index],
				crop_row_x_offset[row_index]
			)


			# Lower/front rows render above back rows.
			crop_row.z_index = row_index


		# Place the next segment after this segment's
		# actual visible right edge.
		var current_right_edge: float = (
			get_segment_visual_right(
				crop_segment
			)
		)


		next_segment_origin_x = (
			current_right_edge
			+
			get_next_crop_gap()
			-
			get_minimum_row_x_offset()
		)


# =====================================================
# ENSURE EXACTLY EIGHT ROW SETTINGS
# =====================================================

func ensure_crop_row_settings() -> void:

	# Add missing Y values.
	while (
		crop_row_ground_global_y.size()
		<
		CROP_ROW_COUNT
	):

		var next_y: float = 600.0

		if not crop_row_ground_global_y.is_empty():

			next_y = (
				crop_row_ground_global_y[
					crop_row_ground_global_y.size() - 1
				]
				+
				30.0
			)


		crop_row_ground_global_y.append(
			next_y
		)


	# Remove excess Y values.
	while (
		crop_row_ground_global_y.size()
		>
		CROP_ROW_COUNT
	):

		crop_row_ground_global_y.remove_at(
			crop_row_ground_global_y.size() - 1
		)


	# Add missing X offsets.
	while (
		crop_row_x_offset.size()
		<
		CROP_ROW_COUNT
	):

		crop_row_x_offset.append(
			0.0
		)


	# Remove excess X offsets.
	while (
		crop_row_x_offset.size()
		>
		CROP_ROW_COUNT
	):

		crop_row_x_offset.remove_at(
			crop_row_x_offset.size() - 1
		)


# =====================================================
# CONFIGURE GENERATED CROP SPRITE
# =====================================================

func configure_crop_sprite(
	crop: Sprite2D,
	crop_type: int,
	ground_local_y: float,
	row_x_offset: float = 0.0
) -> void:

	if crop == null:
		return

	if crop_templates.is_empty():
		return


	var safe_type: int = wrapi(
		crop_type,
		0,
		crop_templates.size()
	)


	var template: Sprite2D = (
		crop_templates[safe_type]
	)


	if template == null:
		return

	if template.texture == null:
		return


	# Copy texture and manually selected region.
	crop.texture = template.texture

	crop.region_enabled = template.region_enabled


	if template.region_enabled:

		crop.region_rect = template.region_rect

		crop.region_filter_clip_enabled = (
			template.region_filter_clip_enabled
		)

	else:

		crop.region_filter_clip_enabled = false


	# Copy template size and appearance.
	crop.scale = template.scale

	crop.flip_h = template.flip_h
	crop.flip_v = template.flip_v

	crop.modulate = template.modulate
	crop.self_modulate = template.self_modulate

	crop.texture_filter = (
		CanvasItem.TEXTURE_FILTER_NEAREST
	)


	# Generated crops must not inherit a parent material.
	crop.use_parent_material = false


	# -------------------------------------------------
	# SWAY MATERIAL
	# -------------------------------------------------

	var base_material: ShaderMaterial = (
		template.material as ShaderMaterial
	)


	if base_material != null:

		# Every generated crop receives a separate material
		# so its sway timing can be different.
		var runtime_material: ShaderMaterial = (
			base_material.duplicate()
			as ShaderMaterial
		)


		if runtime_material != null:

			crop.material = runtime_material


			# Generate these values only once.
			if not crop.has_meta("sway_phase"):

				crop.set_meta(
					"sway_phase",
					crop_rng.randf_range(
						0.0,
						TAU
					)
				)


			if not crop.has_meta("sway_strength_mul"):

				crop.set_meta(
					"sway_strength_mul",
					crop_rng.randf_range(
						maxf(
							0.0,
							1.0 - sway_strength_variation
						),
						1.0 + sway_strength_variation
					)
				)


			if not crop.has_meta("sway_speed_mul"):

				crop.set_meta(
					"sway_speed_mul",
					crop_rng.randf_range(
						maxf(
							0.0,
							1.0 - sway_speed_variation
						),
						1.0 + sway_speed_variation
					)
				)


			runtime_material.set_shader_parameter(
				"sway_phase",
				float(
					crop.get_meta("sway_phase")
				)
			)


			runtime_material.set_shader_parameter(
				"sway_strength_mul",
				float(
					crop.get_meta(
						"sway_strength_mul"
					)
				)
			)


			runtime_material.set_shader_parameter(
				"sway_speed_mul",
				float(
					crop.get_meta(
						"sway_speed_mul"
					)
				)
			)


			runtime_material.set_shader_parameter(
				"source_height",
				get_template_height(template)
			)

	else:

		# Preserve a non-shader material when one exists.
		crop.material = template.material


	# Use top-left positioning.
	crop.centered = false
	crop.offset = Vector2.ZERO


	var displayed_height: float = (
		get_template_height(template)
		*
		abs(template.scale.y)
	)


	# Apply each row's X offset and keep its bottom
	# touching the configured ground line.
	crop.position = Vector2(
		round(row_x_offset),
		round(
			ground_local_y
			-
			displayed_height
		)
	)


	crop.visible = true


# =====================================================
# GET TEMPLATE WIDTH
# =====================================================

func get_template_width(
	template: Sprite2D
) -> float:

	if template == null:
		return 1.0

	if template.texture == null:
		return 1.0


	if template.region_enabled:

		return maxf(
			1.0,
			template.region_rect.size.x
		)


	return maxf(
		1.0,
		float(
			template.texture.get_width()
		)
	)


# =====================================================
# GET TEMPLATE HEIGHT
# =====================================================

func get_template_height(
	template: Sprite2D
) -> float:

	if template == null:
		return 1.0

	if template.texture == null:
		return 1.0


	if template.region_enabled:

		return maxf(
			1.0,
			template.region_rect.size.y
		)


	return maxf(
		1.0,
		float(
			template.texture.get_height()
		)
	)


# =====================================================
# GET ROW OFFSET BOUNDS
# =====================================================

func get_minimum_row_x_offset() -> float:

	if crop_row_x_offset.is_empty():
		return 0.0


	var minimum_offset: float = (
		crop_row_x_offset[0]
	)


	for row_offset in crop_row_x_offset:

		if row_offset < minimum_offset:
			minimum_offset = row_offset


	return minimum_offset


func get_maximum_row_x_offset() -> float:

	if crop_row_x_offset.is_empty():
		return 0.0


	var maximum_offset: float = (
		crop_row_x_offset[0]
	)


	for row_offset in crop_row_x_offset:

		if row_offset > maximum_offset:
			maximum_offset = row_offset


	return maximum_offset


# =====================================================
# GET DISPLAYED WIDTH FOR A CROP TYPE
# =====================================================

func get_crop_type_display_width(
	crop_type: int
) -> float:

	if crop_templates.is_empty():
		return 1.0


	var safe_type: int = wrapi(
		crop_type,
		0,
		crop_templates.size()
	)


	var template: Sprite2D = (
		crop_templates[safe_type]
	)


	if template == null:
		return 1.0


	return maxf(
		1.0,
		get_template_width(template)
		*
		abs(template.scale.x)
	)


# =====================================================
# GET SEGMENT CROP TYPE
# =====================================================

func get_segment_crop_type(
	crop_segment: Node2D
) -> int:

	if crop_segment == null:
		return active_crop_type


	return int(
		crop_segment.get_meta(
			"crop_type",
			active_crop_type
		)
	)


# =====================================================
# GET SEGMENT VISUAL BOUNDS
# =====================================================

func get_segment_visual_left(
	crop_segment: Node2D
) -> float:

	if crop_segment == null:
		return 0.0


	return (
		crop_segment.position.x
		+
		get_minimum_row_x_offset()
	)


func get_segment_visual_right(
	crop_segment: Node2D
) -> float:

	if crop_segment == null:
		return 0.0


	var segment_crop_type: int = (
		get_segment_crop_type(
			crop_segment
		)
	)


	return (
		crop_segment.position.x
		+
		get_maximum_row_x_offset()
		+
		get_crop_type_display_width(
			segment_crop_type
		)
	)


# =====================================================
# CALCULATE NEXT SEGMENT GAP
# =====================================================

func get_next_crop_gap() -> float:

	var selected_gap: float = crop_gap


	if (
		randomize_crop_gap
		and
		crop_gap_variation > 0.0
	):

		selected_gap += crop_rng.randf_range(
			-crop_gap_variation,
			crop_gap_variation
		)


	return selected_gap


# =====================================================
# CHANGE ONE OFFSCREEN SEGMENT
# =====================================================

func set_segment_crop_type(
	crop_segment: Node2D,
	new_crop_type: int
) -> void:

	if crop_segment == null:
		return

	if crop_templates.is_empty():
		return


	var safe_type: int = wrapi(
		new_crop_type,
		0,
		crop_templates.size()
	)


	crop_segment.set_meta(
		"crop_type",
		safe_type
	)


	for row_index in range(CROP_ROW_COUNT):

		var crop_row := (
			crop_segment.get_node_or_null(
				"CropRow_%02d" % row_index
			)
			as Sprite2D
		)


		if crop_row == null:
			continue


		configure_crop_sprite(
			crop_row,
			safe_type,
			crop_row_ground_local_y[row_index],
			crop_row_x_offset[row_index]
		)


# =====================================================
# INFINITE REPETITION WITH STREAMING CROP CHANGES
# =====================================================

func update_crops() -> void:

	if crop_segments.is_empty():
		return

	if camera_node == null:
		return

	if crop_layer == null:
		return


	var camera_local_x: float = crop_layer.to_local(
		camera_node.global_position
	).x


	# A segment changes only after it is safely
	# behind the camera on the left.
	var recycle_boundary: float = (
		camera_local_x - 1500.0
	)


	var recycle_safety: int = 0


	while (
		recycle_safety
		<
		MAXIMUM_CROP_RECYCLES
	):

		recycle_safety += 1


		var leftmost_segment: Node2D = (
			crop_segments[0]
		)


		var leftmost_visual_x: float = (
			get_segment_visual_left(
				leftmost_segment
			)
		)


		var rightmost_visual_edge: float = (
			get_segment_visual_right(
				crop_segments[0]
			)
		)


		# Find the leftmost segment and the furthest
		# visible right edge.
		for crop_segment in crop_segments:

			if not is_instance_valid(crop_segment):
				continue


			var segment_left: float = (
				get_segment_visual_left(
					crop_segment
				)
			)


			var segment_right: float = (
				get_segment_visual_right(
					crop_segment
				)
			)


			if segment_left < leftmost_visual_x:

				leftmost_visual_x = segment_left
				leftmost_segment = crop_segment


			if segment_right > rightmost_visual_edge:

				rightmost_visual_edge = segment_right


		var leftmost_right_edge: float = (
			get_segment_visual_right(
				leftmost_segment
			)
		)


		# The leftmost segment has not fully passed
		# the recycle boundary yet.
		if (
			leftmost_right_edge
			>=
			recycle_boundary
		):
			break


		recycled_segment_count += 1


		var required_recycles: int = maxi(
			1,
			segments_before_crop_change
		)


		# After the selected number of recycled segments,
		# begin sending the next crop type from the right.
		if (
			recycled_segment_count
			>=
			required_recycles
		):

			recycled_segment_count = 0


			active_crop_type = wrapi(
				active_crop_type + 1,
				0,
				crop_templates.size()
			)


		# This segment is already offscreen.
		# Change only this segment, not the entire field.
		set_segment_crop_type(
			leftmost_segment,
			active_crop_type
		)


		# Move it to the right side of the crop field.
		leftmost_segment.position.x = round(
			rightmost_visual_edge
			+
			get_next_crop_gap()
			-
			get_minimum_row_x_offset()
		)
# =====================================================
# GROUND
# =====================================================


func build_ground() -> void:


	var template := (
		$Ground/GroundTemplate
		as Node2D
	)



	if template == null:
		return



	var g1 := template.get_node_or_null(
		"ground1"
	)as Sprite2D



	if g1 == null:
		return



	if g1.texture == null:
		return



	segment_w = (
		g1.texture.get_width()
		*
		abs(g1.scale.x)
	)



	var segment_count: int = 8



	var step: float = (
		segment_w
		-
		SEGMENT_OVERLAP
	)



	for i in range(segment_count):


		var duplicate_segment := (
			template.duplicate()
			as Node2D
		)



		if duplicate_segment == null:
			continue



		duplicate_segment.position.x = round(
			i * step
		)



		ground.add_child(
			duplicate_segment
		)



	template.queue_free()
	
	# =====================================================
# PLAYER PHYSICS
# =====================================================


func _physics_process(delta: float) -> void:


	if player == null:
		return


	if game_over:

		player.velocity = Vector2.ZERO

		player.move_and_slide()

		return


	if countdown_active:

		update_countdown(
			delta
		)

		apply_gravity(delta)

		player.velocity.x = 0.0

		player.move_and_slide()

		update_animation(
			0.0
		)

		update_camera()

		update_ground()

		update_crops()

		return


	apply_gravity(delta)

	read_player_input()

	update_attack_timer(delta)

	update_aim_timer(delta)


	player.move_and_slide()

	# Ren never jumps — pin vertical velocity so he can never leave the
	# ground, even during physics edge-cases.
	player.velocity.y = 0.0



	var movement_direction: float = (
		player.velocity.x
	)



	update_animation(
		movement_direction
	)

	update_crouch_bob(
		movement_direction,
		delta
	)

	update_footsteps(
		movement_direction,
		delta
	)



	update_camera()

	update_ground()

	update_crops()


	update_run_timer(delta)

	check_destination()





func apply_gravity(delta: float) -> void:


	if not player.is_on_floor():

		player.velocity.y += (
			player_gravity
			*
			delta
		)







# =====================================================
# INPUT
# =====================================================


func read_player_input() -> void:


	is_crouching = (
		Input.is_key_pressed(KEY_DOWN)
		or
		Input.is_key_pressed(KEY_W)
	)



	is_defending = Input.is_key_pressed(
		KEY_S
	)



	var direction: float = 0.0



	if Input.is_key_pressed(
		KEY_RIGHT
	):

		direction = 1.0

		facing_right = true





	var current_speed: float



	if is_crouching:

		current_speed = (
			RED_ZONE_CRAWL_SPEED
			if is_red_alert_active()
			else PLAYER_CROUCH_SPEED
		)

	else:

		current_speed = PLAYER_SPEED





	player.velocity.x = (
		direction
		*
		current_speed
	)





	var kk_space_down: bool = (
		Input.is_key_pressed(KEY_SPACE)
	)

	if (
		kk_space_down
		and
		not kk_space_was_pressed
	):

		trigger_kk_from_keyboard()

	kk_space_was_pressed = kk_space_down

	# Jumping is REMOVED from Chapter 2 — Ren never jumps, ever.

	if (
		Input.is_key_pressed(KEY_F)
		and
		not is_attacking
	):

		attack()


# Space → activate the KK Assist button (keyboard click), honoring the same
# gates the on-screen button uses (red alert, game over, remaining uses).
func trigger_kk_from_keyboard() -> void:

	var kk_button: Node = get_node_or_null(
		"KkHud/KkAssistButton"
	)
	if kk_button == null:
		return

	if kk_button.has_method("trigger_from_keyboard"):
		kk_button.call("trigger_from_keyboard")

# =====================================================
# ATTACK TIMER
# =====================================================


func update_attack_timer(
	delta: float
) -> void:


	if attack_timer <= 0.0:

		return



	attack_timer -= delta



	if attack_timer <= 0.0:

		attack_timer = 0.0

		is_attacking = false







# =====================================================
# ANIMATION
# =====================================================


func update_animation(
	direction: float
) -> void:


	if anim == null:

		return


	if is_aiming:

		return

	# Ren never jumps, so there is no airborne state. Always walk or idle.
	if is_crouching:


		play_animation(
			"crouch_right"
		)



	elif direction != 0.0:


		play_animation(
			"walk_right"
		)



	else:


		play_animation(
			"idle_right"
		)







func play_animation(
	animation_name: StringName
) -> void:


	if anim.current_animation != animation_name:

		anim.play(
			animation_name
		)




func is_red_alert_active() -> bool:


	return (
		drone_manager != null
		and
		drone_manager.has_method("is_alert_active")
		and
		drone_manager.is_alert_active()
	)




func update_crouch_bob(
	direction: float,
	delta: float
) -> void:


	if is_aiming:

		return


	if not is_crouching:

		crouch_bob_time = 0.0

		player_sprite.position.y = lerpf(
			player_sprite.position.y,
			0.0,
			delta * CROUCH_BOB_RESET_LERP
		)

		return


	if direction == 0.0:

		crouch_bob_time = 0.0

		player_sprite.position.y = lerpf(
			player_sprite.position.y,
			0.0,
			delta * CROUCH_BOB_RESET_LERP
		)

		return


	if is_aiming:

		return


	crouch_bob_time += delta * CROUCH_BOB_SPEED

	var bob_offset: float = (
		abs(
			sin(
				crouch_bob_time
			)
		)
		*
		CROUCH_BOB_AMOUNT
	)

	if is_red_alert_active():

		bob_offset *= 2.0

	player_sprite.position.y = -bob_offset




# =====================================================
# SHOOTING
# =====================================================


func trigger_shoot(
	target_pos: Vector2,
	on_hit: Callable = Callable()
) -> void:


	Sfx.play(
		"shot",
		-8.0,
		randf_range(0.95, 1.05)
	)

	# Cancel any upward motion so Ren doesn't appear to jump while shooting.
	if player.velocity.y < 0.0:
		player.velocity.y = 0.0


	var to_target := (
		target_pos
		-
		player.global_position
	)


	var shoot_left: bool = (
		to_target.x < 0.0
	)


	var elevation: float = (
		abs(
			rad_to_deg(
				atan2(
					to_target.y,
					abs(to_target.x)
				)
			)
		)
	)


	var pose: Texture2D

	var muzzle_offset: Vector2

	if shoot_left:

		if elevation >= 45.0:

			pose = AIM_60_LEFT
			muzzle_offset = MUZZLE_60_LEFT

		else:

			pose = AIM_30_LEFT
			muzzle_offset = MUZZLE_30_LEFT

	else:

		if elevation >= 45.0:

			pose = AIM_60_RIGHT
			muzzle_offset = MUZZLE_60_RIGHT

		else:

			pose = AIM_30_RIGHT
			muzzle_offset = MUZZLE_30_RIGHT


	if anim != null:

		anim.stop()


	player_sprite.texture = pose

	player_sprite.position.y = (
		player_sprite.position.y
		+
		AIM_GROUND_LIFT
		*
		player_sprite.scale.y
	)

	is_aiming = true
	aim_timer = 0.3


	var muzzle_world := (
		player.global_position
		+
		player_sprite.position
		+
		muzzle_offset * player_sprite.scale
	)


	spawn_shot(
		muzzle_world,
		target_pos,
		on_hit
	)




func update_aim_timer(
	delta: float
) -> void:


	if not is_aiming:

		return


	aim_timer -= delta

	if aim_timer <= 0.0:

		is_aiming = false




func spawn_shot(
	from_pos: Vector2,
	to_pos: Vector2,
	on_hit: Callable = Callable()
) -> void:


	if beam_sprite == null:

		beam_sprite = Sprite2D.new()

		beam_sprite.texture = BEAM_SHOT_7

		beam_sprite.texture_filter = (
			CanvasItem.TEXTURE_FILTER_LINEAR
		)

		beam_sprite.z_index = 5

		add_child(
			beam_sprite
		)


	if shot_tween != null and shot_tween.is_valid():

		shot_tween.kill()


	var to_target := (
		to_pos
		-
		from_pos
	)

	var distance := to_target.length()

	if distance <= 0.001:

		distance = 0.001


	beam_sprite.scale = Vector2(
		SHOT_SCALE,
		SHOT_SCALE
	)

	beam_sprite.global_rotation = (
		to_target.angle()
		+
		PI
		-
		BEAM_TRAIL_ANGLE
	)


	var shot_head_local := (
		BEAM_MUZZLE_OFFSET
		*
		beam_sprite.scale
	).rotated(
		beam_sprite.global_rotation
	)


	beam_sprite.position = (
		from_pos
		-
		shot_head_local
	)

	beam_sprite.visible = true


	var flight_time: float = clampf(
		distance / SHOT_SPEED,
		0.1,
		0.4
	)


	shot_tween = create_tween()

	shot_tween.tween_property(
		beam_sprite,
		"position",
		to_pos,
		flight_time
	)

	shot_tween.tween_callback(
		func() -> void:

			beam_sprite.visible = false

			if on_hit.is_valid():

				on_hit.call()
	)


func update_camera() -> void:


	camera_x = (
		player.global_position.x
		-
		300.0
	)



	if camera_x < 0.0:

		camera_x = 0.0





	camera_node.global_position.x = round(
		camera_x + 640.0
	)



	camera_node.global_position.y = 360.0







# =====================================================
# GROUND LOOPING
# =====================================================


func update_ground() -> void:


	var children: Array[Node] = (
		ground.get_children()
	)



	if children.is_empty():

		return





	var segments: Array[Node2D] = []



	for child in children:


		var segment := (
			child
			as
			Node2D
		)



		if segment != null:

			segments.append(
				segment
			)





	if segments.is_empty():

		return





	var step: float = (
		segment_w
		-
		SEGMENT_OVERLAP
	)





	var camera_left: float = (
		camera_node.global_position.x
		-
		900.0
	)





	var maximum_x: float = (
		segments[0].position.x
	)





	for segment in segments:


		if segment.position.x > maximum_x:

			maximum_x = segment.position.x






	for segment in segments:


		if (
			segment.position.x
			+
			step
			<
			camera_left
		):


			maximum_x += step



			segment.position.x = round(
				maximum_x
			)







# =====================================================
# COMBAT
# =====================================================


func attack() -> void:


	is_attacking = true

	attack_timer = 0.3







func take_damage(
	amount: int
) -> void:


	if is_defending:


		amount = int(
			ceil(
				amount * 0.5
			)
		)





	hp -= amount





	if hp <= 0:

		die()







func die() -> void:


	if game_over:
		return


	get_tree().reload_current_scene()



# =====================================================
# RUN TIMER / SAFE ZONE / SCORE
# =====================================================

func update_run_timer(delta: float) -> void:


	game_time_left -= delta


	update_red_standing(delta)


	if game_time_left <= 0.0:

		game_time_left = 0.0

		end_game(false)

		return


	update_hud()


func update_red_standing(delta: float) -> void:


	if not is_red_alert_active():

		red_standing_time = 0.0

		return


	if is_crouching:

		red_standing_time = 0.0

	else:

		red_standing_time += delta

		# Standing upright in a red alert drains the timer fast (1x normal
		# countdown + 4x bleed = 5x total).
		game_time_left = max(
			0.0,
			game_time_left - RED_BLEED_RATE * delta
		)


	if game_time_left <= 0.0:

		game_time_left = 0.0


func check_destination() -> void:


	if player == null:

		return


	if player.global_position.x >= DESTINATION_X:

		end_game(true)


func reveal_from_bg_start() -> void:


	if not bg_start_requested or bg_start_revealed:

		return


	bg_start_revealed = true


	if OS.get_environment("OVERRIDE_CH2_FULLSCREEN") == "1":

		DisplayServer.window_set_mode(
			DisplayServer.WINDOW_MODE_FULLSCREEN
		)

	else:

		DisplayServer.window_set_mode(
			DisplayServer.WINDOW_MODE_MAXIMIZED
		)

	DisplayServer.window_move_to_foreground()

	get_window().grab_focus()


func _unhandled_input(event: InputEvent) -> void:

	if event is InputEventKey and event.pressed and not event.echo:

		match event.physical_keycode:

			KEY_ESCAPE:
				if is_fullscreen_active():
					exit_fullscreen()

			KEY_F11:
				toggle_fullscreen()


func is_fullscreen_active() -> bool:
	return DisplayServer.window_get_mode() == DisplayServer.WINDOW_MODE_FULLSCREEN


func exit_fullscreen() -> void:
	DisplayServer.window_set_mode(DisplayServer.WINDOW_MODE_WINDOWED)
	get_window().grab_focus()


func toggle_fullscreen() -> void:
	if is_fullscreen_active():
		exit_fullscreen()
	else:
		DisplayServer.window_set_mode(DisplayServer.WINDOW_MODE_FULLSCREEN)
		get_window().grab_focus()


func update_countdown(delta: float) -> void:

	# Background-start: stay hidden (minimized) under the Java fake loading
	# bar for the configured delay, then reveal and run the real countdown.
	if (
		bg_start_requested
		and
		not bg_start_revealed
		and
		bg_start_reveal_delay > 0.0
	):

		bg_start_reveal_delay -= delta

		if bg_start_reveal_delay > 0.0:
			return

	reveal_from_bg_start()

	countdown_time -= delta

	var step: int = maxi(
		int(ceil(countdown_time)),
		0
	)

	if step != countdown_displayed:

		countdown_displayed = step

		Sfx.play(
			"beep",
			-6.0,
			1.0 + float(3 - step) * 0.15
		)


	if countdown_time <= 0.0:

		countdown_active = false

		if countdown_label != null:

			countdown_label.text = "GO!"

			countdown_label.modulate.a = 1.0

			countdown_label.visible = true

			var fade := create_tween()

			fade.tween_property(
				countdown_label,
				"modulate:a",
				0.0,
				0.7
			)

			fade.tween_callback(
				func() -> void:

					countdown_label.visible = false
			)

		Sfx.play(
			"go",
			-2.0
		)

		return


	if countdown_label != null:

		countdown_label.text = "%d" % step


func update_footsteps(
	direction: float,
	_delta: float
) -> void:


	if walk_player == null:

		return


	var should_walk: bool = (
		player != null
		and
		player.is_on_floor()
		and
		abs(direction) > 1.0
	)


	if not should_walk:

		walk_player.stop()

		return


	if is_crouching:

		walk_player.pitch_scale = 0.75

		walk_player.volume_db = -10.0

	else:

		walk_player.pitch_scale = 1.0

		walk_player.volume_db = -8.0


	if not walk_player.playing:

		walk_player.play()


func end_game(win: bool) -> void:


	if game_over:
		return


	game_over = true

	game_won_result = win


	if win:

		Sfx.play(
			"win",
			-4.0
		)


	if drone_manager != null:

		drone_manager.set_process(false)


	if player != null:

		player.velocity = Vector2.ZERO


	var overlay := get_node_or_null(
		"AlertOverlay/ColorRect"
	) as ColorRect


	if overlay != null:

		var tw: Tween

		if win:

			tw = create_tween()

			tw.tween_property(
				overlay,
				"color:a",
				0.0,
				0.6
			)

		else:

			tw = create_tween()

			tw.tween_property(
				overlay,
				"color:a",
				1.0,
				0.7
			)


	final_score_percent = calc_final_score()

	show_end_card(win)


func calc_final_score() -> float:


	var rate: float

	if bad_spawned <= 0:

		rate = 100.0

	else:

		rate = (
			float(bad_killed)
			/
			float(bad_spawned)
			*
			100.0
		)


	var good_penalty: float = (
		float(good_killed)
		*
		GOOD_DRONE_SCORE_PENALTY
	)


	rate -= good_penalty

	if rate < 0.0:

		rate = 0.0


	return rate


func register_drone_spawned(kind: int) -> void:


	if kind == Drone.DroneType.BAD:

		bad_spawned += 1


func register_drone_killed(kind: int) -> void:


	match kind:

		Drone.DroneType.GOOD:

			good_killed += 1

		Drone.DroneType.BAD:

			bad_killed += 1


func show_end_card(win: bool) -> void:


	if end_card_layer == null:

		build_hud()


	var progress_percent: float = 0.0

	if player != null:

		progress_percent = clampf(
			(
				player.global_position.x
				-
				PLAYER_START_X
			)
			/ TOTAL_RUN_DISTANCE
			* 100.0,
			0.0,
			100.0
		)


	var kill_phase: String = (
		"Bad drones killed: %d / %d (%.1f%%)"
		% [
			bad_killed,
			bad_spawned,
			(
				float(bad_killed)
				/ float(bad_spawned)
				* 100.0
				if bad_spawned > 0
				else 100.0
			),
		]
	)


	var good_phase: String = (
		"Good drones hurt: %d x %.1f%% = -%.1f%%"
		% [
			good_killed,
			GOOD_DRONE_SCORE_PENALTY,
			float(good_killed) * GOOD_DRONE_SCORE_PENALTY,
		]
	)


	if win:

		end_card_label.text = (
			"SAFE ZONE REACHED!"
			+ "
"
			+ "Time left: "
			+ ("%.2f s" % [game_time_left])
			+ "
"
			+ kill_phase
			+ "
"
			+ good_phase
			+ "
"
			+ "
FINAL SCORE: "
			+ ("%.1f%%" % [final_score_percent])
		)

	else:

		end_card_label.text = (
			"TIME UP - ENFORCEMENT CAUGHT YOU!"
			+ "
"
			+ "Safe zone reached only "
			+ ("%.1f%%" % [progress_percent])
			+ " of the way"
			+ "
"
			+ kill_phase
			+ "
"
			+ good_phase
			+ "
"
			+ "
FINAL SCORE: "
			+ ("%.1f%%" % [final_score_percent])
		)


	# Result page CUT per player request: Chapter 2 no longer shows its own
	# end/result page. The game just ends here. The result info is still stored
	# to JSON (save_result_to_json above) so the Java project can show it as its
	# own result screen later.
	save_result_to_json(win)
	get_tree().quit()


func save_result_to_json(win: bool) -> void:
	# Chapter 2 stores its end result so the main Java project can show it as
	# its own result screen. Java passes the file path via
	# OVERRIDE_CH2_RESULT_FILE (pattern: OS.get_environment), defaulting next to
	# the game's build folder when not set.
	var json_path: String = OS.get_environment("OVERRIDE_CH2_RESULT_FILE")
	if json_path.is_empty():
		json_path = "res://../build/chapter2_result.json"

	var data := {
		"win": win,
		"final_score_percent": final_score_percent,
		"bad_killed": bad_killed,
		"bad_spawned": bad_spawned,
		"good_killed": good_killed,
		"game_time_left": game_time_left,
		"progress_percent": clampf(
			(
				(player.global_position.x - PLAYER_START_X)
				/ TOTAL_RUN_DISTANCE
				* 100.0
			) if player != null else 0.0,
			0.0,
			100.0
		),
		"saved_at_unix": Time.get_unix_time_from_system(),
	}
	var file := FileAccess.open(json_path, FileAccess.WRITE)
	if file == null:
		push_warning("Chapter2: could not store result to " + json_path)
		return
	file.store_string(JSON.stringify(data))
	file.close()


func build_hud() -> void:


	if end_card_layer == null:

		var end_layer := CanvasLayer.new()

		end_layer.name = "EndCard"

		end_layer.layer = 120

		add_child(end_layer)

		var rect := ColorRect.new()

		rect.name = "Backdrop"

		rect.anchor_right = 1.0

		rect.anchor_bottom = 1.0

		rect.color = Color(
			0.0,
			0.0,
			0.0,
			0.78
		)

		end_layer.add_child(rect)

		var lbl := Label.new()

		lbl.name = "Message"

		lbl.anchor_left = 0.5

		lbl.anchor_right = 0.5

		lbl.anchor_top = 0.5

		lbl.anchor_bottom = 0.5

		lbl.offset_left = -430.0

		lbl.offset_right = 430.0

		lbl.offset_top = -130.0

		lbl.offset_bottom = 160.0

		lbl.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER

		lbl.vertical_alignment = VERTICAL_ALIGNMENT_CENTER

		lbl.add_theme_font_size_override("font_size", 34)

		lbl.add_theme_font_override("font", HUD_FONT)

		lbl.add_theme_color_override("font_color", Color(1, 1, 1, 1))

		lbl.add_theme_color_override("font_outline_color", Color(0, 0, 0, 1))

		lbl.add_theme_constant_override("outline_size", 8)

		end_layer.add_child(lbl)

		end_layer.visible = false

		end_card_layer = end_layer

		end_card_label = lbl


	if countdown_label == null:

		var count_layer := CanvasLayer.new()

		count_layer.name = "CountdownLayer"

		count_layer.layer = 115

		add_child(count_layer)

		countdown_label = Label.new()

		countdown_label.anchor_left = 0.5

		countdown_label.anchor_right = 0.5

		countdown_label.anchor_top = 0.4

		countdown_label.anchor_bottom = 0.4

		countdown_label.offset_left = -260.0

		countdown_label.offset_right = 260.0

		countdown_label.offset_top = -80.0

		countdown_label.offset_bottom = 120.0

		countdown_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER

		countdown_label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER

		countdown_label.add_theme_font_size_override("font_size", 110)

		countdown_label.add_theme_font_override("font", HUD_FONT)

		countdown_label.add_theme_color_override("font_color", Color(1, 1, 0.6, 1))

		countdown_label.add_theme_color_override("font_outline_color", Color(0, 0, 0, 1))

		countdown_label.add_theme_constant_override("outline_size", 14)

		count_layer.add_child(countdown_label)


	var kk_hud := get_node_or_null(
		"KkHud"
	) as CanvasLayer

	if kk_hud == null:

		return


	if time_label == null:

		time_label = get_node_or_null(
			"KkHud/TimeLabel"
		) as Label


	if distance_bar_control == null:

		distance_bar_control = get_node_or_null(
			"KkHud/DistanceBarControl"
		) as Control


	if bar_fill == null:

		bar_fill = get_node_or_null(
			"KkHud/DistanceBarControl/BarFill"
		) as ColorRect


	if bar_image == null:

		bar_image = get_node_or_null(
			"KkHud/DistanceBarControl/BarImage"
		) as TextureRect
		if bar_image != null:
			bar_image.texture = preload(
				"res://assets/game-stuffs/disbar.png"
			)
			bar_image.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
			bar_image.stretch_mode = TextureRect.STRETCH_SCALE


	if bar_pointer == null:

		bar_pointer = get_node_or_null(
			"KkHud/DistanceBarControl/DistancePointer"
		) as TextureRect
		if bar_pointer != null:
			bar_pointer.texture = preload(
				"res://assets/game-stuffs/dispointer.png"
			)
			bar_pointer.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
			bar_pointer.stretch_mode = TextureRect.STRETCH_SCALE


	if bad_drone_icon == null:

		bad_drone_icon = get_node_or_null(
			"KkHud/BadDroneTracker/BadDroneIcon"
		) as TextureRect
		if bad_drone_icon != null:
			bad_drone_icon.texture = preload(
				"res://assets/Drones/bad_drone.png"
			)
			bad_drone_icon.expand_mode = TextureRect.EXPAND_IGNORE_SIZE
			bad_drone_icon.stretch_mode = TextureRect.STRETCH_SCALE


	if bad_drone_label == null:

		bad_drone_label = get_node_or_null(
			"KkHud/BadDroneTracker/BadDronePercent"
		) as Label
		if bad_drone_label != null:
			bad_drone_label.add_theme_font_override("font", HUD_FONT)
			bad_drone_label.add_theme_font_size_override("font_size", HUD_BIG_FONT_SIZE)
			bad_drone_label.add_theme_color_override(
				"font_color",
				Color(1, 0, 1)
			)
			bad_drone_label.add_theme_constant_override("outline_size", 6)


	if warning_label == null:

		warning_label = get_node_or_null(
			"KkHud/WarningLabel"
		) as Label


	style_hud_labels()
	style_time_label()

	align_hud_elements()

	update_hud()


func align_hud_elements() -> void:

	var margin_x: float = 40.0

	var row_1_y: float = 40.0

	var row_2_y: float = 110.0

	var kk_control: Control = get_node_or_null(
		"KkHud/KkAssistButton"
	) as Control
	if kk_control != null:

		kk_control.position = Vector2(margin_x, row_1_y)

	var drone_parent: Control = get_node_or_null(
		"KkHud/BadDroneTracker"
	) as Control
	if drone_parent != null:

		drone_parent.position = Vector2(margin_x, row_2_y)

	if time_label != null:

		time_label.offset_left = -time_label.size.x - margin_x

		time_label.offset_right = -margin_x

		time_label.offset_top = row_1_y

		time_label.offset_bottom = row_1_y + time_label.size.y

	if distance_bar_control != null:

		distance_bar_control.offset_left = -distance_bar_control.size.x - margin_x

		distance_bar_control.offset_right = -margin_x

		distance_bar_control.offset_top = row_2_y

		distance_bar_control.offset_bottom = row_2_y + distance_bar_control.size.y


func style_hud_labels() -> void:

	for lbl: Label in [
			warning_label,
		]:

		if lbl == null:

			continue

		lbl.add_theme_font_override("font", HUD_FONT)

		lbl.add_theme_font_size_override("font_size", HUD_FONT_SIZE)

		lbl.add_theme_color_override("font_color", Color(1, 0, 1))

		lbl.add_theme_color_override("font_outline_color", Color(0, 0, 0, 1))

		lbl.add_theme_constant_override("outline_size", 6)


func style_time_label() -> void:

	if time_label == null:

		return

	time_label.add_theme_font_override("font", HUD_FONT)

	time_label.add_theme_font_size_override("font_size", HUD_BIG_FONT_SIZE)

	time_label.add_theme_color_override("font_color", Color(1, 1, 1, 1))

	time_label.add_theme_color_override("font_outline_color", Color(0, 0, 0, 1))

	time_label.add_theme_constant_override("outline_size", 10)


func update_hud() -> void:


	if time_label != null:

		time_label.text = "\u23F3 %d" % [
			int(game_time_left)
		]
		var urgent: bool = game_time_left <= 10.0
		if urgent and not time_label_red:
			time_label.add_theme_color_override("font_color", Color(1.0, 0.2, 0.2, 1))
			time_label_red = true
		elif not urgent and time_label_red:
			time_label.add_theme_color_override("font_color", Color(1, 1, 1, 1))
			time_label_red = false


	if bar_fill != null:

		var traveled: float = 0.0

		if player != null:

			traveled = max(
				player.global_position.x
				-
				PLAYER_START_X,
				0.0
			)

		current_run_pct = clampf(
			traveled
			/ TOTAL_RUN_DISTANCE,
			0.0,
			1.0
		)
		_update_distance_bar()


	if bad_drone_label != null:

		var bad_pct2: float = 0.0

		if bad_spawned > 0:

			bad_pct2 = (
				float(bad_killed)
				/ float(bad_spawned)
				* 100.0
			)

		bad_drone_label.text = "%d%%" % clampi(
			int(round(bad_pct2)),
			0,
			100
		)


	if warning_label != null:

		if (
			is_red_alert_active()
			and
			not is_crouching
			and
			red_standing_time > RED_STAND_WARN
		):

			warning_label.text = (
				"GET DOWN!  BLEEDING TIME"
			)

			warning_label.visible = true

		else:

			warning_label.visible = false


# Moves the distance-bar fill and diamond marker nodes; the bar frame,
# stem, diamond and dot are real scene nodes editable in main.tscn.
func _update_distance_bar() -> void:

	if bar_fill == null:

		return

	var origin_x: float = 36.0

	var rail_width: float = 149.0

	var pct: float = clampf(current_run_pct, 0.0, 1.0)

	var traveled_w: float = rail_width * pct

	bar_fill.position.x = origin_x + traveled_w

	bar_fill.size.x = max(rail_width - traveled_w, 0.0)

	if bar_pointer != null:

		bar_pointer.position.x = origin_x + traveled_w - bar_pointer.size.x * 0.5


# =====================================================
# DESTINATION (SAFE GREEN ZONE) VISUAL
# =====================================================

func build_destination() -> void:


	var dest := Node2D.new()

	dest.name = "DestinationZone"

	dest.position = Vector2(
		DESTINATION_X,
		0.0
	)


	# Neon light column running from the top to the bottom of the screen.
	var beam := Sprite2D.new()

	beam.name = "Beam"

	beam.texture = make_neon_beam_texture()

	beam.position = Vector2(
		0.0,
		360.0
	)

	beam.z_index = 0

	dest.add_child(beam)


	# Pulsing glow so the safe zone reads as "neon lighting".
	var pulse := create_tween()

	pulse.set_loops()

	pulse.tween_method(
		func(v: float) -> void: beam.modulate = Color(1, 1, 1, v),
		0.38,
		1.0,
		1.1
	)

	pulse.tween_method(
		func(v: float) -> void: beam.modulate = Color(1, 1, 1, v),
		1.0,
		0.6,
		1.1
	)


	var lbl := Label.new()

	lbl.name = "Sign"

	lbl.text = "SAFE ZONE"

	lbl.position = Vector2(
		-150.0,
		72.0
	)

	lbl.z_index = 3

	lbl.add_theme_font_size_override("font_size", 44)

	lbl.add_theme_color_override("font_color", Color(0.3, 1.0, 0.6, 1.0))

	lbl.add_theme_color_override("font_outline_color", Color(0, 0, 0, 0.9))

	lbl.add_theme_constant_override("outline_size", 10)

	dest.add_child(lbl)


	add_child(dest)


func make_neon_beam_texture() -> Texture2D:

	# Height covers the whole 720 tall play area plus a shadow margin, so the
	# beam reads as one continuous neon column from the top to the bottom of
	# the screen.
	var w: int = 360

	var h: int = 1000

	var img := Image.create(
		w,
		h,
		false,
		Image.FORMAT_RGBA8
	)


	for y in h:

		var bg_t: float = (
			float(y)
			/
			float(h)
		)

		# Brightest just below mid-screen (ground/hero level), soft at the top
		# and bottom, like a neon tube.
		var v_falloff: float = 1.0 - (
			0.35
			*
			abs(
				bg_t
				-
				0.62
			)
			/
			0.5
		)

		v_falloff = clampf(
			v_falloff,
			0.18,
			1.0
		)

		for x in w:

			var edge: float = 1.0 - (
				abs(
					float(x)
					-
					float(w) * 0.5
				)
				/ (float(w) * 0.5)
			)

			edge = pow(
				edge,
				1.8
			)

			var alpha: float = v_falloff * edge

			# Cyan-white hot core with blue-ish spill at the edges.
			var col := Color(
				0.35 + 0.65 * edge,
				1.0,
				0.85 + 0.15 * edge,
				alpha
			)

			img.set_pixel(
				x,
				y,
				col
			)


	return ImageTexture.create_from_image(img)
