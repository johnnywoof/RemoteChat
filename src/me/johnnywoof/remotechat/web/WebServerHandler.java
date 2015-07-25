package me.johnnywoof.remotechat.web;

import com.google.common.io.BaseEncoding;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.*;
import io.netty.handler.codec.http.websocketx.*;
import io.netty.util.CharsetUtil;
import me.johnnywoof.remotechat.Settings;

import java.util.concurrent.CopyOnWriteArraySet;
import java.util.regex.Pattern;

@ChannelHandler.Sharable
public class WebServerHandler extends SimpleChannelInboundHandler<Object> {

	private final CopyOnWriteArraySet<ChannelHandlerContext> connectedClients = new CopyOnWriteArraySet<>();

	private static final String WEBSOCKET_PATH = "/websocket";

	private WebSocketServerHandshaker handshaker;

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, Object msg) throws Exception {
		if (msg instanceof FullHttpRequest) {
			handleHttpRequest(ctx, (FullHttpRequest) msg);
		} else if (msg instanceof WebSocketFrame) {
			handleWebSocketFrame(ctx, (WebSocketFrame) msg);
		}
	}

	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}

	public void broadcastChatMessage(String username, String formattedMessage) {

		String sentMessage = username + ":" + formattedMessage;

		for (ChannelHandlerContext ctx : this.connectedClients) {

			ctx.writeAndFlush(new TextWebSocketFrame(sentMessage));

		}

	}

	@Override
	public void channelInactive(ChannelHandlerContext ctx) throws Exception {

		this.connectedClients.remove(ctx);

	}

	private void handleHttpRequest(ChannelHandlerContext ctx, FullHttpRequest req) {
		// Handle a bad request.
		if (!req.getDecoderResult().isSuccess()) {
			sendHttpResponse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.BAD_REQUEST));
			return;
		}

		// Allow only GET methods.
		if (req.getMethod() != HttpMethod.GET) {
			sendHttpResponse(ctx, new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.FORBIDDEN));
			return;
		}

		//Are we authenticated?
		if (Settings.httpRequiresAuth) {

			String authData = req.headers().get("Authorization");

			if (authData != null && authData.startsWith("Basic ")) {

				String encodedLoginData = authData.split(Pattern.quote(" "))[1];

				String[] loginData = new String(BaseEncoding.base64Url().
						decode(encodedLoginData), CharsetUtil.UTF_8).split(Pattern.quote(":"));

				if (loginData.length != 2 || !Settings.httpUsername.equals(loginData[0])
						|| !Settings.httpPassword.equals(loginData[1])) {

					//Not authenticated
					DefaultFullHttpResponse httpResponse =
							new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);

					httpResponse.headers().add("WWW-Authenticate", "Basic realm=\"" + Settings.realm + "\"");

					sendHttpResponse(ctx, httpResponse);
					return;

				}

			} else {

				//Not authenticated
				DefaultFullHttpResponse httpResponse =
						new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.UNAUTHORIZED);

				httpResponse.headers().add("WWW-Authenticate", "Basic realm=\"" + Settings.realm + "\"");

				sendHttpResponse(ctx, httpResponse);
				return;

			}

		}

		//ProxyServer.getInstance().getLogger().info(req.getUri() + " | " + req.headers().entries().toString());

		if ("websocket".equals(req.headers().get("Upgrade"))) {

			if (!this.connectedClients.contains(ctx)) {

				// Handshake
				WebSocketServerHandshakerFactory wsFactory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, true);
				handshaker = wsFactory.newHandshaker(req);
				if (handshaker == null) {
					WebSocketServerHandshakerFactory.sendUnsupportedVersionResponse(ctx.channel());
				} else {
					handshaker.handshake(ctx.channel(), req);
				}

				this.connectedClients.add(ctx);

			} else {

				this.connectedClients.remove(ctx);
				ctx.close();

			}

		} else {

			switch (req.getUri()) {

				case "/":

					ByteBuf content = WebSocketServerIndexPage.getContent();
					FullHttpResponse response = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.OK, content);

					response.headers().set(HttpHeaders.Names.CONTENT_TYPE, "text/html; charset=UTF-8");
					HttpHeaders.setContentLength(response, content.readableBytes());

					sendHttpResponse(ctx, response);
					break;

				case "/favicon.ico":
				default:
					FullHttpResponse notFoundResponse = new DefaultFullHttpResponse(HttpVersion.HTTP_1_1, HttpResponseStatus.NOT_FOUND);
					sendHttpResponse(ctx, notFoundResponse);
					break;

			}

		}

	}

	private void handleWebSocketFrame(ChannelHandlerContext ctx, WebSocketFrame frame) {

		// Check for closing frame
		if (frame instanceof CloseWebSocketFrame) {
			handshaker.close(ctx.channel(), (CloseWebSocketFrame) frame.retain());
			//return;
		} else if (frame instanceof PingWebSocketFrame) {
			ctx.channel().write(new PongWebSocketFrame(frame.content().retain()));
			//return;
		}
		/*if (!(frame instanceof TextWebSocketFrame)) {
			throw new UnsupportedOperationException(String.format("%s frame types not supported", frame.getClass()
					.getName()));
		}

		// Send the uppercase string back.
		String request = ((TextWebSocketFrame) frame).text();

		ctx.channel().write(new TextWebSocketFrame(request.toUpperCase()));*/
	}

	private static void sendHttpResponse(
			ChannelHandlerContext ctx, FullHttpResponse res) {
		// Generate an error page if response getStatus code is not OK (200).
		if (res.getStatus().code() != 200) {
			ByteBuf buf = Unpooled.copiedBuffer(res.getStatus().toString(), CharsetUtil.UTF_8);
			res.content().writeBytes(buf);
			buf.release();
			HttpHeaders.setContentLength(res, res.content().readableBytes());
		}

		res.headers().add("Connection", "close");
		res.headers().add("x-powered-by", "RemoteChat (spigot and bungeecord plugin)");

		// Send the response and close the connection.
		ctx.channel().writeAndFlush(res).addListener(ChannelFutureListener.CLOSE);

		//ctx.disconnect();
		//ctx.close();
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
		this.connectedClients.remove(ctx);
		cause.printStackTrace();
		ctx.disconnect();
		ctx.close();
	}

	private static String getWebSocketLocation(FullHttpRequest req) {
		String location = req.headers().get("HOST") + WEBSOCKET_PATH;
		return "ws://" + location;
	}

}
