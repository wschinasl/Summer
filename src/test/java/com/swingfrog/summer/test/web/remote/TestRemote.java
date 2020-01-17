package com.swingfrog.summer.test.web.remote;

import com.swingfrog.summer.annotation.Autowired;
import com.swingfrog.summer.annotation.Optional;
import com.swingfrog.summer.annotation.Remote;
import com.swingfrog.summer.protocol.SessionRequest;
import com.swingfrog.summer.server.SessionContext;
import com.swingfrog.summer.server.async.AsyncResponse;
import com.swingfrog.summer.test.web.service.TestService;
import com.swingfrog.summer.web.WebFileUpload;
import com.swingfrog.summer.web.view.ModelView;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Remote
public class TestRemote {

    @Autowired
    private TestService testService;

    private static final ExecutorService executor = Executors.newSingleThreadExecutor();

    public String hello() {
        return "hello world!";
    }

    public int add(int a, @Optional Integer b) {
        return testService.add(a, b == null ? 0 : b);
    }

    public String say(SessionContext sctx, String msg) {
        return String.format("sessionId[%s]: %s %s", sctx.getSessionId(), msg,
                sctx.getSessionId() == null ? "tips: The first time you access this interface, the sessionId is null because the browser has no cookies" : "");
    }

    public ModelView template(String msg) {
        ModelView modelView = new ModelView("msg.html");
        modelView.put("msg", msg);
        return modelView;
    }

    public ModelView uploadFile(WebFileUpload upload) {
        ModelView modelView = new ModelView("msg.html");
        modelView.put("msg", upload.getFileName());
        return modelView;
    }

    public AsyncResponse asyncHello(SessionContext sctx, SessionRequest request) {
        executor.execute(() -> sctx.send(request, "hello async world!"));
        return AsyncResponse.of();
    }

}
