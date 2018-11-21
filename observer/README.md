# Observer

Observer je nakodeny v takmer pure JavaScripte. Cely kod je zabaleny do wrappera
**Electron**, ktory simuluje web browser a navonok sa tvari ako normalna appka.
Electron je multiplatformovy, a teda observer sa da zbuldil na vsetky platformy...

## Rozbehanie

Na rozbehanie elektronu v development verzii treba:
1) Mat nainstalovany Node a npm
2) Nainstalovat dependencies `npm i` v root adresari projektu
3) Na spustenie observera `npm run start`

## Buildovanie pre linux/windows

Staci pouzit command `npm run pack:linux` resp. `npm run pack:win`.
Buildovanie pre vasu platformu by malo byt lahke, pri inych bude treba
instalovat dalsie deps *(napr. Wine pre buildenie pre Windows na Linuxe)*.
