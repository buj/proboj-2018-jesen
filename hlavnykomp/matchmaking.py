#!/usr/bin/python3
import os
import sys
import random
import subprocess
import re
import time
import trueskill


script_path = os.path.dirname(sys.argv[0]) 
builds_path = os.path.join(script_path, 'buildy')
teams_file = os.path.join(script_path, 'web', 'teams.txt')
client_wrapper = os.path.join(script_path, '..', 'klienti', 'client.py')

class Team:
  def __init__(self, id, name, hash, mu = 25, sigma = 8.333333):
    self.id = id
    self.name = name
    self.hash = hash
    self.rating = trueskill.Rating(mu = mu, sigma = sigma)
  
  @staticmethod
  def load_from_string(line):
    id, name, hash, mu, sigma = line.split()
    return Team(id, name, hash, float(mu), float(sigma))
  
  def to_string(self):
    return '{} {} {} {} {}'.format(self.id, self.name, self.hash, self.rating.mu, self.rating.sigma)

def load_teams(filename):
  teams = []
  with open(filename, 'r') as f:
    teams = [Team.load_from_string(l) for l in f]
  return teams

def save_teams(teams, filename):
  with open(filename, 'w') as f:
    for t in teams:
      f.write('{}\n'.format(t.to_string()))


def get_current_build(teamid):
  build_dir = os.path.join(builds_path, teamid)
  if os.path.exists(build_dir):
    builds = [os.path.join(build_dir, b) for b in os.listdir(build_dir)]
    by_age = [(os.path.getmtime(b), b) for b in builds]
    current_build = max(by_age)[1]
    return current_build
  return None

def remove_prefix(prefix, s):
  prefix = prefix + '/'
  if s[:len(prefix)] == prefix:
    return s[len(prefix):]
  return s

def get_exec(build):
  """Scans the folder <build> for 'hrac*' and return one of the matched
  file names."""
  files = [f for f in os.listdir(build) if os.path.isfile(os.path.join(build, f))]
  for f in files:
    if f[:4] == "hrac" and f[-4:] != ".cpp":
      return os.path.join(build, f)
  return None

def get_eligible_teams(teams):
  return [t for t in teams if get_current_build(t.id)]

def generate_round(teams):
  if len(teams) < 2:
    return []
  tmp = teams.copy()
  n = len(tmp)
  
  # if n is odd, leave out one team at random
  if n % 2 == 1:
    k = random.randint(0, n-1)
    tmp = tmp[:k] + tmp[k+1:]
    n -= 1
  
  # 1st against 2nd, 3rd against 4th, ...
  res = []
  for i in range(0, n, 2):
    res.append(tmp[i:i+2])
  return res

def get_next_zaznam_dir(path):
  files = os.listdir(path)
  highest = 0
  for f in files:
    match = re.fullmatch('^[0-9]{6}$', f)
    if match is not None:
      highest = max(highest, int(f))
  return os.path.join(path, '{:06}'.format(highest+1))

def get_random_map(path):
  return os.path.join(path, random.choice(os.listdir(path)))

def update_ratings(rank_by_id):
  teams = load_teams(teams_file)
  team_by_id = {t.id : t for t in teams}
  ratings, ranks, ids = [], [], []
  for id, rank in rank_by_id.items():
    ratings.append([team_by_id[id].rating])
    ranks.append(rank)
    ids.append(id)
  new_ratings = trueskill.rate(ratings, ranks)
  for id, rating in zip(ids, new_ratings):
    team_by_id[id].rating = rating[0]
  save_teams(teams, teams_file)

def play_match(match):
  match_ids = [t.id for t in match]
  builds = [get_current_build(id) for id in match_ids]
  execs = [get_exec(b) for b in builds]
  server_path = os.path.join(script_path, '..', 'server', 'server.jar')
  zaznamy_path = os.path.join(script_path, '..', 'zaznamy')
  mapy_path = os.path.join(script_path, '..', 'mapy')
  mapa = get_random_map(mapy_path)
  
  # try each client in both roles
  seed = random.randint(0, 1023456789)
  total_score = [0] * 2
  bad = False
  for i in range(2):
    zaznam_path = get_next_zaznam_dir(zaznamy_path)
    try:
      os.mkdir(zaznam_path)
    except FileExistsError:
      print("Error while creating log folder: it already exists. We will be rewriting it...", file = sys.stderr)
    zaznam_file = os.path.join(zaznam_path, "observer.log")
    rank_file = os.path.join(zaznam_path, "rank")
    
    manifest_lines = []
    manifest_lines.append('clients={}\n'.format(','.join([remove_prefix(builds_path, b) for b in builds])))
    manifest_lines.append('server={}\n'.format(int(os.path.getmtime(server_path))))
    manifest_lines.append('map={}\n'.format(mapa))
    manifest_lines.append('begin={}\n'.format(int(time.time())))
    manifest_lines.append('titles={}\n'.format(','.join([t.name for t in match])))
    manifest_lines.append('state=playing\n')
    
    with open(zaznam_path + '.manifest', 'w') as f:
      for l in manifest_lines:
        f.write(l)
    
    # run server and clients
    server = subprocess.Popen("java -jar {} map={} seed={} log={}".format(server_path, mapa, seed, zaznam_path), shell = True)
    time.sleep(1.)
    clients = [None] * 2
    for j in range(2):
      clients[j] = subprocess.Popen("python3 {} {} {} --log={}".format(client_wrapper, execs[i^j], j, os.path.join(zaznam_path, "{}.log".format(j))), shell = True)
    
    # read score of the attacker
    if server.wait() == 0:
      with open(rank_file, 'r') as f:
        score = [int(l) for l in f]
        for j in range(2):
          total_score[i^j] += score[j]
      with open(zaznam_path + '.manifest', 'w') as f:
        manifest_lines[-1] = 'state=displaying\n'
        manifest_lines.append('rank={}\n'.format(','.join(map(str, score))))
        for l in manifest_lines:
          f.write(l)
      with open(os.path.join(zaznam_path, 'titles'), 'w') as f:
        for t, b in zip(match, builds):
          f.write('{}\n'.format(t.id))
          f.write('{}\n'.format(remove_prefix(os.path.join(builds_path, t.id), b)))
          f.write('{}\n'.format(t.name))
    else:
      bad = True
      with open(zaznam + '.manifest', 'w') as f:
        manifest_lines[-1] = 'state=crashed\n'
        for l in manifest_lines:
          f.write(l)
    
    # print info
    print(zaznam_path + ".manifest", file = sys.stderr)
    for line in manifest_lines:
      print(line.rstrip('\n'), file = sys.stderr)
    print("------------------------------------------------------------")
  
  # if no error occured, recalculate ratings
  if not bad:
    id_by_score = list(zip(total_score, match_ids))
    id_by_score.sort(reverse=True)
    cur_rank = 0
    rank_by_id = {}
    for i, val in enumerate(id_by_score):
      if i > 0 and id_by_score[i-1][0] > val[0]:
        cur_rank += 1
      rank_by_id[val[1]] = cur_rank
    update_ratings(rank_by_id)

while True:
  all_teams = load_teams(teams_file)
  playing_teams = get_eligible_teams(all_teams)
  matches = generate_round(playing_teams)
  for match in matches:
    play_match(match)
