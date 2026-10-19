import {
  Component, OnInit, OnDestroy, ViewChild, ElementRef,
  signal, computed, inject, ChangeDetectorRef
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';
import { IconComponent } from '../../shared/icon.component';
import { UiService } from '../../services/ui.service';
import { AuthService } from '../../services/auth.service';
import {
  TutorService, MessageDto, ConversationSummary,
  MindMapNode, MindMapResponse, ExplainResponse, SummarizeResponse
} from '../../services/tutor.service';
import { FormationService } from '../../services/formation.service';
import { Formation } from '../../models/formation.model';

type Tab = 'chat' | 'explain' | 'summarize' | 'mindmap';

@Component({
  selector: 'app-tutor',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule, IconComponent],
  template: `
<div class="tutor-layout">

  <!-- ── Sidebar ── -->
  <aside class="tutor-sidebar" [class.open]="sidebarOpen()">
    <div class="sidebar-head">
      <span class="sidebar-title">💬 Conversations</span>
      <button class="btn-icon" (click)="newConversation()" title="Nouvelle conversation">
        <app-icon name="plus"/>
      </button>
    </div>

    <div class="sidebar-conversations">
      @if (loadingHistory()) {
        <p class="sidebar-hint">Chargement…</p>
      } @else if (conversations().length === 0) {
        <p class="sidebar-hint">Aucune conversation. Posez une question !</p>
      } @else {
        @for (c of conversations(); track c.id) {
          <button class="conv-item" [class.active]="activeConvId() === c.id"
                  (click)="loadConversation(c.id)">
            <span class="conv-title">{{ c.title }}</span>
            <span class="conv-meta">{{ c.messageCount }} msgs · {{ c.updatedAt | date:'dd/MM' }}</span>
            <button class="conv-delete" (click)="deleteConversation(c.id, $event)" title="Supprimer">✕</button>
          </button>
        }
      }
    </div>
  </aside>

  <!-- ── Main ── -->
  <main class="tutor-main">

    <!-- Header -->
    <header class="tutor-header">
      <button class="btn-icon sidebar-toggle" (click)="sidebarOpen.update(v => !v)" title="Conversations">
        <app-icon name="menu"/>
      </button>
      <h1 class="tutor-logo">🤖 AI Tutor <span class="tutor-logo-sub">& Mind Map</span></h1>
      @if (!aiConfigured()) {
        <span class="badge-warn">⚠ IA non configurée</span>
      }
    </header>

    <!-- Tabs -->
    <nav class="tutor-tabs" role="tablist">
      @for (t of tabs; track t.id) {
        <button class="tab-btn" [class.active]="activeTab() === t.id"
                (click)="activeTab.set(t.id)" role="tab"
                [attr.aria-selected]="activeTab() === t.id">
          {{ t.icon }} {{ t.label }}
        </button>
      }
    </nav>

    <!-- ─── TAB: CHAT ─── -->
    @if (activeTab() === 'chat') {
      <section class="chat-section" role="tabpanel">

        <!-- Context selector -->
        <div class="chat-context-bar">
          <label for="formationSelect">📚 Contexte cours :</label>
          <select id="formationSelect" [(ngModel)]="selectedFormationId" (change)="onFormationChange()">
            <option [ngValue]="null">— Général (sans cours) —</option>
            @for (f of formations(); track f.id) {
              <option [ngValue]="f.id">{{ f.titre }}</option>
            }
          </select>
        </div>

        <!-- Messages -->
        <div class="chat-messages" #messagesContainer>
          @if (messages().length === 0) {
            <div class="chat-welcome">
              <div class="welcome-icon">🎓</div>
              <h2>Bonjour, je suis votre assistant pédagogique !</h2>
              <p>Posez-moi des questions sur vos cours, demandez des explications, ou choisissez un outil dans les onglets ci-dessus.</p>
              <div class="welcome-suggestions">
                @for (s of suggestions; track s) {
                  <button class="suggestion-chip" (click)="sendSuggestion(s)">{{ s }}</button>
                }
              </div>
            </div>
          } @else {
            @for (m of messages(); track m.id) {
              <div class="chat-msg" [class.user]="m.role === 'user'" [class.assistant]="m.role === 'assistant'">
                <div class="msg-avatar">{{ m.role === 'user' ? '👤' : '🤖' }}</div>
                <div class="msg-bubble">
                  <div class="msg-content" [innerHTML]="formatMessage(m.content)"></div>
                  <div class="msg-time">{{ m.createdAt | date:'HH:mm' }}</div>
                </div>
              </div>
            }
            @if (chatLoading()) {
              <div class="chat-msg assistant">
                <div class="msg-avatar">🤖</div>
                <div class="msg-bubble typing-indicator">
                  <span></span><span></span><span></span>
                </div>
              </div>
            }
          }
        </div>

        <!-- Input -->
        <div class="chat-input-bar">
          <textarea #chatInput class="chat-textarea" [(ngModel)]="chatMessage"
                    placeholder="Posez votre question…" rows="2"
                    (keydown.control.enter)="sendChat()"
                    [disabled]="chatLoading()"></textarea>
          <button class="btn-send" (click)="sendChat()"
                  [disabled]="chatLoading() || !chatMessage.trim()"
                  title="Envoyer (Ctrl+Entrée)">
            @if (chatLoading()) { ⏳ } @else { ➤ }
          </button>
        </div>
        <p class="input-hint">Ctrl+Entrée pour envoyer</p>
      </section>
    }

    <!-- ─── TAB: EXPLAIN ─── -->
    @if (activeTab() === 'explain') {
      <section class="tool-section" role="tabpanel">
        <h2 class="tool-title">💡 Explication simplifiée</h2>
        <p class="tool-desc">Entrez un concept et obtenez une explication claire avec exemples et analogies.</p>

        <div class="form-grid">
          <div class="form-field">
            <label>Concept à expliquer *</label>
            <input type="text" [(ngModel)]="explainConcept"
                   placeholder="ex: injection de dépendances, récursion, JWT…"
                   maxlength="2000"/>
          </div>
          <div class="form-field">
            <label>Niveau</label>
            <select [(ngModel)]="explainLevel">
              <option value="débutant">Débutant</option>
              <option value="intermédiaire">Intermédiaire</option>
              <option value="avancé">Avancé</option>
            </select>
          </div>
          <div class="form-field full-width">
            <label>Contexte source (optionnel)</label>
            <textarea [(ngModel)]="explainContext" rows="3"
                      placeholder="Collez ici un extrait de cours pour contextualiser l'explication…"
                      maxlength="3000"></textarea>
          </div>
        </div>

        <button class="btn-primary" (click)="runExplain()"
                [disabled]="explainLoading() || !explainConcept.trim()">
          {{ explainLoading() ? '⏳ Génération…' : '✨ Expliquer' }}
        </button>

        @if (explainResult()) {
          <div class="result-card">
            <div class="result-section">
              <h3>📖 Explication</h3>
              <p>{{ explainResult()!.explanation }}</p>
            </div>
            <div class="result-section">
              <h3>🔬 Exemple concret</h3>
              <p>{{ explainResult()!.example }}</p>
            </div>
            <div class="result-section">
              <h3>🔗 Analogie</h3>
              <p class="analogy-text">{{ explainResult()!.analogy }}</p>
            </div>
            <div class="result-section highlight">
              <h3>💎 À retenir</h3>
              <p>{{ explainResult()!.keyTakeaway }}</p>
            </div>
            <button class="btn-secondary" (click)="sendExplainToChat()">
              💬 Continuer dans le chat
            </button>
          </div>
        }
      </section>
    }

    <!-- ─── TAB: SUMMARIZE ─── -->
    @if (activeTab() === 'summarize') {
      <section class="tool-section" role="tabpanel">
        <h2 class="tool-title">📝 Résumé automatique</h2>
        <p class="tool-desc">Collez un texte de cours et obtenez un résumé structuré avec les points clés.</p>

        <div class="form-grid">
          <div class="form-field full-width">
            <label>Texte à résumer * <span class="char-count">{{ summarizeText.length }}/16000</span></label>
            <textarea [(ngModel)]="summarizeText" rows="8"
                      placeholder="Collez ici le contenu de votre cours, chapitre ou document…"
                      maxlength="16000"></textarea>
          </div>
          <div class="form-field">
            <label>Langue de sortie</label>
            <select [(ngModel)]="summarizeLang">
              <option value="fr">Français</option>
              <option value="en">English</option>
              <option value="ar">العربية</option>
            </select>
          </div>
        </div>

        <button class="btn-primary" (click)="runSummarize()"
                [disabled]="summarizeLoading() || !summarizeText.trim()">
          {{ summarizeLoading() ? '⏳ Résumé en cours…' : '📝 Générer le résumé' }}
        </button>

        @if (summarizeResult()) {
          <div class="result-card">
            <div class="result-section">
              <div class="result-meta">⏱ {{ summarizeResult()!.estimatedReadTime }}</div>
              <h3>📄 Résumé</h3>
              <p>{{ summarizeResult()!.summary }}</p>
            </div>
            <div class="result-section">
              <h3>🔑 Points clés</h3>
              <ul class="key-points">
                @for (p of summarizeResult()!.keyPoints; track p) {
                  <li>{{ p }}</li>
                }
              </ul>
            </div>
          </div>
        }
      </section>
    }

    <!-- ─── TAB: MIND MAP ─── -->
    @if (activeTab() === 'mindmap') {
      <section class="tool-section" role="tabpanel">
        <h2 class="tool-title">🗺 Carte mentale</h2>
        <p class="tool-desc">Entrez un sujet et obtenez une carte mentale structurée et interactive.</p>

        <div class="form-grid">
          <div class="form-field">
            <label>Sujet *</label>
            <input type="text" [(ngModel)]="mindmapTopic"
                   placeholder="ex: Architecture MVC, Bases de données relationnelles…"
                   maxlength="200"/>
          </div>
          <div class="form-field">
            <label>Langue</label>
            <select [(ngModel)]="mindmapLang">
              <option value="fr">Français</option>
              <option value="en">English</option>
              <option value="ar">العربية</option>
            </select>
          </div>
          <div class="form-field full-width">
            <label>Contexte source (optionnel)</label>
            <textarea [(ngModel)]="mindmapContext" rows="3"
                      placeholder="Collez ici un extrait de cours pour orienter la carte…"
                      maxlength="3000"></textarea>
          </div>
        </div>

        <button class="btn-primary" (click)="runMindMap()"
                [disabled]="mindmapLoading() || !mindmapTopic.trim()">
          {{ mindmapLoading() ? '⏳ Génération…' : '🗺 Générer la carte' }}
        </button>

        @if (mindmapResult()) {
          <div class="mindmap-container">
            <div class="mindmap-title">🗺 {{ mindmapResult()!.topic }}</div>
            <div class="mindmap-tree">
              <!-- Root -->
              <div class="mm-root">
                <div class="mm-node root-node">
                  <span class="mm-label">{{ mindmapResult()!.root.label }}</span>
                  @if (mindmapResult()!.root.description) {
                    <span class="mm-desc">{{ mindmapResult()!.root.description }}</span>
                  }
                </div>
                <!-- Level 1 -->
                <div class="mm-children">
                  @for (branch of mindmapResult()!.root.children; track branch.label; let bi = $index) {
                    <div class="mm-branch">
                      <div class="mm-node branch-node" [class]="'branch-color-' + (bi % 6)">
                        <span class="mm-label">{{ branch.label }}</span>
                        @if (branch.description) {
                          <span class="mm-desc">{{ branch.description }}</span>
                        }
                      </div>
                      <!-- Level 2 -->
                      @if (branch.children && branch.children.length) {
                        <div class="mm-leaves">
                          @for (leaf of branch.children; track leaf.label) {
                            <div class="mm-node leaf-node">
                              <span class="mm-label">{{ leaf.label }}</span>
                              @if (leaf.description) {
                                <span class="mm-desc">{{ leaf.description }}</span>
                              }
                            </div>
                          }
                        </div>
                      }
                    </div>
                  }
                </div>
              </div>
            </div>
            <button class="btn-secondary" (click)="sendMindMapToChat()">
              💬 Approfondir dans le chat
            </button>
          </div>
        }
      </section>
    }

  </main>
</div>
  `,
  styles: [`
    /* ── Layout ── */
    .tutor-layout { display: flex; height: calc(100vh - 64px); overflow: hidden; background: #f8faff; }

    /* ── Sidebar ── */
    .tutor-sidebar { width: 280px; background: #fff; border-right: 1px solid #e2e8f0; display: flex; flex-direction: column; flex-shrink: 0; overflow: hidden; transition: width .2s; }
    .tutor-sidebar:not(.open) { width: 0; }
    .sidebar-head { display: flex; align-items: center; justify-content: space-between; padding: 16px; border-bottom: 1px solid #e2e8f0; }
    .sidebar-title { font-weight: 600; font-size: .95rem; white-space: nowrap; }
    .sidebar-conversations { flex: 1; overflow-y: auto; padding: 8px; }
    .sidebar-hint { color: #94a3b8; font-size: .85rem; text-align: center; padding: 24px 8px; }
    .conv-item { position: relative; width: 100%; text-align: left; background: none; border: none; border-radius: 8px; padding: 10px 36px 10px 12px; cursor: pointer; margin-bottom: 4px; transition: background .15s; }
    .conv-item:hover, .conv-item.active { background: #f0f4ff; }
    .conv-item.active { border-left: 3px solid #6366f1; }
    .conv-title { display: block; font-size: .87rem; font-weight: 500; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #1e293b; }
    .conv-meta { display: block; font-size: .75rem; color: #94a3b8; margin-top: 2px; }
    .conv-delete { position: absolute; right: 8px; top: 50%; transform: translateY(-50%); background: none; border: none; cursor: pointer; color: #cbd5e1; font-size: .8rem; padding: 2px 4px; border-radius: 4px; }
    .conv-delete:hover { color: #ef4444; background: #fee2e2; }

    /* ── Main ── */
    .tutor-main { flex: 1; display: flex; flex-direction: column; overflow: hidden; }

    /* ── Header ── */
    .tutor-header { display: flex; align-items: center; gap: 12px; padding: 12px 20px; background: #fff; border-bottom: 1px solid #e2e8f0; flex-shrink: 0; }
    .tutor-logo { font-size: 1.1rem; font-weight: 700; color: #1e293b; margin: 0; flex: 1; }
    .tutor-logo-sub { color: #6366f1; font-weight: 400; }
    .badge-warn { background: #fef3c7; color: #92400e; font-size: .78rem; padding: 4px 10px; border-radius: 20px; border: 1px solid #fde68a; }

    /* ── Tabs ── */
    .tutor-tabs { display: flex; gap: 4px; padding: 8px 16px; background: #fff; border-bottom: 1px solid #e2e8f0; flex-shrink: 0; }
    .tab-btn { padding: 8px 16px; border: none; background: none; border-radius: 8px; cursor: pointer; font-size: .9rem; color: #64748b; font-weight: 500; transition: all .15s; }
    .tab-btn:hover { background: #f1f5f9; color: #1e293b; }
    .tab-btn.active { background: #eef2ff; color: #6366f1; font-weight: 600; }

    /* ── Chat ── */
    .chat-section { flex: 1; display: flex; flex-direction: column; overflow: hidden; }
    .chat-context-bar { display: flex; align-items: center; gap: 10px; padding: 8px 20px; background: #f8faff; border-bottom: 1px solid #e2e8f0; font-size: .85rem; flex-shrink: 0; }
    .chat-context-bar select { padding: 4px 8px; border: 1px solid #cbd5e1; border-radius: 6px; font-size: .85rem; background: #fff; }
    .chat-messages { flex: 1; overflow-y: auto; padding: 20px; display: flex; flex-direction: column; gap: 16px; }
    .chat-welcome { max-width: 560px; margin: 40px auto; text-align: center; }
    .welcome-icon { font-size: 3rem; margin-bottom: 12px; }
    .chat-welcome h2 { font-size: 1.25rem; color: #1e293b; margin-bottom: 8px; }
    .chat-welcome p { color: #64748b; font-size: .9rem; }
    .welcome-suggestions { display: flex; flex-wrap: wrap; gap: 8px; justify-content: center; margin-top: 20px; }
    .suggestion-chip { padding: 8px 14px; background: #eef2ff; border: 1px solid #c7d2fe; border-radius: 20px; cursor: pointer; font-size: .82rem; color: #6366f1; transition: all .15s; }
    .suggestion-chip:hover { background: #e0e7ff; }
    .chat-msg { display: flex; gap: 10px; align-items: flex-end; }
    .chat-msg.user { flex-direction: row-reverse; }
    .msg-avatar { font-size: 1.4rem; flex-shrink: 0; }
    .msg-bubble { max-width: 72%; background: #fff; border: 1px solid #e2e8f0; border-radius: 16px; padding: 12px 16px; box-shadow: 0 1px 3px rgba(0,0,0,.06); }
    .chat-msg.user .msg-bubble { background: #6366f1; border-color: #6366f1; color: #fff; }
    .msg-content { font-size: .9rem; line-height: 1.6; white-space: pre-wrap; word-break: break-word; }
    .msg-time { font-size: .72rem; color: #94a3b8; margin-top: 4px; text-align: right; }
    .chat-msg.user .msg-time { color: #c7d2fe; }
    .typing-indicator { display: flex; gap: 5px; align-items: center; padding: 14px 18px; }
    .typing-indicator span { width: 8px; height: 8px; background: #94a3b8; border-radius: 50%; animation: bounce .9s infinite; }
    .typing-indicator span:nth-child(2) { animation-delay: .2s; }
    .typing-indicator span:nth-child(3) { animation-delay: .4s; }
    @keyframes bounce { 0%,60%,100% { transform: translateY(0); } 30% { transform: translateY(-6px); } }
    .chat-input-bar { display: flex; gap: 8px; padding: 12px 20px; background: #fff; border-top: 1px solid #e2e8f0; flex-shrink: 0; }
    .chat-textarea { flex: 1; resize: none; border: 1px solid #cbd5e1; border-radius: 12px; padding: 10px 14px; font-size: .9rem; font-family: inherit; transition: border-color .15s; }
    .chat-textarea:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 3px #eef2ff; }
    .btn-send { width: 46px; height: 46px; border-radius: 12px; background: #6366f1; color: #fff; border: none; cursor: pointer; font-size: 1.2rem; transition: background .15s; flex-shrink: 0; }
    .btn-send:hover:not(:disabled) { background: #4f46e5; }
    .btn-send:disabled { background: #c7d2fe; cursor: not-allowed; }
    .input-hint { font-size: .75rem; color: #94a3b8; text-align: right; padding: 0 20px 8px; margin: 0; }

    /* ── Tool sections ── */
    .tool-section { flex: 1; overflow-y: auto; padding: 28px; }
    .tool-title { font-size: 1.3rem; font-weight: 700; color: #1e293b; margin: 0 0 6px; }
    .tool-desc { color: #64748b; font-size: .9rem; margin: 0 0 24px; }
    .form-grid { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; margin-bottom: 20px; }
    .form-field { display: flex; flex-direction: column; gap: 6px; }
    .form-field.full-width { grid-column: 1 / -1; }
    .form-field label { font-size: .85rem; font-weight: 600; color: #374151; }
    .form-field input, .form-field select, .form-field textarea { padding: 9px 12px; border: 1px solid #d1d5db; border-radius: 8px; font-size: .9rem; font-family: inherit; transition: border-color .15s; }
    .form-field input:focus, .form-field select:focus, .form-field textarea:focus { outline: none; border-color: #6366f1; box-shadow: 0 0 0 3px #eef2ff; }
    .char-count { font-weight: 400; color: #94a3b8; font-size: .78rem; }
    .btn-primary { padding: 10px 24px; background: #6366f1; color: #fff; border: none; border-radius: 10px; font-size: .9rem; font-weight: 600; cursor: pointer; transition: background .15s; }
    .btn-primary:hover:not(:disabled) { background: #4f46e5; }
    .btn-primary:disabled { background: #c7d2fe; cursor: not-allowed; }
    .btn-secondary { padding: 8px 18px; background: #f1f5f9; color: #6366f1; border: 1px solid #e2e8f0; border-radius: 8px; font-size: .85rem; font-weight: 500; cursor: pointer; transition: all .15s; margin-top: 12px; }
    .btn-secondary:hover { background: #eef2ff; }
    .btn-icon { background: none; border: none; cursor: pointer; display: inline-flex; align-items: center; justify-content: center; width: 34px; height: 34px; border-radius: 8px; color: #64748b; transition: background .15s; }
    .btn-icon:hover { background: #f1f5f9; color: #1e293b; }

    /* ── Result card ── */
    .result-card { background: #fff; border: 1px solid #e2e8f0; border-radius: 16px; padding: 24px; margin-top: 20px; box-shadow: 0 2px 8px rgba(0,0,0,.06); }
    .result-section { margin-bottom: 20px; padding-bottom: 20px; border-bottom: 1px solid #f1f5f9; }
    .result-section:last-of-type { border-bottom: none; margin-bottom: 0; padding-bottom: 0; }
    .result-section h3 { font-size: .95rem; font-weight: 700; color: #1e293b; margin: 0 0 8px; }
    .result-section p { font-size: .9rem; color: #374151; line-height: 1.7; margin: 0; }
    .result-section.highlight { background: #fafaf5; border-radius: 10px; padding: 16px; margin: 0; border: 1px solid #fde68a; }
    .analogy-text { font-style: italic; color: #6366f1 !important; }
    .result-meta { font-size: .78rem; color: #94a3b8; margin-bottom: 4px; }
    .key-points { list-style: none; padding: 0; margin: 0; display: flex; flex-direction: column; gap: 8px; }
    .key-points li { display: flex; gap: 8px; font-size: .9rem; color: #374151; }
    .key-points li::before { content: "→"; color: #6366f1; flex-shrink: 0; }

    /* ── Mind Map ── */
    .mindmap-container { margin-top: 20px; }
    .mindmap-title { font-size: 1rem; font-weight: 600; color: #64748b; margin-bottom: 16px; }
    .mindmap-tree { background: #fff; border: 1px solid #e2e8f0; border-radius: 16px; padding: 24px; overflow-x: auto; }
    .mm-root { display: flex; flex-direction: column; align-items: center; gap: 24px; }
    .mm-node { background: #fff; border: 2px solid #e2e8f0; border-radius: 12px; padding: 12px 16px; min-width: 120px; max-width: 220px; cursor: default; text-align: center; transition: box-shadow .2s; }
    .mm-node:hover { box-shadow: 0 4px 12px rgba(0,0,0,.1); }
    .root-node { background: #6366f1; border-color: #6366f1; color: #fff; font-weight: 700; font-size: 1rem; min-width: 180px; }
    .root-node .mm-desc { color: #c7d2fe; }
    .mm-label { display: block; font-weight: 600; font-size: .88rem; color: inherit; }
    .mm-desc { display: block; font-size: .75rem; color: #94a3b8; margin-top: 4px; font-weight: 400; }
    .mm-children { display: flex; flex-wrap: wrap; gap: 16px; justify-content: center; }
    .mm-branch { display: flex; flex-direction: column; align-items: center; gap: 10px; }
    .branch-node { color: #fff; font-weight: 600; }
    .branch-color-0 { background: #6366f1; border-color: #6366f1; } .branch-color-0 .mm-desc { color: #c7d2fe; }
    .branch-color-1 { background: #0ea5e9; border-color: #0ea5e9; } .branch-color-1 .mm-desc { color: #bae6fd; }
    .branch-color-2 { background: #10b981; border-color: #10b981; } .branch-color-2 .mm-desc { color: #a7f3d0; }
    .branch-color-3 { background: #f59e0b; border-color: #f59e0b; } .branch-color-3 .mm-desc { color: #fde68a; }
    .branch-color-4 { background: #ef4444; border-color: #ef4444; } .branch-color-4 .mm-desc { color: #fecaca; }
    .branch-color-5 { background: #8b5cf6; border-color: #8b5cf6; } .branch-color-5 .mm-desc { color: #ddd6fe; }
    .mm-leaves { display: flex; flex-direction: column; gap: 8px; align-items: center; }
    .leaf-node { background: #f8faff; border-color: #e2e8f0; font-size: .82rem; min-width: 100px; }

    /* ── Responsive ── */
    @media (max-width: 768px) {
      .tutor-sidebar { position: absolute; z-index: 10; height: 100%; }
      .form-grid { grid-template-columns: 1fr; }
      .mm-children { gap: 10px; }
    }
  `]
})
export class TutorComponent implements OnInit {

