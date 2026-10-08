#!/usr/bin/env python3
"""Small standard-library reader for Java Edition NBT (raw or gzip)."""
import gzip, io, json, struct

class Reader:
    def __init__(self, data): self.data, self.i = data, 0
    def take(self, n):
        j=self.i+n
        if j > len(self.data): raise EOFError("truncated NBT")
        b=self.data[self.i:j]; self.i=j
        return b
    def num(self, fmt):
        s='>'+fmt; v=struct.unpack_from(s,self.data,self.i)[0]; self.i+=struct.calcsize(s); return v
    def string(self): return self.take(self.num('H')).decode('utf-8', 'replace')
    def payload(self, t):
        if t == 0: return None
        if t == 1: return self.num('b')
        if t == 2: return self.num('h')
        if t == 3: return self.num('i')
        if t == 4: return self.num('q')
        if t == 5: return self.num('f')
        if t == 6: return self.num('d')
        if t == 7:
            n=self.num('i'); return list(self.take(n))
        if t == 8: return self.string()
        if t == 9:
            subtype, n = self.num('b'), self.num('i')
            return [self.payload(subtype) for _ in range(n)]
        if t == 10:
            out = {}
            while True:
                st = self.num('b')
                if st == 0: return out
                key = self.string()
                out[key] = self.payload(st)
        if t == 11:
            n=self.num('i'); out=struct.unpack_from('>'+str(n)+'i',self.data,self.i) if n else (); self.i+=n*4; return list(out)
        if t == 12:
            n=self.num('i'); out=struct.unpack_from('>'+str(n)+'q',self.data,self.i) if n else (); self.i+=n*8; return list(out)
        raise ValueError('unknown NBT tag '+str(t))
    def root(self):
        typ = self.num('b')
        if typ != 10: raise ValueError('root is not compound')
        name = self.string()
        return name, self.payload(10)

def load(path):
    with open(path,'rb') as f: data=f.read()
    if data[:2] == b'\x1f\x8b': data=gzip.decompress(data)
    return Reader(data).root()

def main():
    import argparse
    p=argparse.ArgumentParser(); p.add_argument('file'); a=p.parse_args()
    name, root=load(a.file); print(json.dumps({'name':name,'root':root},ensure_ascii=False,indent=2))
if __name__=='__main__': main()
