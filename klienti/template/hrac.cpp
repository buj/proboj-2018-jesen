#include <bits/stdc++.h>
using namespace std;

#include "common.cpp"
#include "marshal.cpp"
#include "util.cpp"


int ja;                   // ktory hrac som ja, 0 alebo 1?
Stav stav;                // momentalny stav hry
vector<Prikaz> prikazy;   // zoznam prikazov v dane kolo


/** Vypytame si pociatocne informacie. */
void intro () {
  cout << "intro" << endl;
  cin >> ja;
  nacitajPociatok(stav);
}

/** Vypytame si najnovsi stav (ale novsi, ako dosial mame). */
void dalsiStav () {
  cout << "get " << stav.kolo << endl;
  nacitajStav(stav);
}

/** Odosle prikazy serveru. */
void odosliPrikazy () {
  cout << "commands " << prikazy.size();
  for (Prikaz& p : prikazy) {
    cout << " " << p;
  }
  cout << endl;
  prikazy.clear();
}

/** Pomocna metoda na skratenie kodu. */
void prikaz (Pozicia odkial, int typ, Pozicia kam) {
  prikazy.push_back(Prikaz(odkial, typ, kam));
}


void odohrajTah () {
  // pohneme sa s kazdou jednotkou nahodne
  // dokonca skusime pohnut aj superovimi jednotkami
  for (int id = 0; id < stav.pocetJednotiek(); id++) {
    Jednotka& x = stav.jednotkaCislo(id);
    if (x.mrtva()) {
      continue;
    }
    int smer = randint(0, 3);
    Pozicia ciel = x.poz + dpoz[smer];
    int p = randint(0, 1);
    prikaz(x.poz, p, ciel);
  }
}


int main () {
  intro();
  while (!stav.koniecHry) {
    loguj(0, "[Zacina kolo %d]", stav.kolo);
    odohrajTah();
    odosliPrikazy();
    dalsiStav();
  }
  return 0;
}
