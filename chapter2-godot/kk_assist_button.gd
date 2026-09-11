extends Control

# KK ASSIST life indicator: a blocky pixel-art hexagon "KK" logo plus a 5-cell
# bar showing remaining KK uses. Every use empties one cell from the right.
# The Control is clickable (gui_input); it stays inactive during the red alert
# and after game over.

@onready var drone_manager = get_parent().get_parent().get_node_or_null("DroneManager")

const MAX_USES: int = 5
const BAR_COUNT: int = 5

const PIXEL_FONT := preload("res://assets/Pixel Game.otf")
const FONT_SIZE: int = 20

# Blocky, pixel-art proportions
const CELL_W: float = 17.0
const CELL_H: float = 14.0
const CELL_GAP: float = 3.0
const CONTAINER_PADDING_X: float = 4.0
const CONTAINER_PADDING_Y: float = 4.0

# Flat, retro pixel-art colors
const COLOR_BG_PANEL = Color("#40E0D0") # Aquamarine / turquoise panel background
const COLOR_BORDER_PANEL = Color("#006D6F") # Dark teal outline
const COLOR_ACTIVE_FILL = Color("#FF00FF") # Magenta active bar
const COLOR_EMPTY_FILL = Color("#45283c") # Dark muted maroon/gray
const COLOR_TEXT = Color("#FF00FF") # Magenta KK text inside the hexagon
const COLOR_SHADOW = Color("#000000") # Pure black for hard drop shadows

var uses_left: int = MAX_USES
var container_panel: Panel = Panel.new()
var logo_control: Control = Control.new()
var logo_label: Label = Label.new() # Defined globally for easier access
var _cells: Array[Panel] = []

func _ready() -> void:
	# Forces Godot to render the entire control blocky instead of blurry
	texture_filter = CanvasItem.TEXTURE_FILTER_NEAREST
	mouse_default_cursor_shape = Control.CURSOR_POINTING_HAND

	layout_ui()
	_build_life_bars()
	_refresh_life_bars()

func _gui_input(event: InputEvent) -> void:
	if event is InputEventMouseButton and event.button_index == MOUSE_BUTTON_LEFT and event.pressed:
		_on_pressed()

func _process(_delta: float) -> void:
	var interactable := true
	var main: Node = get_parent().get_parent()
	if main != null and main.get("game_over") == true:
		interactable = false
	elif drone_manager != null and is_instance_valid(drone_manager):
		interactable = not drone_manager.is_alert_active() and uses_left > 0
	mouse_filter = Control.MOUSE_FILTER_STOP if interactable else Control.MOUSE_FILTER_IGNORE
	modulate = Color.WHITE if interactable else Color(0.6, 0.6, 0.6)


func press_from_keyboard() -> void:
	if (
		mouse_filter != Control.MOUSE_FILTER_STOP
		or
		uses_left <= 0
	):
		return
	_on_pressed()

func layout_ui() -> void:
	custom_minimum_size = Vector2(210, 42)

	# 1. Setup the Hexagon Logo (Pixel style)
	logo_control.custom_minimum_size = Vector2(35, 35)
	logo_control.position = Vector2(0, 3)
	logo_control.mouse_filter = Control.MOUSE_FILTER_IGNORE
	logo_control.draw.connect(_on_logo_draw)
	add_child(logo_control)

	# SETUP THE KK LABEL INSIDE THE HEXAGON
	logo_label.text = "KK"
	logo_label.add_theme_font_override("font", PIXEL_FONT)

	logo_label.add_theme_color_override("font_color", COLOR_TEXT)
	logo_label.add_theme_font_size_override("font_size", FONT_SIZE)
	logo_label.mouse_filter = Control.MOUSE_FILTER_IGNORE

	# Configure centering *inside* the hexagon
	logo_label.horizontal_alignment = HORIZONTAL_ALIGNMENT_CENTER
	logo_label.vertical_alignment = VERTICAL_ALIGNMENT_CENTER

	# Set anchors to fill the 50x50 control perfectly
	logo_label.set_anchors_and_offsets_preset(PRESET_FULL_RECT, PRESET_MODE_KEEP_WIDTH, 0)

	# Visually offset the text slightly up if it looks low within the drawn boundary
	# logo_label.position.y -= 1

	logo_control.add_child(logo_label)

	# 2. Setup the Blocky Background for the Segments
	var total_w: float = BAR_COUNT * CELL_W + (BAR_COUNT - 1) * CELL_GAP + (CONTAINER_PADDING_X * 2.0)
	var total_h: float = CELL_H + (CONTAINER_PADDING_Y * 2.0)

	container_panel.position = Vector2(32, 8)
	container_panel.size = Vector2(total_w, total_h)

	var container_sb := StyleBoxFlat.new()
	container_sb.bg_color = COLOR_BG_PANEL
	container_sb.border_color = COLOR_BORDER_PANEL
	container_sb.set_border_width_all(3) # Thick, blocky border

	# CRITICAL: This disables smooth curves to make the corners sharp and pixelated
	container_sb.anti_aliasing = false

	# Hard pixel drop shadow (No blur)
	container_sb.shadow_color = COLOR_SHADOW
	container_sb.shadow_size = 0
	container_sb.shadow_offset = Vector2(0, 3)

	container_panel.add_theme_stylebox_override("panel", container_sb)
	container_panel.mouse_filter = Control.MOUSE_FILTER_IGNORE
	add_child(container_panel)

	# Ensures the container sits visually behind the hexagon logo
	move_child(container_panel, 0)

