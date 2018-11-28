import argparse
import subprocess, socket, sys, threading
import traceback, signal


sub = None            # the subprocess (client)
sock = None           # the underlying socket for communication with server

fin = None            # input from socket
fout = None           # output to socket
ferr = sys.stderr     # log file


def clean(signum, stack_frame):
  """Run at the end: kill the subprocess."""
  sub.kill()
  ferr.close()
  exit()

def truncate(filename):
  """Creates an empty file with the given name. If it exists, it is
  truncated to size 0."""
  open(filename, 'w').close()

def redirect(src, dest):
  """Redirects output from readable stream <src> to writable stream <dest>."""
  for line in src:
    print(line, end = '', file = dest, flush = True)


parser = argparse.ArgumentParser()
parser.add_argument("exec_path", help = "the path to the executable client")
parser.add_argument("player_id", help = "the player ID (what side the client should play)")
parser.add_argument("--host", help = "the Inet address of the game host", default = "127.0.0.1")
parser.add_argument("--port", type = int, help = "the port to connect to", default = 4247)
parser.add_argument("--log", help = "where to create the log file")
args = parser.parse_args()

# Set logfile.
if args.log is not None:
  truncate(args.log)
  ferr = open(args.log, 'a')

# Gracefully handle all signals, so that key held resources are released.
for sig in [signal.SIGQUIT, signal.SIGTERM, signal.SIGINT]:
  signal.signal(sig, clean)

try:
  while True:
    # Connect to server.
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
    command = args.exec_path
    if command[-3:] == ".py":
      command = "python3 {}".format(command)
    sub = subprocess.Popen(command, stdin = subprocess.PIPE, stdout = subprocess.PIPE, stderr = ferr, universal_newlines = True)
    from_server = threading.Thread(target = redirect, args = (fin, sub.stdin), daemon = True)
    from_client = threading.Thread(target = redirect, args = (sub.stdout, fout), daemon = True)
    from_server.start()
    from_client.start()
    from_server.join()
    
    # Release held resources.
    sub.kill()
    fin.close()
    fout.close()
    sock.close()

except Exception:
  traceback.print_exc()
