# -*- coding: utf-8 -*-
import os, sys
sys.path.insert(0, os.path.dirname(os.path.abspath(__file__)))
from gl_min2 import new_deck
import gl_min2 as A
import gl_min2b as B

OUT = os.path.normpath(os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "Gully_Labs_Strategy_Deck.pptx"))
BUILDERS = [A.s01, A.s02, A.s03, A.s04, A.s05, A.s06, B.s07, B.s08, B.s09, B.s10, B.s11, B.s12, B.s13, B.s14, B.s15]

prs = new_deck()
for b in BUILDERS:
    b(prs)
prs.save(OUT)
print("slides:", len(prs.slides._sldIdLst), "->", OUT)