  private tutor = inject(TutorService);
  private ui = inject(UiService);
  private formationService = inject(FormationService);

  // ── State ──────────────────────────────────────────────────────────────────
  activeTab = signal<Tab>('chat');
  sidebarOpen = signal(false);
  aiConfigured = signal(true);

  // Chat
  conversations = signal<ConversationSummary[]>([]);
  messages = signal<MessageDto[]>([]);
  activeConvId = signal<number | null>(null);
  chatMessage = '';
  chatLoading = signal(false);
  loadingHistory = signal(false);
  selectedFormationId: number | null = null;
  formations = signal<Formation[]>([]);

  // Explain
  explainConcept = '';
  explainLevel = 'intermédiaire';
  explainContext = '';
  explainLoading = signal(false);
  explainResult = signal<ExplainResponse | null>(null);

  // Summarize
  summarizeText = '';
  summarizeLang = 'fr';
  summarizeLoading = signal(false);
  summarizeResult = signal<SummarizeResponse | null>(null);

  // Mind map
  mindmapTopic = '';
  mindmapLang = 'fr';
  mindmapContext = '';
  mindmapLoading = signal(false);
  mindmapResult = signal<MindMapResponse | null>(null);

  @ViewChild('messagesContainer') private messagesContainer?: ElementRef<HTMLDivElement>;

