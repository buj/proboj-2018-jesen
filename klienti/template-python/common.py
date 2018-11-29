from collections import namedtuple


def read_empty_line(f):
    line = f.readline().strip()
    assert(not line)


def read_ints(f):
    line = f.readline().strip()
    assert line
    return list(map(int, line.split()))


def is_position(pos):
    return len(pos) == 2


class Pozicia:
    def __init__(self, x, y):
        self.x = x
        self.y = y

    def __add__(self, other):
        return Pozicia(
            self.x + other.x,
            self.y + other.y,
        )

    def serialize(self):
        return "%d %d" % (self.x, self.y)

    def __str__(self):
        return "(%d,%d)" % (self.x, self.y)

    def __repr__(self):
        return str(self)


class Jednotka:
    BOJOVNIK = 0
    LUKOSTRELEC = 1

    MAX_ZIVOTY = 100
    MAX_ENERGIA = 100

    def __init__(self, x, y, id, hrac, typ, zivoty, energia):
        self.id = id
        self.pozicia = Pozicia(x, y)
        assert(hrac in [0, 1])
        assert(typ in [Jednotka.BOJOVNIK, Jednotka.LUKOSTRELEC])

        self.hrac = hrac
        self.typ = typ
        self.zivoty = zivoty
        self.energia = energia

    def mrtva(self):
        return self.zivoty <= 0

    def ziva(self):
        return self.zivoty()

    @classmethod
    def fromFile(cls, f):
        return cls(*read_ints(f))


class Prikaz:
    POHYB = 0
    UTOK = 1

    def __init__(self, jednotka, akcia, ciel):
        assert(akcia in [Prikaz.POHYB, Prikaz.UTOK])
        assert(isinstance(jednotka, Jednotka))
        assert(isinstance(ciel, Pozicia))
        self.pozicia = jednotka.pozicia
        self.akcia = akcia
        self.ciel = ciel

    def serialize(self):
        return "%s %d %s" % (
            self.pozicia.serialize(),
            self.akcia,
            self.ciel.serialize(),
        )


CellInfo = namedtuple('CellInfo', ['typ', 'vyska', 'vidim'])


class Teren:
    def __init__(self, typ, vyska, vidim):
        self.typ = typ
        self.vyska = vyska
        self.vidim = vidim

    @property
    def r(self):
        return len(self.vyska)

    @property
    def s(self):
        return len(self.vyska[0])

    def cellAt(self, x, y):
        typ = self.typ[x][y]
        vyska = self.vyska[x][y]
        vidim = self.vidim[x][y]
        return CellInfo(typ, vyska, vidim)

    @classmethod
    def fromFile(cls, f):
        n, m = read_ints(f)
        read_empty_line(f)

        typ = []
        for i in range(n):
            typ.append(read_ints(f))

        read_empty_line(f)
        vysky = []
        for i in range(n):
            vysky.append(read_ints(f))

        read_empty_line(f)
        vidim = []
        for i in range(n):
            row = []
            for j in range(m):
                tmp = read_ints(f)
                assert(tmp[0]*2 + 1 == len(tmp))
                c = tmp[0]
                tmp = tmp[1:]  # skip len

                row.append([
                    Pozicia(tmp[k*2], tmp[k*2 + 1]) for k in range(c)
                ])
            vidim.append(row)
        return cls(typ, vysky, vidim)


class Stav:
    def __init__(self, kolo, skore, koniecHry, jednotky):
        self.kolo = kolo
        self.skore = skore
        self.koniecHry = koniecHry
        self.jednotky = jednotky

    @staticmethod
    def nacitaj_mapu_jednotiek(f):
        (n, ) = read_ints(f)
        jednotky = []
        for i in range(n):
            jednotky.append(Jednotka.fromFile(f))
        return jednotky

    @classmethod
    def fromFile(cls, f):
        kolo, skore, koniecHry = read_ints(f)
        jednotky = cls.nacitaj_mapu_jednotiek(f)
        return cls(kolo, skore, koniecHry, jednotky)
