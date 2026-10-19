import { TestBed } from '@angular/core/testing';
import { ChangeDetectorRef, signal } from '@angular/core';
import { ActivatedRoute, Router, provideRouter, convertToParamMap } from '@angular/router';
import { BehaviorSubject, of } from 'rxjs';
import { ForumComponent } from './forum.component';
import { ForumService, Analysis } from '../../services/forum.service';
import { AuthService } from '../../services/auth.service';
import { FormationService } from '../../services/formation.service';

describe('Forum AI workflow', () => {
  const analysis: Analysis = { matches: [], summary: 'No match', scannedCount: 1, availableCount: 1, durationMs: 10, cached: false, aiUsed: true, model: 'test', promptVersion: 'forum-v2' };
  function component(service: Partial<ForumService>) {
    TestBed.configureTestingModule({ providers: [{ provide: ChangeDetectorRef, useValue: { markForCheck: () => {} } }] });
    const auth = { isAdmin: false, user: signal({ id: 1 }) } as unknown as AuthService;
    return TestBed.runInInjectionContext(() => new ForumComponent(auth, service as ForumService, {} as FormationService, {} as ActivatedRoute, {} as Router));
  }
  it('renders completed requests without requiring another click', async () => {
    let resolve!: (value: any) => void;
    const service = { list: () => new Promise(r => resolve = r), status: async () => ({ configured: false, model: 'gpt-6-luna' }) };
    await TestBed.configureTestingModule({
      imports: [ForumComponent], providers: [provideRouter([]),
        { provide: ForumService, useValue: service },
        { provide: FormationService, useValue: { getAll: () => of([]) } },
        { provide: AuthService, useValue: { isAdmin: false, user: signal({ id: 1 }) } }]
    }).compileComponents();
    const fixture = TestBed.createComponent(ForumComponent);
    fixture.autoDetectChanges();
    expect(fixture.nativeElement.textContent).toContain('Loading discussions');
    resolve({ items: [], total: 7, page: 0, totalPages: 1 });
    await Promise.resolve();
    await fixture.whenStable();
    expect(fixture.nativeElement.textContent).toContain('7 discussions');
    expect(fixture.nativeElement.textContent).not.toContain('Loading discussions');
  });
  it('finishes saving without refetching the saved detail or waiting for the list', async () => {
    const params = new BehaviorSubject(convertToParamMap({}));
    const saved = { discussion: { id: 7, title: 'Saved question' } as any, replies: [] };
    const detail = vi.fn();
    const service = { create: async () => saved, detail, status: async () => ({}), list: () => new Promise<any>(() => {}) };
    TestBed.configureTestingModule({ providers: [{ provide: ChangeDetectorRef, useValue: { markForCheck: () => {} } }] });
    const c = TestBed.runInInjectionContext(() => new ForumComponent(
      { isAdmin: false, user: signal({ id: 1 }) } as unknown as AuthService, service as unknown as ForumService,
      { getAll: () => of([]) } as unknown as FormationService, { queryParamMap: params } as unknown as ActivatedRoute,
      { navigate: async () => { params.next(convertToParamMap({ discussion: '7' })); return true; } } as unknown as Router));
    c.ngOnInit(); c.draft = { title: 'Protect Angular routes', body: 'How do I stop anonymous access?', formationId: null };
    await c.publish();
    expect(c.saving).toBe(false); expect(c.detail).toBe(saved); expect(detail).not.toHaveBeenCalled();
  });
  it('discards late AI results when the draft changes', async () => {
    let resolve!: (value: Analysis) => void;
    const c = component({ check: () => new Promise(r => resolve = r) });
    c.draft = { title: 'Protect Angular routes', body: 'How do I stop anonymous access?', formationId: null };
    const pending = c.check();
    c.draft.body = 'A different question about styling a login form'; c.draftChanged();
    resolve(analysis); await pending;
    expect(c.analysis).toBeNull(); expect(c.checking).toBe(false);
  });
  it('keeps the draft publishable after an AI outage', async () => {
    const c = component({ check: () => Promise.reject({ status: 503, error: { message: 'AI unavailable' } }) });
    c.draft = { title: 'Protect Angular routes', body: 'How do I stop anonymous access?', formationId: null };
    await c.check();
    expect(c.aiError).toBe('AI unavailable'); expect(c.validDraft).toBe(true); expect(c.checking).toBe(false);
    expect(c.draft.title).toBe('Protect Angular routes');
  });
  it('does not reuse analysis after changing the course', () => {
    const c = component({}); c.analysis = analysis; c.draft.formationId = 2; c.draftChanged(); expect(c.analysis).toBeNull();
  });
  it('does not overwrite edited tags with a late suggestion', async () => {
    let resolve!: (value: any) => void;
    const c = component({ suggestTags: () => new Promise(r => resolve = r) });
    c.draft = { title: 'Protect Angular routes', body: 'How do I stop anonymous access?', formationId: null };
    const pending = c.suggestTags();
    c.tagsInput = 'authentication'; c.tagsChanged();
    resolve({ result: { tags: ['angular'], explanation: 'Route protection' }, meta: {} });
    await pending;
    expect(c.tagsInput).toBe('authentication'); expect(c.tagSuggestion).toBeNull(); expect(c.tagging).toBe(false);
  });
  it('discards a summary after selecting another discussion', async () => {
    let resolve!: (value: any) => void;
    const c = component({ summarize: () => new Promise(r => resolve = r), detail: () => Promise.resolve({ discussion: { id: 2 } as any, replies: [] }) });
    c.detail = { discussion: { id: 1, updatedAt: '2026-10-19T00:00:00Z' } as any, replies: [] };
    const pending = c.summarize();
    await c.open(2);
    resolve({ result: { discussionUpdatedAt: '2026-10-19T00:00:00Z' }, meta: {} }); await pending;
    expect(c.summaryResult).toBeNull(); expect(c.detail?.discussion.id).toBe(2); expect(c.summarizing).toBe(false);
  });
  it('ignores late details from the previously selected discussion', async () => {
    let reject!: (reason: unknown) => void;
    const c = component({ detail: id => id === 1 ? new Promise((_, r) => reject = r) : Promise.resolve({ discussion: { id: 2 } as any, replies: [] }) });
    const old = c.open(1); await c.open(2); reject({ status: 404 }); await old;
    expect(c.detail?.discussion.id).toBe(2); expect(c.error).toBe('');
  });
});
