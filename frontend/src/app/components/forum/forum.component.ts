import { ChangeDetectorRef, Component, DestroyRef, LOCALE_ID, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { firstValueFrom } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { FormationService } from '../../services/formation.service';
import { Formation } from '../../models/formation.model';
import { ForumService, ForumDraft, Detail, Topic, Reply, Topics, Analysis, AiStatus, Insight, TagSuggestions, ThreadSummary, LearningSuggestions } from '../../services/forum.service';
import { IconComponent } from '../../shared/icon.component';

@Component({
  selector: 'app-forum', standalone: true, imports: [CommonModule, FormsModule, RouterLink, IconComponent],
  host: { lang: 'en' },
  providers: [{ provide: LOCALE_ID, useValue: 'en-US' }],
  templateUrl: './forum.component.html', styleUrl: './forum.component.css'
})
export class ForumComponent implements OnInit {
  private destroyRef = inject(DestroyRef);
  private changeDetector = inject(ChangeDetectorRef);
  topics: Topics = { items: [], total: 0, page: 0, totalPages: 0 };
  courses: Formation[] = [];
  detail: Detail | null = null;
  status: AiStatus | null = null;
  search = ''; state = 'all'; course: number | null = null;
  loading = true; detailLoading = false; saving = false; checking = false;
  error = ''; aiError = ''; notice = '';
  composing = false; editingId: number | null = null;
  draft: ForumDraft = { title: '', body: '', formationId: null };
  analysis: Analysis | null = null;
  tagsInput = ''; tagging = false; tagSuggestion: Insight<TagSuggestions> | null = null;
  summaryResult: Insight<ThreadSummary> | null = null;
  learningResult: Insight<LearningSuggestions> | null = null;
  summarizing = false; suggestingLearning = false; insightError = '';
  private insightVersion = 0;
  replyBody = ''; editingReplyId: number | null = null; editingReplyBody = '';
  private listVersion = 0; private detailVersion = 0; private aiVersion = 0;
  constructor(public auth: AuthService, private forum: ForumService, private formations: FormationService,
              private route: ActivatedRoute, private router: Router) {}
  ngOnInit() {
    this.refreshStatus();
    firstValueFrom(this.formations.getAll()).then(c => this.courses = c).catch(() => this.error = 'Courses could not be loaded. General discussions are still available.').finally(() => this.changeDetector.markForCheck());
    this.load();
    this.route.queryParamMap.pipe(takeUntilDestroyed(this.destroyRef)).subscribe(params => {
      const id = Number(params.get('discussion'));
      if (Number.isSafeInteger(id) && id > 0) {
        if (this.detail?.discussion.id !== id) void this.open(id);
      }
      else { this.detailVersion++; this.detail = null; this.detailLoading = false; this.clearInsights(); }
    });
  }
  async refreshStatus() { try { this.status = await this.forum.status(); } catch { this.status = null; } finally { this.changeDetector.markForCheck(); } }
  async load(page = 0) {
    const version = ++this.listVersion; this.loading = true; this.error = '';
    try { const result = await this.forum.list(this.search.trim(), this.state, page, this.course); if (version === this.listVersion) this.topics = result; }
    catch (e) { if (version === this.listVersion) this.error = this.message(e); }
    finally { if (version === this.listVersion) { this.loading = false; this.changeDetector.markForCheck(); } }
  }
  async open(id: number) {
    this.clearInsights();
    const version = ++this.detailVersion; this.detail = null; this.detailLoading = true; this.error = ''; this.replyBody = ''; this.editingReplyId = null;
    try { const result = await this.forum.detail(id); if (version === this.detailVersion) this.detail = result; }
    catch (e) { if (version === this.detailVersion) this.error = this.message(e); }
    finally { if (version === this.detailVersion) { this.detailLoading = false; this.changeDetector.markForCheck(); } }
  }
  start(topic?: Topic) {
    this.editingId = topic?.id ?? null;
    this.draft = topic ? { title: topic.title, body: topic.body, formationId: topic.formationId, tags: [...(topic.tags || [])] } : { title: '', body: '', formationId: this.course, tags: [] };
    this.tagsInput = (this.draft.tags || []).join(', ');
    this.composing = true; this.draftChanged(); this.notice = ''; this.error = '';
  }
  cancel() { this.composing = false; this.editingId = null; this.draftChanged(); }
  draftChanged() { this.aiVersion++; this.analysis = null; this.aiError = ''; this.tagSuggestion = null; }
  get parsedTags(): string[] { return [...new Set(this.tagsInput.split(',').map(t => t.trim().toLowerCase().replace(/\s+/g, '-')).filter(Boolean))]; }
  tagsChanged() { this.draft.tags = this.parsedTags; this.draftChanged(); }
  get validTags() { return this.parsedTags.length <= 6 && this.parsedTags.every(t => /^[a-z0-9][a-z0-9+#.\-]{0,27}$/.test(t)); }
  get validQuestion() { return this.draft.title.trim().length >= 8 && this.draft.title.length <= 180 && this.draft.body.trim().length >= 15 && this.draft.body.length <= 6000; }
  get validDraft() { return this.validQuestion && this.validTags; }
  get aiBusy() { return this.checking || this.tagging || this.summarizing || this.suggestingLearning; }
  get summarySections() {
    const summary = this.summaryResult?.result;
    return summary ? [
      { title: 'Key points', items: summary.keyPoints },
      { title: 'Proposed solutions', items: summary.proposedSolutions },
      { title: 'Still unresolved', items: summary.unresolvedQuestions }
    ] : [];
  }
  canManage(authorId: number) { return this.auth.isAdmin || this.auth.user()?.id === authorId; }
  async check() {
    if (!this.validDraft || this.aiBusy) return;
    const version = ++this.aiVersion; const draft = { ...this.draft };
    this.checking = true; this.analysis = null; this.aiError = '';
    try { const result = await this.forum.check(draft, this.editingId); if (version === this.aiVersion) this.analysis = result; }
    catch (e) { if (version === this.aiVersion) this.aiError = this.message(e); }
    finally { this.checking = false; this.changeDetector.markForCheck(); }
  }
  async suggestTags() {
    if (!this.validQuestion || this.aiBusy || this.saving) return;
    const version = ++this.aiVersion;
    this.tagging = true; this.aiError = ''; this.tagSuggestion = null;
    try {
      const result = await this.forum.suggestTags({ title: this.draft.title, body: this.draft.body, formationId: this.draft.formationId });
      if (version === this.aiVersion) {
        this.tagsInput = result.result.tags.join(', '); this.draft.tags = [...result.result.tags]; this.tagSuggestion = result;
      }
    } catch (e) { if (version === this.aiVersion) this.aiError = this.message(e); }
    finally { this.tagging = false; this.changeDetector.markForCheck(); }
  }
  private clearInsights() { this.insightVersion++; this.summaryResult = null; this.learningResult = null; this.insightError = ''; }
  async summarize() {
    if (!this.detail || this.aiBusy || this.saving) return;
    const discussion = this.detail.discussion; const version = this.insightVersion; const detailVersion = this.detailVersion;
    this.summarizing = true; this.insightError = ''; this.summaryResult = null;
    try {
      const result = await this.forum.summarize(discussion.id);
      if (version === this.insightVersion && detailVersion === this.detailVersion) {
        if (result.result.discussionUpdatedAt !== discussion.updatedAt) this.insightError = 'The discussion changed. Refresh it before generating a summary.';
        else this.summaryResult = result;
      }
    } catch (e) { if (version === this.insightVersion && detailVersion === this.detailVersion) this.insightError = this.message(e); }
    finally { this.summarizing = false; this.changeDetector.markForCheck(); }
  }
  async suggestLearning() {
    if (!this.detail || this.aiBusy || this.saving) return;
    const discussion = this.detail.discussion; const version = this.insightVersion; const detailVersion = this.detailVersion;
    this.suggestingLearning = true; this.insightError = ''; this.learningResult = null;
    try {
      const result = await this.forum.learning(discussion.id);
      if (version === this.insightVersion && detailVersion === this.detailVersion) {
        if (result.result.discussionUpdatedAt !== discussion.updatedAt) this.insightError = 'The discussion changed. Refresh it before generating suggestions.';
        else this.learningResult = result;
      }
    } catch (e) { if (version === this.insightVersion && detailVersion === this.detailVersion) this.insightError = this.message(e); }
    finally { this.suggestingLearning = false; this.changeDetector.markForCheck(); }
  }
  focusSource(sourceId: string) {
    const element = document.getElementById(sourceId === 'question' ? 'forum-question' : sourceId);
    element?.scrollIntoView({ behavior: 'smooth', block: 'center' }); element?.focus({ preventScroll: true });
  }
  async publish() {
    if (!this.validDraft || this.saving) return;
    this.saving = true; this.error = '';
    try {
      const draft = { ...this.draft, tags: this.parsedTags };
      const result = this.editingId === null ? await this.forum.create(draft) : await this.forum.update(this.editingId, draft);
      this.cancel(); this.clearInsights(); this.detail = result; this.notice = 'Your question has been saved.';
      await this.router.navigate([], { relativeTo: this.route, queryParams: { discussion: result.discussion.id } });
      void this.load();
    } catch (e) { this.error = this.message(e); } finally { this.saving = false; this.changeDetector.markForCheck(); }
  }
  async removeTopic() {
    if (!this.detail || this.saving || !confirm('Delete this discussion and all its replies?')) return;
    this.saving = true;
    try { await this.forum.delete(this.detail.discussion.id); this.detail = null; this.clearInsights(); this.cancel(); this.notice = 'Discussion deleted.'; await this.router.navigate([], { relativeTo: this.route, queryParams: {} }); await this.load(); }
    catch (e) { this.error = this.message(e); } finally { this.saving = false; this.changeDetector.markForCheck(); }
  }
  async addReply() {
    if (!this.detail || this.replyBody.trim().length < 2 || this.saving) return;
    await this.mutate(() => this.forum.reply(this.detail!.discussion.id, this.replyBody), 'Reply added.');
    if (!this.error) this.replyBody = '';
  }
  editReply(reply: Reply) { this.editingReplyId = reply.id; this.editingReplyBody = reply.body; }
  async saveReply(reply: Reply) {
    if (!this.detail || this.editingReplyBody.trim().length < 2) return;
    await this.mutate(() => this.forum.editReply(this.detail!.discussion.id, reply.id, this.editingReplyBody), 'Reply updated. An edited answer must be accepted again.');
    if (!this.error) this.editingReplyId = null;
  }
  async removeReply(reply: Reply) {
    if (!this.detail || !confirm('Delete this reply?')) return;
    await this.mutate(() => this.forum.deleteReply(this.detail!.discussion.id, reply.id), 'Reply deleted.');
  }
  async accept(replyId: number | null) {
    if (!this.detail) return;
    await this.mutate(() => this.forum.accept(this.detail!.discussion.id, replyId), replyId === null ? 'Discussion reopened.' : 'Answer accepted. Discussion resolved.');
  }
  private async mutate(action: () => Promise<Detail>, notice: string) {
    if (this.saving) return;
    this.saving = true; this.error = ''; const version = this.detailVersion;
    try { const result = await action(); if (version === this.detailVersion) { this.clearInsights(); this.detail = result; this.notice = notice; } void this.load(); }
    catch (e) { this.error = this.message(e); } finally { this.saving = false; this.changeDetector.markForCheck(); }
  }
  private message(e: unknown): string {
    if ((e as { name?: string }).name === 'TimeoutError') return 'The request timed out. Refresh the discussion to check its latest state before retrying.';
    const error = e as { status?: number; error?: { message?: string } };
    if (error.status === 0) return 'The server is unavailable. Please retry.';
    if (error.status === 401) return 'Your session expired. Please sign in again.';
    if (error.status === 403) return 'You do not have permission, or your session expired. Try signing in again.';
    return error.error?.message || 'Something went wrong. Please try again.';
  }
}
