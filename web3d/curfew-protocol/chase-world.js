import * as THREE from 'three';

/* ---------- procedural textures (palette lifted from the OVERRIDE 3D classroom) ---------- */
function canvasTex(draw, w = 512, h = 512) {
  const c = document.createElement('canvas');
  c.width = w; c.height = h;
  draw(c.getContext('2d'), w, h);
  const t = new THREE.CanvasTexture(c);
  t.anisotropy = 4;
  return t;
}

const floorTex = () => {
  const t = canvasTex((g, w, h) => {
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
  t.wrapS = t.wrapT = THREE.RepeatWrapping;
  t.repeat.set(14, 9);
  return t;
};

const wallTex = () => {
  const t = canvasTex((g, w, h) => {
    g.fillStyle = '#232a31'; g.fillRect(0, 0, w, h);
    for (let i = 0; i < 900; i++) {
      g.fillStyle = 'rgba(0,0,0,' + (Math.random() * 0.12) + ')';
      g.fillRect(Math.random() * w, Math.random() * h, 3, 3);
    }
    g.fillStyle = 'rgba(10,14,18,0.55)'; g.fillRect(0, h * 0.78, w, h * 0.22);
    g.strokeStyle = 'rgba(126,243,232,0.06)'; g.lineWidth = 2;
    g.beginPath(); g.moveTo(0, h * 0.78); g.lineTo(w, h * 0.78); g.stroke();
  });
  t.wrapS = t.wrapT = THREE.RepeatWrapping;
  return t;
};

const boardTex = () => canvasTex((g, w, h) => {
  g.fillStyle = '#1d2a2c'; g.fillRect(0, 0, w, h);
  g.strokeStyle = 'rgba(120,220,215,0.10)'; g.lineWidth = 2;
  for (let i = 0; i < 18; i++) { g.beginPath(); g.moveTo(Math.random() * w, Math.random() * h); g.lineTo(Math.random() * w, Math.random() * h); g.stroke(); }
  g.fillStyle = '#7ef3e8'; g.font = 'bold 58px monospace';
  g.fillText('CURFEW PROTOCOL — LESSON 03', 36, 100);
  g.font = 'bold 40px monospace'; g.fillStyle = '#cfeeea';
  g.fillText('the unit does not blink. it listens.', 36, 180);
  g.fillText('3 NODES MUST BE CLEARED BEFORE THE', 36, 250);
  g.fillText('EXIT BAY WILL RELEASE YOU.', 36, 300);
  g.fillStyle = '#ffb347'; g.font = 'bold 34px monospace';
  g.fillText('hide. the shelves are deeper than they look.', 36, 400);
}, 1024, 512);

const screenTex = (lines, accent) => canvasTex((g, w, h) => {
  g.fillStyle = '#04100f'; g.fillRect(0, 0, w, h);
  g.fillStyle = accent || '#7ef3e8'; g.font = 'bold 26px monospace';
  lines.forEach((l, i) => g.fillText(l, 22, 52 + i * 40));
  g.strokeStyle = 'rgba(126,243,232,0.18)'; g.lineWidth = 1;
  for (let y = 0; y < h; y += 4) { g.beginPath(); g.moveTo(0, y); g.lineTo(w, y); g.stroke(); }
}, 512, 384);

const signTex = (text, accent) => canvasTex((g, w, h) => {
  g.fillStyle = '#0a0f14'; g.fillRect(0, 0, w, h);
  g.strokeStyle = accent; g.lineWidth = 8; g.strokeRect(10, 10, w - 20, h - 20);
  g.fillStyle = accent; g.font = 'bold 52px monospace';
  g.textAlign = 'center'; g.textBaseline = 'middle';
  g.fillText(text, w / 2, h / 2);
}, 512, 128);

/* ---------- world ---------- */
export function createWorld(container, opts) {
  opts = opts || {};
  const cb = (n, ...a) => { if (opts[n]) opts[n](...a); };

  const scene = new THREE.Scene();
  scene.background = new THREE.Color(0x05080b);
  scene.fog = new THREE.FogExp2(0x05080b, 0.042);

  const camera = new THREE.PerspectiveCamera(74, 1, 0.05, 120);
  const renderer = new THREE.WebGLRenderer({ antialias: true, powerPreference: 'high-performance' });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 1.75));
  renderer.shadowMap.enabled = true;
  renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  renderer.domElement.style.display = 'block';
  container.appendChild(renderer.domElement);

  const M = (o) => new THREE.MeshStandardMaterial(o);
  const HX = 25, HZ = 15, H = 3.6;
  const wallMat = M({ map: wallTex(), roughness: 0.95 });
  const darkMat = M({ color: 0x161b20, roughness: 0.8 });
  const metalMat = M({ color: 0x39434b, metalness: 0.6, roughness: 0.42 });

  const colliders = [];     // Box3, blocks movement
  const sightBlockers = []; // Box3, blocks line of sight
  const interactables = [];
  const named = {};
  const animators = [];
  const flickers = [];
  const coins = [];
  const hideSpots = [];

  /* floor + ceiling */
  const floor = new THREE.Mesh(new THREE.PlaneGeometry(HX * 2, HZ * 2), M({ map: floorTex(), roughness: 0.75, metalness: 0.06 }));
  floor.rotation.x = -Math.PI / 2; floor.receiveShadow = true; scene.add(floor);
  const ceil = new THREE.Mesh(new THREE.PlaneGeometry(HX * 2, HZ * 2), M({ color: 0x12161b, roughness: 1 }));
  ceil.rotation.x = Math.PI / 2; ceil.position.y = H; scene.add(ceil);

  /* walls */
  function wallSeg(x1, z1, x2, z2, h) {
    h = h || H;
    const len = Math.hypot(x2 - x1, z2 - z1);
    const horiz = Math.abs(x2 - x1) > Math.abs(z2 - z1);
    const geo = new THREE.BoxGeometry(horiz ? len : 0.3, h, horiz ? 0.3 : len);
    const mat = wallMat.clone();
    mat.map = wallMat.map.clone();
    mat.map.needsUpdate = true;
    mat.map.repeat.set(horiz ? len / 4 : 1, h / 4);
    const m = new THREE.Mesh(geo, mat);
    m.position.set((x1 + x2) / 2, h / 2, (z1 + z2) / 2);
    m.receiveShadow = true; m.castShadow = false;
    scene.add(m);
    const b = new THREE.Box3().setFromObject(m);
    b.expandByScalar(0.18);
    colliders.push(b);
    sightBlockers.push(b);
    return m;
  }

  // outer shell
  wallSeg(-HX, -HZ, HX, -HZ); wallSeg(-HX, HZ, HX, HZ);
  wallSeg(-HX, -HZ, -HX, HZ); wallSeg(HX, -HZ, HX, HZ);
  // corridor walls with doorways at room centres
  const doorX = [-16.75, 0, 16.75], gap = 1.7;
  [-3, 3].forEach(z => {
    let x = -HX;
    doorX.forEach(d => { wallSeg(x, z, d - gap, z); x = d + gap; });
    wallSeg(x, z, HX, z);
  });
  // room dividers
  [-8.5, 8.5].forEach(x => { wallSeg(x, -HZ, x, -3); wallSeg(x, 3, x, HZ); });

  /* lighting — cyan/amber cyberpunk wash, flickering ceiling panels, neon strips */
  scene.add(new THREE.AmbientLight(0x44607a, 1.05));
  scene.add(new THREE.HemisphereLight(0x2d5f6e, 0x161b20, 0.8));

  function panelLight(x, z, flick, colour, intensity, shadow) {
    const panel = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.06, 0.5),
      M({ color: 0xdff6ff, emissive: colour || 0xbfeaff, emissiveIntensity: 1.4 }));
    panel.position.set(x, H - 0.06, z); scene.add(panel);
    const l = new THREE.PointLight(colour || 0xcfe9ff, intensity || 14, 14, 2);
    l.position.set(x, H - 0.45, z); l.castShadow = !!shadow; scene.add(l);
    if (flick) flickers.push({ panel, light: l, base: intensity || 14 });
  }
  [-19, -11, -3, 5, 13, 21].forEach((x, i) => panelLight(x, 0, i === 2 || i === 5, 0xcfe9ff, 15, i % 3 === 0));
  [[-16.75, -6.5], [0, -6.5], [16.75, -6.5], [-16.75, 6.5], [0, 6.5], [16.75, 6.5]]
    .forEach(([x, z], i) => panelLight(x, z, i === 1 || i === 4, i === 4 ? 0xffd2a0 : 0xcfe9ff, 17, false));
  [[-16.75, -12], [0, -12], [16.75, -12], [-16.75, 12], [0, 12], [16.75, 12]]
    .forEach(([x, z], i) => panelLight(x, z, false, i === 2 ? 0xffd2a0 : 0xcfe9ff, 13, false));

  function neonStrip(x, z, len, horiz, colour) {
    const m = new THREE.Mesh(
      new THREE.BoxGeometry(horiz ? len : 0.08, 0.08, horiz ? 0.08 : len),
      M({ color: 0x000000, emissive: colour, emissiveIntensity: 2.2 }));
    m.position.set(x, 2.62, z); scene.add(m);
    const l = new THREE.PointLight(colour, 3.4, 9); l.position.set(x, 2.4, z); scene.add(l);
    return m;
  }
  neonStrip(-12, -2.55, 24, true, 0xff3d7f);
  neonStrip(12, 2.55, 24, true, 0x35e0d8);
  neonStrip(-8.35, -9, 11, false, 0x7a4dff);
  neonStrip(8.35, 9, 11, false, 0xff9a3d);

  function sign(text, accent, x, y, z, ry) {
    const tex = signTex(text, accent);
    const m = new THREE.Mesh(new THREE.PlaneGeometry(2.2, 0.55),
      M({ map: tex, emissiveMap: tex, emissive: 0xffffff, emissiveIntensity: 0.75, roughness: 0.9 }));
    m.position.set(x, y, z); m.rotation.y = ry; scene.add(m);
  }
  sign('LAB 01', '#7ef3e8', -16.75, 2.02, -3.2, 0);
  sign('CLASS 2A', '#ffb347', 0, 2.02, -3.2, 0);
  sign('LAB 02', '#7ef3e8', 16.75, 2.02, -3.2, 0);
  sign('CLASS 1B', '#ffb347', -16.75, 2.02, 3.2, Math.PI);
  sign('SERVER', '#ff3d7f', 0, 2.02, 3.2, Math.PI);
  sign('EXIT BAY', '#4dff9e', 16.75, 2.02, 3.2, Math.PI);

  /* ---------- helpers ---------- */
  function addCollider(obj, shrink, blocksSight) {
    const b = new THREE.Box3().setFromObject(obj);
    b.expandByScalar(shrink === undefined ? 0.2 : shrink);
    colliders.push(b);
    if (blocksSight) sightBlockers.push(b);
    return b;
  }
  function reg(obj, id, label, verb, kind) {
    obj.userData.id = id;
    obj.userData.label = label;
    obj.userData.verb = verb || 'Use';
    obj.userData.kind = kind || 'prop';
    obj.traverse(o => { if (o.isMesh) o.userData.hitId = id; });
    interactables.push(obj);
    named[id] = obj;
    return obj;
  }
  function anim(o) { animators.push(o); return o; }

  /* ---------- furniture builders ---------- */
  function deskProto() {
    const g = new THREE.Group();
    const t = new THREE.Mesh(new THREE.BoxGeometry(1.1, 0.07, 0.65), M({ color: 0x51402f, roughness: 0.85 }));
    t.position.y = 0.72; t.castShadow = true; t.receiveShadow = true; g.add(t);
    const legMat = M({ color: 0x8b969e, metalness: 0.8, roughness: 0.4 });
    [[-0.48, -0.26], [0.48, -0.26], [-0.48, 0.26], [0.48, 0.26]].forEach(([x, z]) => {
      const l = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, 0.72, 6), legMat);
      l.position.set(x, 0.36, z); g.add(l);
    });
    const seat = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.06, 0.45), M({ color: 0x243b45, roughness: 0.9 }));
    seat.position.set(0, 0.46, 0.7); g.add(seat);
    const back = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.42, 0.06), M({ color: 0x243b45, roughness: 0.9 }));
    back.position.set(0, 0.68, 0.92); g.add(back);
    g.userData.seat = seat;
    return g;
  }

  let chairN = 0;
  function deskGrid(cx, cz, rows, cols) {
    for (let r = 0; r < rows; r++) for (let c = 0; c < cols; c++) {
      const d = deskProto();
      const x = cx + (c - (cols - 1) / 2) * 2.2, z = cz + (r - (rows - 1) / 2) * 2.0;
      d.position.set(x, 0, z);
      scene.add(d); addCollider(d, -0.08);
      const seat = d.userData.seat;
      seat.userData.sit = { x: x, z: z + 1.32, yaw: 0 };
      reg(seat, 'chair' + (++chairN), 'Chair', 'Sit down', 'chair');
      hideSpots.push({ id: 'desk' + chairN, pos: new THREE.Vector3(x, 0, z + 1.25), obj: d, label: 'Crawl under the desk' });
    }
  }

  /* almirah: two doors that actually swing open and shut, and you can climb inside */
  let almirahN = 0;
  function almirah(x, z, ry, contents) {
    const n = ++almirahN;
    const id = 'almirah' + n;
    const g = new THREE.Group();
    const bodyMat = M({ color: 0x2b3a40, roughness: 0.6, metalness: 0.45 });
    const shell = new THREE.Mesh(new THREE.BoxGeometry(1.9, 2.4, 0.72), bodyMat);
    shell.position.y = 1.2; shell.castShadow = true; shell.receiveShadow = true; g.add(shell);
    const cavity = new THREE.Mesh(new THREE.BoxGeometry(1.74, 2.2, 0.6), M({ color: 0x090d10, roughness: 1 }));
    cavity.position.set(0, 1.2, 0.09); g.add(cavity);
    for (let i = 0; i < 2; i++) {
      const shelf = new THREE.Mesh(new THREE.BoxGeometry(1.72, 0.05, 0.58), M({ color: 0x1d272b, roughness: 0.9 }));
      shelf.position.set(0, 0.75 + i * 0.78, 0.09); g.add(shelf);
    }
    if (contents) {
      const glow = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.06, 0.34),
        M({ color: 0x000000, emissive: 0x4dff9e, emissiveIntensity: 1.6 }));
      glow.position.set(0.2, 0.83, 0.1); g.add(glow);
      g.userData.stash = glow;
    }
    const doorMat = M({ color: 0x35464d, roughness: 0.5, metalness: 0.55 });
    const doors = [];
    [-1, 1].forEach(s => {
      const pivot = new THREE.Group();
      pivot.position.set(s * 0.93, 1.2, 0.37);
      const leaf = new THREE.Mesh(new THREE.BoxGeometry(0.92, 2.3, 0.07), doorMat);
      leaf.position.x = -s * 0.46; leaf.castShadow = true;
      const handle = new THREE.Mesh(new THREE.BoxGeometry(0.05, 0.3, 0.05), M({ color: 0xa9b4bd, metalness: 0.9, roughness: 0.28 }));
      handle.position.set(-s * 0.86, 0, 0.07);
      const vent = new THREE.Mesh(new THREE.BoxGeometry(0.52, 0.16, 0.02), M({ color: 0x121a1e, emissive: 0x0d3a44, emissiveIntensity: 0.5 }));
      vent.position.set(-s * 0.46, 0.82, 0.05);
      pivot.add(leaf, handle, vent);
      g.add(pivot);
      doors.push({ pivot, s });
    });
    g.position.set(x, 0, z); g.rotation.y = ry;
    scene.add(g);
    addCollider(g, 0.05, true);
    reg(g, id, 'Almirah ' + n, 'Open', 'almirah');
    const st = anim({
      t: 0, target: 0, speed: 2.1,
      apply(v) { doors.forEach(d => { d.pivot.rotation.y = -d.s * v * 1.95; }); }
    });
    g.userData.door = st;
    hideSpots.push({
      id: id,
      pos: new THREE.Vector3(x + Math.sin(ry) * 1.2, 0, z + Math.cos(ry) * 1.2),
      obj: g, needsOpen: st, label: 'Hide inside almirah ' + n
    });
    return g;
  }

  /* bookshelf with individually removable books */
  function bookshelf(x, z, ry, keyIndex) {
    const g = new THREE.Group();
    const frame = new THREE.Mesh(new THREE.BoxGeometry(2.6, 2.2, 0.42), M({ color: 0x3a2c22, roughness: 0.88 }));
    frame.position.y = 1.1; frame.castShadow = true; g.add(frame);
    const inner = new THREE.Mesh(new THREE.BoxGeometry(2.44, 2.04, 0.36), M({ color: 0x120f0c, roughness: 1 }));
    inner.position.set(0, 1.1, 0.06); g.add(inner);
    [0.5, 1.12, 1.74].forEach(y => {
      const s = new THREE.Mesh(new THREE.BoxGeometry(2.44, 0.05, 0.36), M({ color: 0x2a2019, roughness: 0.9 }));
      s.position.set(0, y, 0.06); g.add(s);
    });
    g.position.set(x, 0, z); g.rotation.y = ry;
    scene.add(g); addCollider(g, 0.08, true);

    const cols = [0x8a3b3b, 0x2f5f7a, 0x6a6a2f, 0x6b3b7a, 0x2f7a5f, 0x7a5a2f];
    let k = 0;
    [0.55, 1.17, 1.79].forEach((y, row) => {
      for (let i = 0; i < 4; i++) {
        const id = 'book_' + x.toFixed(0) + '_' + (k);
        const book = new THREE.Group();
        const b = new THREE.Mesh(new THREE.BoxGeometry(0.16, 0.44, 0.3),
          M({ color: cols[(row * 4 + i) % cols.length], roughness: 0.85 }));
        b.castShadow = true;
        const spine = new THREE.Mesh(new THREE.BoxGeometry(0.03, 0.3, 0.02),
          M({ color: 0xd9c9a0, roughness: 0.7 }));
        spine.position.set(0, 0, 0.16);
        book.add(b, spine);
        book.position.set(-1.0 + i * 0.3 + (row * 0.06), y + 0.22, 0.06);
        g.add(book);
        const isKey = (k === keyIndex);
        reg(book, id, isKey ? 'Heavy ledger' : 'Book', 'Take', isKey ? 'keybook' : 'book');
        book.userData.parentShelf = g;
        k++;
      }
    });
    return g;
  }

  /* desk with a sliding drawer */
  function deskWithDrawer(x, z, ry, id, label) {
    const g = new THREE.Group();
    const top = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.1, 1.15), M({ color: 0x4a3a2c, roughness: 0.8 }));
    top.position.y = 0.78; top.castShadow = true; top.receiveShadow = true;
    const body = new THREE.Mesh(new THREE.BoxGeometry(2.2, 0.72, 1.0), M({ color: 0x2f2620, roughness: 0.9 }));
    body.position.y = 0.39; body.castShadow = true;
    g.add(top, body);
    const drawer = new THREE.Group();
    const face = new THREE.Mesh(new THREE.BoxGeometry(0.95, 0.34, 0.08), M({ color: 0x5b4838, roughness: 0.7 }));
    const handle = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.05, 0.05), M({ color: 0xa9b4bd, metalness: 0.9, roughness: 0.3 }));
    handle.position.z = 0.06;
    const coinInside = new THREE.Mesh(new THREE.CylinderGeometry(0.11, 0.11, 0.02, 16),
      M({ color: 0xffb347, emissive: 0xffb347, emissiveIntensity: 0.9, metalness: 0.7, roughness: 0.3 }));
    coinInside.rotation.x = Math.PI / 2; coinInside.position.set(0, -0.06, -0.2);
    drawer.add(face, handle, coinInside);
    drawer.position.set(-0.5, 0.5, 0.52);
    g.add(drawer);
    g.position.set(x, 0, z); g.rotation.y = ry;
    scene.add(g); addCollider(g, 0.0);
    reg(drawer, id, label, 'Pull open', 'drawer');
    drawer.userData.slide = anim({ t: 0, target: 0, speed: 2.6, apply(v) { drawer.position.z = 0.52 + v * 0.44; } });
    hideSpots.push({
      id: 'under_' + id,
      pos: new THREE.Vector3(x + Math.sin(ry) * 1.25, 0, z + Math.cos(ry) * 1.25),
      obj: g, label: 'Hide under desk'
    });
    return g;
  }

  function lockerBank(x, z, ry) {
    const g = new THREE.Group();
    for (let i = 0; i < 5; i++) {
      const l = new THREE.Mesh(new THREE.BoxGeometry(0.8, 2.0, 0.55),
        M({ color: i === 2 ? 0x2f5c63 : 0x35434b, roughness: 0.6, metalness: 0.5 }));
      l.position.set(i * 0.84, 1.0, 0); l.castShadow = true; g.add(l);
      const vent = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.22, 0.02), darkMat);
      vent.position.set(i * 0.84, 1.72, 0.29); g.add(vent);
    }
    g.position.set(x, 0, z); g.rotation.y = ry;
    scene.add(g); addCollider(g, 0.08, true);
    return g;
  }

  function terminal(x, z, ry, id, title, accent, lines) {
    const g = new THREE.Group();
    const stand = new THREE.Mesh(new THREE.BoxGeometry(1.6, 1.0, 0.75), M({ color: 0x1d242a, roughness: 0.6, metalness: 0.4 }));
    stand.position.y = 0.5; stand.castShadow = true;
    const bez = new THREE.Mesh(new THREE.BoxGeometry(1.5, 1.12, 0.1), M({ color: 0x11181d, roughness: 0.5, metalness: 0.6 }));
    bez.position.set(0, 1.58, -0.04);
    const scr = new THREE.Mesh(new THREE.PlaneGeometry(1.34, 0.96),
      M({ map: screenTex(lines, accent), emissive: new THREE.Color(accent), emissiveIntensity: 1.15, roughness: 0.3 }));
    scr.position.set(0, 1.58, 0.03);
    g.add(stand, bez, scr);
    g.position.set(x, 0, z); g.rotation.y = ry;
    scene.add(g); addCollider(g, 0.12);
    reg(g, id, title, 'Jack in', 'terminal');
    g.userData.screen = scr;
    const l = new THREE.PointLight(new THREE.Color(accent), 6, 8);
    l.position.set(x, 1.9, z + Math.cos(ry) * 0.8); scene.add(l);
    g.userData.light = l;
    return g;
  }

  function coin(x, z, value, y) {
    const m = new THREE.Mesh(new THREE.CylinderGeometry(0.17, 0.17, 0.035, 18),
      M({ color: 0xffcf70, emissive: 0xffb347, emissiveIntensity: 1.1, metalness: 0.8, roughness: 0.25 }));
    m.position.set(x, y || 0.75, z);
    m.rotation.x = Math.PI / 2;
    scene.add(m);
    const gl = new THREE.PointLight(0xffb347, 1.3, 2.6); gl.position.set(x, (y || 0.75) + 0.1, z); scene.add(gl);
    coins.push({ mesh: m, light: gl, value: value || 5 });
  }

  /* ---------- room dressing ---------- */
  // LAB 01 (north-west): kernel node
  terminal(-21.5, -12.5, 0.5, 'node1', 'KERNEL NODE', '#7ef3e8',
    ['> NODE 01 / KERNEL', '> integrity: degraded', '> minigame: KERNEL PANIC', '> reward: 40 CR + token']);
  for (let i = 0; i < 3; i++) {
    const bench = new THREE.Mesh(new THREE.BoxGeometry(3.4, 0.9, 0.9), M({ color: 0x2a3238, roughness: 0.8 }));
    bench.position.set(-19 + i * 0.0, 0.45, -10 + i * 2.4); bench.castShadow = true;
    scene.add(bench); addCollider(bench, 0.05);
    const beaker = new THREE.Mesh(new THREE.CylinderGeometry(0.11, 0.13, 0.32, 12),
      M({ color: 0x9fe8ff, emissive: 0x2f9fb8, emissiveIntensity: 1.1, transparent: true, opacity: 0.72 }));
    beaker.position.set(-18.2 + i * 0.6, 1.06, -10 + i * 2.4); scene.add(beaker);
  }
  almirah(-23.6, -6.2, Math.PI / 2, true);
  deskWithDrawer(-13.4, -13.0, 0, 'drawer1', 'Lab drawer');
  coin(-16.5, -8.4, 5);

  // CLASS 2A (north-middle): whiteboard, desks, bookshelf with the ledger
  const board = new THREE.Mesh(new THREE.BoxGeometry(5.4, 2.2, 0.12),
    M({ map: boardTex(), roughness: 0.55, emissive: 0x1b3a3d, emissiveIntensity: 0.95 }));
  board.position.set(-1.2, 2.0, -14.78); scene.add(board);
  deskGrid(0, -9.4, 3, 3);
  bookshelf(6.6, -13.2, 0, 5);
  deskWithDrawer(-4.6, -13.2, 0, 'drawer2', 'Teacher desk drawer');
  coin(2.4, -6.0, 5);

  // LAB 02 (north-east): circuit node
  terminal(21.8, -12.4, 0.7, 'node2', 'CIRCUIT NODE', '#ff3d7f',
    ['> NODE 02 / CIRCUIT', '> bus: fragmented', '> minigame: CIRCUIT BREAKER', '> reward: 50 CR + token']);
  lockerBank(10.4, -13.4, 0);
  for (let i = 0; i < 4; i++) {
    const spool = new THREE.Mesh(new THREE.CylinderGeometry(0.42, 0.42, 0.5, 14), M({ color: 0x22303a, roughness: 0.85 }));
    spool.position.set(13 + (i % 2) * 1.2, 0.25, -7 - Math.floor(i / 2) * 1.3);
    spool.rotation.z = Math.PI / 2; spool.castShadow = true;
    scene.add(spool); addCollider(spool, 0.02);
  }
  bookshelf(23.4, -6.6, -Math.PI / 2, -1);
  coin(17.2, -9.2, 5);

  // CLASS 1B (south-west)
  deskGrid(-16.75, 9.4, 3, 3);
  almirah(-23.6, 6.4, Math.PI / 2, false);
  bookshelf(-11.0, 13.0, Math.PI, 2);
  coin(-20.4, 12.6, 5);

  // SERVER ROOM (south-middle): silent code node + racks
  terminal(-4.2, 13.6, Math.PI, 'node3', 'SILENT CODE NODE', '#4dff9e',
    ['> NODE 03 / SEQUENCE', '> order lost', '> minigame: SILENT CODE', '> reward: 35 CR + token']);
  for (let i = 0; i < 5; i++) {
    const rack = new THREE.Mesh(new THREE.BoxGeometry(1.1, 2.4, 0.9), M({ color: 0x141a20, roughness: 0.7, metalness: 0.4 }));
    rack.position.set(-6 + i * 2.8, 1.2, 8.6); rack.castShadow = true;
    scene.add(rack); addCollider(rack, 0.1, true);
    const leds = new THREE.Mesh(new THREE.PlaneGeometry(0.85, 2.0),
      M({ color: 0x02110d, emissive: i % 2 ? 0x35e0d8 : 0xff3d7f, emissiveIntensity: 0.85 }));
    leds.position.set(-6 + i * 2.8, 1.25, 8.15); leds.rotation.y = Math.PI; scene.add(leds);
    flickers.push({ panel: leds, light: { intensity: 0 }, base: 0 });
  }
  coin(4.6, 12.2, 5);

  // EXIT BAY (south-east): almirah, crates, exit door + keypad
  almirah(23.4, 12.6, -Math.PI / 2, true);
  for (let i = 0; i < 5; i++) {
    const c = new THREE.Mesh(new THREE.BoxGeometry(1.0, 1.0, 1.0), M({ color: 0x33404a, roughness: 0.85 }));
    c.position.set(11.5 + (i % 3) * 1.2, 0.5 + (i > 2 ? 1 : 0), 6.4 + Math.floor(i / 3) * 1.2);
    c.castShadow = true; scene.add(c); addCollider(c, 0.02, false);
  }
  deskWithDrawer(13.6, 13.4, Math.PI, 'drawer3', 'Storage drawer');

  const exitFrame = new THREE.Mesh(new THREE.BoxGeometry(2.4, 2.9, 0.3), metalMat);
  exitFrame.position.set(19.6, 1.45, 14.86); scene.add(exitFrame);
  const doorPivot = new THREE.Group();
  doorPivot.position.set(18.5, 0, 14.74);
  const doorMesh = new THREE.Mesh(new THREE.BoxGeometry(2.1, 2.6, 0.12), M({ color: 0x384249, roughness: 0.6, metalness: 0.45 }));
  doorMesh.position.set(1.05, 1.3, 0); doorMesh.castShadow = true;
  const doorGlass = new THREE.Mesh(new THREE.PlaneGeometry(1.3, 0.5),
    M({ color: 0x0a1c22, emissive: 0xff5a4a, emissiveIntensity: 0.85 }));
  doorGlass.position.set(1.05, 1.95, 0.07);
  doorPivot.add(doorMesh, doorGlass);
  scene.add(doorPivot);
  reg(doorPivot, 'exit', 'EXIT BAY DOOR', 'Force open', 'exit');
  named.exitGlass = doorGlass;
  const exitAnim = anim({ t: 0, target: 0, speed: 0.9, apply(v) { doorPivot.rotation.y = -v * 1.6; } });
  named.exitAnim = exitAnim;
  const exitLight = new THREE.PointLight(0xff5a4a, 5, 9); exitLight.position.set(19.6, 2.2, 13.4); scene.add(exitLight);
  named.exitLight = exitLight;

  /* loose corridor coins */
  [[-22, 0], [-7.5, 1.2], [7.5, -1.2], [22, 0.6]].forEach(([x, z]) => coin(x, z, 5, 0.55));

  /* ---------- the SENTINEL (robed pursuer) ---------- */
  const sentinel = new THREE.Group();
  const robe = new THREE.Mesh(new THREE.ConeGeometry(0.68, 2.0, 14),
    M({ color: 0x10141b, roughness: 0.95 }));
  robe.position.y = 1.0; robe.castShadow = true;
  const shoulders = new THREE.Mesh(new THREE.SphereGeometry(0.44, 14, 10),
    M({ color: 0x151a22, roughness: 0.9 }));
  shoulders.position.y = 1.92; shoulders.scale.set(1.1, 0.8, 1.1);
  const hood = new THREE.Mesh(new THREE.SphereGeometry(0.36, 14, 12, 0, Math.PI * 2, 0, Math.PI * 0.72),
    M({ color: 0x0b0e13, roughness: 1 }));
  hood.position.y = 2.16;
  const visor = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.07, 0.05),
    M({ color: 0x000000, emissive: 0x35e0d8, emissiveIntensity: 2.6 }));
  visor.position.set(0, 2.12, 0.3);
  const hem = new THREE.Mesh(new THREE.TorusGeometry(0.66, 0.035, 6, 18),
    M({ color: 0x000000, emissive: 0x35e0d8, emissiveIntensity: 1.1 }));
  hem.rotation.x = Math.PI / 2; hem.position.y = 0.09;
  const cone = new THREE.Mesh(new THREE.ConeGeometry(4.6, 13, 18, 1, true),
    new THREE.MeshBasicMaterial({ color: 0x35e0d8, transparent: true, opacity: 0.055, side: THREE.DoubleSide, depthWrite: false }));
  cone.rotation.x = -Math.PI / 2; cone.position.set(0, 1.5, 6.5);
  const sLight = new THREE.PointLight(0x35e0d8, 6, 10);
  sLight.position.y = 2.1;
  sentinel.add(robe, shoulders, hood, visor, hem, cone, sLight);
  sentinel.position.set(0, 0, 0);
  scene.add(sentinel);

  const waypoints = [
    new THREE.Vector3(-22, 0, 0), new THREE.Vector3(-16.75, 0, -9),
    new THREE.Vector3(-16.75, 0, 0), new THREE.Vector3(-16.75, 0, 9),
    new THREE.Vector3(-4, 0, 0), new THREE.Vector3(0, 0, -9),
    new THREE.Vector3(0, 0, 0), new THREE.Vector3(0, 0, 9),
    new THREE.Vector3(8, 0, 0), new THREE.Vector3(16.75, 0, -9),
    new THREE.Vector3(16.75, 0, 0), new THREE.Vector3(16.75, 0, 9),
    new THREE.Vector3(22, 0, 0)
  ];
  const ai = { state: 'PATROL', wp: 6, yaw: 0, lost: 0, lastSeen: new THREE.Vector3(), stun: 0, growl: 0 };
  sentinel.position.copy(waypoints[6]);

  /* ---------- player ---------- */
  const spawn = new THREE.Vector3(-22.4, 0, 1.6);
  const player = {
    pos: spawn.clone(), yaw: -1.2, pitch: -0.04, bob: 0,
    hidden: false, hideSpot: null, stamina: 1,
    crouch: false, sitting: null, y: 0, vy: 0, grounded: true
  };
  const keys = {};
  let paused = true, hoverId = null, running = true;

  const onKey = (e) => {
    const k = e.key.toLowerCase();
    if (['w', 'a', 's', 'd', 'shift', ' ', 'e', 'f', 'arrowleft', 'arrowright', 'arrowup', 'arrowdown'].includes(k)) e.preventDefault();
    const down = e.type === 'keydown';
    keys[k] = down;
    if (['c', 'control'].includes(k)) e.preventDefault();
    if (!down || paused) return;
    if (k === 'e') tryUse();
    if (k === 'f') toggleHide();
    if (k === ' ') jump();
  };
  window.addEventListener('keydown', onKey);
  window.addEventListener('keyup', onKey);

  let dragging = false, dragDist = 0, lastX = 0, lastY = 0;
  const look = (dx, dy) => {
    player.yaw -= dx * 0.0024;
    player.pitch = Math.max(-1.3, Math.min(1.3, player.pitch - dy * 0.0024));
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
  renderer.domElement.addEventListener('click', () => { if (dragDist <= 8 && !paused) tryUse(); });

  const ray = new THREE.Raycaster();
  ray.far = 3.6;

  /* ---------- interaction ---------- */
  function currentHideSpot() {
    let best = null, bd = 1.9;
    for (const s of hideSpots) {
      const d = Math.hypot(s.pos.x - player.pos.x, s.pos.z - player.pos.z);
      if (d < bd) { bd = d; best = s; }
    }
    if (!best) return null;
    best.ready = !best.needsOpen || best.needsOpen.t > 0.6;
    return best;
  }

  function jump() {
    if (player.hidden || player.sitting || !player.grounded) return;
    player.vy = 4.7; player.grounded = false;
  }

  function sit(spot) {
    player.sitting = spot;
    player.pos.x = spot.x; player.pos.z = spot.z;
    player.yaw = spot.yaw; player.y = 0; player.vy = 0;
    cb('onPosture', 'SEATED', 'Seated — the desk hides your outline. E to stand.');
  }

  function stand() {
    player.sitting = null;
    cb('onPosture', player.crouch ? 'CROUCHED' : 'STANDING', 'On your feet.');
  }

  function toggleHide() {
    if (player.hidden) {
      player.hidden = false; player.hideSpot = null;
      cb('onHide', false);
      return;
    }
    if (player.sitting) { stand(); return; }
    const s = currentHideSpot();
    if (!s) return;
    if (!s.ready) { cb('onHide', false, null, 'Open the almirah before you can climb in.'); return; }
    player.hidden = true; player.hideSpot = s;
    player.pos.x = s.pos.x; player.pos.z = s.pos.z;
    player.y = 0; player.vy = 0;
    cb('onHide', true, s.label);
  }

  function tryUse() {
    if (player.hidden) { toggleHide(); return; }
    if (player.sitting) { stand(); return; }
    if (!hoverId) { const s = currentHideSpot(); if (s && s.ready) toggleHide(); return; }
    const obj = named[hoverId];
    if (!obj) return;
    const kind = obj.userData.kind;

    if (kind === 'chair') { sit(obj.userData.sit); return; }

    if (kind === 'almirah') {
      const d = obj.userData.door;
      d.target = d.target > 0.5 ? 0 : 1;
      obj.userData.verb = d.target > 0.5 ? 'Close' : 'Open';
      cb('onUse', { id: hoverId, kind: 'almirah', open: d.target > 0.5, label: obj.userData.label });
      if (d.target > 0.5 && obj.userData.stash) {
        const s = obj.userData.stash;
        obj.remove(s);
        obj.userData.stash = null;
        cb('onCoin', 15, 'Almirah stash');
      }
      cb('onHover', hoverId, obj.userData.label, obj.userData.verb);
      return;
    }
    if (kind === 'drawer') {
      const s = obj.userData.slide;
      s.target = s.target > 0.5 ? 0 : 1;
      obj.userData.verb = s.target > 0.5 ? 'Push shut' : 'Pull open';
      if (s.target > 0.5 && !obj.userData.looted) {
        obj.userData.looted = true;
        cb('onCoin', 10, 'Drawer');
      }
      cb('onUse', { id: hoverId, kind: 'drawer', open: s.target > 0.5, label: obj.userData.label });
      cb('onHover', hoverId, obj.userData.label, obj.userData.verb);
      return;
    }
    if (kind === 'book' || kind === 'keybook') {
      const shelf = obj.userData.parentShelf;
      if (shelf) shelf.remove(obj);
      const i = interactables.indexOf(obj); if (i >= 0) interactables.splice(i, 1);
      delete named[hoverId];
      hoverId = null;
      cb('onHover', null, null, null);
      cb('onUse', { id: obj.userData.id, kind: kind, label: obj.userData.label });
      if (kind === 'keybook') cb('onCoin', 20, 'Hollow ledger');
      return;
    }
    cb('onUse', { id: hoverId, kind: kind, label: obj.userData.label });
  }

  /* ---------- collision & sight ---------- */
  function blocked(nx, nz) {
    for (const b of colliders) {
      if (b.max.y < 0.6) continue;
      if (nx > b.min.x && nx < b.max.x && nz > b.min.z && nz < b.max.z) return true;
    }
    return false;
  }
  function segBlocked(ax, az, bx, bz) {
    const dx = bx - ax, dz = bz - az;
    for (const b of sightBlockers) {
      if (b.max.y < 1.2) continue;
      let t0 = 0, t1 = 1, ok = true;
      [[ax, dx, b.min.x, b.max.x], [az, dz, b.min.z, b.max.z]].forEach(([o, d, mn, mx]) => {
        if (!ok) return;
        if (Math.abs(d) < 1e-6) { if (o < mn || o > mx) ok = false; return; }
        let a = (mn - o) / d, c = (mx - o) / d;
        if (a > c) { const tmp = a; a = c; c = tmp; }
        t0 = Math.max(t0, a); t1 = Math.min(t1, c);
        if (t0 > t1) ok = false;
      });
      if (ok) return true;
    }
    return false;
  }

  function resetPlayer() {
    player.pos.copy(spawn);
    player.yaw = -1.2; player.hidden = false; player.hideSpot = null;
    player.sitting = null; player.crouch = false;
    player.y = 0; player.vy = 0; player.grounded = true;
    ai.state = 'PATROL'; ai.stun = 3.2; ai.wp = 6;
    sentinel.position.copy(waypoints[6]);
  }

  /* ---------- loop ---------- */
  let t0 = performance.now(), tickAcc = 0;
  let difficulty = 1;

  function frame() {
    if (!running) return;
    requestAnimationFrame(frame);
    const now = performance.now();
    const dt = Math.min(0.05, (now - t0) / 1000); t0 = now;
    const tt = now / 1000;

    /* flicker + emissive life */
    flickers.forEach(f => {
      const n = (Math.sin(tt * 13.7 + f.base) * Math.sin(tt * 4.1) + 1) / 2;
      const v = n > 0.35 ? 1 : 0.14 + Math.random() * 0.2;
      if (f.light && f.light.intensity !== undefined && f.base) f.light.intensity = f.base * v;
      f.panel.material.emissiveIntensity = (f.base ? 1.4 : 0.9) * v;
    });
    coins.forEach(c => { c.mesh.rotation.z += dt * 2.4; c.mesh.position.y += Math.sin(tt * 2 + c.mesh.position.x) * 0.0008; });

    animators.forEach(a => {
      if (a.t !== a.target) {
        a.t += Math.sign(a.target - a.t) * dt * a.speed;
        if (Math.abs(a.target - a.t) < 0.02) a.t = a.target;
        a.t = Math.max(0, Math.min(1, a.t));
        a.apply(a.t);
      }
    });

    if (!paused) {
      /* posture */
      const wantCrouch = !!(keys['c'] || keys['control']);
      if (wantCrouch !== player.crouch && !player.hidden && !player.sitting) {
        player.crouch = wantCrouch;
        cb('onPosture', wantCrouch ? 'CROUCHED' : 'STANDING', wantCrouch ? 'Crouched — harder to spot, slower to run.' : null);
      }

      /* jump arc */
      if (!player.grounded || player.y > 0) {
        player.vy -= 15.5 * dt;
        player.y += player.vy * dt;
        if (player.y <= 0) { player.y = 0; player.vy = 0; player.grounded = true; }
      }

      /* movement */
      if (!player.hidden && !player.sitting) {
        const wantSprint = keys['shift'] && player.stamina > 0.05 && !player.crouch;
        const sp = (wantSprint ? 5.0 : player.crouch ? 1.45 : 2.9) * dt;
        player.stamina = Math.max(0, Math.min(1, player.stamina + (wantSprint ? -dt * 0.28 : dt * 0.17)));
        let fx = 0, fz = 0;
        if (keys['w']) fz += 1; if (keys['s']) fz -= 1;
        if (keys['a']) fx -= 1; if (keys['d']) fx += 1;
        if (fx || fz) {
          const len = Math.hypot(fx, fz);
          const sin = Math.sin(player.yaw), cos = Math.cos(player.yaw);
          const dx = (-sin * fz / len + cos * fx / len) * sp;
          const dz = (-cos * fz / len - sin * fx / len) * sp;
          if (!blocked(player.pos.x + dx, player.pos.z)) player.pos.x += dx;
          if (!blocked(player.pos.x, player.pos.z + dz)) player.pos.z += dz;
          player.pos.x = Math.max(-HX + 0.45, Math.min(HX - 0.45, player.pos.x));
          player.pos.z = Math.max(-HZ + 0.45, Math.min(HZ - 0.45, player.pos.z));
          player.bob += dt * (wantSprint ? 13 : 8.5);
        }
      }
      const turn = 1.9 * dt;
      if (keys['arrowleft']) player.yaw += turn;
      if (keys['arrowright']) player.yaw -= turn;
      if (keys['arrowup']) player.pitch = Math.min(1.3, player.pitch + turn * 0.6);
      if (keys['arrowdown']) player.pitch = Math.max(-1.3, player.pitch - turn * 0.6);

      /* coins */
      for (let i = coins.length - 1; i >= 0; i--) {
        const c = coins[i];
        if (Math.hypot(c.mesh.position.x - player.pos.x, c.mesh.position.z - player.pos.z) < 1.15) {
          scene.remove(c.mesh); scene.remove(c.light);
          coins.splice(i, 1);
          cb('onCoin', c.value, 'Credit chip');
        }
      }

      /* sentinel AI */
      const sp3 = sentinel.position;
      const dToPlayer = Math.hypot(player.pos.x - sp3.x, player.pos.z - sp3.z);
      const facing = new THREE.Vector2(Math.sin(ai.yaw), Math.cos(ai.yaw));
      const toP = new THREE.Vector2(player.pos.x - sp3.x, player.pos.z - sp3.z).normalize();
      const angle = Math.acos(Math.max(-1, Math.min(1, facing.dot(toP))));
      const sightRange = player.crouch || player.sitting ? 8.5 : 15;
      const visible = !player.hidden && ai.stun <= 0 && dToPlayer < sightRange &&
        (angle < 0.62 || dToPlayer < 3.2) && !segBlocked(sp3.x, sp3.z, player.pos.x, player.pos.z);

      if (ai.stun > 0) ai.stun -= dt;

      if (visible) {
        if (ai.state !== 'CHASE') cb('onAlert', 'CHASE');
        ai.state = 'CHASE'; ai.lost = 0; ai.lastSeen.set(player.pos.x, 0, player.pos.z);
      } else if (ai.state === 'CHASE') {
        ai.lost += dt;
        if (ai.lost > 3.4) { ai.state = 'SEARCH'; ai.lost = 0; cb('onAlert', 'SEARCH'); }
      } else if (ai.state === 'SEARCH') {
        ai.lost += dt;
        if (ai.lost > 5.5) { ai.state = 'PATROL'; ai.lost = 0; cb('onAlert', 'PATROL'); }
      }

      let target;
      if (ai.state === 'CHASE') target = player.pos;
      else if (ai.state === 'SEARCH') target = ai.lastSeen;
      else {
        target = waypoints[ai.wp];
        if (Math.hypot(target.x - sp3.x, target.z - sp3.z) < 1.0) {
          const near = waypoints.map((w, i) => [i, Math.hypot(w.x - sp3.x, w.z - sp3.z)])
            .filter(([i, d]) => d > 1 && d < 11 && i !== ai.wp);
          ai.wp = near.length ? near[Math.floor(Math.random() * near.length)][0]
            : Math.floor(Math.random() * waypoints.length);
        }
      }

      const spd = (ai.state === 'CHASE' ? 3.45 : ai.state === 'SEARCH' ? 2.5 : 1.95) * difficulty * (ai.stun > 0 ? 0.25 : 1);
      let vx = target.x - sp3.x, vz = target.z - sp3.z;
      const vl = Math.hypot(vx, vz) || 1;
      vx = vx / vl * spd * dt; vz = vz / vl * spd * dt;
      const pad = 0.55;
      const free = (x, z) => !blocked(x, z);
      if (free(sp3.x + vx, sp3.z)) sp3.x += vx;
      else if (free(sp3.x, sp3.z + Math.sign(vz || 1) * spd * dt)) sp3.z += Math.sign(vz || 1) * spd * dt;
      if (free(sp3.x, sp3.z + vz)) sp3.z += vz;
      else if (free(sp3.x + Math.sign(vx || 1) * spd * dt, sp3.z)) sp3.x += Math.sign(vx || 1) * spd * dt;
      sp3.x = Math.max(-HX + pad, Math.min(HX - pad, sp3.x));
      sp3.z = Math.max(-HZ + pad, Math.min(HZ - pad, sp3.z));

      const wantYaw = Math.atan2(target.x - sp3.x, target.z - sp3.z);
      let dy = wantYaw - ai.yaw;
      while (dy > Math.PI) dy -= Math.PI * 2;
      while (dy < -Math.PI) dy += Math.PI * 2;
      ai.yaw += dy * Math.min(1, dt * 4.5);
      sentinel.rotation.y = ai.yaw;
      sentinel.position.y = Math.sin(tt * 2.6) * 0.045;

      const chase = ai.state === 'CHASE';
      const col = chase ? 0xff3d5a : ai.state === 'SEARCH' ? 0xffb347 : 0x35e0d8;
      visor.material.emissive.setHex(col);
      hem.material.emissive.setHex(col);
      cone.material.color.setHex(col);
      cone.material.opacity = chase ? 0.1 : 0.055;
      sLight.color.setHex(col);
      sLight.intensity = 5 + (chase ? 3.5 + Math.sin(tt * 9) * 1.6 : 0);

      if (dToPlayer < 1.25 && !player.hidden && ai.stun <= 0) {
        player.sitting = null; player.crouch = false;
        cb('onCaught');
        resetPlayer();
      }

      /* hover raycast */
      ray.setFromCamera({ x: 0, y: 0 }, camera);
      const hits = ray.intersectObjects(interactables, true);
      let id = null;
      for (const h of hits) { if (h.object.userData.hitId && named[h.object.userData.hitId]) { id = h.object.userData.hitId; break; } }
      if (id !== hoverId) {
        hoverId = id;
        const o = id ? named[id] : null;
        cb('onHover', id, o ? o.userData.label : null, o ? o.userData.verb : null);
      }
      interactables.forEach(o => {
        const on = o.userData.id === hoverId;
        o.traverse(m => {
          if (m.isMesh && m.material && m.material.emissive) {
            if (m.userData.baseEmi === undefined) m.userData.baseEmi = m.material.emissiveIntensity;
            if (m.userData.baseCol === undefined) m.userData.baseCol = m.material.emissive.getHex();
            const tg = on ? Math.max(0.6, m.userData.baseEmi) : m.userData.baseEmi;
            if (on && m.userData.baseCol === 0x000000) m.material.emissive.setHex(0x1f6d68);
            else if (!on && m.userData.baseCol === 0x000000) m.material.emissive.setHex(0x000000);
            m.material.emissiveIntensity += (tg - m.material.emissiveIntensity) * 0.2;
          }
        });
      });

      /* HUD tick */
      tickAcc += dt;
      if (tickAcc > 0.08) {
        tickAcc = 0;
        const hs = player.hidden ? null : currentHideSpot();
        cb('onTick', {
          px: player.pos.x, pz: player.pos.z, pyaw: player.yaw,
          sx: sp3.x, sz: sp3.z, state: ai.state,
          dist: dToPlayer, hidden: player.hidden,
          stamina: player.stamina,
          posture: player.hidden ? 'HIDDEN' : player.sitting ? 'SEATED' : player.crouch ? 'CROUCHED' : 'STANDING',
          hideHint: hs ? (hs.ready ? hs.label : 'Open the almirah first') : null
        });
      }
    }

    /* camera */
    const eye = player.hidden ? 1.0 : player.sitting ? 1.18 : player.crouch ? 0.95 : 1.62;
    const smooth = camera.userData.eye === undefined ? eye : camera.userData.eye + (eye - camera.userData.eye) * 0.18;
    camera.userData.eye = smooth;
    camera.position.set(player.pos.x, smooth + player.y + Math.sin(player.bob) * 0.035, player.pos.z);
    camera.rotation.order = 'YXZ';
    camera.rotation.y = player.yaw; camera.rotation.x = player.pitch;

    renderer.render(scene, camera);
  }
  frame();

  function resize() {
    const w = container.clientWidth || window.innerWidth;
    const h = container.clientHeight || window.innerHeight;
    camera.aspect = w / h; camera.updateProjectionMatrix();
    renderer.setSize(w, h, false);
  }
  const ro = new ResizeObserver(resize); ro.observe(container); resize();

  return {
    setPaused(v) { paused = v; if (!v) t0 = performance.now(); },
    setDifficulty(v) { difficulty = v; },
    unlockExit() {
      named.exitGlass.material.emissive.setHex(0x4dff9e);
      named.exitLight.color.setHex(0x4dff9e);
      named.exitAnim.target = 0;
    },
    openExit() { named.exitAnim.target = 1; },
    setTerminalDone(id) {
      const t = named[id];
      if (!t) return;
      t.userData.verb = 'Cleared';
      t.userData.kind = 'done';
      t.userData.screen.material.map = screenTex(['> NODE CLEARED', '> token extracted', '> credits banked', '> move.'], '#4dff9e');
      t.userData.screen.material.emissive.setHex(0x4dff9e);
      t.userData.screen.material.needsUpdate = true;
      t.userData.light.color.setHex(0x4dff9e);
    },
    stunSentinel(s) { ai.stun = s || 3; },
    tp(x, z, yaw) { player.pos.set(x, 1.62, z); if (yaw !== undefined) player.yaw = yaw; ai.stun = 6; },
    resetPlayer,
    lock() {
      try { const p = renderer.domElement.requestPointerLock(); if (p && p.catch) p.catch(() => {}); }
      catch (e) { /* drag-to-look fallback */ }
    },
    unlock() { if (document.pointerLockElement) document.exitPointerLock(); },
    dispose() {
      running = false; ro.disconnect();
      window.removeEventListener('keydown', onKey);
      window.removeEventListener('keyup', onKey);
      window.removeEventListener('mouseup', onUp);
      document.removeEventListener('mousemove', onMove);
      renderer.dispose();
      if (renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement);
    }
  };
}
