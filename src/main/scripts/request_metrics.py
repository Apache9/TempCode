import sys
import time
import re
import threading
import heapq
import json

lock = threading.Lock()
metrics = {}

class Cost(object):
    def __init__(self, cost):
        self.cost = cost
    def __lt__(self, obj):
        return self.cost > obj.cost

def feed(name, cost):
    global lock, metrics
    lock.acquire()
    metric = metrics.setdefault(name, {'costs':[], 'min':sys.maxint, 'max':-1, 'sum':0})
    min = metric['min']
    if cost < min:
        metric['min'] = cost;
    max = metric['max']
    if cost > max:
        metric['max'] = cost;
    metric['sum'] += cost;
    heapq.heappush(metric['costs'], Cost(cost))
    lock.release()

parts = [
    r'(?P<host>\S+)',       # host %h
    r'\S+',                 # indent %l (unused)
    r'(?P<user>\S+)',       # user %u
    r'\[(?P<time>.+)\]',    # time %t
    r'"(?P<request>.+)"',   # request "%r"
    r'(?P<status>[0-9]+)',  # status %>s
    r'(?P<size>\S+)',       # size %b (careful, can be '-')
    r'"(?P<referer>.*)"',   # referer "%{Referer}i"
    r'"(?P<agent>.*)"',     # user agent "%{User-agent}i"
    r'(?P<latency>[0-9]+)', # latency
]

pattern = re.compile(r'\s+'.join(parts) + r'\s*\Z')

def read_input():
    while True:
        line = sys.stdin.readline().strip()
        global pattern
        matcher = pattern.match(line)
        parts = matcher.groupdict()
        name = 'req' # could be parsed from request
        cost = int(parts['latency'])
        feed(name, cost)

def calc(metric):
    costs = metric.pop('costs')
    costs_length = len(costs)
    metric['num'] = costs_length
    metric['avg'] = metric.pop('sum') * 1.0 / costs_length;
    pop_count = costs_length / 100;
    while pop_count > 1:
        heapq.heappop(costs)
        pop_count -= 1
    metric['p99'] = heapq.heappop(costs).cost;

def calc_and_report(interval):
    while True:
        time.sleep(interval)
        global lock, metrics
        lock.acquire()
        old_metrics = metrics
        metrics = {}
        lock.release()
        for (name, metric) in old_metrics.items():
            calc(metric)
        fp = open("metrics.json", "w")
        json.dump(old_metrics, fp)
        fp.close();

if __name__ == '__main__':
    if len(sys.argv) == 2:
        interval = int(sys.argv[1])
    else:
        interval = 60
    threading.Thread(target = calc_and_report, args = [interval], name = 'report-thread').start()
    read_input()
