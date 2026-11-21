package io.riskshield.adapter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.concurrent.Semaphore;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Bounded per-instance admission; fraud velocity is a separate domain concept. */
@Component @Order(1)
public class IntakeLimits extends OncePerRequestFilter {
    private final Semaphore permits=new Semaphore(24);
    @Override protected void doFilterInternal(HttpServletRequest req,HttpServletResponse res,FilterChain chain) throws ServletException,IOException {
        if(req.getContentLengthLong()>32768) { SecurityConfiguration.problem(res,413,"Payload too large"); return; }
        if(!permits.tryAcquire()) { res.setHeader("Retry-After","1"); SecurityConfiguration.problem(res,429,"Admission limit reached"); return; }
        try {
            if("POST".equals(req.getMethod())) {
                byte[] body=req.getInputStream().readNBytes(32769);
                if(body.length>32768) { SecurityConfiguration.problem(res,413,"Payload too large");return; }
                HttpServletRequestWrapper bounded=new HttpServletRequestWrapper(req) {
                    @Override public ServletInputStream getInputStream() {
                        var in=new java.io.ByteArrayInputStream(body);
                        return new ServletInputStream() {
                            public int read() { return in.read(); }
                            public boolean isFinished() { return in.available()==0; }
                            public boolean isReady() { return true; }
                            public void setReadListener(ReadListener l) { throw new UnsupportedOperationException("synchronous intake only"); }
                        };
                    }
                };
                chain.doFilter(bounded,res);
            } else chain.doFilter(req,res);
        } finally { permits.release(); }
    }
}
