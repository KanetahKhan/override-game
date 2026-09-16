"""FBX -> Override .mesh (offline). Merges every mesh, triangulates, bakes scale."""
import struct, sys
from fbx import parse

TARGET_H = 2.2   # world units, feet to head

def geoms(path):
    ver, roots = parse(path)
    objs = [n for n in roots if n[0]=='Objects'][0][2]
    for name, props, kids in objs:
        if name != 'Geometry': continue
        sub = {k[0]: k for k in kids}
        def arr(n, key='Vertices'):
            return sub[n][1][0]
        layers = {}
        for key in ('LayerElementNormal','LayerElementUV'):
            if key not in sub: continue
            layers[key] = {c[0]: (c[1][0] if c[1] else None) for c in sub[key][2]}
        yield arr('Vertices'), arr('PolygonVertexIndex'), layers

def build(path):
    P, T, N, F = [], [], [], []      # points, texcoords, normals, faces
    for verts, pvi, layers in geoms(path):
        nrm = layers['LayerElementNormal']; uv = layers['LayerElementUV']
        nvals, nidx = nrm['Normals'], nrm['NormalsIndex']
        uvals, uidx = uv['UV'], uv['UVIndex']
        poly = []
        for corner, raw in enumerate(pvi):
            vi = ~raw if raw < 0 else raw
            poly.append((vi, corner))
            if raw >= 0: continue
            base = len(P)//3
            for vi2, c2 in poly:
                P.extend(verts[vi2*3:vi2*3+3])
                ni = nidx[c2]; N.extend(nvals[ni*3:ni*3+3])
                ui = uidx[c2]; u, v = uvals[ui*2], uvals[ui*2+1]
                T.extend((u, 1.0 - v))            # FBX V is bottom-up
            for k in range(1, len(poly)-1):       # fan triangulation
                a, b, c = base, base+k, base+k+1
                F.extend((a,a,a, b,b,b, c,c,c))   # point, normal, texcoord
            poly = []
    return P, T, N, F

def main(src, out):
    P, T, N, F = build(src)
    lo = [min(P[i::3]) for i in range(3)]
    hi = [max(P[i::3]) for i in range(3)]
    h = hi[1] - lo[1]
    s = TARGET_H / h
    cx, cz = (lo[0]+hi[0])/2, (lo[2]+hi[2])/2
    for i in range(0, len(P), 3):                  # centre on X/Z, feet to y=0, scale
        P[i]   = (P[i] - cx) * s
        P[i+1] = (P[i+1] - lo[1]) * s
        P[i+2] = (P[i+2] - cz) * s
    # The exporter writes normals at 0.01 length; JavaFX lights by the raw dot
    # product, so anything but unit length silently darkens the whole model.
    import math
    for i in range(0, len(N), 3):
        l = math.sqrt(N[i]*N[i] + N[i+1]*N[i+1] + N[i+2]*N[i+2])
        if l > 1e-12:
            N[i] /= l; N[i+1] /= l; N[i+2] /= l
        else:
            N[i], N[i+1], N[i+2] = 0.0, 1.0, 0.0
    print(f"tris={len(F)//9}  points={len(P)//3}  raw height={h:.1f} -> {TARGET_H}  scale={s:.6g}")
    print(f"scaled bbox X {min(P[0::3]):.2f}..{max(P[0::3]):.2f}  "
          f"Y {min(P[1::3]):.2f}..{max(P[1::3]):.2f}  Z {min(P[2::3]):.2f}..{max(P[2::3]):.2f}")
    with open(out,'wb') as f:
        f.write(b'OVMESH01')
        f.write(struct.pack('>III', len(P)//3, len(T)//2, len(F)//9))
        f.write(struct.pack('>%df'%len(P), *P))
        f.write(struct.pack('>%df'%len(N), *N))
        f.write(struct.pack('>%df'%len(T), *T))
        f.write(struct.pack('>%di'%len(F), *F))
    import os; print("wrote", out, round(os.path.getsize(out)/1048576,2), "MB")
    return P

if __name__ == '__main__':
    main(sys.argv[1], sys.argv[2])