  tabs = [
    { id: 'chat' as Tab,      icon: '💬', label: 'Chat'      },
    { id: 'explain' as Tab,   icon: '💡', label: 'Expliquer' },
    { id: 'summarize' as Tab, icon: '📝', label: 'Résumer'   },
    { id: 'mindmap' as Tab,   icon: '🗺', label: 'Mind Map'  },
  ];

  suggestions = [
    'Explique-moi la programmation orientée objet',
    'Quelle est la différence entre HTTP et HTTPS ?',
    'Comment fonctionne Spring Boot ?',
    "C'est quoi l'injection de dépendances ?",
  ];

  async ngOnInit() {
    await this.checkStatus();
    await this.loadHistory();
    await this.loadFormations();
  }

  // ── Status ──────────────────────────────────────────────────────────────────
  private async checkStatus() {
    try {
      const s = await this.tutor.status();
      this.aiConfigured.set(s.configured);
    } catch { this.aiConfigured.set(false); }
  }

  // ── Formations pour le RAG ──────────────────────────────────────────────────
  private async loadFormations() {
    try {
      const list = await firstValueFrom(this.formationService.getAll());
      this.formations.set(list);
    } catch { /* non bloquant */ }
  }

  onFormationChange() {
    // Quand on change de cours, on démarre une nouvelle conversation
    this.newConversation();
  }

