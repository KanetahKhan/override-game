import * as THREE from 'three';

// OVERRIDE palette (src/main/resources/styles/main.css)
const CY = 0x28e0c0, CY_BRIGHT = 0x4dffe0, AMBER = 0xffd66e, RED = 0xff7e7e;
const BG = 0x050a14, FLOOR = 0x0a1622;

const M = (o) => new THREE.MeshStandardMaterial(o);

function tex(draw, w = 512, h = 512) {
  const c = document.createElement('canvas');
  c.width = w; c.height = h;
  draw(c.getContext('2d'), w, h);
  const t = new THREE.CanvasTexture(c); t.anisotropy = 4; return t;
}

const gridTex = (base, line) => tex((g, w, h) => {
  g.fillStyle = base; g.fillRect(0, 0, w, h);
  g.strokeStyle = line; g.lineWidth = 3;
  for (let i = 0; i <= 8; i++) {
    g.beginPath(); g.moveTo(i * w / 8, 0); g.lineTo(i * w / 8, h); g.stroke();
    g.beginPath(); g.moveTo(0, i * h / 8); g.lineTo(w, i * h / 8); g.stroke();
  }
  for (let i = 0; i < 900; i++) { g.fillStyle = 'rgba(255,255,255,' + (Math.random() * 0.025) + ')'; g.fillRect(Math.random() * w, Math.random() * h, 2, 2); }
});

const plateTex = (label, sub) => tex((g, w, h) => {
  g.fillStyle = '#081018'; g.fillRect(0, 0, w, h);
  g.strokeStyle = '#28e0c0'; g.lineWidth = 6; g.strokeRect(8, 8, w - 16, h - 16);
  g.fillStyle = '#e7faff'; g.font = 'bold 52px "Segoe UI",sans-serif';
  g.fillText(label, 26, 78);
  g.fillStyle = '#7fcfd9'; g.font = '600 26px "Segoe UI",sans-serif';
  g.fillText(sub, 26, 116);
}, 512, 148);

const boardTex = (lines) => tex((g, w, h) => {
  g.fillStyle = '#122029'; g.fillRect(0, 0, w, h);
  g.strokeStyle = 'rgba(40,224,192,.10)'; g.lineWidth = 2;
  for (let i = 0; i < 14; i++) { g.beginPath(); g.moveTo(Math.random() * w, Math.random() * h); g.lineTo(Math.random() * w, Math.random() * h); g.stroke(); }
  g.fillStyle = '#28e0c0'; g.font = 'bold 58px "Consolas",monospace';
  lines.forEach((l, i) => g.fillText(l, 40, 110 + i * 78));
}, 1024, 512);

const screenTex = (lines, accent) => tex((g, w, h) => {
  g.fillStyle = '#040c12'; g.fillRect(0, 0, w, h);
  g.fillStyle = accent || '#28e0c0'; g.font = 'bold 26px "Consolas",monospace';
  lines.forEach((l, i) => g.fillText(l, 22, 52 + i * 40));
  g.strokeStyle = 'rgba(40,224,192,.16)';
  for (let y = 0; y < h; y += 4) { g.beginPath(); g.moveTo(0, y); g.lineTo(w, y); g.stroke(); }
}, 512, 320);

export const ROOMS = [
  { name: 'Lecture Hall A', kind: 'class', tag: 'ROOM 01' },
  { name: 'Logic Lab', kind: 'lab', tag: 'ROOM 02' },
  { name: 'Study Room 3B', kind: 'class', tag: 'ROOM 03' },
  { name: 'Server Closet', kind: 'lab', tag: 'ROOM 04' },
  { name: 'Staff Room', kind: 'class', tag: 'ROOM 05' },
  { name: 'Robotics Lab', kind: 'lab', tag: 'ROOM 06' },
  { name: 'Archive 7', kind: 'class', tag: 'ROOM 07' }
];

// ---------------------------------------------------------------- robot
function buildRobot() {
  const g = new THREE.Group();
  const chassis = new THREE.Mesh(new THREE.CylinderGeometry(0.34, 0.42, 0.9, 12), M({ color: 0x243444, metalness: .7, roughness: .35 }));
  chassis.position.y = 0.55; chassis.castShadow = true;
  const torso = new THREE.Mesh(new THREE.BoxGeometry(0.6, 0.7, 0.45), M({ color: 0x1b2836, metalness: .6, roughness: .4 }));
  torso.position.y = 1.32; torso.castShadow = true;
  const head = new THREE.Mesh(new THREE.BoxGeometry(0.42, 0.3, 0.36), M({ color: 0x2c3f52, metalness: .7, roughness: .3 }));
  head.position.y = 1.82;
  const eye = new THREE.Mesh(new THREE.BoxGeometry(0.3, 0.08, 0.04), M({ color: 0x120404, emissive: RED, emissiveIntensity: 3 }));
  eye.position.set(0, 1.84, 0.19);
  const armL = new THREE.Mesh(new THREE.CapsuleGeometry(0.08, 0.5, 4, 8), M({ color: 0x1b2836, metalness: .6, roughness: .4 }));
  armL.position.set(-0.4, 1.3, 0);
  const armR = armL.clone(); armR.position.x = 0.4;
  const lamp = new THREE.PointLight(RED, 3, 6); lamp.position.set(0, 1.84, 0.3);
  g.add(chassis, torso, head, eye, armL, armR, lamp);
  g.userData.eye = eye; g.userData.lamp = lamp;
  return g;
}

