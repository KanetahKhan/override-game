shader_type canvas_item;

uniform float sway_strength : hint_range(0.0, 30.0, 0.1) = 6.0;
uniform float sway_speed : hint_range(0.0, 5.0, 0.1) = 1.0;

uniform float sway_phase = 0.0;
uniform float sway_strength_mul = 1.0;
uniform float sway_speed_mul = 1.0;
uniform float source_height = 100.0;

void vertex() {
	float normalized_y = clamp(
		VERTEX.y / max(source_height, 1.0),
		0.0,
		1.0
	);

	float sway_mask = pow(
		1.0 - normalized_y,
		2.0
	);

	float movement = sin(
		TIME
		*
		sway_speed
		*
		sway_speed_mul
		+
		sway_phase
	);

	VERTEX.x += (
		movement
		*
		sway_strength
		*
		sway_strength_mul
		*
		sway_mask
	);
}