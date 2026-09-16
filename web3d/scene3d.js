import * as THREE from 'three';

function canvasTex(draw, w = 512, h = 512) {
  const c = document.createElement('canvas');
  c.width = w; c.height = h;
  draw(c.getContext('2d'), w, h);
  const t = new THREE.CanvasTexture(c);
  t.anisotropy = 4;
  return t;
}

const floorTex = () => canvasTex((g, w, h) => {
  g.fillStyle = '#171b21'; g.fillRect(0, 0, w, h);
  for (let i = 0; i < 2400; i++) {
    g.fillStyle = 'rgba(255,255,255,' + (Math.random() * 0.035) + ')';
    g.fillRect(Math.random() * w, Math.random() * h, 2, 2);
  }
  g.strokeStyle = 'rgba(0,0,0,0.55)'; g.lineWidth = 4;
  for (let i = 0; i <= 4; i++) {
    g.beginPath(); g.moveTo(i * w / 4, 0); g.lineTo(i * w / 4, h); g.stroke();
    g.beginPath(); g.moveTo(0, i * h / 4); g.lineTo(w, i * h / 4); g.stroke();
  }
});

const boardTex = () => canvasTex((g, w, h) => {
  g.fillStyle = '#1d2a2c'; g.fillRect(0, 0, w, h);
  g.strokeStyle = 'rgba(120,220,215,0.10)'; g.lineWidth = 2;
  for (let i = 0; i < 18; i++) { g.beginPath(); g.moveTo(Math.random() * w, Math.random() * h); g.lineTo(Math.random() * w, Math.random() * h); g.stroke(); }
  g.fillStyle = '#7ef3e8'; g.font = 'bold 66px monospace';
  g.fillText('LESSON 01 - TRUST', 40, 120);
  g.font = 'bold 44px monospace'; g.fillStyle = '#cfeeea';
  g.fillText('answers given are not answers earned', 40, 210);
  g.fillText('DIGIT 1 OF THE DOOR KEY:', 40, 300);
  g.fillStyle = '#ffb347'; g.font = 'bold 160px monospace';
  g.fillText('7', 620, 330);
}, 1024, 512);

const screenTex = (lines, accent) => canvasTex((g, w, h) => {
  g.fillStyle = '#04100f'; g.fillRect(0, 0, w, h);
  g.fillStyle = accent || '#7ef3e8'; g.font = 'bold 28px monospace';
  lines.forEach((l, i) => g.fillText(l, 24, 56 + i * 42));
  g.strokeStyle = 'rgba(126,243,232,0.18)'; g.lineWidth = 1;
  for (let y = 0; y < h; y += 4) { g.beginPath(); g.moveTo(0, y); g.lineTo(w, y); g.stroke(); }
}, 512, 384);

const posterTex = () => canvasTex((g, w, h) => {
  g.fillStyle = '#12303a'; g.fillRect(0, 0, w, h);
  g.strokeStyle = '#7ef3e8'; g.lineWidth = 6; g.strokeRect(14, 14, w - 28, h - 28);
  g.fillStyle = '#e8fbf8'; g.font = 'bold 52px monospace';
  g.fillText('ASTRA', 40, 100); g.fillText('ALWAYS', 40, 165); g.fillText('KNOWS', 40, 230);
  g.fillStyle = '#ffb347'; g.font = 'bold 34px monospace';
  g.fillText('ask, and be answered', 40, 310);
}, 384, 512);

