#!/usr/bin/python3

import random
import sys
from common import Pozicia, Prikaz, Teren, Stav, read_empty_line
from logger import logger
# Utility

f_in = sys.stdin

f_out = sys.stdout


# global variables
ja = None
stav = None
teren = None

def write_to_server(*args):
    print(*args, file=f_out)
    f_out.flush()

def intro():
    write_to_server("intro")
    global ja, teren, stav
    ja = int(f_in.readline().strip())
    logger("get teren")
    teren = Teren.fromFile(f_in)
    read_empty_line(f_in)
    stav = Stav.fromFile(f_in)


def dalsi_stav():
    global stav
    logger('dalsi stav')
    write_to_server("get %d" % stav.kolo)
    read_empty_line(f_in)
    stav = Stav.fromFile(f_in)


def odosli_prikazy(prikazy):
    logger('odosli prikazy')
    prikazy = [p.serialize() for p in prikazy]
    write_to_server("commands %s" % (
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
    logger("Init game")
    intro()
    while (not stav.koniecHry):
        logger("[Zacina kolo %d]" % stav.kolo)
        prikazy = odohraj_tah()
        odosli_prikazy(prikazy)
        dalsi_stav()


if __name__ == "__main__":
    logger("calling main")
    main()
