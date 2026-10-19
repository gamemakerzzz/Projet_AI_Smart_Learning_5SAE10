import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { TutorComponent } from './tutor.component';
import { TutorService, ChatResponse } from '../../services/tutor.service';
import { FormationService } from '../../services/formation.service';
import { UiService } from '../../services/ui.service';

describe('Tutor module', () => {
  const notify=vi.fn();
  function setup(service: Partial<TutorService>) {
    TestBed.configureTestingModule({ imports:[TutorComponent], providers:[provideRouter([]),
      {provide:TutorService,useValue:{status:async()=>({configured:true,model:'test',provider:'Groq'}),listConversations:async()=>[],...service}},
      {provide:FormationService,useValue:{getAll:()=>of([])}}, {provide:UiService,useValue:{notify,confirm:async()=>true}}] });
    return TestBed.runInInjectionContext(()=>new TutorComponent());
  }
  const reply: ChatResponse={conversationId:1,title:'Keys',userMessage:{id:1,role:'user',content:'Question',createdAt:'2026-10-19T00:00:00Z'},assistantMessage:{id:2,role:'assistant',content:'Answer',createdAt:'2026-10-19T00:00:01Z'}};
  it('does not append a late reply to a new conversation',async()=>{
    let resolve!:(value:ChatResponse)=>void;const c=setup({chat:()=>new Promise(r=>resolve=r)});
    c.chatMessage='Explain keys';const pending=c.sendChat();c.newConversation();resolve(reply);await pending;
    expect(c.messages()).toEqual([]);expect(c.activeConvId()).toBeNull();expect(c.chatLoading()).toBe(false);
  });
  it('ignores older conversation loads',async()=>{
    let resolve!:(value:any)=>void;const c=setup({getConversation:id=>id===1?new Promise(r=>resolve=r):Promise.resolve({id:2,title:'New',formationId:null,messages:[],createdAt:'',updatedAt:''})});
    const older=c.loadConversation(1);await c.loadConversation(2);resolve({id:1,messages:[],formationId:null});await older;
    expect(c.activeConvId()).toBe(2);expect(c.loadingConversation()).toBe(false);
  });
  it('restores the chat draft and clears busy state on provider failure',async()=>{
    const c=setup({chat:async()=>{throw {error:{message:'Provider unavailable'}};}});c.chatMessage='Explain keys';await c.sendChat();
    expect(c.chatMessage).toBe('Explain keys');expect(c.chatLoading()).toBe(false);expect(c.messages()).toEqual([]);
  });
  it('runs explain, summarize and mind map and exposes their results',async()=>{
    const c=setup({explain:async()=>({explanation:'Explanation',example:'Example',analogy:'Analogy',keyTakeaway:'Key'}),
      summarize:async()=>({summary:'Summary',keyPoints:['Point'],estimatedReadTime:'1 minute'}),
      mindMap:async()=>({topic:'Keys',language:'en',root:{label:'Keys',description:'Root',children:[]}})});
    c.explainConcept='Keys';await c.runExplain();c.summarizeText='Keys identify rows';await c.runSummarize();c.mindmapTopic='Keys';await c.runMindMap();
    expect(c.explainResult()?.example).toBe('Example');expect(c.summarizeResult()?.keyPoints).toEqual(['Point']);expect(c.mindmapResult()?.root.label).toBe('Keys');
  });
  it('renders all four tabs, generated branches and persisted messages',async()=>{
    setup({});const fixture=TestBed.createComponent(TutorComponent);fixture.autoDetectChanges();await fixture.whenStable();
    expect(fixture.nativeElement.querySelectorAll('[role="tab"]').length).toBe(4);
    const c=fixture.componentInstance;c.activeTab.set('mindmap');c.mindmapResult.set({topic:'Keys',language:'en',root:{label:'Keys',description:'Root',children:[{label:'Primary',description:'Identifier',children:[{label:'Unique',description:'No duplicates',children:[]}]}]}});
    await fixture.whenStable();expect(fixture.nativeElement.querySelectorAll('.branch-node').length).toBe(1);expect(fixture.nativeElement.textContent).toContain('No duplicates');
    c.activeTab.set('chat');c.messages.set([reply.userMessage,reply.assistantMessage]);await fixture.whenStable();expect(fixture.nativeElement.textContent).toContain('Answer');
  });
});
