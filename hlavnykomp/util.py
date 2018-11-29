import subprocess


def restart():
  """Removes all data from the server: uploads, list of teams, webconfig
  and srvconfig."""
  subprocess.run("cp srvconfig.default srvconfig", shell = True)
  subprocess.run("cp webconfig.default webconfig", shell = True)
  subprocess.run("rm -r uploady/*", shell = True)
  subprocess.run("rm -r buildy/*", shell = True)
  subprocess.run("rm web/teams.txt; touch web/teams.txt", shell = True)
