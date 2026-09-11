extends Node


# Procedurally generated SFX bank. Every sound is synthesized at runtime, so
# no external .wav/.ogg assets are needed. Autoloaded as "Sfx".

const RATE: int = 22050

var _streams: Dictionary = {}

var _players: Dictionary = {}


func stream(key: String) -> AudioStreamWAV:
	return _get_stream(key)


func play(
	key: String,
	volume_db: float = 0.0,
	pitch: float = 1.0
) -> void:
	var player: AudioStreamPlayer = _players.get(
		key
	) as AudioStreamPlayer
	if player == null:
		player = AudioStreamPlayer.new()
		player.stream = _get_stream(key)
		add_child(player)
		_players[key] = player
	player.pitch_scale = pitch
	player.volume_db = volume_db
	player.play()


func _get_stream(key: String) -> AudioStreamWAV:
	if !_streams.has(key):
		_streams[key] = _build(key)
	return _streams[key] as AudioStreamWAV


func _build(key: String) -> AudioStreamWAV:
	match key:
		"step":
			return _make(_gen_step())
		"crawl_step":
			return _make(_gen_crawl_step())
		"drone_hum":
			return _make(_gen_drone_hum(), true)
		"guard_beep":
			return _make(_gen_guard_beep(), true)
		"shot":
			return _make(_gen_shot())
		"boom":
			return _make(_gen_boom())
		"kk_boom":
			return _make(_gen_kk_boom())
		"win":
			return _make(_gen_win())
		"beep":
			return _make(_gen_beep())
		"go":
			return _make(_gen_go())
	return _make(PackedFloat32Array())


func _make(
	samples: PackedFloat32Array,
	loop: bool = false
) -> AudioStreamWAV:
	var data := PackedByteArray()
	data.resize(samples.size() * 2)
	var off := 0
	for s in samples:
		var v: int = clampi(
			int(s * 32767.0),
			-32768,
			32767
		)
		data.encode_s16(off, v)
		off += 2
	var wav := AudioStreamWAV.new()
	wav.format = AudioStreamWAV.FORMAT_16_BITS
	wav.mix_rate = RATE
	wav.stereo = false
	wav.data = data
	if loop and samples.size() > 0:
		wav.loop_mode = AudioStreamWAV.LOOP_FORWARD
		wav.loop_begin = 0
		wav.loop_end = samples.size()
	return wav


func _gen_step() -> PackedFloat32Array:
	var n := int(RATE * 0.11)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var noise := randf() * 2.0 - 1.0
		var s := (
			noise * exp(-t * 45.0) * 0.8
			+ sin(TAU * 55.0 * t) * exp(-t * 40.0) * 0.55
		)
		out[i] = clampf(s, -1.0, 1.0)
	return out


func _gen_crawl_step() -> PackedFloat32Array:
	var n := int(RATE * 0.15)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var noise := randf() * 2.0 - 1.0
		var s := (
			noise * exp(-t * 30.0) * 0.45
			+ sin(TAU * 40.0 * t) * exp(-t * 28.0) * 0.6
		)
		out[i] = clampf(s, -1.0, 1.0)
	return out


func _gen_drone_hum() -> PackedFloat32Array:
	var n := int(RATE * 2.0)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var s := (
			sin(TAU * 45.0 * t) * (0.3 + 0.08 * sin(TAU * 2.0 * t))
			+ 0.13 * sin(TAU * 90.0 * t)
			+ 0.05 * sin(TAU * 22.0 * t)
		)
		out[i] = clampf(s, -1.0, 1.0)
	return out


func _gen_guard_beep() -> PackedFloat32Array:
	var n := int(RATE * 0.72)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var s := 0.0
		if t < 0.16 or (t > 0.21 and t < 0.37):
			var f: float = 800.0 if t < 0.16 else 600.0
			var local_t: float = t if t < 0.16 else t - 0.21
			var env := clampf(local_t / 0.012, 0.0, 1.0) * exp(-local_t * 10.0)
			s = sign(sin(TAU * f * local_t)) * 0.35 * env
		out[i] = clampf(s, -1.0, 1.0)
	return out


func _gen_shot() -> PackedFloat32Array:
	var n := int(RATE * 0.16)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var noise := randf() * 2.0 - 1.0
		var s := (
			noise * exp(-t * 60.0) * 1.0
			+ sin(TAU * 95.0 * t) * exp(-t * 55.0) * 0.5
		)
		out[i] = clampf(s, -1.0, 1.0)
	return out


func _gen_boom() -> PackedFloat32Array:
	var n := int(RATE * 0.9)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var noise := randf() * 2.0 - 1.0
		var s := (
			noise * exp(-t * 6.0) * 0.8
			+ sin(TAU * 40.0 * t) * exp(-t * 5.0) * 0.9
			+ sin(TAU * 18.0 * t) * exp(-t * 4.0) * 0.5
		)
		out[i] = clampf(s, -1.0, 1.0)
	return out


func _gen_kk_boom() -> PackedFloat32Array:
	var n := int(RATE * 1.3)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var noise := randf() * 2.0 - 1.0
		var s := (
			noise * exp(-t * 3.2) * 1.0
			+ sin(TAU * 30.0 * t) * exp(-t * 2.6) * 1.0
			+ sin(TAU * 14.0 * t) * exp(-t * 2.0) * 0.6
		)
		out[i] = clampf(tanh(s * 1.4), -1.0, 1.0)
	return out


func _gen_win() -> PackedFloat32Array:
	var notes := [
		[392.0, 0.0],
		[493.88, 0.22],
		[587.33, 0.44],
		[783.99, 0.66],
	]
	var n := int(RATE * 1.5)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var s := 0.0
		for note in notes:
			var f: float = note[0]
			var start: float = note[1]
			var lt := t - start
			if lt >= 0.0 and lt < 0.26:
				s += sin(TAU * f * lt) * exp(-lt * 9.0) * 0.3
		for fchord in [392.0, 493.88, 587.33, 783.99]:
			var lt := t - 0.92
			if lt >= 0.0 and lt < 0.58:
				s += sin(TAU * fchord * lt) * exp(-lt * 3.5) * 0.18
		out[i] = clampf(s, -1.0, 1.0)
	return out


func _gen_beep() -> PackedFloat32Array:
	var n := int(RATE * 0.18)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var env := clampf(t / 0.01, 0.0, 1.0) * exp(-t * 9.0)
		var s := sin(TAU * 520.0 * t) * 0.5 * env
		out[i] = clampf(s, -1.0, 1.0)
	return out


func _gen_go() -> PackedFloat32Array:
	var n := int(RATE * 0.35)
	var out := PackedFloat32Array()
	out.resize(n)
	for i in n:
		var t := float(i) / float(RATE)
		var prog := t / 0.35
		var f := lerpf(350.0, 700.0, prog)
		var phase := f * t
		var env := clampf(t / 0.01, 0.0, 1.0) * exp(-t * 3.0)
		var s := sin(TAU * phase) * 0.55 * env
		out[i] = clampf(s, -1.0, 1.0)
	return out