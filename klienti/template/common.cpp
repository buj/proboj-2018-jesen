#pragma once

#include <vector>
#include <iostream>
using namespace std;


#define ROVINA 0
#define LES 1
#define VODA 2
#define SENTINEL 3
#define CIEL 4


#define BOJOVNIK 0
#define LUKOSTRELEC 1

// statistiky jednotlivych typov jednotiek
const int kUtok[2] = {8, 7};
const int kObrana[2] = {8, 0};

#define MAX_ZIVOTY 100
#define MAX_ENERGIA 100


#define OBRANCA 0
#define UTOCNIK 1

#define PRIKAZ_POHYB 0
#define PRIKAZ_UTOK 1

#define ZIADNA_JEDNOTKA Jednotka()
#define NIC -1


/** Zmeny v riadku/stlpci, ked ideme hore, doprava, dole, dolava. */
int dr[4] = {0, 1, 0, -1};
int ds[4] = {1, 0, -1, 0};

struct Pozicia {
  int r, s;
  
  Pozicia () : r(0), s(0) {}
  Pozicia (int r0, int s0) : r(r0), s(s0) {}
  
  Pozicia operator+ (const Pozicia B) const {
    return Pozicia(r + B.r, s + B.s);
  }
  Pozicia operator- (const Pozicia B) const {
    return Pozicia(r - B.r, s - B.s);
  }
  Pozicia operator* (const int k) const {
    return Pozicia(k*r, k*s);
  }
};

Pozicia operator* (const int k, const Pozicia A) {
  return Pozicia(k*A.r, k*A.r);
}

/** Zmeny v pozicii: hore, doprava, dole, dolava. */
Pozicia dpoz[4] = {Pozicia(0, 1), Pozicia(1, 0), Pozicia(0, -1), Pozicia(-1, 0)};



struct Teren {
  int r, s; // pocet riadkov, pocet stlpcov
  vector<vector<int> > _typ, _vyska;
  vector<vector<vector<Pozicia> > > _vidimZ;
  
  bool vitaznyRiadok (int i) {
    return i == r;
  }
  bool vnutriMapy (int i, int j) {
    return (i >= 0) && (i < r) && (j >= 0) && (j < s);
  }
  bool vnutriMapy (Pozicia poz) {
    return vnutriMapy(poz.r, poz.s);
  }
  
  /** Vrati typ na policku [i, j]. Nema problem, ked to je mimo mapy.
   * Nepouzivat _typ, ale toto. */
  int typ (int i, int j) {
    if (vitaznyRiadok(i)) {
      return CIEL;
    }
    if (!vnutriMapy(i, j)) {
      return SENTINEL;
    }
    return _typ[i][j];
  }
  int typ (Pozicia poz) {
    return typ(poz.r, poz.s);
  }
  
  /** Vrati vysku na policku [i, j]. Ak to je mimo mapy, vrati 0.
   * Nepouzivat _vyska, ale toto. */
  int vyska (int i, int j) {
    if (!vnutriMapy(i, j)) {
      return 0;
    }
    return _vyska[i][j];
  }
  int vyska (Pozicia poz) {
    return vyska(poz.r, poz.s);
  }
  
  /** Vrati zoznam policok, ktore vidno z [i, j]. */
  vector<Pozicia> vidimZ (int i, int j) {
    if (!vnutriMapy(i, j)) {
      return vector<Pozicia>();
    }
    return _vidimZ[i][j];
  }
};



struct Jednotka {
  int id, hrac, typ;
  int zivoty, energia;
  Pozicia poz;
  
  Jednotka () : id(-1), hrac(-1), zivoty(0) {}
  Jednotka (int id0, int hrac0, int typ0, int zivoty0 = MAX_ZIVOTY, int energia0 = MAX_ENERGIA)
    : id(id0), hrac(hrac0), typ(typ0), zivoty(zivoty0), energia(energia0) {}
  
