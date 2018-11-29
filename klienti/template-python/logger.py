import sys

f_log = sys.stderr


def logger(*args):
    print(*args, file=f_log)
    f_log.flush()
