from hashlib import sha1
import os, getpass, sys

# get credentials
name = input()
login = input()
password = getpass.getpass('Password:')

# create necessary structure
folder = "../hlavnykomp/uploady/{}".format(login)
try:
  os.mkdir(folder)
except FileExistsError:
  print("User already exists, aborting.", file = sys.stderr)

# encode them
encoded_passwd = sha1('abc{}def'.format(password).encode("utf-8")).hexdigest()
s = "  ('{}', u'{}', b'{}'),\n".format(login, name, encoded_passwd)

# Now, tell web that there is a new team.
# read old web data...
with open('../hlavnykomp/webconfig') as fin:
  lines = fin.readlines()
  lines = lines[:3] + [s] + lines[3:]

# rewrite with new web data...
with open('../hlavnykomp/webconfig', 'w') as fin:
	for line in lines:
		fin.write(line)


# read old server data...
with open('../hlavnykomp/srvconfig') as fin:
	lines = fin.readlines()
	lines = lines[:4] + list("  '{}'\n".format(name)) + lines[4:]
	lines[0] = ' '.join(lines[0].split()[:-1] + [login, ')']) + '\n'

# rewrite with new server data...
with open('../hlavnykomp/srvconfig', 'w') as subor:
	for line in lines:
		subor.write(line)
