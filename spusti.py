import random
import sys, subprocess

if len(sys.argv) < 4:
  print("expected 4 arguments: client 1, client 2 and map file")
  exit(0)

seed = random.randint(0, 1023456789)
klient1 = sys.argv[1]
klient2 = sys.argv[2]
mapa = sys.argv[3]

server = subprocess.Popen("java -jar server/server.jar --map={} --seed={}".format(mapa, seed), shell = True)
subprocess.Popen("python3 klienti/client.py klienti/{} 0 --log=0.log".format(klient1), shell = True)
subprocess.Popen("python3 klienti/client.py klienti/{} 1 --log=1.log".format(klient2), shell = True)

server.wait()
