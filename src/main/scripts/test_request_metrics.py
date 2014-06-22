import subprocess
import sys
import time

p = subprocess.Popen(('python', 'request_metrics.py', '5'), stdin=subprocess.PIPE)
file = open('access.log')
for line in file.readlines():
    p.stdin.write(line)
time.sleep(6)
fp = open("metrics.json")
for line in fp.readlines():
    print line;
fp.close();
p.kill()