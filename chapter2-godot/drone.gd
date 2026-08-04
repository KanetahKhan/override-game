extends CharacterBody2D

enum DroneType { SCOUT, BOMBER, SWARM }

var drone_type: DroneType = DroneType.SCOUT
var speed: float = 150.0
var hp: int = 30
var poison_damage: int = 10
var move_direction: Vector2 = Vector2.LEFT

func _ready() -> void:
	match drone_type:
		DroneType.SCOUT:
			speed = 200.0
			hp = 20
			poison_damage = 5
		DroneType.BOMBER:
			speed = 100.0
			hp = 50
			poison_damage = 15
		DroneType.SWARM:
			speed = 250.0
			hp = 10
			poison_damage = 3

func _physics_process(delta: float) -> void:
	velocity = move_direction * speed
	move_and_slide()

func take_damage(amount: int) -> void:
	hp -= amount
	if hp <= 0:
		die()

func die() -> void:
	# placeholder: explosion effect
	queue_free()