  // ── History ──────────────────────────────────────────────────────────────────
  private async loadHistory() {
    this.loadingHistory.set(true);
    try {
      const list = await this.tutor.listConversations();
      this.conversations.set(list);
    } catch { /* silencieux */ } finally {
      this.loadingHistory.set(false);
    }
  }

  async loadConversation(id: number) {
    if (this.activeConvId() === id) return;
    try {
      const detail = await this.tutor.getConversation(id);
      this.messages.set(detail.messages);
      this.activeConvId.set(id);
      this.selectedFormationId = detail.formationId;
      this.sidebarOpen.set(false);
      this.activeTab.set('chat');
      setTimeout(() => this.scrollToBottom(), 50);
    } catch {
      this.ui.notify('Impossible de charger la conversation.', 'error');
    }
  }

  newConversation() {
    this.messages.set([]);
    this.activeConvId.set(null);
  }

  async deleteConversation(id: number, event: Event) {
    event.stopPropagation();
    try {
      await this.tutor.deleteConversation(id);
      this.conversations.update(list => list.filter(c => c.id !== id));
      if (this.activeConvId() === id) this.newConversation();
    } catch {
      this.ui.notify('Erreur lors de la suppression.', 'error');
    }
  }

  // ── Chat ─────────────────────────────────────────────────────────────────────
  async sendChat() {
    const msg = this.chatMessage.trim();
    if (!msg || this.chatLoading()) return;
    this.chatMessage = '';
    this.chatLoading.set(true);

    try {
      const res = await this.tutor.chat({
        message: msg,
        conversationId: this.activeConvId(),
        formationId: this.selectedFormationId
      });
      this.messages.update(m => [...m, res.userMessage, res.assistantMessage]);
      this.activeConvId.set(res.conversationId);
      // Mettre à jour la liste des conversations
      await this.loadHistory();
      setTimeout(() => this.scrollToBottom(), 50);
    } catch (e: any) {
      this.ui.notify(e?.error?.message || 'Erreur du chat IA.', 'error');
      this.chatMessage = msg; // remettre le message
    } finally {
      this.chatLoading.set(false);
    }
  }

