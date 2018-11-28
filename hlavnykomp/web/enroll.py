from hashlib import sha1
import getpass
import os
import shutil

class Team:
  def __init__(self, id, name, hash, mu = 25, sigma = 8.333333):
    self.id = id
    self.name = name
    self.hash = hash
    self.mu = mu
    self.sigma = sigma
  
  @staticmethod
  def load_from_string(line):
    id, name, hash, mu, sigma = line.split()
    return Team(id, name, hash, mu, sigma)
  
  def to_string(self):
    return '{} {} {} {} {}'.format(self.id, self.name, self.hash, self.mu, self.sigma)
  

teamsfile = open('teams.txt', 'r')
teams = []
for l in teamsfile:
  teams.append(Team.load_from_string(l))
teamsfile.close()

def list_all():  
  print('\nCurrent teams are:\n{}'.format('\n'.join([t.name for t in teams])))

def quit():
  teamsfile = open('teams.txt', 'w')
  for t in teams:
    teamsfile.write(t.to_string() + '\n')
  teamsfile.close()
  webconfig = open('../webconfig', 'w')
  webconfig.write("""# -*- coding: utf8 -*-\n\nDRUZINKY = [\n""")
  for t in teams:
    webconfig.write("  ('{}', u'{}', b'{}'),\n".format(t.id, t.name, t.hash))
  webconfig.write("""]\n\nGIT = ''\n\nSECRET_KEY = 'blablablablabla'""")
  webconfig.close()
  
  srvconfig = open('../srvconfig', 'w')
  srvconfig.write("""\nklienti=( {} )\nmapy=( mapy/*.ppm )\nnazvy=(\n""".format(' '.join([t.id for t in teams])))
  for t in teams:
    srvconfig.write("  '{}'\n".format(t.name))
  srvconfig.write(""")\n""")
  srvconfig.close()
  
  for t in teams:
    path = '../uploady/{}'.format(t.id)
    if not os.path.isdir(path):
      os.mkdir(path)
      default_client = '2018-07-14-00-00-00.tar.gz'
      shutil.copyfile(os.path.join('default', default_client), os.path.join(path, default_client))
  exit(0)

def to_alpha(n):
  if n == 0:
    return 'a'
  res = ''
  while n > 0:
    res += chr(n%26 + ord('a'))
    n /= 26
  return res[::-1]

def add():
  id = 'team' + to_alpha(len(teams))
  name = raw_input('Team name: ')
  passwd = getpass.getpass()
  hash = sha1('abc'+passwd+'def').hexdigest()
  teams.append(Team(id, name, hash))

commands = {'q': ('quit', quit), 'a': ('add team', add), 'l': ('list all teams', list_all)}

while True:
  print('\n-----------------------')
  print('What do you want to do?')
  for c, val in commands.items():
    print(c + ': ' + val[0])
  print('-----------------------')
  command = raw_input()
  if command not in commands:
    print('Unknown command: ' + command)
    continue
  commands[command][1]()
