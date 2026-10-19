package com.smartlearn.tutor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import java.net.URI;
import java.net.http.*;
import java.nio.file.*;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.Semaphore;
import static org.springframework.http.HttpStatus.*;

/** Groq/OpenAI Chat Completions transport with independent tutor credentials. */
@Service
public class TutorAiClient {
    private final ObjectMapper mapper;
    private final String key, keyFile, model, url;
    private final HttpClient http=HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
    private final Semaphore slots=new Semaphore(2);
    public TutorAiClient(ObjectMapper mapper,
        @Value("${smartlearn.tutor.ai.api-key:}") String key,
        @Value("${smartlearn.tutor.ai.key-file:.local/groq-api-key.txt}") String keyFile,
        @Value("${smartlearn.tutor.ai.model:qwen/qwen3.8-27b}") String model,
        @Value("${smartlearn.tutor.ai.url:https://api.groq.com/openai/v1/chat/completions}") String url) {
        this.mapper=mapper; this.key=key; this.keyFile=keyFile; this.model=model; this.url=url;
    }
    private String apiKey() {
        if(!key.isBlank()) return key.strip();
        try { return Files.readString(Path.of(keyFile)).strip(); } catch(Exception e) { return ""; }
    }
    public boolean configured() { return !apiKey().isBlank(); }
    public String model() { return model; }
    public String provider() { return URI.create(url).getHost().equals("api.groq.com") ? "Groq" : "OpenAI-compatible"; }
    public String chat(String systemPrompt,String userMessage) { return request(systemPrompt,List.of(),userMessage,false); }
    public String chatJson(String systemPrompt,String userMessage) { return request(systemPrompt,List.of(),userMessage,true); }
    public String chatMultiTurn(String systemPrompt,List<Map<String,String>> history,String userMessage) {
        return request(systemPrompt,history,userMessage,false);
    }
    private String request(String system,List<Map<String,String>> history,String message,boolean json) {
        String credential=apiKey();
        if(credential.isBlank()) throw new ResponseStatusException(SERVICE_UNAVAILABLE,
            "Configurez la clé Groq du tuteur dans .local/groq-api-key.txt, puis actualisez le statut.");
        if(!slots.tryAcquire()) throw new ResponseStatusException(TOO_MANY_REQUESTS,"Le tuteur traite déjà deux demandes. Réessayez dans un instant.");
        try {
            var body=mapper.createObjectNode().put("model",model).put("temperature",0.4).put("max_completion_tokens",json?4096:3000);
            if(model.startsWith("qwen/") || model.equals("gpt-6-luna")) body.put("reasoning_effort","none");
            if(json) body.set("response_format",mapper.createObjectNode().put("type","json_object"));
            var messages=mapper.createArrayNode();
            messages.add(mapper.createObjectNode().put("role","system").put("content",system));
            for(var item:history) messages.add(mapper.createObjectNode().put("role",item.get("role")).put("content",item.get("content")));
            messages.add(mapper.createObjectNode().put("role","user").put("content",message)); body.set("messages",messages);
            var request=HttpRequest.newBuilder(URI.create(url)).timeout(Duration.ofSeconds(60))
                .header("Authorization","Bearer "+credential).header("Content-Type","application/json")
                .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            var response=http.send(request,HttpResponse.BodyHandlers.ofString());
            int status=response.statusCode();
            if(status==401 || status==403) throw new ResponseStatusException(SERVICE_UNAVAILABLE,"La clé ou les autorisations du fournisseur du tuteur sont invalides. Vérifiez sa configuration.");
            if(status==429) throw new ResponseStatusException(TOO_MANY_REQUESTS,"Limite du fournisseur IA atteinte. Attendez puis réessayez.");
            if(status!=200) throw new ResponseStatusException(BAD_GATEWAY,"Le fournisseur du tuteur a refusé la demande. Vérifiez le modèle et réessayez.");
            var choice=mapper.readTree(response.body()).path("choices").path(0);
            if(!"stop".equals(choice.path("finish_reason").asText())) throw new ResponseStatusException(BAD_GATEWAY,"La réponse du tuteur est incomplète. Réessayez avec un sujet plus précis.");
            var content=choice.path("message").path("content");
            if(!content.isTextual() || content.asText().isBlank() || content.asText().length()>20000)
                throw new ResponseStatusException(BAD_GATEWAY,"Le tuteur n'a pas renvoyé de réponse exploitable.");
            return content.asText().strip();
        } catch(ResponseStatusException e) { throw e;
        } catch(HttpTimeoutException e) { throw new ResponseStatusException(GATEWAY_TIMEOUT,"Le tuteur met trop de temps à répondre. Réessayez.");
        } catch(InterruptedException e) { Thread.currentThread().interrupt(); throw new ResponseStatusException(SERVICE_UNAVAILABLE,"Demande interrompue.");
        } catch(Exception e) { throw new ResponseStatusException(BAD_GATEWAY,"Connexion au fournisseur du tuteur impossible. Réessayez.");
        } finally { slots.release(); }
    }
}
