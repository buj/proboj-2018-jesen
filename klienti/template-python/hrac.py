#!/usr/bin/python3

import random
import sys
from common import Pozicia, Prikaz, Teren, Stav
from logger import logger
# Utility

f_in = sys.stdin

f_out = sys.stdout


# global variables
ja = None
stav = None
teren = None


def intro():
    print("intro")
    global ja, teren, stav
    ja = int(f_in.readline().strip())
    teren = Teren.fromFile(f_in)
    stav = Stav.fromFile(f_in)


def dalsi_stav():
    global stav
    print("get %d" % stav.kolo)
    stav = Stav.fromFile(f_in)


def odosli_prikazy(prikazy):
    prikazy = [p.serialize() for p in prikazy]
    print("commands %s" % (
        " ".join([str(len(prikazy))] + prikazy)
    ))


def odohraj_tah():
    prikazy = []
    for x in stav.jednotky:
        if x.mrtva():
            continue
        pohyb = random.choice([
            Pozicia(0, 1),
            Pozicia(0, -1),
            Pozicia(1, 0),
            Pozicia(-1, 0),
        ])
        ciel = x.pozicia + pohyb
        akcia = random.choice([
            Prikaz.POHYB,
            Prikaz.UTOK
        ])

        prikazy.append(
            Prikaz(x, akcia, ciel)
        )

    return prikazy


def main():
    intro()
    while (not stav.koniecHry):
        logger("[Zacina kolo %d]" % stav.kolo)
        prikazy = odohraj_tah()
        odosli_prikazy(prikazy)
        dalsi_stav()


if __name__ == "__main__":
    main()
