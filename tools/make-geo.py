#!/usr/bin/env python3
"""Erzeugt app/src/main/assets/geo.txt aus der Weltkarte (assets/world.txt, Natural Earth):
Mittelpunkt (Breite, Länge) und Nachbarländer je Land – für die Erfolge „Nachbarschaft“, „Antipode“ und
„Alle Himmelsrichtungen“. Nachbarn = Länder mit gemeinsamen Grenzpunkten. Aufruf: python3 tools/make-geo.py"""
import os
ROOT = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
ASSETS = os.path.join(ROOT, 'app', 'src', 'main', 'assets')
shapes = {}
for line in open(os.path.join(ASSETS, 'world.txt'), encoding='utf-8'):
    line = line.strip()
    if not line or line.startswith('#'):
        continue
    cc, rest = line.split('|')
    shapes[cc] = [[tuple(map(int, p.split(','))) for p in ring.split()] for ring in rest.split(';')]

def centroid(rings):
    # Flächenschwerpunkt des größten Rings (Hauptland, ohne ferne Inseln)
    best = None
    for r in rings:
        a = cx = cy = 0.0
        for (x1, y1), (x2, y2) in zip(r, r[1:] + r[:1]):
            f = x1 * y2 - x2 * y1
            a += f; cx += (x1 + x2) * f; cy += (y1 + y2) * f
        if a and (best is None or abs(a) > best[0]):
            best = (abs(a), cx / (3 * a), cy / (3 * a))
    if best is None:
        x, y = rings[0][0]
        return 85 - y / 10, x / 10 - 180
    return 85 - best[2] / 10, best[1] / 10 - 180

points = {cc: {p for r in rings for p in r} for cc, rings in shapes.items()}
owner = {}
for cc, ps in points.items():
    for p in ps:
        owner.setdefault(p, set()).add(cc)
neighbors = {cc: set() for cc in shapes}
for p, ccs in owner.items():
    if len(ccs) > 1:
        for c in ccs:
            neighbors[c] |= ccs - {c}
out = ['# Mittelpunkt und Nachbarländer je Land, erzeugt von tools/make-geo.py aus world.txt (Natural Earth, gemeinfrei).',
       '# Zeile: ISO-Code|Breite|Länge|Nachbar,Nachbar,…']
for cc in sorted(shapes):
    lat, lon = centroid(shapes[cc])
    out.append(f'{cc}|{lat:.1f}|{lon:.1f}|{",".join(sorted(neighbors[cc]))}')
open(os.path.join(ASSETS, 'geo.txt'), 'w', encoding='utf-8').write('\n'.join(out) + '\n')
print(len(shapes), 'Länder')
