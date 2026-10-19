import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { firstValueFrom } from 'rxjs';

// ─── Models ──────────────────────────────────────────────────────────────────

export interface ChatRequest {
  message: string;
  conversationId?: number | null;
  formationId?: number | null;
}

export interface MessageDto {
  id: number;
  role: 'user' | 'assistant';
  content: string;
  createdAt: string;
}

export interface ChatResponse {
  conversationId: number;
  title: string;
  userMessage: MessageDto;
  assistantMessage: MessageDto;
}

export interface ConversationSummary {
  id: number;
  title: string;
  formationId: number | null;
  messageCount: number;
  createdAt: string;
  updatedAt: string;
}

export interface ConversationDetail {
  id: number;
  title: string;
  formationId: number | null;
  messages: MessageDto[];
  createdAt: string;
  updatedAt: string;
}

export interface ExplainRequest {
  concept: string;
  context?: string;
  level?: string;
}

export interface ExplainResponse {
  explanation: string;
  example: string;
  analogy: string;
  keyTakeaway: string;
}

export interface SummarizeRequest {
  text: string;
  language?: string;
}

export interface SummarizeResponse {
  summary: string;
  keyPoints: string[];
  estimatedReadTime: string;
}

export interface MindMapRequest {
  topic: string;
  context?: string;
  language?: string;
}

export interface MindMapNode {
  label: string;
  description: string;
  children: MindMapNode[];
}

export interface MindMapResponse {
  topic: string;
  root: MindMapNode;
  language: string;
}

export interface TutorStatus {
  configured: boolean;
  model: string;
  features: string[];
}

// ─── Service ─────────────────────────────────────────────────────────────────

@Injectable({ providedIn: 'root' })
export class TutorService {
  private base = '/api/tutor';

  constructor(private http: HttpClient) {}

  status(): Promise<TutorStatus> {
    return firstValueFrom(this.http.get<TutorStatus>(`${this.base}/status`));
  }

  chat(req: ChatRequest): Promise<ChatResponse> {
    return firstValueFrom(this.http.post<ChatResponse>(`${this.base}/chat`, req));
  }

  explain(req: ExplainRequest): Promise<ExplainResponse> {
    return firstValueFrom(this.http.post<ExplainResponse>(`${this.base}/explain`, req));
  }

  summarize(req: SummarizeRequest): Promise<SummarizeResponse> {
    return firstValueFrom(this.http.post<SummarizeResponse>(`${this.base}/summarize`, req));
  }

  mindMap(req: MindMapRequest): Promise<MindMapResponse> {
    return firstValueFrom(this.http.post<MindMapResponse>(`${this.base}/mindmap`, req));
  }

  listConversations(): Promise<ConversationSummary[]> {
    return firstValueFrom(this.http.get<ConversationSummary[]>(`${this.base}/conversations`));
  }

  getConversation(id: number): Promise<ConversationDetail> {
    return firstValueFrom(this.http.get<ConversationDetail>(`${this.base}/conversations/${id}`));
  }

  deleteConversation(id: number): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`${this.base}/conversations/${id}`));
  }
}
