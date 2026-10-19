"""Opt-in live evaluation using the production prompt and OpenAI Responses API.
Uses only the synthetic examples in docs/forum-evaluation.json. Never prints credentials.
Run: python scripts/evaluate-forum-ai.py --model YOUR_MODEL --prompt both
"""
import argparse, json, os, re, time, urllib.request, urllib.error
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
BASELINE = 'Classify each question pair as DUPLICATE, RELATED or UNRELATED. Return one result per candidate with its discussionId, relationship and a short English explanation.'

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument('--model', default=os.getenv('OPENAI_MODEL', 'gpt-6-luna'))
    parser.add_argument('--prompt', choices=['baseline','improved','both'], default='both')
    parser.add_argument('--limit', type=int, default=12)
    parser.add_argument('--output', type=Path, default=ROOT/'.local/forum-evaluation-results.json')
    args = parser.parse_args()
    key = os.getenv('OPENAI_API_KEY', '').strip()
    if not key:
        key_path = Path(os.getenv('OPENAI_API_KEY_FILE', str(ROOT/'.local/openai-api-key.txt')))
        if key_path.is_file(): key = key_path.read_text().strip()
    if not key: raise SystemExit('Set OPENAI_API_KEY or OPENAI_API_KEY_FILE locally before running. Do not put the key in your report.')
    source = (ROOT/'backend/src/main/java/com/smartlearn/forum/ForumAiClient.java').read_text(encoding='utf-8')
    improved = re.search(r'PROMPT\s*=\s*"""(.*?)"""', source, re.S).group(1).strip()
    cases = json.loads((ROOT/'docs/forum-evaluation.json').read_text())[:max(1,min(args.limit,12))]
    prompts = {'baseline': BASELINE, 'improved': improved}
    if args.prompt != 'both': prompts = {args.prompt: prompts[args.prompt]}
    item = {'type':'object','properties':{'discussionId':{'type':'integer'},'relationship':{'type':'string','enum':['DUPLICATE','RELATED','UNRELATED']},'explanation':{'type':'string'}},'required':['discussionId','relationship','explanation'],'additionalProperties':False}
    schema = {'type':'object','properties':{'results':{'type':'array','items':item}},'required':['results'],'additionalProperties':False}
    report = {'model':args.model,'dataset':'forum-evaluation.json','synthetic':True,'runs':[]}
    for name, prompt in prompts.items():
        rows = []
        for case in cases:
            data = {'draft':{'title':case['draft'],'body':case['draft']},'candidates':[{'id':1,'title':case['candidate'],'body':case['candidate']}]}
            payload = {'model':args.model,'store':False,'instructions':prompt,'input':[{'role':'user','content':json.dumps(data)}],'max_output_tokens':1200,'text':{'format':{'type':'json_schema','name':'forum_duplicates','strict':True,'schema':schema}}}
            request = urllib.request.Request('https://api.openai.com/v1/responses',data=json.dumps(payload).encode(),headers={'Authorization':'Bearer '+key,'Content-Type':'application/json'})
            started = time.perf_counter()
            try:
                with urllib.request.urlopen(request, timeout=90) as response: result = json.load(response)
                if result.get('status') != 'completed': raise ValueError('Incomplete response')
                text = ''.join(c.get('text','') for output in result.get('output',[]) for c in output.get('content',[]) if c.get('type')=='output_text')
                classified = json.loads(text)['results']
                if len(classified)!=1 or classified[0]['discussionId']!=1: raise ValueError('Invalid references')
                predicted = classified[0]['relationship']
                if predicted not in ('DUPLICATE','RELATED','UNRELATED'): raise ValueError('Invalid label')
                rows.append({'id':case['id'],'expected':case['expected'],'predicted':predicted,'correct':predicted==case['expected'],'latencyMs':round((time.perf_counter()-started)*1000),'usage':result.get('usage',{}),'explanation':classified[0]['explanation']})
                print(name,case['id'],predicted,'OK' if predicted==case['expected'] else 'MISMATCH')
            except (urllib.error.URLError, ValueError, KeyError, TimeoutError) as error:
                # Do not print remote response bodies, credentials or request headers.
                rows.append({'id':case['id'],'expected':case['expected'],'predicted':None,'correct':False,'error':type(error).__name__,'latencyMs':round((time.perf_counter()-started)*1000)})
                print(name,case['id'],'ERROR',type(error).__name__)
                if isinstance(error,urllib.error.HTTPError) and error.code in (401,403,429): break
        successful = [r for r in rows if r['predicted'] is not None]
        tp=sum(r['predicted']=='DUPLICATE' and r['expected']=='DUPLICATE' for r in rows)
        fp=sum(r['predicted']=='DUPLICATE' and r['expected']!='DUPLICATE' for r in rows)
        fn=sum(r['predicted']!='DUPLICATE' and r['expected']=='DUPLICATE' for r in rows)
        report['runs'].append({'prompt':name,'attempted':len(rows),'planned':len(cases),'completed':len(successful),'accuracyIncludingErrors':sum(r['correct'] for r in rows)/len(rows) if rows else None,'duplicatePrecision':tp/(tp+fp) if tp+fp else None,'duplicateRecall':tp/(tp+fn) if tp+fn else None,'falseDuplicates':fp,'meanLatencyMs':sum(r['latencyMs'] for r in successful)/len(successful) if successful else None,'rows':rows})
    args.output.parent.mkdir(parents=True,exist_ok=True)
    args.output.write_text(json.dumps(report,indent=2),encoding='utf-8')
    print('Results saved to',args.output)

if __name__ == '__main__': main()
