package io.hyperfoil.core.client.netty;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoop;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.SupportedCipherSuiteFilter;
import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.EventExecutorGroup;
import io.hyperfoil.api.config.Http;
import io.hyperfoil.api.connection.HttpClientPool;
import io.hyperfoil.api.connection.HttpConnection;
import io.hyperfoil.api.connection.HttpConnectionPool;
import io.netty.channel.EventLoopGroup;
import io.netty.handler.codec.http2.Http2SecurityUtil;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.hyperfoil.api.http.HttpVersion;
import io.vertx.core.AsyncResult;
import io.vertx.core.Handler;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiConsumer;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import javax.net.ssl.SSLException;

/**
 * @author <a href="mailto:julien@julienviet.com">Julien Viet</a>
 */
public class HttpClientPoolImpl implements HttpClientPool {
   private static final Logger log = LoggerFactory.getLogger(HttpClientPoolImpl.class);

   final Http http;
   final int port;
   final String host;
   final String scheme;
   final String authority;
   final SslContext sslContext;
   final boolean forceH2c;
   private final EventLoopGroup eventLoopGroup;
   private final HttpConnectionPoolImpl[] children;
   private final AtomicInteger idx = new AtomicInteger();
   private final Supplier<HttpConnectionPool> nextSupplier;

   public HttpClientPoolImpl(int threads, Http http) throws SSLException {
      this(new NioEventLoopGroup(threads), http);
   }

   public HttpClientPoolImpl(EventLoopGroup eventLoopGroup, Http http) throws SSLException {
        this.eventLoopGroup = eventLoopGroup;
        this.http = http;
        this.sslContext = http.protocol().secure() ? createSslContext(http.versions()) : null;
        this.host = http.host();
        this.port = http.port();
        this.scheme = sslContext == null ? "http" : "https";
        this.authority = host + ":" + port;
        this.forceH2c = http.versions().length == 1 && http.versions()[0] == HttpVersion.HTTP_2_0;

        int numExecutors = (int) StreamSupport.stream(eventLoopGroup.spliterator(), false).count();
        this.children = new HttpConnectionPoolImpl[numExecutors];
        int sharedConnections = http.sharedConnections();
        if (sharedConnections < numExecutors) {
            log.warn("Connection pool size ({}) too small: the event loop has {} executors. Setting connection pool size to {}",
                  http.sharedConnections(), numExecutors, numExecutors);
           sharedConnections = numExecutors;
        }
        Iterator<EventExecutor> iterator = eventLoopGroup.iterator();
        for (int i = 0; i < numExecutors; ++i) {
            assert iterator.hasNext();
            int childSize = (i + 1) * sharedConnections / numExecutors - i * sharedConnections / numExecutors;
            children[i] = new HttpConnectionPoolImpl(this, (EventLoop) iterator.next(), childSize);
        }

        if (Integer.bitCount(children.length) == 1) {
           int shift = 32 - Integer.numberOfLeadingZeros(children.length - 1);
           int mask = (1 << shift) - 1;
           nextSupplier = () -> children[idx.getAndIncrement() & mask];
        } else {
           nextSupplier = () -> children[idx.getAndIncrement() % children.length];
        }
    }

   private SslContext createSslContext(HttpVersion[] versions) throws SSLException {
      SslProvider provider = OpenSsl.isAlpnSupported() ? SslProvider.OPENSSL : SslProvider.JDK;
      SslContextBuilder builder = SslContextBuilder.forClient()
            .sslProvider(provider)
            /* NOTE: the cipher filter may not include all ciphers required by the HTTP/2 specification.
             * Please refer to the HTTP/2 specification for cipher requirements. */
            .ciphers(Http2SecurityUtil.CIPHERS, SupportedCipherSuiteFilter.INSTANCE)
            .trustManager(InsecureTrustManagerFactory.INSTANCE);
      builder.applicationProtocolConfig(new ApplicationProtocolConfig(
            ApplicationProtocolConfig.Protocol.ALPN,
            // NO_ADVERTISE is currently the only mode supported by both OpenSsl and JDK providers.
            ApplicationProtocolConfig.SelectorFailureBehavior.NO_ADVERTISE,
            // ACCEPT is currently the only mode supported by both OpenSsl and JDK providers.
            ApplicationProtocolConfig.SelectedListenerFailureBehavior.ACCEPT,
            Stream.of(versions).map(HttpVersion::protocolName).toArray(String[]::new)
      ));
      return builder.build();
   }

   @Override
   public Http config() {
      return http;
   }

   @Override
    public void start(Handler<AsyncResult<Void>> completionHandler) {
       AtomicInteger countDown = new AtomicInteger(children.length);
       for (HttpConnectionPoolImpl child : children) {
          child.start(result -> {
             if (result.failed() || countDown.decrementAndGet() == 0) {
                if (result.failed()) {
                   shutdown();
                }
                completionHandler.handle(result);
             }
          });
       }
    }

    @Override
    public void shutdown() {
       for (HttpConnectionPoolImpl child : children) {
          child.shutdown();
       }
       eventLoopGroup.shutdownGracefully(0, 10, TimeUnit.SECONDS);
    }

    void connect(final HttpConnectionPool pool, BiConsumer<HttpConnection, Throwable> handler) {
       Bootstrap bootstrap = new Bootstrap();
       bootstrap.channel(NioSocketChannel.class);
       bootstrap.group(eventLoopGroup);
       bootstrap.option(ChannelOption.SO_KEEPALIVE, true);
       bootstrap.option(ChannelOption.SO_REUSEADDR, true);

       bootstrap.handler(new HttpChannelInitializer(this, pool, handler));

       String address = this.host;
       if (http.addresses().length != 0) {
          address = http.addresses()[ThreadLocalRandom.current().nextInt(http.addresses().length)];
       }

       ChannelFuture fut = bootstrap.connect(new InetSocketAddress(address, port));
       fut.addListener(v -> {
          if (!v.isSuccess()) {
             handler.accept(null, v.cause());
          }
       });
    }

    @Override
    public EventExecutorGroup executors() {
        return eventLoopGroup;
    }

    @Override
    public HttpConnectionPool next() {
       return nextSupplier.get();
    }

   @Override
   public HttpConnectionPool connectionPool(EventExecutor executor) {
      for (HttpConnectionPoolImpl pool : children) {
         if (pool.executor() == executor) {
            return pool;
         }
      }
      throw new IllegalStateException();
   }

   @Override
   public String host() {
      return host;
   }

   @Override
   public String authority() {
      return authority;
   }

   @Override
   public String scheme() {
      return scheme;
   }

   @Override
   public boolean isSecure() {
      return sslContext != null;
   }
}