function coneMesh(len, halfDeg, color) {
  const shape = new THREE.Shape();
  const a = THREE.MathUtils.degToRad(halfDeg);
  shape.moveTo(0, 0);
  for (let i = -a; i <= a; i += a / 10) shape.lineTo(Math.sin(i) * len, Math.cos(i) * len);
  shape.lineTo(0, 0);
  const m = new THREE.Mesh(new THREE.ShapeGeometry(shape), new THREE.MeshBasicMaterial({ color: color, transparent: true, opacity: 0.16, side: THREE.DoubleSide, depthWrite: false }));
  m.rotation.x = -Math.PI / 2; m.position.y = 0.06;
  return m;
}

// ---------------------------------------------------------------- game
export function createGame(container, cb) {
  cb = cb || {};
  const renderer = new THREE.WebGLRenderer({ antialias: true, preserveDrawingBuffer: true });
  renderer.setPixelRatio(Math.min(window.devicePixelRatio, 2));
  renderer.shadowMap.enabled = true; renderer.shadowMap.type = THREE.PCFSoftShadowMap;
  renderer.domElement.style.display = 'block';
  renderer.domElement.style.width = '100%';
  renderer.domElement.style.height = '100%';
  container.appendChild(renderer.domElement);

  const keys = {};
  let mode = 'hall';
  let hover = null, prompt = null;
  let caughtCool = 0;

  // ============================ HALLWAY (top-down) ============================
  const HL = 52, HW = 9;
  const hall = new THREE.Scene();
  hall.background = new THREE.Color(BG);
  hall.fog = new THREE.Fog(BG, 26, 46);
  const hallCam = new THREE.OrthographicCamera(-1, 1, 1, -1, 0.1, 120);
  hallCam.position.set(0, 22, 9.5);
  hallCam.zoom = 1;

  const hOuter = new THREE.Mesh(new THREE.PlaneGeometry(HL + 10, HW + 12), M({ color: 0x070d16, roughness: 1 }));
  hOuter.rotation.x = -Math.PI / 2; hOuter.position.y = -0.03; hall.add(hOuter);
  const hFloor = new THREE.Mesh(new THREE.PlaneGeometry(HL, HW), M({ map: gridTex('#0a1622', 'rgba(40,224,192,.07)'), roughness: .8, metalness: .1 }));
  hFloor.rotation.x = -Math.PI / 2; hFloor.receiveShadow = true; hall.add(hFloor);

  const wallMat = M({ color: 0x14202f, roughness: .9 });
  [-1, 1].forEach(s => {
    const w = new THREE.Mesh(new THREE.BoxGeometry(HL, 3.2, 0.4), wallMat);
    w.position.set(0, 1.6, s * (HW / 2)); w.castShadow = true; hall.add(w);
  });
  [-1, 1].forEach(s => {
    const w = new THREE.Mesh(new THREE.BoxGeometry(0.4, 3.2, HW), wallMat);
    w.position.set(s * (HL / 2), 1.6, 0); hall.add(w);
  });

  hall.add(new THREE.AmbientLight(0x2a4356, 0.75));
  hall.add(new THREE.HemisphereLight(0x1d4a58, 0x070c12, 0.6));
  for (let i = -3; i <= 3; i++) {
    const strip = new THREE.Mesh(new THREE.BoxGeometry(5.4, 0.08, 0.34), M({ color: 0xdff6ff, emissive: 0xbfeaff, emissiveIntensity: 1.5 }));
    strip.position.set(i * 7, 3.05, 0); hall.add(strip);
    const l = new THREE.PointLight(0xbfe6ff, 11, 15, 2); l.position.set(i * 7, 2.7, 0); l.castShadow = i % 2 === 0; hall.add(l);
  }

  // doors
  const doors = [];
  ROOMS.forEach((r, i) => {
    const side = i % 2 === 0 ? -1 : 1;
    const x = -HL / 2 + 6 + i * 6.6;
    const g = new THREE.Group();
    const frame = new THREE.Mesh(new THREE.BoxGeometry(2.5, 2.7, 0.5), M({ color: 0x1c2b3a, metalness: .5, roughness: .5 }));
    frame.position.y = 1.35;
    const panel = new THREE.Mesh(new THREE.BoxGeometry(2.1, 2.4, 0.16), M({ color: 0x0e1a26, emissive: 0x0b2a2a, emissiveIntensity: .8, metalness: .4, roughness: .5 }));
    panel.position.set(0, 1.2, side * -0.2);
    const plate = new THREE.Mesh(new THREE.PlaneGeometry(2.0, 0.58), M({ map: plateTex(r.tag, r.name.toUpperCase()), emissive: 0x143a3a, emissiveIntensity: 1.0 }));
    plate.position.set(0, 2.35, side * -0.29); plate.rotation.y = side < 0 ? 0 : Math.PI;
    plate.rotation.x = -0.0;
    const glow = new THREE.Mesh(new THREE.PlaneGeometry(2.6, 2.0), new THREE.MeshBasicMaterial({ color: CY, transparent: true, opacity: 0.07, depthWrite: false }));
    glow.rotation.x = -Math.PI / 2; glow.position.set(0, 0.04, side * -1.3);
    g.add(frame, panel, plate, glow);
    g.position.set(x, 0, side * (HW / 2 - 0.05));
    g.userData = { index: i, glow: glow, panel: panel };
    hall.add(g); doors.push(g);
  });

  const hPlayer = new THREE.Group();
  const pBody = new THREE.Mesh(new THREE.CapsuleGeometry(0.32, 0.85, 6, 12), M({ color: 0x1a3a44, emissive: 0x0d3a3a, emissiveIntensity: .55, roughness: .6 }));
  pBody.position.y = 0.95; pBody.castShadow = true;
  const pHead = new THREE.Mesh(new THREE.SphereGeometry(0.26, 14, 12), M({ color: 0x2b4b56, roughness: .7 }));
  pHead.position.y = 1.7;
  const pRing = new THREE.Mesh(new THREE.RingGeometry(0.42, 0.56, 24), new THREE.MeshBasicMaterial({ color: CY, transparent: true, opacity: .55, side: THREE.DoubleSide, depthWrite: false }));
  pRing.rotation.x = -Math.PI / 2; pRing.position.y = 0.04;
  hPlayer.add(pBody, pHead, pRing);
  hall.add(hPlayer);

  const hRobot = buildRobot();
  const hCone = coneMesh(11, 26, RED); hRobot.add(hCone);
  hall.add(hRobot);

  const hState = { px: -HL / 2 + 3, pz: 0, pyaw: 0, rx: 8, rz: 0, ryaw: Math.PI, chase: false, patrolDir: -1, alert: 0 };

  // ============================ ROOM (first person) ============================
  let room = null;

  function makeRoom(index) {
    const info = ROOMS[index];
    const isLab = info.kind === 'lab';
    const RW = 13, RD = 10, RH = 3.3;
    const sc = new THREE.Scene();
    sc.background = new THREE.Color(0x03070c);
    sc.fog = new THREE.FogExp2(0x03070c, 0.085);
    const cam = new THREE.PerspectiveCamera(74, 1, 0.05, 60);

    const fl = new THREE.Mesh(new THREE.PlaneGeometry(RW, RD), M({ map: gridTex(isLab ? '#0b1620' : '#101a22', 'rgba(0,0,0,.5)'), roughness: .85 }));
    fl.rotation.x = -Math.PI / 2; fl.receiveShadow = true; sc.add(fl);
    const cl = new THREE.Mesh(new THREE.PlaneGeometry(RW, RD), M({ color: 0x0c141c, roughness: 1 }));
    cl.rotation.x = Math.PI / 2; cl.position.y = RH; sc.add(cl);
    const wm = M({ color: isLab ? 0x16242f : 0x1a2530, roughness: .95 });
    const mkWall = (w, x, z, ry) => { const m = new THREE.Mesh(new THREE.PlaneGeometry(w, RH), wm); m.position.set(x, RH / 2, z); m.rotation.y = ry; m.receiveShadow = true; sc.add(m); };
    mkWall(RW, 0, -RD / 2, 0); mkWall(RW, 0, RD / 2, Math.PI);
    mkWall(RD, -RW / 2, 0, Math.PI / 2); mkWall(RD, RW / 2, 0, -Math.PI / 2);

    sc.add(new THREE.AmbientLight(0x16323f, 0.5));
    sc.add(new THREE.HemisphereLight(0x14424f, 0x05080c, 0.35));
    const em = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.14, 0.5), M({ color: 0x0e2a20, emissive: 0x1f7a4a, emissiveIntensity: 1.6 }));
    em.position.set(0, RH - 0.1, RD / 2 - 0.6); sc.add(em);
    const emL = new THREE.PointLight(0x2fd48a, 3.5, 8); emL.position.set(0, RH - 0.5, RD / 2 - 1); sc.add(emL);

    const flash = new THREE.SpotLight(0xdfefff, 26, 17, 0.45, 0.55, 1.4);
    flash.castShadow = true; flash.shadow.mapSize.set(1024, 1024);
    sc.add(flash, flash.target);

    const inter = [], solids = [], hideSpots = [];
    const regI = (obj, id, label, type) => {
      obj.userData.iid = id; obj.userData.label = label; obj.userData.type = type;
      obj.traverse(o => { if (o.isMesh) o.userData.hitId = id; });
      inter.push(obj); return obj;
    };
    const solid = (obj, pad) => { const b = new THREE.Box3().setFromObject(obj); b.expandByScalar(pad === undefined ? 0.05 : pad); solids.push(b); };

    // exit door
    const exitG = new THREE.Group();
    const exd = new THREE.Mesh(new THREE.BoxGeometry(1.3, 2.4, 0.12), M({ color: 0x22323f, metalness: .5, roughness: .5 }));
    exd.position.y = 1.2;
    const exp2 = new THREE.Mesh(new THREE.PlaneGeometry(1.0, 0.3), M({ map: plateTex('EXIT', 'BACK TO HALLWAY'), emissive: 0x1d5a52, emissiveIntensity: 1.2 }));
    exp2.position.set(0, 2.15, 0.08);
    exitG.add(exd, exp2); exitG.position.set(0, 0, RD / 2 - 0.08);
    sc.add(exitG); regI(exitG, 'exit', 'Back to the hallway', 'exit');

    const searchIds = [];
    if (!isLab) {
      const board = new THREE.Mesh(new THREE.BoxGeometry(4.6, 1.9, 0.1), M({ map: boardTex(['ATTENDANCE: 4 / 38', 'ASTRA TUTOR: OFFLINE', 'PLEASE WAIT.']), emissive: 0x123033, emissiveIntensity: .9 }));
      board.position.set(-2, 1.9, -RD / 2 + 0.1); sc.add(board);

      for (let r = 0; r < 3; r++) for (let c = 0; c < 3; c++) {
        const d = new THREE.Group();
        const t = new THREE.Mesh(new THREE.BoxGeometry(1.15, 0.08, 0.62), M({ color: 0x3d4c58, roughness: .8 }));
        t.position.y = 0.74; t.castShadow = true; t.receiveShadow = true; d.add(t);
        [[-0.5, -0.25], [0.5, -0.25], [-0.5, 0.25], [0.5, 0.25]].forEach(([x, z]) => {
          const lg = new THREE.Mesh(new THREE.CylinderGeometry(0.03, 0.03, 0.74, 6), M({ color: 0x6b7d8f, metalness: .7, roughness: .4 }));
          lg.position.set(x, 0.37, z); d.add(lg);
        });
        const ch = new THREE.Mesh(new THREE.BoxGeometry(0.5, 0.06, 0.45), M({ color: 0x1d3a44, roughness: .85 }));
        ch.position.set(0, 0.46, 0.75); ch.castShadow = true; d.add(ch);
        d.position.set(-3.6 + c * 2.6, 0, -1.6 + r * 2.1);
        sc.add(d); solid(d, -0.1);
        if (r === 1 && c === 1) { regI(d, 'hide_desk', 'Hide under the desk', 'hide'); hideSpots.push('hide_desk'); }
      }
      const cab = new THREE.Group();
      const cb = new THREE.Mesh(new THREE.BoxGeometry(1.6, 1.1, 0.55), M({ color: 0x27323d, metalness: .3, roughness: .7 }));
      cb.position.y = 0.55; cb.castShadow = true;
      const dr = new THREE.Mesh(new THREE.BoxGeometry(1.4, 0.3, 0.06), M({ color: 0x39485a, roughness: .6 }));
      dr.position.set(0, 0.72, 0.29);
      cab.add(cb, dr); cab.position.set(4.6, 0, -3.4);
      sc.add(cab); solid(cab); regI(cab, 'cabinet', 'Search the cabinet', 'search'); searchIds.push('cabinet');

      const lk = new THREE.Group();
      for (let i = 0; i < 3; i++) {
        const l = new THREE.Mesh(new THREE.BoxGeometry(0.8, 1.9, 0.5), M({ color: 0x223744, metalness: .5, roughness: .55 }));
        l.position.set(i * 0.84, 0.95, 0); l.castShadow = true; lk.add(l);
      }
      lk.position.set(-RW / 2 + 0.4, 0, 1.6); lk.rotation.y = Math.PI / 2;
      sc.add(lk); solid(lk); regI(lk, 'lockers', 'Search the lockers', 'search'); searchIds.push('lockers');

      const shelf = new THREE.Group();
      for (let i = 0; i < 3; i++) {
        const s = new THREE.Mesh(new THREE.BoxGeometry(2.4, 0.07, 0.4), M({ color: 0x3a4b3a, roughness: .9 }));
        s.position.set(0, 0.5 + i * 0.55, 0); shelf.add(s);
        for (let b = 0; b < 6; b++) {
          const bk = new THREE.Mesh(new THREE.BoxGeometry(0.13, 0.32, 0.28), M({ color: [0x7a3b3b, 0x2f5f7a, 0x6a6a2f, 0x4a3a6a][b % 4], roughness: .9 }));
          bk.position.set(-1.05 + b * 0.34, 0.7 + i * 0.55, 0); shelf.add(bk);
        }
      }
      shelf.position.set(4.4, 0, 2.6);
      sc.add(shelf); solid(shelf); regI(shelf, 'shelf', 'Search the shelf', 'search'); searchIds.push('shelf');
    } else {
      for (let i = 0; i < 3; i++) {
        const b = new THREE.Group();
        const t = new THREE.Mesh(new THREE.BoxGeometry(4.4, 0.1, 1.0), M({ color: 0x2b3a44, roughness: .55, metalness: .25 }));
        t.position.y = 0.92; t.castShadow = true; t.receiveShadow = true;
        const u = new THREE.Mesh(new THREE.BoxGeometry(4.2, 0.85, 0.85), M({ color: 0x1a2731, roughness: .8 }));
        u.position.y = 0.45;
        b.add(t, u); b.position.set(-1, 0, -2.6 + i * 2.4);
        sc.add(b); solid(b, -0.05);
        if (i === 1) { regI(b, 'hide_bench', 'Hide under the bench', 'hide'); hideSpots.push('hide_bench'); }
        for (let k = 0; k < 3; k++) {
          const bk = new THREE.Mesh(new THREE.CylinderGeometry(0.1, 0.14, 0.28, 10), M({ color: 0x9fdfe6, transparent: true, opacity: .5, emissive: 0x1c5a5a, emissiveIntensity: .8, roughness: .1 }));
          bk.position.set(-2.4 + k * 1.4 + i * 0.3, 1.11, -2.6 + i * 2.4); sc.add(bk);
        }
      }
      const rack = new THREE.Group();
      for (let i = 0; i < 2; i++) {
        const rk = new THREE.Mesh(new THREE.BoxGeometry(0.9, 2.1, 0.8), M({ color: 0x18242e, metalness: .6, roughness: .45 }));
        rk.position.set(i * 1.05, 1.05, 0); rk.castShadow = true; rack.add(rk);
        for (let j = 0; j < 8; j++) {
          const led = new THREE.Mesh(new THREE.BoxGeometry(0.06, 0.05, 0.02), M({ color: 0x061a16, emissive: j % 3 ? CY : AMBER, emissiveIntensity: 2.2 }));
          led.position.set(i * 1.05 - 0.3, 0.4 + j * 0.22, 0.41); rack.add(led);
        }
      }
      rack.position.set(RW / 2 - 1.3, 0, -3);
      sc.add(rack); solid(rack); regI(rack, 'rack', 'Search the server rack', 'search'); searchIds.push('rack');

      const hood = new THREE.Group();
      const hb = new THREE.Mesh(new THREE.BoxGeometry(2.2, 2.2, 0.9), M({ color: 0x223039, metalness: .4, roughness: .6 }));
      hb.position.y = 1.1; hb.castShadow = true;
      const hg = new THREE.Mesh(new THREE.PlaneGeometry(1.9, 0.9), M({ color: 0x0e2a30, emissive: 0x18707a, emissiveIntensity: .7, transparent: true, opacity: .75 }));
      hg.position.set(0, 1.5, 0.46);
      hood.add(hb, hg); hood.position.set(-RW / 2 + 1.4, 0, -2.4);
      sc.add(hood); solid(hood); regI(hood, 'hood', 'Search the fume hood', 'search'); searchIds.push('hood');

      const crate = new THREE.Mesh(new THREE.BoxGeometry(1.1, 0.9, 1.1), M({ color: 0x37414a, roughness: .9 }));
      crate.position.set(-4.2, 0.45, 3.0); crate.castShadow = true;
      sc.add(crate); solid(crate); regI(crate, 'crate', 'Search the crate', 'search'); searchIds.push('crate');

      const term = new THREE.Group();
      const ts = new THREE.Mesh(new THREE.PlaneGeometry(1.1, 0.7), M({ map: screenTex(['> ASTRA NODE', '> lab tutor: OFFLINE', '> nobody has logged a', '> real experiment in 14mo'], '#28e0c0'), emissive: 0x1c8a80, emissiveIntensity: 1.1 }));
      ts.position.set(0, 1.42, 0.03);
      const tb = new THREE.Mesh(new THREE.BoxGeometry(1.25, 0.85, 0.1), M({ color: 0x101a22, metalness: .5, roughness: .5 }));
      tb.position.set(0, 1.42, -0.03);
      term.add(tb, ts); term.position.set(3.4, 0, RD / 2 - 0.4);
      sc.add(term); regI(term, 'terminal', 'Read the terminal', 'read');
    }

    // hide spot 2: supply closet corner
    const closet = new THREE.Mesh(new THREE.BoxGeometry(1.2, 2.0, 1.0), M({ color: 0x1e2b36, metalness: .35, roughness: .65 }));
    closet.position.set(RW / 2 - 0.9, 1.0, 3.4); closet.castShadow = true;
    sc.add(closet); solid(closet); regI(closet, 'hide_closet', 'Hide in the supply closet', 'hide'); hideSpots.push('hide_closet');

    const robot = buildRobot();
    const rcone = coneMesh(9, 24, RED); robot.add(rcone);
    robot.visible = false; sc.add(robot);

    return {
      index: index, info: info, sc: sc, cam: cam, flash: flash,
      inter: inter, solids: solids, searchIds: searchIds, hideSpots: hideSpots,
      robot: robot, RW: RW, RD: RD,
      px: 0, pz: RD / 2 - 1.6, yaw: 0, pitch: -0.02, bob: 0,
      searched: {}, hidden: false, robotIn: false, robotT: 0, robotPath: 0, spawnAt: 9 + Math.random() * 9
    };
  }

  // ============================ input ============================
  const onKey = (e) => {
    const k = e.key.toLowerCase();
    if (['w', 'a', 's', 'd', 'shift', ' ', 'arrowleft', 'arrowright', 'arrowup', 'arrowdown'].includes(k)) e.preventDefault();
    keys[k] = e.type === 'keydown';
    if (e.type === 'keydown' && (k === 'e' || k === 'f' || k === 'enter')) api.interact();
  };
  window.addEventListener('keydown', onKey); window.addEventListener('keyup', onKey);

  let dragging = false, dragDist = 0, lastX = 0, lastY = 0;
  const onDown = (e) => { dragging = true; dragDist = 0; lastX = e.clientX; lastY = e.clientY; };
  const onUp = () => { dragging = false; };
  const onMove = (e) => {
    let dx, dy;
    if (document.pointerLockElement === renderer.domElement) { dx = e.movementX; dy = e.movementY; }
    else if (dragging) { dx = e.clientX - lastX; dy = e.clientY - lastY; lastX = e.clientX; lastY = e.clientY; dragDist += Math.abs(dx) + Math.abs(dy); }
    else return;
    if (mode === 'room' && room) {
      room.yaw -= dx * 0.0023;
      room.pitch = Math.max(-1.2, Math.min(1.2, room.pitch - dy * 0.0023));
    }
  };
  renderer.domElement.addEventListener('mousedown', onDown);
  window.addEventListener('mouseup', onUp);
  document.addEventListener('mousemove', onMove);
  renderer.domElement.addEventListener('click', () => { if (dragDist <= 8) api.interact(); });

  const ray = new THREE.Raycaster(); ray.far = 4.0;

  function setPrompt(p) { if (p !== prompt) { prompt = p; if (cb.onPrompt) cb.onPrompt(p); } }

  function hallCollide(x, z) {
    return Math.abs(z) > HW / 2 - 0.55 || Math.abs(x) > HL / 2 - 0.6;
  }

  // ============================ loop ============================
  let running = true, last = performance.now();
  function loop() {
    if (!running) return;
    requestAnimationFrame(loop);
    const now = performance.now();
    const dt = Math.min(0.05, (now - last) / 1000); last = now;
    if (caughtCool > 0) caughtCool -= dt;

    let fx = 0, fz = 0;
    if (keys['w']) fz += 1;
    if (keys['s']) fz -= 1;
    if (keys['a']) fx -= 1;
    if (keys['d']) fx += 1;
    if (mode === 'hall') {
      if (keys['arrowup']) fz += 1;
      if (keys['arrowdown']) fz -= 1;
      if (keys['arrowleft']) fx -= 1;
      if (keys['arrowright']) fx += 1;
    }
    const nrm = Math.hypot(fx, fz) || 1;

    if (mode === 'hall') stepHall(dt, fx / nrm, fz / nrm);
    else if (room) stepRoom(dt, fx / nrm, fz / nrm);

    renderer.render(mode === 'hall' ? hall : room.sc, mode === 'hall' ? hallCam : room.cam);
  }

  function stepHall(dt, fx, fz) {
    const run = keys['shift'] ? 8.4 : 5.2;
    const nx = hState.px + fx * run * dt, nz = hState.pz - fz * run * dt;
    if (!hallCollide(nx, hState.pz)) hState.px = nx;
    if (!hallCollide(hState.px, nz)) hState.pz = nz;
    if (fx || fz) hState.pyaw = Math.atan2(fx, -fz);
    hPlayer.position.set(hState.px, 0, hState.pz);
    hPlayer.rotation.y = hState.pyaw;

    const halfW = (hallCam.right - hallCam.left) / 2;
    const camX = Math.max(-HL / 2 + halfW, Math.min(HL / 2 - halfW, hState.px));
    hallCam.position.set(camX, 19, 7.2);
    hallCam.lookAt(camX, 0.9, -0.8);

    // robot patrol / chase
    const dxp = hState.px - hState.rx, dzp = hState.pz - hState.rz;
    const dist = Math.hypot(dxp, dzp);
    const facing = new THREE.Vector2(Math.sin(hState.ryaw), -Math.cos(hState.ryaw));
    const toP = new THREE.Vector2(dxp, dzp).normalize();
    const ang = Math.acos(Math.max(-1, Math.min(1, facing.dot(toP)))) * 180 / Math.PI;
    const sees = caughtCool <= 0 && dist < 11 && ang < 26;

    if (sees) hState.chase = true;
    else if (hState.chase && dist > 16) hState.chase = false;

    if (hState.chase) {
      const sp = 4.6 * dt;
      hState.rx += (dxp / (dist || 1)) * sp;
      hState.rz += (dzp / (dist || 1)) * sp;
      hState.ryaw = Math.atan2(dxp, -dzp);
      if (dist < 1.3 && caughtCool <= 0) {
        caughtCool = 4.5;
        hState.px = -HL / 2 + 3; hState.pz = 0;
        hState.chase = false;
        hState.rx = HL / 2 - 5; hState.rz = 0; hState.patrolDir = -1;
        if (cb.onCaught) cb.onCaught('hall');
      }
    } else {
      hState.rx += hState.patrolDir * 3.1 * dt;
      if (hState.rx < -HL / 2 + 14) { hState.patrolDir = 1; }
      if (hState.rx > HL / 2 - 4) { hState.patrolDir = -1; }
      hState.rz = Math.sin(performance.now() / 2600) * 2.4;
      hState.ryaw = hState.patrolDir > 0 ? Math.PI / 2 : -Math.PI / 2;
    }
    hRobot.position.set(hState.rx, 0, hState.rz);
    hRobot.rotation.y = hState.ryaw;
    hRobot.userData.eye.material.emissive.setHex(hState.chase ? 0xff3b3b : RED);
    hRobot.userData.lamp.intensity = hState.chase ? 6 : 3;
    hCone.material.color.setHex(hState.chase ? 0xff3b3b : RED);
    hCone.material.opacity = hState.chase ? 0.28 : 0.16;

    // nearest door
    let near = null, nd = 1.9;
    doors.forEach(d => {
      const side = Math.sign(d.position.z);
      const dx = Math.abs(d.position.x - hState.px);
      const onSide = side > 0 ? hState.pz > 0.4 : hState.pz < -0.4;
      d.userData.glow.material.opacity = 0.05;
      if (dx < nd && onSide) { nd = dx; near = d; }
    });
    if (near) {
      near.userData.glow.material.opacity = 0.2;
      hover = { type: 'door', index: near.userData.index };
      setPrompt('Enter ' + ROOMS[near.userData.index].name);
    } else { hover = null; setPrompt(null); }
  }

  function stepRoom(dt, fx, fz) {
    const r = room;
    const turn = 2.0 * dt;
    if (keys['arrowleft']) r.yaw += turn;
    if (keys['arrowright']) r.yaw -= turn;
    if (keys['arrowup']) r.pitch = Math.min(1.2, r.pitch + turn * 0.6);
    if (keys['arrowdown']) r.pitch = Math.max(-1.2, r.pitch - turn * 0.6);
    if (!r.hidden) {
      const run = keys['shift'] ? 3.9 : 2.4;
      const s = Math.sin(r.yaw), c = Math.cos(r.yaw);
      const dx = (-s * fz + c * fx) * run * dt;
      const dz = (-c * fz - s * fx) * run * dt;
      const test = (x, z) => {
        if (Math.abs(x) > r.RW / 2 - 0.45 || Math.abs(z) > r.RD / 2 - 0.45) return true;
        for (const b of r.solids) { if (b.max.y < 0.55) continue; if (x > b.min.x && x < b.max.x && z > b.min.z && z < b.max.z) return true; }
        return false;
      };
      if (!test(r.px + dx, r.pz)) r.px += dx;
      if (!test(r.px, r.pz + dz)) r.pz += dz;
      if (fx || fz) r.bob += dt * (keys['shift'] ? 11 : 7.5);
    }
    const eye = r.hidden ? 0.62 : 1.62 + Math.sin(r.bob) * 0.035;
    r.cam.position.set(r.px, eye, r.pz);
    r.cam.rotation.order = 'YXZ';
    r.cam.rotation.set(r.pitch, r.yaw, 0);
    r.flash.position.copy(r.cam.position);
    const fwd = new THREE.Vector3(0, 0, -1).applyQuaternion(r.cam.quaternion);
    r.flash.target.position.copy(r.cam.position).add(fwd.multiplyScalar(6));
    r.flash.target.updateMatrixWorld();
    r.flash.intensity = r.hidden ? 0 : 26;

    // robot entering the room
    r.robotT += dt;
    if (!r.robotIn && r.robotT > r.spawnAt) {
      r.robotIn = true; r.robot.visible = true; r.robotPath = 0;
      r.robot.position.set(0, 0, r.RD / 2 - 0.9);
      if (cb.onRobotEnter) cb.onRobotEnter();
    }
    if (r.robotIn) {
      r.robotPath += dt * 0.36;
      const t = r.robotPath;
      const rx = Math.sin(t * 1.6) * (r.RW / 2 - 2.2);
      const rz = (r.RD / 2 - 1.2) - t * 1.9;
      r.robot.position.set(rx, 0, Math.max(-r.RD / 2 + 1.2, rz));
      const dxp = r.px - r.robot.position.x, dzp = r.pz - r.robot.position.z;
      const d = Math.hypot(dxp, dzp);
      r.robot.rotation.y = Math.atan2(Math.cos(t * 1.6), -1) * 0.6 + Math.sin(t * 2.2) * 0.5;
      const f = new THREE.Vector2(Math.sin(r.robot.rotation.y), -Math.cos(r.robot.rotation.y));
      const tp = new THREE.Vector2(dxp, dzp).normalize();
      const ang = Math.acos(Math.max(-1, Math.min(1, f.dot(tp)))) * 180 / Math.PI;
      if (!r.hidden && d < 9 && ang < 26 && caughtCool <= 0) {
        caughtCool = 2.6;
        if (cb.onCaught) cb.onCaught('room');
        r.robotIn = false; r.robot.visible = false; r.robotT = 0; r.spawnAt = 14 + Math.random() * 8;
      }
      if (t > 5.5) { r.robotIn = false; r.robot.visible = false; r.robotT = 0; r.spawnAt = 12 + Math.random() * 10; if (cb.onRobotLeave) cb.onRobotLeave(); }
    }

    ray.setFromCamera({ x: 0, y: 0 }, r.cam);
    const hits = ray.intersectObjects(r.inter, true);
    let id = null;
    for (const h of hits) { if (h.object.userData.hitId) { id = h.object.userData.hitId; break; } }
    if (r.hidden) { hover = { type: 'unhide' }; setPrompt('Come out'); }
    else if (id) {
      const obj = r.inter.find(o => o.userData.iid === id);
      hover = { type: obj.userData.type, id: id };
      setPrompt(obj.userData.type === 'search' && r.searched[id] ? 'Already searched' : obj.userData.label);
    } else { hover = null; setPrompt(null); }
  }

  function resize() {
    const w = container.clientWidth || 1280, h = container.clientHeight || 720;
    renderer.setSize(w, h);
    const aspect = w / h;
    const vh = 9.2;
    hallCam.left = -vh * aspect / 2; hallCam.right = vh * aspect / 2;
    hallCam.top = vh / 2; hallCam.bottom = -vh / 2;
    hallCam.updateProjectionMatrix();
    if (room) { room.cam.aspect = aspect; room.cam.updateProjectionMatrix(); }
  }
  const ro = new ResizeObserver(resize); ro.observe(container); resize();
  loop();

  const api = {
    mode: () => mode,
    hover: () => hover,
    pos: () => (mode === 'hall' ? { x: hState.px, z: hState.pz } : { x: room.px, z: room.pz }),
    dbg(sec, fx, fz) {
      const n = Math.round(sec / 0.016);
      for (let i = 0; i < n; i++) { if (mode === 'hall') stepHall(0.016, fx || 0, fz || 0); else if (room) stepRoom(0.016, fx || 0, fz || 0); }
      return api.pos();
    },
    enterRoom(i) {
      room = makeRoom(i); mode = 'room'; resize();
      if (cb.onMode) cb.onMode('room', ROOMS[i], i);
    },
    exitRoom() {
      mode = 'hall'; room = null; hState.chase = false;
      if (cb.onMode) cb.onMode('hall', null, -1);
    },
    interact() {
      if (!hover) return;
      if (mode === 'hall' && hover.type === 'door') { api.enterRoom(hover.index); return; }
      if (mode !== 'room' || !room) return;
      if (hover.type === 'unhide') { room.hidden = false; if (cb.onHide) cb.onHide(false); return; }
      if (hover.type === 'exit') { api.exitRoom(); return; }
      if (hover.type === 'hide') { room.hidden = true; if (cb.onHide) cb.onHide(true); return; }
      if (hover.type === 'read') { if (cb.onRead) cb.onRead(room.index); return; }
      if (hover.type === 'search') {
        if (room.searched[hover.id]) return;
        room.searched[hover.id] = true;
        if (cb.onSearch) cb.onSearch(room.index, hover.id, room.searchIds.length, Object.keys(room.searched).length);
      }
    },
    roomProgress() { return room ? { total: room.searchIds.length, done: room.searchIds.filter(i => room.searched[i]).length } : null; },
    dispose() {
      running = false; ro.disconnect();
      window.removeEventListener('keydown', onKey); window.removeEventListener('keyup', onKey);
      window.removeEventListener('mouseup', onUp); document.removeEventListener('mousemove', onMove);
      renderer.dispose();
      if (renderer.domElement.parentNode) renderer.domElement.parentNode.removeChild(renderer.domElement);
    }
  };
  return api;
}