  sendSuggestion(s: string) {
    this.chatMessage = s;
    this.sendChat();
  }

  private scrollToBottom() {
    const el = this.messagesContainer?.nativeElement;
    if (el) el.scrollTop = el.scrollHeight;
  }

  // ── Explain ──────────────────────────────────────────────────────────────────
  async runExplain() {
    if (!this.explainConcept.trim() || this.explainLoading()) return;
    this.explainLoading.set(true);
    this.explainResult.set(null);
    try {
      const res = await this.tutor.explain({
        concept: this.explainConcept.trim(),
        context: this.explainContext.trim() || undefined,
        level: this.explainLevel
      });
      this.explainResult.set(res);
    } catch (e: any) {
      this.ui.notify(e?.error?.message || 'Erreur lors de l\'explication.', 'error');
    } finally {
      this.explainLoading.set(false);
    }
  }

  sendExplainToChat() {
    if (!this.explainResult()) return;
    const r = this.explainResult()!;
    this.chatMessage = `Explique-moi davantage : ${this.explainConcept}`;
    this.activeTab.set('chat');
    this.sendChat();
  }

  // ── Summarize ────────────────────────────────────────────────────────────────
  async runSummarize() {
    if (!this.summarizeText.trim() || this.summarizeLoading()) return;
    this.summarizeLoading.set(true);
    this.summarizeResult.set(null);
    try {
      const res = await this.tutor.summarize({
        text: this.summarizeText.trim(),
        language: this.summarizeLang
      });
      this.summarizeResult.set(res);
    } catch (e: any) {
      this.ui.notify(e?.error?.message || 'Erreur lors du résumé.', 'error');
    } finally {
      this.summarizeLoading.set(false);
    }
  }

