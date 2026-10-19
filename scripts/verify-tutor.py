"""Real HTTP + restart verification with a synthetic provider and isolated H2 files.
No real AI service is called. Run after packaging the backend, from the project root.
"""
import http.cookiejar
import http.server
import json
import os
from pathlib import Path
import socket
import subprocess
import threading
import time
import urllib.error
import urllib.parse
import urllib.request
import uuid

ROOT=Path(__file__).resolve().parents[1]
WORK=ROOT/'.local'/('tutor-verification-'+uuid.uuid4().hex[:8])
WORK.mkdir(parents=True)
assert WORK.resolve().is_relative_to((ROOT/'.local').resolve())
java=next(p for p in (ROOT/'.local/tools').glob('jdk-*/bin/java.exe') if p.is_file())
requests=[]

class Provider(http.server.BaseHTTPRequestHandler):
    def log_message(self,*args): pass
    def do_POST(self):
        body=json.loads(self.rfile.read(int(self.headers['Content-Length'])))
        requests.append(body)
        system=body['messages'][0]['content']
        if 'pedagogical expert' in system:
            result={'explanation':'A key identifies a row.','example':'Student number.','analogy':'A passport.','keyTakeaway':'Use unique keys.'}
        elif 'educational summaries' in system:
            result={'summary':'Keys identify rows.','keyPoints':['Unique identifiers.'],'estimatedReadTime':'1 minute'}
        elif 'mind maps' in system:
            leaf=lambda label:{'label':label,'description':'A detail','children':[]}
            result={'root':{'label':'Databases','description':'Structured data','children':[
                {'label':label,'description':'A concept','children':[leaf('First'),leaf('Second')]} for label in ['Tables','Keys','Queries']]}}
        else:
            result=None
        text=json.dumps(result) if result else 'A primary key identifies one row. See the supplied course chapter.'
        payload=json.dumps({'choices':[{'finish_reason':'stop','message':{'content':text}}]}).encode()
        self.send_response(200);self.send_header('Content-Type','application/json');self.end_headers();self.wfile.write(payload)

provider=http.server.ThreadingHTTPServer(('127.0.0.1',0),Provider)
threading.Thread(target=provider.serve_forever,daemon=True).start()
with socket.socket() as probe:
    probe.bind(('127.0.0.1',0)); port=probe.getsockname()[1]
base=f'http://127.0.0.1:{port}'
backend=None
handles=[]
def start():
    global backend
    env=os.environ.copy()
    env.update({'SPRING_PROFILES_ACTIVE':'local','SERVER_PORT':str(port),'SERVER_ADDRESS':'127.0.0.1',
        'SPRING_DATASOURCE_URL':f'jdbc:h2:file:{(WORK/"db").as_posix()};MODE=MySQL;DB_CLOSE_ON_EXIT=FALSE;WRITE_DELAY=0',
        'SPRING_DATASOURCE_USERNAME':'sa','SPRING_DATASOURCE_PASSWORD':'','SMARTLEARN_ADMIN_PASSWORD':'synthetic-local-test',
        'TUTOR_AI_KEY':'synthetic-provider-key','TUTOR_AI_MODEL':'synthetic-test-model',
        'TUTOR_AI_URL':f'http://127.0.0.1:{provider.server_port}/chat/completions'})
    log=(WORK/f'backend-{len(handles)}.log').open('wb');handles.append(log)
    backend=subprocess.Popen([str(java),'-jar',str(ROOT/'backend/target/smartlearn-backend-0.0.1-SNAPSHOT.jar')],cwd=ROOT,env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=subprocess.CREATE_NO_WINDOW)
    until=time.monotonic()+75
    while time.monotonic()<until:
        if backend.poll() is not None: raise RuntimeError(f'Test backend exited; see {WORK}')
        try:
            urllib.request.urlopen(base+'/api/auth/csrf',timeout=2).close();return
        except (OSError,urllib.error.URLError): time.sleep(.3)
    raise RuntimeError('Test backend startup timeout')
def stop():
    global backend
    if backend and backend.poll() is None:
        backend.terminate()
        try: backend.wait(timeout=20)
        except subprocess.TimeoutExpired: backend.kill();backend.wait()
    backend=None
def session(email):
    opener=urllib.request.build_opener(urllib.request.HTTPCookieProcessor(http.cookiejar.CookieJar()))
    def call(path,data=None,method=None):
        headers={}
        if data is not None or method=='DELETE':
            csrf=json.load(opener.open(base+'/api/auth/csrf'))['token'];headers['X-XSRF-TOKEN']=csrf
        content=None
        if data is not None:content=json.dumps(data).encode();headers['Content-Type']='application/json'
        with opener.open(urllib.request.Request(base+path,data=content,headers=headers,method=method),timeout=15) as response:
            raw=response.read();return json.loads(raw) if raw else None
    until=time.monotonic()+10
    while True:
        csrf=call('/api/auth/csrf')['token']
        try:
            opener.open(urllib.request.Request(base+'/api/auth/login',data=urllib.parse.urlencode({'email':email,'password':'password123'}).encode(),headers={'X-XSRF-TOKEN':csrf})).close()
            break
        except urllib.error.HTTPError as error:
            # The HTTP server starts before the demo-data initializer finishes.
            if error.code!=401 or time.monotonic()>until: raise
            time.sleep(.25)
    return call

try:
    start();call=session('ahmed@example.com')
    courses=call('/api/formations'); course=next(c for c in courses if c['titre']=='Java pour débutants')
    first=call('/api/tutor/chat',{'message':'Explain Java classes','formationId':course['id']});cid=first['conversationId']
    call('/api/tutor/chat',{'message':'Give an example','conversationId':cid})
    assert 'COURSE CONTEXT' in requests[0]['messages'][0]['content']
    assert len(requests[1]['messages'])==4
    assert call('/api/tutor/explain',{'concept':'Primary keys'})['example']=='Student number.'
    assert call('/api/tutor/summarize',{'text':'Keys identify rows.'})['keyPoints']
    assert len(call('/api/tutor/mindmap',{'topic':'Databases'})['root']['children'])==3
    assert len(call(f'/api/tutor/conversations/{cid}')['messages'])==4
    stop();start();call=session('ahmed@example.com')
    assert len(call(f'/api/tutor/conversations/{cid}')['messages'])==4
    other=session('sarah@example.com')
    try:other(f'/api/tutor/conversations/{cid}');raise AssertionError('Another learner accessed private history')
    except urllib.error.HTTPError as error:assert error.code==403
    call(f'/api/tutor/conversations/{cid}',method='DELETE');assert not call('/api/tutor/conversations')
    report={'provider':'synthetic local fixture, no real AI request','all_four_tools':True,'rag_context':True,'multi_turn':True,'history_survived_restart':True,'ownership':True,'deletion':True}
    (ROOT/'.local/tutor-http-verification.json').write_text(json.dumps(report,indent=2))
    print(json.dumps(report),flush=True)
finally:
    stop();provider.shutdown()
    for handle in handles:handle.close()
