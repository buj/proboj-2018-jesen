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
  cout << "intro\n";
  cout.flush();
  cin >> ja;
  nacitajPociatok(stav);
}

/** Vypytame si najnovsi stav (ale novsi, ako dosial mame). */
void dalsiStav () {
  cout << "get " << stav.kolo << "\n";
  cout.flush();
  timeStamp();
  cerr << "citam stav...\n";
  nacitajStav(stav);
  timeStamp();
  cerr << "nacital som stav " << stav.kolo << "\n";
}

/** Pomocna metoda na skratenie kodu. */
void prikaz (Pozicia odkial, int typ, Pozicia kam) {
  prikazy.push_back(Prikaz(odkial, typ, kam));
}


void tah () {
  // pohneme sa s kazdou jednotkou nahodne
  // dokonca skusime pohnut aj superovimi jednotkami
  cerr << "Ziju a vidim tieto jednotky:";
  for (int id = 0; id < stav.pocetJednotiek(); id++) {
    Jednotka& x = stav.jednotkaCislo(id);
    if (x.mrtva()) {
      continue;
    }
    cerr << " " << id;
    int smer = randint(0, 3);
    Pozicia ciel = x.poz + dpoz[smer];
    int p = randint(0, 1);
    prikaz(x.poz, p, ciel);
  }
  cerr << ";\n";
}


int main () {
  intro();
  while (!stav.koniecHry) {
    loguj(0, "%d", stav.kolo);
    /*
    tah();
    for (Prikaz& p : prikazy) {
      cout << "command " << p << "\n";
    }
    cerr << "Poslal som " << prikazy.size() << " prikazov!\n\n";
    prikazy.clear();
    cout.flush();
    */
    dalsiStav();
  }
  return 0;
}
