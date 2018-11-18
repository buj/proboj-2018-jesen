import argparse, subprocess, socket, traceback, sys


def truncate(filename):
  """Creates an empty file with the given name. If it exists, it is
  truncated to size 0."""
  open(filename, 'w').close()


parser = argparse.ArgumentParser()
parser.add_argument("exec_path", help = "the path to the executable client")
parser.add_argument("player_id", help = "the player ID (what side the client should play)")
parser.add_argument("--host", help = "the Inet address of the game host", default = "127.0.0.1")
parser.add_argument("--port", type = int, help = "the port to connect to", default = 4247)
parser.add_argument("--log", help = "where to create the log file")
args = parser.parse_args()

ferr = sys.stderr
if args.log is not None:
  truncate(args.log)
  ferr = open(args.log, 'a')


try:
  while True:
    # connect to server
    sock = socket.create_connection((args.host, args.port))
    fin = sock.makefile('r')
    fout = sock.makefile('w')
    
    # Take seat num. <player_id>
    while True:
      print("take", args.player_id, file = fout, flush = True)
      ans = fin.readline().strip()
      if ans == "ok":
        break
    print("finish", file = fout, flush = True)
    
    # Execute client and have him play.
    subprocess.run(args.exec_path, stdin = fin, stdout = fout, stderr = ferr)
    
    # release held resources
    fin.close()
    fout.close()
    sock.close()

except Exception:
  traceback.print_exc()


if ferr:
  ferr.close()
