#pragma once

#include <iostream>
#include <random>
using namespace std;


/** Definujeme si prijemne logovanie. Cislo na zaciatku hovori, ze chceme
 * mat spravu odsadenu <lv> medzerami. */
#define loguj(lv, args...)                    \
  for (int i = 0; i < lv; i++) cerr << " ";   \
  fprintf(stderr, args);                      \
  cerr << "\n";


const int seed = 1023456789;
mt19937 mt(seed);

/** Nahodny vyber celeho cisla z [l, r] (teda vratane oboch koncov). */
int randint (int l, int r) {
  uniform_int_distribution<int> distrib(l, r);
  return distrib(mt);
}

/** Nahodne realne cislo z intervalu [l, r). */
double randFrom (double l, double r) {
  uniform_real_distribution<double> distrib(l, r);
  return distrib(mt);
}

/** S pravdepdobnostou <p> dostaneme 1, a inak 0. */
bool coin (double p = 0.5) {
  return randFrom(0., 1.) < p;
}
