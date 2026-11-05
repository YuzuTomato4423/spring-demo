package com.example.training.webmvc;

import java.net.URI;
import java.net.http.*;
import java.nio.charset.StandardCharsets;
import tools.jackson.databind.ObjectMapper;

/** SPR-W6-D2: 実HTTPの結果をEclipse Consoleで再現する学習用クライアント。 */
public class Week6Day2HttpClient {
    public static void main(String[] args) throws Exception {
        String fetch=args.length==0?"plain":args[0];
        String url="http://localhost:8080/training/departments?fetch="+fetch;
        System.out.println("[SPR-W6-D2] GET "+url);
        var result=HttpClient.newHttpClient().send(HttpRequest.newBuilder(URI.create(url)).GET().build(),
            HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        System.out.println("HTTP "+result.statusCode());
        for (var header:java.util.List.of("X-Training-After-Load-Selects","X-Training-Total-Selects","X-Training-Initially-Loaded"))
            System.out.println(header+": "+result.headers().firstValue(header).orElse("missing"));
        var mapper=new ObjectMapper();
        System.out.println(mapper.writerWithDefaultPrettyPrinter().writeValueAsString(mapper.readTree(result.body())));
        if(result.statusCode()!=200) throw new IllegalStateException("HTTP failed");
    }
}