  int zmenZivoty (int zmena) {
    zivoty += zmena;
    zivoty = min(MAX_ZIVOTY, max(0, zivoty));
    return zivoty;
  }
  double percentoZivotov () const {
    return (double)zivoty / MAX_ZIVOTY;
  }
  bool mrtva () {
    return zivoty <= 0;
  }
  bool ziva () {
    return !mrtva();
  }
  
  int zmenEnergiu (int zmena) {
    energia += zmena;
    energia = min(MAX_ENERGIA, max(0, energia));
    return energia;
  }
  double percentoEnergie () const {
    return (double)energia / MAX_ENERGIA;
  }
  
  double utok () {
    double hp = percentoZivotov();
    double sp = percentoEnergie();
    return (double)kUtok[typ] * hp * (1. + sp) / 2.;
  }
  double obrana () {
    double hp = percentoZivotov();
    double sp = percentoEnergie();
    return (double)kObrana[typ] * hp * (1. + sp) / 2.;
  }
};



struct MapaJednotiek {
  int r, s;
  vector<Jednotka> _jednotky; // neclearovat, nepushovat! vraciame totiz referencie
  vector<vector<int> > _id;
  
  Jednotka mojaZiadna = ZIADNA_JEDNOTKA; // ked sme mimo, vraciame referenciu na nu
  
  /** Na zaciatku (po nacitani Terenu) tymto inicializujeme MapuTerenu. */
  void inicializuj (int r0, int s0) {
    r = r0;
    s = s0;
    _id.resize(r, vector<int>(s, NIC));
  }
  
  bool vnutriMapy (int i, int j) const {
    return (i >= 0) && (i < r) && (j >= 0) && (j < s);
  }
  
  void prislaJednotka (Jednotka x) {
    if (x.id >= (int)_jednotky.size()) {
      _jednotky.resize(x.id + 1, ZIADNA_JEDNOTKA);
    }
    _jednotky[x.id] = x;
    _id[x.poz.r][x.poz.s] = x.id;
  }
  
  /** Vrati pociatocny pocet jednotiek. */
  int pocetJednotiek () const {
    return (int)_jednotky.size();
  }
  
  /** Vrati jednotku nachadzajucu sa na policku [i, j]. Ak tam nie je
   * ziadna jednotka, vrati ZIADNA_JEDNOTKA. */
  Jednotka& jednotkaNa (int i, int j) {
    if (!vnutriMapy(i, j)) {
      return mojaZiadna;
    }
    int id = _id[i][j];
    if (id == NIC) {
      return mojaZiadna;
    }
    return _jednotky[id];
  }
  Jednotka& jednotkaNa (Pozicia poz) {
    return jednotkaNa(poz.r, poz.s);
  }
  
  /** Vrati jednotku s danym cislom (id). Ak taka nie je, vrati
   * ZIADNA_JEDNOTKA. */
  Jednotka& jednotkaCislo (int id) {
    if (id < 0 || id > (int)_jednotky.size()) {
      return mojaZiadna;
    }
    return _jednotky[id];
  }
  
  /** Vyprazdni tuto mapu, napr. aby sme do nej mohli nacitat nove data. */
  void vyprazdni () {
    fill(_jednotky.begin(), _jednotky.end(), ZIADNA_JEDNOTKA);
    for (int i = 0; i < r; i++) {
      for (int j = 0; j < s; j++) {
        _id[i][j] = NIC;
      }
    }
  }
};



/** Otazky k multiple inheritance? He he he... */
struct Stav : MapaJednotiek, Teren {
  int kolo, skore;
  bool koniecHry;
};



/** Prikaz jednotke na pozicii <odkial> nech spravi akciu <typ>
 * na poziciu <kam>. */
struct Prikaz {
  Pozicia odkial, kam;
  int typ;
  
  Prikaz (Pozicia od0, int typ0, Pozicia kam0) :
    odkial(od0), typ(typ0), kam(kam0) {}
};