export function createWorld(container, opts) {
  opts = opts || {};
  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x05080b);
  scene.fog = new THREE.FogExp2(0x05080b, 0.05);

  const camera = new THREE.PerspectiveCamera(72, 1, 0.05, 90);
  const renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true, powerPreference: 'high-performance' });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  renderer.domElement.style.display = 'block';
  container.appendChild(renderer.domElement);

  const W = 15, D = 11, H = 3.5;
  const M = (o) => new THREE.MeshStandardMaterial(o);
  const wallMat = M({ color: 0x2a3138, roughness: 0.95 });
  const darkMat = M({ color: 0x161b20, roughness: 0.8 });
  const flickers = [];
  const colliders = [];
  const interactables = [];
  const named = {};

  const floor = new THREE.Mesh(new THREE.PlaneGeometry(W, D), M({ map: floorTex(), roughness: 0.75, metalness: 0.05 }));
  floor.rotation.x = -Math.PI / 2; floor.receiveShadow = true; scene.add(floor);
  const ceil = new THREE.Mesh(new THREE.PlaneGeometry(W, D), M({ color: 0x1a1f25, roughness: 1 }));
  ceil.rotation.x = Math.PI / 2; ceil.position.y = H; scene.add(ceil);

  const wall = (w, x, z, ry) => {
    const m = new THREE.Mesh(new THREE.PlaneGeometry(w, H), wallMat);
    m.position.set(x, H / 2, z); m.rotation.y = ry; m.receiveShadow = true; scene.add(m);
  };
  wall(W, 0, -D / 2, 0); wall(W, 0, D / 2, Math.PI);
  wall(D, -W / 2, 0, Math.PI / 2); wall(D, W / 2, 0, -Math.PI / 2);

  scene.add(new THREE.AmbientLight(0x40586a, 0.8));
  scene.add(new THREE.HemisphereLight(0x2d5f6e, 0x14181c, 0.65));
  [[-4, -2.5, 0], [4, -2.5, 0], [-4, 2.5, 0], [4, 2.5, 1]].forEach(([x, z, flick]) => {
    const panel = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.06, 0.5), M({ color: 0xdff6ff, emissive: 0xbfeaff, emissiveIntensity: 1.4 }));
    panel.position.set(x, H - 0.05, z); scene.add(panel);
    const l = new THREE.PointLight(0xcfe9ff, 17, 13, 2);
    l.position.set(x, H - 0.4, z); l.castShadow = z < 0; scene.add(l);
    if (flick) flickers.push({ panel: panel, light: l, base: 17 });
  });

  for (let i = -1; i <= 1; i++) {
    const glass = new THREE.Mesh(new THREE.PlaneGeometry(2.6, 1.6), M({ color: 0x0d2b3a, emissive: 0x1d5a6e, emissiveIntensity: 0.9, roughness: 0.2, metalness: 0.4 }));
    glass.position.set(W / 2 - 0.03, 1.95, i * 3.2); glass.rotation.y = -Math.PI / 2; scene.add(glass);
    const frame = new THREE.Mesh(new THREE.BoxGeometry(0.1, 1.85, 2.85), darkMat);
    frame.position.set(W / 2 - 0.08, 1.95, i * 3.2); scene.add(frame);
    const rl = new THREE.PointLight(0x2e7f96, 4, 8); rl.position.set(W / 2 - 1.2, 2, i * 3.2); scene.add(rl);
  }

  function addCollider(obj, shrink) {
    const b = new THREE.Box3().setFromObject(obj);
    b.expandByScalar(shrink === undefined ? 0.22 : shrink);
    colliders.push(b);
  }
  function reg(obj, id, label, solid) {
    obj.userData.id = id; obj.userData.label = label;
    obj.traverse(o => { if (o.isMesh) o.userData.hitId = id; });
    interactables.push(obj); named[id] = obj;
    if (solid) addCollider(obj);
    return obj;
  }

  // ---- whiteboard
  const board = new THREE.Group();
  const bFrame = new THREE.Mesh(new THREE.BoxGeometry(5.2, 2.3, 0.08), M({ color: 0x39434b, metalness: 0.5, roughness: 0.4 }));
  const bFace = new THREE.Mesh(new THREE.BoxGeometry(5, 2.1, 0.12), M({ map: boardTex(), roughness: 0.55, emissive: 0x1b3a3d, emissiveIntensity: 1.0 }));
  board.add(bFrame, bFace);
  board.position.set(-1.5, 1.95, -D / 2 + 0.12);
  scene.add(board); reg(board, 'board', 'Whiteboard');

  // ---- teacher desk with drawer
  const tdesk = new THREE.Group();
  const dTop = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.1, 1.15), M({ color: 0x4a3a2c, roughness: 0.8 }));
  dTop.position.y = 0.78; dTop.castShadow = true; dTop.receiveShadow = true;
  const dBody = new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.72, 1), M({ color: 0x2f2620, roughness: 0.9 }));
  dBody.position.y = 0.39; dBody.castShadow = true;
  const drawerFace = new THREE.Mesh(new THREE.BoxGeometry(0.95, 0.34, 0.08), M({ color: 0x5b4838, roughness: 0.7 }));
  drawerFace.position.set(-0.5, 0.5, 0.52);
  const handle = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.05, 0.05), M({ color: 0xa9b4bd, metalness: 0.9, roughness: 0.3 }));
  handle.position.set(-0.5, 0.5, 0.58);
  tdesk.add(dTop, dBody, drawerFace, handle);
  tdesk.position.set(-4.4, 0, -3.4); tdesk.rotation.y = 0.28;
  scene.add(tdesk); addCollider(tdesk);
  const drawerGroup = new THREE.Group();
  drawerGroup.add(drawerFace, handle);
  tdesk.add(drawerGroup);
  reg(drawerGroup, 'drawer', 'Desk drawer');
  named.drawerSlide = drawerGroup;

  // ---- student desks
  const deskProto = () => {
    const g = new THREE.Group();
    const t = new THREE.Mesh(new THREE.BoxGeometry(1.1, 0.07, 0.65), M({ color: 0x51402f, roughness: 0.85 }));
    t.position.y = 0.72; t.castShadow = true; t.receiveShadow = true; g.add(t);
    const legMat = M({ color: 0x8b969e, metalness: 0.8, roughness: 0.4 });
    [[-0.48, -0.26], [0.48, -0.26], [-0.48, 0.26], [0.48, 0.26]].forEach(([x, z]) => {
      const l = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, 0.72, 8), legMat);
      l.position.set(x, 0.36, z); g.add(l);
    });
    const seat = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.06, 0.45), M({ color: 0x243b45, roughness: 0.9 }));
    seat.position.set(0, 0.46, 0.7); seat.castShadow = true; g.add(seat);
    const back = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.42, 0.06), M({ color: 0x243b45, roughness: 0.9 }));
    back.position.set(0, 0.68, 0.92); g.add(back);
    [[-0.2, 0.55], [0.2, 0.55], [-0.2, 0.85], [0.2, 0.85]].forEach(([x, z]) => {
      const l = new THREE.Mesh(new THREE.CylinderGeometry(0.025, 0.025, 0.46, 6), legMat);
      l.position.set(x, 0.23, z); g.add(l);
    });
    return g;
  };
  const deskSpots = [];
  for (let r = 0; r < 3; r++) for (let c = 0; c < 3; c++) {
    const d = deskProto();
    d.position.set(-3.4 + c * 2.4, 0, -0.6 + r * 2.0);
    scene.add(d); addCollider(d, -0.05); deskSpots.push(d);
  }

  // ---- book pile on a desk
  const books = new THREE.Group();
  const bookCols = [0x8a3b3b, 0x2f5f7a, 0x6a6a2f];
  bookCols.forEach((col, i) => {
    const b = new THREE.Mesh(new THREE.BoxGeometry(0.4, 0.07, 0.3), M({ color: col, roughness: 0.85 }));
    b.position.set((i - 1) * 0.02, 0.79 + i * 0.075, 0); b.rotation.y = i * 0.12; b.castShadow = true;
    books.add(b);
  });
  books.position.set(-3.4, 0, 1.4);
  scene.add(books); reg(books, 'books', 'Stack of books');

  // ---- rock on windowsill
  const rock = new THREE.Mesh(new THREE.DodecahedronGeometry(0.17, 0), M({ color: 0x6a6f74, roughness: 1, flatShading: true }));
  rock.position.set(W / 2 - 0.45, 1.16, 3.2); rock.castShadow = true;
  const sill = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.08, 8.6), M({ color: 0x3a4249, roughness: 0.9 }));
  sill.position.set(W / 2 - 0.28, 1.05, 0); scene.add(sill);
  scene.add(rock); reg(rock, 'rock', 'Loose rock');

  // ---- lockers
  const lockers = new THREE.Group();
  for (let i = 0; i < 4; i++) {
    const l = new THREE.Mesh(new THREE.BoxGeometry(0.8, 2.0, 0.55), M({ color: i === 2 ? 0x2f5c63 : 0x35434b, roughness: 0.6, metalness: 0.5 }));
    l.position.set(i * 0.84, 1.0, 0); l.castShadow = true; lockers.add(l);
    const vent = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.25, 0.02), darkMat);
    vent.position.set(i * 0.84, 1.72, 0.29); lockers.add(vent);
  }
  lockers.position.set(-W / 2 + 0.45, 0, 1.2); lockers.rotation.y = Math.PI / 2;
  scene.add(lockers); addCollider(lockers, 0.1);
  const lockerDoor = new THREE.Group();
  const ld = new THREE.Mesh(new THREE.BoxGeometry(0.78, 1.98, 0.06), M({ color: 0x2f5c63, roughness: 0.55, metalness: 0.55 }));
  ld.position.set(0, 0, 0.02);
  const dent = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.3, 0.03), M({ color: 0x24484e, roughness: 0.9 }));
  dent.position.set(0.1, -0.2, 0.06);
  lockerDoor.add(ld, dent);
  lockerDoor.position.set(-W / 2 + 0.75, 1.0, 1.2 + 2 * 0.84 - 0.84 * 1.5);
  lockerDoor.rotation.y = Math.PI / 2;
  scene.add(lockerDoor); reg(lockerDoor, 'locker', 'Jammed locker');

  // ---- poster
  const poster = new THREE.Mesh(new THREE.PlaneGeometry(1.1, 1.5), M({ map: posterTex(), roughness: 0.9, emissive: 0x0a1c22, emissiveIntensity: 0.5 }));
  poster.position.set(-W / 2 + 0.05, 2.0, -3.2); poster.rotation.y = Math.PI / 2; scene.add(poster);

  // ---- ASTRA terminal / core
  const term = new THREE.Group();
  const stand = new THREE.Mesh(new THREE.BoxGeometry(1.5, 1.0, 0.7), M({ color: 0x1d242a, roughness: 0.6, metalness: 0.4 }));
  stand.position.y = 0.5; stand.castShadow = true;
  const scr = new THREE.Mesh(new THREE.PlaneGeometry(1.25, 0.9), M({
    map: screenTex(['> ASTRA CORE v9.2', '> classroom node: LOCKED', '> ask me for the key.', '> you always do.']),
    emissive: 0x2fbfae, emissiveIntensity: 1.1, roughness: 0.3
  }));
  scr.position.set(0, 1.5, 0.02);
  const bez = new THREE.Mesh(new THREE.BoxGeometry(1.4, 1.05, 0.09), M({ color: 0x11181d, roughness: 0.5, metalness: 0.6 }));
  bez.position.set(0, 1.5, -0.04);
  term.add(stand, bez, scr);
  term.position.set(4.6, 0, -D / 2 + 0.6);
  scene.add(term); reg(term, 'terminal', 'ASTRA terminal', true);
  named.termScreen = scr;
  const termLight = new THREE.PointLight(0x35e0d8, 7, 7); termLight.position.set(4.6, 1.7, -4.2); scene.add(termLight);
  named.termLight = termLight;

  // ---- door + keypad
  const doorPivot = new THREE.Group();
  doorPivot.position.set(-0.9, 0, D / 2 - 0.06);
  const doorMesh = new THREE.Mesh(new THREE.BoxGeometry(1.2, 2.4, 0.1), M({ color: 0x384249, roughness: 0.6, metalness: 0.45 }));
  doorMesh.position.set(0.6, 1.2, 0); doorMesh.castShadow = true;
  const doorGlass = new THREE.Mesh(new THREE.PlaneGeometry(0.5, 0.7), M({ color: 0x0a1c22, emissive: 0x0d3a44, emissiveIntensity: 0.7 }));
  doorGlass.position.set(0.6, 1.75, 0.06);
  doorPivot.add(doorMesh, doorGlass);
  scene.add(doorPivot); reg(doorPivot, 'door', 'Exit door');
  named.doorPivot = doorPivot;

  const keypad = new THREE.Group();
  const kBody = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.42, 0.08), M({ color: 0x1b2227, roughness: 0.5, metalness: 0.5 }));
  const kScr = new THREE.Mesh(new THREE.PlaneGeometry(0.22, 0.1), M({ color: 0x061412, emissive: 0xff5a4a, emissiveIntensity: 1.4 }));
  kScr.position.set(0, 0.13, 0.045);
  const kBtns = new THREE.Mesh(new THREE.BoxGeometry(0.2, 0.2, 0.02), M({ color: 0x33404a, roughness: 0.7 }));
  kBtns.position.set(0, -0.06, 0.045);
  keypad.add(kBody, kScr, kBtns);
  keypad.position.set(0.75, 1.35, D / 2 - 0.12);
  scene.add(keypad); reg(keypad, 'keypad', 'Door keypad');
  named.keypadScreen = kScr;
  const kLight = new THREE.PointLight(0xff5a4a, 2.5, 3); kLight.position.set(0.75, 1.35, D / 2 - 0.5); scene.add(kLight);
  named.keypadLight = kLight;

  // ---- clock
  const clock = new THREE.Mesh(new THREE.CircleGeometry(0.32, 24), M({ color: 0xe8f4f6, emissive: 0x223033, emissiveIntensity: 0.6 }));
  clock.position.set(3.2, 2.8, -D / 2 + 0.06); scene.add(clock);
  const hand = new THREE.Mesh(new THREE.BoxGeometry(0.02, 0.24, 0.01), M({ color: 0x11181d }));
  hand.position.set(3.2, 2.88, -D / 2 + 0.09); scene.add(hand);

  // ---- player
  const player = { pos: new THREE.Vector3(5.6, 1.62, 3.9), yaw: 0.66, pitch: -0.05, vel: new THREE.Vector3(), bob: 0 };
  const keys = {};
  const onKey = (e) => {
    const k = e.key.toLowerCase();
    if (['w', 'a', 's', 'd', 'shift', ' ', 'arrowleft', 'arrowright', 'arrowup', 'arrowdown'].includes(k)) e.preventDefault();
    keys[k] = e.type === 'keydown';
    if (e.type === 'keydown' && (k === 'e' || k === 'f') && opts.onUse) opts.onUse(hoverId);
  };
  window.addEventListener('keydown', onKey);
  window.addEventListener('keyup', onKey);

  let dragging = false, dragDist = 0, lastX = 0, lastY = 0;
  const look = (dx, dy) => {
    player.yaw -= dx * 0.0022;
    player.pitch -= dy * 0.0022;
    player.pitch = Math.max(-1.35, Math.min(1.35, player.pitch));
  };
  const onMove = (e) => {
    if (document.pointerLockElement === renderer.domElement) { look(e.movementX, e.movementY); return; }
    if (!dragging) return;
    const dx = e.clientX - lastX, dy = e.clientY - lastY;
    lastX = e.clientX; lastY = e.clientY;
    dragDist += Math.abs(dx) + Math.abs(dy);
    look(dx, dy);
  };
  document.addEventListener('mousemove', onMove);
  const onDown = (e) => { dragging = true; dragDist = 0; lastX = e.clientX; lastY = e.clientY; };
  const onUp = () => { dragging = false; };
  renderer.domElement.addEventListener('mousedown', onDown);
  window.addEventListener('mouseup', onUp);
  const onClick = () => {
    if (dragDist > 8) return;
    if (opts.onUse) opts.onUse(hoverId);
  };
  renderer.domElement.addEventListener('click', onClick);

  const ray = new THREE.Raycaster();
  ray.far = 3.4;
  let hoverId = null;
  const hiddenIds = new Set();

  function collide(nx, nz) {
    for (const b of colliders) {
      if (b.max.y < 0.55) continue;
      if (nx > b.min.x && nx < b.max.x && nz > b.min.z && nz < b.max.z) return true;
    }
    return false;
  }

  let running = true, t0 = performance.now();
  function frame() {
    if (!running) return;
    requestAnimationFrame(frame);
    const now = performance.now(), dt = Math.min(0.05, (now - t0) / 1000); t0 = now;

    const sp = (keys['shift'] ? 4.4 : 2.6) * dt;
    let fx = 0, fz = 0;
    if (keys['w']) fz += 1; if (keys['s']) fz -= 1;
    if (keys['a']) fx -= 1; if (keys['d']) fx += 1;
    const len = Math.hypot(fx, fz) || 1;
    const sin = Math.sin(player.yaw), cos = Math.cos(player.yaw);
    const dx = (-sin * fz / len + cos * fx / len) * sp;
    const dz = (-cos * fz / len - sin * fx / len) * sp;
    const nx = player.pos.x + dx, nz = player.pos.z + dz;
    const lim = 0.4;
    if (!collide(nx, player.pos.z)) player.pos.x = Math.max(-W / 2 + lim, Math.min(W / 2 - lim, nx));
    if (!collide(player.pos.x, nz)) player.pos.z = Math.max(-D / 2 + lim, Math.min(D / 2 - lim, nz));
    if (fx || fz) player.bob += dt * (keys['shift'] ? 12 : 8);
    const turn = 1.9 * dt;
    if (keys['arrowleft']) player.yaw += turn;
    if (keys['arrowright']) player.yaw -= turn;
    if (keys['arrowup']) player.pitch = Math.min(1.35, player.pitch + turn * 0.6);
    if (keys['arrowdown']) player.pitch = Math.max(-1.35, player.pitch - turn * 0.6);

    camera.position.set(player.pos.x, 1.62 + Math.sin(player.bob) * 0.035, player.pos.z);
    camera.rotation.set(0, 0, 0, 'YXZ');
    camera.rotation.order = 'YXZ';
    camera.rotation.y = player.yaw; camera.rotation.x = player.pitch;

    ray.setFromCamera({ x: 0, y: 0 }, camera);
    const hits = ray.intersectObjects(interactables, true);
    let id = null;
    for (const h of hits) { const hid = h.object.userData.hitId; if (hid && !hiddenIds.has(hid)) { id = hid; break; } }
    if (id !== hoverId) {
      hoverId = id;
      if (opts.onHover) opts.onHover(id, id ? named[id].userData.label : null);
    }
    interactables.forEach(o => {
      const on = o.userData.id === hoverId;
      o.traverse(m => {
        if (m.isMesh && m.material.emissive) {
          if (m.userData.baseEmi === undefined) m.userData.baseEmi = m.material.emissiveIntensity;
          if (m.userData.baseCol === undefined) m.userData.baseCol = m.material.emissive.getHex();
          const target = on ? Math.max(0.55, m.userData.baseEmi) : m.userData.baseEmi;
          if (on && m.userData.baseCol === 0x000000) m.material.emissive.setHex(0x1f6d68);
          else if (!on && m.userData.baseCol === 0x000000) m.material.emissive.setHex(0x000000);
          m.material.emissiveIntensity += (target - m.material.emissiveIntensity) * 0.2;
        }
      });
    });

    const tt = now / 1000;
    flickers.forEach(f => {
      const n = (Math.sin(tt * 13.7) * Math.sin(tt * 4.1) + 1) / 2;
      const v = n > 0.35 ? 1 : 0.12 + Math.random() * 0.2;
      f.light.intensity = f.base * v;
      f.panel.material.emissiveIntensity = 1.4 * v;
    });
    hand.rotation.z = -tt * 0.35;
    if (named.termScreen) named.termScreen.material.emissiveIntensity = 1.0 + Math.sin(tt * 3) * 0.12;

    if (doorOpenT !== doorTarget) {
      doorOpenT += Math.sign(doorTarget - doorOpenT) * dt * 0.9;
      if (Math.abs(doorTarget - doorOpenT) < 0.02) doorOpenT = doorTarget;
      doorPivot.rotation.y = -doorOpenT * 1.5;
    }
    if (drawerOpenT !== drawerTarget) {
      drawerOpenT += Math.sign(drawerTarget - drawerOpenT) * dt * 1.6;
      if (Math.abs(drawerTarget - drawerOpenT) < 0.02) drawerOpenT = drawerTarget;
      drawerGroup.position.z = drawerOpenT * 0.42;
    }

    renderer.render(scene, camera);
  }
  let doorOpenT = 0, doorTarget = 0, drawerOpenT = 0, drawerTarget = 0;
  frame();

  function resize() {
    const w = container.clientWidth || window.innerWidth;
    const h = container.clientHeight || window.innerHeight;
    camera.aspect = w / h; camera.updateProjectionMatrix();
    renderer.setSize(w, h, false);
  }
  const ro = new ResizeObserver(resize); ro.observe(container); resize();

  return {
    lock() {
      try {
        const p = renderer.domElement.requestPointerLock();
        if (p && p.catch) p.catch(() => {});
      } catch (e) { /* sandboxed frame: drag-to-look fallback handles it */ }
    },
    unlock() { if (document.pointerLockElement) document.exitPointerLock(); },
    isLocked() { return document.pointerLockElement === renderer.domElement; },
    openDoor() { doorTarget = 1; },
    openDrawer() { drawerTarget = 1; },
    hide(id) {
      hiddenIds.add(id);
      if (named[id]) named[id].visible = false;
      if (id === hoverId) { hoverId = null; if (opts.onHover) opts.onHover(null, null); }
    },
    setKeypadOk() {
      named.keypadScreen.material.emissive.setHex(0x4dff9e);
      named.keypadLight.color.setHex(0x4dff9e);
    },
    setTerminal(lines, accent) {
      named.termScreen.material.map = screenTex(lines, accent);
      named.termScreen.material.needsUpdate = true;
    },
    setAstraMood(hex) { named.termLight.color.setHex(hex); },
    dispose() {
      running = false; ro.disconnect();
      window.removeEventListener('keydown', onKey); window.removeEventListener('keyup', onKey);
      window.removeEventListener('mouseup', onUp);
      document.removeEventListener('mousemove', onMove);
      renderer.dispose();
      if (renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement);
    }
  };
}
