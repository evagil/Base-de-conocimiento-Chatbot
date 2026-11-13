package com.municipio.base_conocimiento.rag.config;

import com.municipio.base_conocimiento.compartido.web.ErrorResponse;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Map;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

/** Acota también JSON fragmentado antes de deserializarlo; se registra después de seguridad. */
@Component @Order(Ordered.LOWEST_PRECEDENCE-10)
public class FiltroConsultaRag extends OncePerRequestFilter {
    public static final int MAX_BYTES=16384;
    private final ObjectMapper json;
    public FiltroConsultaRag(ObjectMapper json){this.json=json;}
    @Override protected boolean shouldNotFilter(HttpServletRequest request){return !request.getMethod().equals("POST")
            || !request.getRequestURI().substring(request.getContextPath().length()).startsWith("/api/rag/");}
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException{
        if(request.getContentLengthLong()>MAX_BYTES){grande(response);return;}
        byte[] cuerpo=request.getInputStream().readNBytes(MAX_BYTES+1);
        if(cuerpo.length>MAX_BYTES){grande(response);return;}
        chain.doFilter(new HttpServletRequestWrapper(request){
            @Override public int getContentLength(){return cuerpo.length;}
            @Override public long getContentLengthLong(){return cuerpo.length;}
            @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),StandardCharsets.UTF_8));}
            @Override public ServletInputStream getInputStream(){
                var entrada=new ByteArrayInputStream(cuerpo);
                return new ServletInputStream(){
                    @Override public int read(){return entrada.read();}
                    @Override public int read(byte[] bytes,int inicio,int cantidad){return entrada.read(bytes,inicio,cantidad);}
                    @Override public boolean isFinished(){return entrada.available()==0;}
                    @Override public boolean isReady(){return true;}
                    @Override public void setReadListener(ReadListener listener){throw new IllegalStateException("La consulta RAG usa lectura síncrona.");}
                };
            }
        },response);
    }
    private void grande(HttpServletResponse response)throws IOException{
        response.setStatus(413);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");response.setHeader("Cache-Control","no-store");
        response.getOutputStream().write(json.writeValueAsBytes(new ErrorResponse("RAG_CONSULTA_DEMASIADO_GRANDE","La consulta supera el límite de 16 KiB.",Map.of(),Instant.now())));
    }
}
