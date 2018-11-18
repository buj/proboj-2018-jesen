#pragma once

#include <iostream>
using namespace std;

#include "common.cpp"


/** Nacita zo standardneho vstupu dve cisla reprezentujuce poziciu,
 * a vrati vyslednu poziciu. */
Pozicia nacitajPoziciu (istream& in = cin) {
  int r, s;
  in >> r >> s;
  return Pozicia(r, s);
}


/** Nacita zo standardneho vstupu mapu. */
void nacitajTeren (Teren& teren, istream& in = cin) {
  in >> teren.r >> teren.s;
  
  // nacitaj typ
  teren._typ.resize(teren.r, vector<int>(teren.s));
  for (int i = 0; i < teren.r; i++) {
    for (int j = 0; j < teren.s; j++) {
      in >> teren._typ[i][j];
    }
  }
  // nacitaj vysky
  teren._vyska.resize(teren.r, vector<int>(teren.s));
  for (int i = 0; i < teren.r; i++) {
    for (int j = 0; j < teren.s; j++) {
      in >> teren._vyska[i][j];
    }
  }
  // nacitaj, co vidim z jednotlivych policok
  teren._vidimZ.resize(teren.r, vector<vector<Pozicia> >(teren.s));
  for (int i = 0; i < teren.r; i++) {
    for (int j = 0; j < teren.s; j++) {
      int n;
      in >> n;
      teren._vidimZ[i][j].resize(n);
      for (int k = 0; k < n; k++) {
        teren._vidimZ[i][j][k] = nacitajPoziciu();
      }
    }
  }
}


/** Zo standardneho vstupu nacita jednotku a vrati ju. */
Jednotka nacitajJednotku (istream& in = cin) {
  Jednotka x;
  x.poz = nacitajPoziciu(in);
  in >> x.id >> x.hrac >> x.typ >> x.zivoty >> x.energia;
  return x;
}


/** Zo vstupu nacita mapu jednotiek. Predpokladame, ze uz pozname
 * jej rozmery (boli inicializovane na zaciatku podla Terenu). */
void nacitajMapuJednotiek (MapaJednotiek& mapa, istream& in = cin) {
  mapa.vyprazdni();
  int n;
  cin >> n;
  for (int i = 0; i < n; i++) {
    mapa.prislaJednotka(nacitajJednotku(in));
  }
}


/** Nacitame stav hry. */
void nacitajStav (Stav& stav, istream& in = cin) {
  in >> stav.kolo >> stav.skore >> stav.koniecHry;
  nacitajMapuJednotiek(stav, in);
}

/** Nacitame pociatocny stav hry. */
void nacitajPociatok (Stav& stav, istream& in = cin) {
  nacitajTeren(stav, in);
  stav.inicializuj(stav.Teren::r, stav.Teren::s);
  nacitajStav(stav, in);
}


ostream& operator<< (ostream& out, Pozicia poz) {
  out << poz.r << " " << poz.s;
  return out;
}

ostream& operator<< (ostream& out, Prikaz p) {
  out << p.odkial << " " << p.typ << " " << p.kam;
  return out;
}
