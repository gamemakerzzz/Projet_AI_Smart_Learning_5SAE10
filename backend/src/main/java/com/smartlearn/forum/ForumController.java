package com.smartlearn.forum;

import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import java.util.Map;
import static com.smartlearn.forum.ForumModels.*;

@RestController
@RequestMapping("/api/forum")
public class ForumController {
    private final ForumService forum; private final ForumAiService analysis; private final ForumAiClient ai; private final ForumInsightsService insights;
    public ForumController(ForumService forum,ForumAiService analysis,ForumAiClient ai,ForumInsightsService insights) { this.forum=forum; this.analysis=analysis; this.ai=ai; this.insights=insights; }
    @GetMapping("/discussions") public Topics list(@RequestParam(required=false) Long formationId,@RequestParam(defaultValue="all") String state,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="0") int page) { return forum.list(formationId,state,search,page); }
    @GetMapping("/discussions/{id}") public Detail detail(@PathVariable Long id) { return forum.detail(id); }
    @PostMapping("/discussions") @ResponseStatus(HttpStatus.CREATED) public Detail create(@Valid @RequestBody Draft draft,Authentication auth) { return forum.create(draft,auth); }
    @PutMapping("/discussions/{id}") public Detail update(@PathVariable Long id,@Valid @RequestBody Draft draft,Authentication auth) { return forum.update(id,draft,auth); }
    @DeleteMapping("/discussions/{id}") @ResponseStatus(HttpStatus.NO_CONTENT) public void delete(@PathVariable Long id,Authentication auth) { forum.delete(id,auth); }
    @PostMapping("/discussions/{id}/replies") @ResponseStatus(HttpStatus.CREATED) public Detail reply(@PathVariable Long id,@Valid @RequestBody ReplyDraft draft,Authentication auth) { return forum.reply(id,draft,auth); }
    @PutMapping("/discussions/{id}/replies/{replyId}") public Detail editReply(@PathVariable Long id,@PathVariable Long replyId,@Valid @RequestBody ReplyDraft draft,Authentication auth) { return forum.editReply(id,replyId,draft,auth); }
    @DeleteMapping("/discussions/{id}/replies/{replyId}") public Detail deleteReply(@PathVariable Long id,@PathVariable Long replyId,Authentication auth) { return forum.deleteReply(id,replyId,auth); }
    @PutMapping("/discussions/{id}/accepted-reply") public Detail accept(@PathVariable Long id,@Valid @RequestBody Acceptance body,Authentication auth) { return forum.accept(id,body.replyId(),auth); }
    @GetMapping("/ai/status") public Map<String,Object> status() { return Map.of("configured",ai.configured(),"model",ai.model(),"promptVersion",ForumAiClient.VERSION,"candidateLimit",ForumAiService.LIMIT,
        "insightsPromptVersion",ForumInsightsService.VERSION,"features",java.util.List.of("duplicate-detection","discussion-summary","topic-tags","learning-links")); }
    @PostMapping("/ai/check") public Analysis check(@Valid @RequestBody Draft draft,@RequestParam(required=false) Long excludeId,Authentication auth) { return analysis.analyze(draft,excludeId,forum.actor(auth).getId()); }
    @PostMapping("/ai/tags") public ForumInsightModels.Insight<ForumInsightModels.TagSuggestions> tags(@Valid @RequestBody Draft draft,Authentication auth) { return insights.tags(draft,forum.actor(auth).getId()); }
    @PostMapping("/discussions/{id}/ai/summary") public ForumInsightModels.Insight<ForumInsightModels.ThreadSummary> summary(@PathVariable Long id,Authentication auth) { return insights.summary(id,forum.actor(auth).getId()); }
    @PostMapping("/discussions/{id}/ai/learning") public ForumInsightModels.Insight<ForumInsightModels.LearningSuggestions> learning(@PathVariable Long id,Authentication auth) { return insights.learning(id,forum.actor(auth).getId()); }
}
