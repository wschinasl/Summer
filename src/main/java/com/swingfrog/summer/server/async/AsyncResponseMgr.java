package com.swingfrog.summer.server.async;

import com.alibaba.fastjson.JSON;
import com.swingfrog.summer.protocol.SessionRequest;
import com.swingfrog.summer.protocol.SessionResponse;
import com.swingfrog.summer.server.Server;
import com.swingfrog.summer.server.ServerConst;
import com.swingfrog.summer.server.ServerMgr;
import com.swingfrog.summer.server.ServerStringHandler;
import com.swingfrog.summer.server.SessionContext;
import com.swingfrog.summer.statistics.RemoteStatistics;
import com.swingfrog.summer.web.WebMgr;
import com.swingfrog.summer.web.WebRequest;
import com.swingfrog.summer.web.WebRequestHandler;
import com.swingfrog.summer.web.view.TextView;
import com.swingfrog.summer.web.view.WebView;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;

public class AsyncResponseMgr {

    private static Logger log = LoggerFactory.getLogger(AsyncResponseMgr.class);

    private static class SingleCase {
        public static final AsyncResponseMgr INSTANCE = new AsyncResponseMgr();
    }

    private AsyncResponseMgr() {

    }

    public static AsyncResponseMgr get() {
        return AsyncResponseMgr.SingleCase.INSTANCE;
    }

    public void sendResponse(SessionContext sctx, SessionRequest request, Object data) {
        Objects.requireNonNull(sctx);
        Objects.requireNonNull(request);
        Server server = ServerMgr.get().findServer(sctx);
        if (server == null) {
            log.error("Async send response failure. cause: can't found server by session context");
            return;
        }
        ChannelHandlerContext ctx = server.getServerContext().getSessionContextGroup().getChannelBySession(sctx);
        if (ServerConst.SERVER_PROTOCOL_HTTP.equals(server.getServerContext().getConfig().getProtocol())) {
            WebView webView;
            if (data == null) {
                webView = WebMgr.get().getInteriorViewFactory().createBlankView();
                log.debug("server async response {} status[{}] from {}", webView, webView.getStatus(), sctx);
                WebRequestHandler.write(ctx, sctx, (WebRequest) request, webView);
                return;
            }
            if (data instanceof WebView) {
                webView = (WebView) data;
            } else {
                webView = new TextView(JSON.toJSONString(data));
            }
            log.debug("server async response {} status[{}] from {}", webView, webView.getStatus(), sctx);
            try {
                webView.ready();
                WebRequestHandler.write(ctx, sctx, (WebRequest) request, webView);
            } catch (Exception e) {
                log.error(e.getMessage(), e);
            }
            RemoteStatistics.finish(request, 0);
        } else {
            String response = SessionResponse.buildMsg(request, data).toJSONString();
            log.debug("server async response {} to {}", response, sctx);
            ServerStringHandler.write(ctx, server.getServerContext(), sctx, response);
            RemoteStatistics.finish(request, response.length());
        }
    }

}