  // ── Mind Map ─────────────────────────────────────────────────────────────────
  async runMindMap() {
    if (!this.mindmapTopic.trim() || this.mindmapLoading()) return;
    this.mindmapLoading.set(true);
    this.mindmapResult.set(null);
    try {
      const res = await this.tutor.mindMap({
        topic: this.mindmapTopic.trim(),
        context: this.mindmapContext.trim() || undefined,
        language: this.mindmapLang
      });
      this.mindmapResult.set(res);
    } catch (e: any) {
      this.ui.notify(e?.error?.message || 'Erreur lors de la génération de la carte.', 'error');
    } finally {
      this.mindmapLoading.set(false);
    }
  }

  sendMindMapToChat() {
    if (!this.mindmapResult()) return;
    this.chatMessage = `Approfondir le sujet : ${this.mindmapResult()!.topic}`;
    this.activeTab.set('chat');
    this.sendChat();
  }

  // ── Helpers ──────────────────────────────────────────────────────────────────
  formatMessage(content: string): string {
    // Convertit les ** en gras, les ``` en code, les \n en <br>
    return content
      .replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;')
      .replace(/\*\*(.*?)\*\*/g, '<strong>$1</strong>')
      .replace(/`([^`]+)`/g, '<code>$1</code>')
      .replace(/\n/g, '<br>');
  }
}
