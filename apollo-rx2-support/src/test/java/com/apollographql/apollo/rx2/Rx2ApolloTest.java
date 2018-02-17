package com.apollographql.apollo.rx2;

import com.apollographql.apollo.ApolloClient;
import com.apollographql.apollo.api.OperationName;
import com.apollographql.apollo.api.Query;
import com.apollographql.apollo.api.ResponseFieldMapper;
import com.apollographql.apollo.api.ResponseReader;
import com.apollographql.apollo.exception.ApolloException;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import javax.annotation.Nonnull;

import okhttp3.Dispatcher;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;

public class Rx2ApolloTest {
  private ApolloClient apolloClient;
  private MockWebServer server;
  private OkHttpClient okHttpClient = new OkHttpClient.Builder()
      .dispatcher(new Dispatcher(currentThreadExecutorService()))
      .build();

  @Before
  public void setup() {
    server = new MockWebServer();
    apolloClient = ApolloClient.builder()
        .okHttpClient(okHttpClient)
        .dispatcher(currentThreadExecutorService())
        .serverUrl(server.url("/"))
        .build();
  }

  @Test
  public void responseAfterSubscriptionToObservableFromQuery() throws IOException, ApolloException, InterruptedException {
    server.enqueue(mockResponse());

    Rx2Apollo.from(apolloClient.query(EMPTY_QUERY))
        .test()
        .assertValueCount(1);
  }

  @After
  public void tearDown() {
    try {
      server.shutdown();
    } catch (IOException ignored) {
    }
  }

  private static String readFile(String fileName) throws IOException {
    String line;
    String filePath = ClassLoader.getSystemClassLoader().getResource(fileName).getFile();
    StringBuilder result = new StringBuilder();
    try (BufferedReader bufferedReader = new BufferedReader(new FileReader(filePath))) {
      while ((line = bufferedReader.readLine()) != null) {
        result.append(line);
      }
    }
    return result.toString();
  }

  private static MockResponse mockResponse() throws IOException {
    return new MockResponse().setResponseCode(200).setBody(readFile("mockResponse.json"));
  }

  private static final Query EMPTY_QUERY = new Query() {

    OperationName operationName = new OperationName() {
      @Override public String name() {
        return "EmptyQuery";
      }
    };

    @Override public String queryDocument() {
      return "";
    }

    @Override public Variables variables() {
      return EMPTY_VARIABLES;
    }

    @Override public ResponseFieldMapper<Data> responseFieldMapper() {
      return new ResponseFieldMapper<Data>() {
        @Override public Data map(ResponseReader responseReader) {
          return null;
        }
      };
    }

    @Override public Object wrapData(Data data) {
      return data;
    }

    @Nonnull @Override public OperationName name() {
      return operationName;
    }

    @Nonnull @Override public String operationId() {
      return "";
    }
  };

  private static ExecutorService currentThreadExecutorService() {
    final ThreadPoolExecutor.CallerRunsPolicy callerRunsPolicy = new ThreadPoolExecutor.CallerRunsPolicy();
    return new ThreadPoolExecutor(0, 1, 0L, TimeUnit.SECONDS, new SynchronousQueue<Runnable>(), callerRunsPolicy) {
      @Override
      public void execute(Runnable command) {
        callerRunsPolicy.rejectedExecution(command, this);
      }
    };
  }
}
