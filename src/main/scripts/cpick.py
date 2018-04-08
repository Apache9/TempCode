jira_ids=set(line.strip() for line in open('jiras'))

cid=''
jid=''
cpicks=[]
for line in open('gitlogs'):
 line = line.strip()
 if line.startswith('commit '):
   cid=line.split()[1]
   continue
 if line.startswith('HBASE-'):
   jid=line.split()[0]
   if jid in jira_ids:
     cpicks.insert(0, 'git cherry-pick ' + cid)
     print 'commit id for ' + jid + ' is ' + cid

for cpick in cpicks:
  print cpick
 


