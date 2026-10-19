import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { firstValueFrom, Observable, timeout } from 'rxjs';

export interface ForumDraft { title: string; body: string; formationId: number | null; tags?: string[]; }
export interface Topic extends ForumDraft { id: number; authorId: number; authorName: string; formationTitle: string | null; acceptedReplyId: number | null; replyCount: number; createdAt: string; updatedAt: string; }
export interface Reply { id: number; discussionId: number; authorId: number; authorName: string; body: string; createdAt: string; updatedAt: string; }
export interface Detail { discussion: Topic; replies: Reply[]; }
export interface Topics { items: Topic[]; total: number; page: number; totalPages: number; }
export interface Analysis { matches: { discussionId: number; title: string; relationship: 'DUPLICATE' | 'RELATED'; explanation: string; resolved: boolean }[]; summary: string; scannedCount: number; availableCount: number; durationMs: number; cached: boolean; aiUsed: boolean; model: string; promptVersion: string; }
export interface AiStatus { configured: boolean; model: string; promptVersion: string; candidateLimit: number; }
export interface InsightMeta { durationMs: number; cached: boolean; model: string; promptVersion: string; }
export interface Insight<T> { result: T; meta: InsightMeta; }
export interface Evidence { text: string; sources: { id: string; label: string }[]; }
export interface ThreadSummary {
  overview: Evidence; keyPoints: Evidence[]; proposedSolutions: Evidence[]; unresolvedQuestions: Evidence[];
  acceptedAnswerSummary: string; acceptedReplyId: number | null; includedReplies: number; totalReplies: number;
  truncated: boolean; discussionUpdatedAt: string;
}
export interface TagSuggestions { tags: string[]; explanation: string; }
export interface LearningLink { id: string; title: string; url: string; explanation: string; external: boolean; }
export interface LearningSuggestions {
  courses: LearningLink[]; resources: LearningLink[]; coursesConsidered: number; resourcesConsidered: number;
  scope: string; includedReplies: number; totalReplies: number; truncated: boolean; discussionUpdatedAt: string;
}

@Injectable({ providedIn: 'root' })
export class ForumService {
  private base = '/api/forum';
  constructor(private http: HttpClient) {}
  private response<T>(request: Observable<T>, ai = false) { return firstValueFrom(request.pipe(timeout(ai ? 55000 : 10000))); }
  list(search: string, state: string, page: number, formationId: number | null) {
    let params = new HttpParams().set('search', search).set('state', state).set('page', page);
    if (formationId !== null) params = params.set('formationId', formationId);
    return this.response(this.http.get<Topics>(`${this.base}/discussions`, { params }));
  }
  detail(id: number) { return this.response(this.http.get<Detail>(`${this.base}/discussions/${id}`)); }
  create(draft: ForumDraft) { return this.response(this.http.post<Detail>(`${this.base}/discussions`, draft)); }
  update(id: number, draft: ForumDraft) { return this.response(this.http.put<Detail>(`${this.base}/discussions/${id}`, draft)); }
  delete(id: number) { return this.response(this.http.delete<void>(`${this.base}/discussions/${id}`)); }
  reply(id: number, body: string) { return this.response(this.http.post<Detail>(`${this.base}/discussions/${id}/replies`, { body })); }
  editReply(id: number, replyId: number, body: string) { return this.response(this.http.put<Detail>(`${this.base}/discussions/${id}/replies/${replyId}`, { body })); }
  deleteReply(id: number, replyId: number) { return this.response(this.http.delete<Detail>(`${this.base}/discussions/${id}/replies/${replyId}`)); }
  accept(id: number, replyId: number | null) { return this.response(this.http.put<Detail>(`${this.base}/discussions/${id}/accepted-reply`, { replyId })); }
  status() { return this.response(this.http.get<AiStatus>(`${this.base}/ai/status`)); }
  suggestTags(draft: ForumDraft) { return this.response(this.http.post<Insight<TagSuggestions>>(`${this.base}/ai/tags`, draft), true); }
  summarize(id: number) { return this.response(this.http.post<Insight<ThreadSummary>>(`${this.base}/discussions/${id}/ai/summary`, {}), true); }
  learning(id: number) { return this.response(this.http.post<Insight<LearningSuggestions>>(`${this.base}/discussions/${id}/ai/learning`, {}), true); }
  check(draft: ForumDraft, excludeId: number | null) {
    const params = excludeId === null ? new HttpParams() : new HttpParams().set('excludeId', excludeId);
    return this.response(this.http.post<Analysis>(`${this.base}/ai/check`, draft, { params }), true);
  }
}