func _build_life_bars() -> void:
	for c in _cells:
		c.queue_free()
	_cells.clear()

	for i in BAR_COUNT:
		var panel := Panel.new()
		panel.position = Vector2(CONTAINER_PADDING_X + i * (CELL_W + CELL_GAP), CONTAINER_PADDING_Y)
		panel.size = Vector2(CELL_W, CELL_H)
		panel.mouse_filter = Control.MOUSE_FILTER_IGNORE

		_cells.append(panel)
		container_panel.add_child(panel)

func _refresh_life_bars() -> void:
	for i in BAR_COUNT:
		var sb := StyleBoxFlat.new()
		sb.anti_aliasing = false # Ensures the inner cells remain blocky
		sb.set_border_width_all(2) # Inner blocky border
		sb.border_color = COLOR_BORDER_PANEL

		if i < uses_left:
			sb.bg_color = COLOR_ACTIVE_FILL
		else:
			sb.bg_color = COLOR_EMPTY_FILL

		_cells[i].add_theme_stylebox_override("panel", sb)

# Custom drawing function for a jagged, pixel-art Hexagon
func _on_logo_draw() -> void:
	var center = logo_control.size / 2.0
	var radius = 17.0
	var points = PackedVector2Array()

	# Snapping points to whole pixels for grid alignment
	for i in 6:
		var angle = i * (PI / 3.0) + (PI / 2.0)
		var px = round(center.x + cos(angle) * radius)
		var py = round(center.y + sin(angle) * radius)
		points.append(Vector2(px, py))

	# Draw flat background
	logo_control.draw_colored_polygon(points, COLOR_BG_PANEL)

	var outline_points = points.duplicate()
	outline_points.append(points[0]) # Close the loop

	# Jagged pixel lines, no antialiasing
	# Draw a thick black drop shadow/outer border first
	logo_control.draw_polyline(outline_points, COLOR_SHADOW, 4.0, false)
	# Then draw the inner colored border
	logo_control.draw_polyline(outline_points, COLOR_BORDER_PANEL, 2.0, false)
# Public helper: lets the main loop trigger this button with a keyboard
# press (Space) exactly like clicking it, honoring the same gates (red alert,
# game over, remaining uses).
func trigger_from_keyboard() -> void:

	if (
		input_enabled() == false
	):
		return

	_on_pressed()


func input_enabled() -> bool:

	if uses_left <= 0:
		return false
	var main: Node = get_parent().get_parent()
	if main != null and main.get("game_over") == true:
		return false
	if drone_manager != null and is_instance_valid(drone_manager):
		if drone_manager.is_alert_active():
			return false
	return true


func _on_pressed() -> void:

	if uses_left <= 0:

		return

	uses_left -= 1

	_refresh_life_bars()

	Sfx.play("kk_boom", -3.0)

	if drone_manager != null and is_instance_valid(drone_manager):

		drone_manager.destroy_bad_on_screen()