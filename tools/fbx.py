import struct, zlib

class R:
    def __init__(s, d): s.d=d; s.p=0
    def u(s,n,f):
        v=struct.unpack_from(f,s.d,s.p)[0]; s.p+=n; return v

def parse(path):
    d=open(path,'rb').read()
    ver=struct.unpack_from('<I',d,23)[0]
    big = ver>=7500
    r=R(d); r.p=27
    def node():
        if big: end,np,pl = r.u(8,'<Q'),r.u(8,'<Q'),r.u(8,'<Q')
        else:   end,np,pl = r.u(4,'<I'),r.u(4,'<I'),r.u(4,'<I')
        nl=r.u(1,'<B')
        if end==0: return None
        name=d[r.p:r.p+nl].decode('utf-8','replace'); r.p+=nl
        props=[]
        for _ in range(np): props.append(prop())
        kids=[]
        while r.p < end:
            k=node()
            if k is None: break
            kids.append(k)
        r.p=end
        return (name,props,kids)
    def prop():
        t=chr(d[r.p]); r.p+=1
        if t=='Y': return r.u(2,'<h')
        if t=='C': return bool(r.u(1,'<B'))
        if t=='I': return r.u(4,'<i')
        if t=='F': return r.u(4,'<f')
        if t=='D': return r.u(8,'<d')
        if t=='L': return r.u(8,'<q')
        if t in 'fdlib':
            n=r.u(4,'<I'); enc=r.u(4,'<I'); cl=r.u(4,'<I')
            raw=d[r.p:r.p+cl]; r.p+=cl
            if enc==1: raw=zlib.decompress(raw)
            fmt={'f':'f','d':'d','l':'q','i':'i','b':'b'}[t]
            return list(struct.unpack('<%d%s'%(n,fmt), raw))
        if t in 'SR':
            n=r.u(4,'<I'); v=d[r.p:r.p+n]; r.p+=n
            return v.decode('utf-8','replace') if t=='S' else v
        raise ValueError('prop type %r at %d'%(t,r.p))
    roots=[]
    while True:
        n=node()
        if n is None: break
        roots.append(n)
        if r.p>=len(d)-160: break
    return ver, roots

def walk(nodes, depth=0, maxd=2):
    for name,props,kids in nodes:
        desc=[]
        for p in props[:3]:
            desc.append(('[%d nums]'%len(p)) if isinstance(p,list) else repr(p)[:40])
        print('  '*depth + name + '  ' + ', '.join(desc))
        if depth<maxd: walk(kids, depth+1, maxd)
