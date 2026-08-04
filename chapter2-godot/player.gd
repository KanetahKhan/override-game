extends CharacterBody2D

const SPEED = 300.0
const JUMP_VELOCITY = -500.0

var gravity = ProjectSettings.get_setting("physics/2d/default_gravity")
var facing_right: bool = true
var hp: int = 100
var is_attacking: bool = false
var is_defending: bool = false

@onready var sprite: Sprite2D = $Sprite2D

func _physics_process(delta: float) -> void:
	if not is_on_floor():
		velocity.y += gravity * delta

	var direction := 0.0
	if Input.is_key_pressed(KEY_RIGHT):
		direction += 1.0
		facing_right = true
	if Input.is_key_pressed(KEY_LEFT):
		direction -= 1.0
		facing_right = false

	velocity.x = direction * SPEED

	if Input.is_key_pressed(KEY_SPACE) and is_on_floor():
		velocity.y = JUMP_VELOCITY

	if Input.is_key_pressed(KEY_S):
		is_defending = true
	else:
		is_defending = false

	move_and_slide()

	sprite.flip_h = not facing_right

	if is_on_floor():
		if direction != 0.0:
			sprite.texture = preload("res://assets/ren/walk_east.png")
		else:
			sprite.texture = preload("res://assets/ren/idle_east.png")
	else:
		sprite.texture = preload("res://assets/ren/dodge_east.png")

	if Input.is_key_pressed(KEY_F) and not is_attacking:
		attack()

func attack() -> void:
	is_attacking = true
	sprite.texture = preload("res://assets/ren/dodge_east.png")
	await get_tree().create_timer(0.3).timeout
	is_attacking = false

func take_damage(amount: int) -> void:
	if is_defending:
		amount = amount / 2
	hp -= amount
	if hp <= 0:
		die()

func die() -> void:
	get_tree().reload_current_scene()
